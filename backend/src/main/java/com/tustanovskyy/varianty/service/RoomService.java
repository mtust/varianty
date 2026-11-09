package com.tustanovskyy.varianty.service;

import com.tustanovskyy.varianty.domain.dto.room.AnswerOptionDto;
import com.tustanovskyy.varianty.domain.dto.room.HostQuestionDto;
import com.tustanovskyy.varianty.domain.dto.room.PlayerScoreDto;
import com.tustanovskyy.varianty.domain.dto.room.RevealedOptionDto;
import com.tustanovskyy.varianty.domain.dto.room.RoomSnapshot;
import com.tustanovskyy.varianty.domain.dto.room.RoundResultDto;
import com.tustanovskyy.varianty.domain.dto.room.VotingStartedDto;
import com.tustanovskyy.varianty.domain.entity.GameHistoryEntry;
import com.tustanovskyy.varianty.domain.entity.Question;
import com.tustanovskyy.varianty.domain.game.AnswerOption;
import com.tustanovskyy.varianty.domain.game.GameRoom;
import com.tustanovskyy.varianty.domain.game.PlayerState;
import com.tustanovskyy.varianty.domain.game.RoomStatus;
import com.tustanovskyy.varianty.domain.game.RoundState;
import com.tustanovskyy.varianty.exception.ApiException;
import com.tustanovskyy.varianty.repository.GameHistoryRepository;
import com.tustanovskyy.varianty.repository.QuestionRepository;
import com.tustanovskyy.varianty.security.PlayerPrincipal;
import jakarta.annotation.PreDestroy;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import org.springframework.http.HttpStatus;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

@Service
public class RoomService {

	private static final String CODE_ALPHABET = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
	private static final SecureRandom RANDOM = new SecureRandom();
	private static final int ANSWER_TIMEOUT_SECONDS = 45;
	private static final int VOTE_TIMEOUT_SECONDS = 30;
	public static final int MIN_ROUNDS = 5;
	public static final int MAX_ROUNDS = 20;
	public static final String KICKED_MESSAGE = "Вас видалено з цієї кімнати";

	private final Map<String, GameRoom> rooms = new ConcurrentHashMap<>();
	private final Map<String, String[]> sessionToRoomAndPlayer = new ConcurrentHashMap<>();
	private final QuestionRepository questionRepository;
	private final GameHistoryRepository gameHistoryRepository;
	private final SimpMessagingTemplate messagingTemplate;
	private final int roundsPerGame;
	private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();

	public RoomService(
			QuestionRepository questionRepository,
			GameHistoryRepository gameHistoryRepository,
			SimpMessagingTemplate messagingTemplate,
			@org.springframework.beans.factory.annotation.Value("${varianty.game.rounds-per-game}") int roundsPerGame) {
		this.questionRepository = questionRepository;
		this.gameHistoryRepository = gameHistoryRepository;
		this.messagingTemplate = messagingTemplate;
		this.roundsPerGame = roundsPerGame;
	}

	@PreDestroy
	void shutdown() {
		scheduler.shutdownNow();
	}

	public String createRoom(PlayerPrincipal host, boolean hostless, String category, String language, Integer rounds) {
		requireKnownCategory(category);
		if (!Question.LANGUAGE_UK.equals(language) && !Question.LANGUAGE_EN.equals(language)) {
			throw new ApiException(HttpStatus.BAD_REQUEST, "Невідома мова");
		}
		int requestedRounds = rounds != null ? rounds : roundsPerGame;
		requireRoundsInRange(requestedRounds);
		// Never plan more rounds than there are distinct questions for this category/language.
		int available = questionRepository.findByCategoryAndLanguage(category, language).size();
		int totalRounds = Math.max(1, Math.min(requestedRounds, available));
		String code = generateUniqueCode();
		GameRoom room = new GameRoom(code, host.playerId(), host.displayName(), !host.guest(), totalRounds, hostless, category, language);
		if (hostless) {
			room.players.put(host.playerId(), new PlayerState(host.playerId(), host.displayName(), !host.guest()));
		}
		rooms.put(code, room);
		return code;
	}

	public boolean exists(String code) {
		return rooms.containsKey(code);
	}

	public RoomSnapshot join(String code, PlayerPrincipal player) {
		GameRoom room = getRoomOrThrow(code);
		synchronized (room) {
			if (room.kickedPlayerIds.contains(player.playerId())) {
				throw new ApiException(HttpStatus.FORBIDDEN, KICKED_MESSAGE);
			}
			if (!room.hostless && room.hostId.equals(player.playerId())) {
				return snapshot(room);
			}
			// Newcomers may also join between games, so they can play the next one in this room.
			boolean joinable = room.status == RoomStatus.LOBBY || room.status == RoomStatus.FINISHED;
			if (!joinable && !room.players.containsKey(player.playerId())) {
				throw new ApiException(HttpStatus.CONFLICT, "Гра вже почалась");
			}
			PlayerState existing = room.players.get(player.playerId());
			if (existing != null) {
				existing.connected = true;
			} else {
				room.players.put(player.playerId(), new PlayerState(player.playerId(), player.displayName(), !player.guest()));
			}
			return snapshot(room);
		}
	}

	public void trackSession(String sessionId, String code, String playerId) {
		sessionToRoomAndPlayer.put(sessionId, new String[] {code, playerId});
	}

	/**
	 * Returns the room code the disconnected session belonged to, or null if nothing changed.
	 * A player who came back from the background usually has a new session already (the old
	 * socket's close is often noticed only later), so the player only goes offline when no
	 * other session of theirs is still joined to the room.
	 */
	public String handleSessionDisconnect(String sessionId) {
		String[] ref = sessionToRoomAndPlayer.remove(sessionId);
		if (ref == null) {
			return null;
		}
		boolean stillConnected = sessionToRoomAndPlayer.values().stream()
				.anyMatch(other -> other[0].equals(ref[0]) && other[1].equals(ref[1]));
		if (stillConnected) {
			return null;
		}
		markDisconnected(ref[0], ref[1]);
		return ref[0];
	}

	/** Private state a (re)joining player may have missed while their socket was down. */
	public record CatchUp(HostQuestionDto hostQuestion, VotingStartedDto votingStarted, RoundResultDto roundResult) {
	}

	public CatchUp catchUp(String code, String playerId) {
		GameRoom room = getRoomOrThrow(code);
		synchronized (room) {
			RoundState round = room.currentRound;
			if (round == null) {
				return new CatchUp(null, null, null);
			}
			HostQuestionDto hostQuestion = !room.hostless && room.hostId.equals(playerId)
					&& room.status != RoomStatus.LOBBY && room.status != RoomStatus.FINISHED
					? new HostQuestionDto(round.question.getFact(), capitalize(round.question.getCorrectAnswer()))
					: null;
			VotingStartedDto voting = room.status == RoomStatus.VOTING
					? new VotingStartedDto(round.options.stream().map(o -> new AnswerOptionDto(o.id, o.text)).toList())
					: null;
			RoundResultDto result = room.status == RoomStatus.RESULTS ? room.lastRoundResult : null;
			return new CatchUp(hostQuestion, voting, result);
		}
	}

	public void markDisconnected(String code, String playerId) {
		GameRoom room = rooms.get(code);
		if (room == null) {
			return;
		}
		synchronized (room) {
			PlayerState player = room.players.get(playerId);
			if (player != null) {
				player.connected = false;
			}
		}
	}

	public RoomSnapshot startGame(String code, String hostId) {
		GameRoom room = getRoomOrThrow(code);
		synchronized (room) {
			requireHost(room, hostId);
			if (room.status != RoomStatus.LOBBY) {
				throw new ApiException(HttpStatus.CONFLICT, "Гру вже розпочато");
			}
			if (room.players.size() < 2) {
				throw new ApiException(HttpStatus.BAD_REQUEST, "Потрібно щонайменше 2 гравці");
			}
			beginRound(room);
			return snapshot(room);
		}
	}

	public HostQuestionDto currentHostQuestion(String code, String hostId) {
		GameRoom room = getRoomOrThrow(code);
		synchronized (room) {
			requireHost(room, hostId);
			if (room.currentRound == null) {
				throw new ApiException(HttpStatus.CONFLICT, "Раунд ще не розпочався");
			}
			return new HostQuestionDto(room.currentRound.question.getFact(), capitalize(room.currentRound.question.getCorrectAnswer()));
		}
	}

	public RoomSnapshot submitHostTrap(String code, String hostId, String text) {
		GameRoom room = getRoomOrThrow(code);
		synchronized (room) {
			requireHost(room, hostId);
			if (room.status != RoomStatus.TRAP) {
				throw new ApiException(HttpStatus.CONFLICT, "Зараз не час для відповіді ведучого");
			}
			String trimmed = requireNonBlank(text);
			room.currentRound.hostTrapAnswer = trimmed;
			room.status = RoomStatus.ANSWERING;
			schedulePhaseTimeout(room, ANSWER_TIMEOUT_SECONDS, RoomStatus.ANSWERING, room.roundNumber, () -> forceEndAnswering(room));
			return snapshot(room);
		}
	}

	public record AnswerResult(RoomSnapshot snapshot, VotingStartedDto votingStarted) {
	}

	public AnswerResult submitAnswer(String code, String playerId, String text) {
		GameRoom room = getRoomOrThrow(code);
		synchronized (room) {
			PlayerState player = requirePlayer(room, playerId);
			if (room.status != RoomStatus.ANSWERING) {
				throw new ApiException(HttpStatus.CONFLICT, "Зараз не час подавати відповідь");
			}
			if (room.currentRound.playerAnswers.containsKey(playerId)) {
				throw new ApiException(HttpStatus.CONFLICT, "Ви вже відповіли");
			}
			room.currentRound.playerAnswers.put(playerId, requireNonBlank(text));

			if (everyoneAnswered(room)) {
				VotingStartedDto votingStarted = beginVoting(room);
				return new AnswerResult(snapshot(room), votingStarted);
			}
			return new AnswerResult(snapshot(room), null);
		}
	}

	public record VoteResult(RoomSnapshot snapshot, RoundResultDto roundResult) {
	}

	public VoteResult submitVote(String code, String voterId, String optionId) {
		GameRoom room = getRoomOrThrow(code);
		synchronized (room) {
			requirePlayer(room, voterId);
			if (room.status != RoomStatus.VOTING) {
				throw new ApiException(HttpStatus.CONFLICT, "Зараз не час голосувати");
			}
			if (room.currentRound.votes.containsKey(voterId)) {
				throw new ApiException(HttpStatus.CONFLICT, "Ви вже проголосували");
			}
			AnswerOption option = room.currentRound.options.stream()
					.filter(o -> o.id.equals(optionId))
					.findFirst()
					.orElseThrow(() -> new ApiException(HttpStatus.BAD_REQUEST, "Невідомий варіант відповіді"));
			if (voterId.equals(option.ownerId)) {
				throw new ApiException(HttpStatus.BAD_REQUEST, "Не можна голосувати за власну відповідь");
			}
			room.currentRound.votes.put(voterId, optionId);

			if (everyoneVoted(room)) {
				RoundResultDto result = resolveRound(room);
				return new VoteResult(snapshot(room), result);
			}
			return new VoteResult(snapshot(room), null);
		}
	}

	public record KickResult(RoomSnapshot snapshot, VotingStartedDto votingStarted, RoundResultDto roundResult) {
	}

	/**
	 * Removes a player from the room for good. Works in every phase: the removed player's
	 * answer and vote are dropped, and if everyone left has now answered/voted the round
	 * moves on straight away instead of waiting for the timer.
	 */
	public KickResult kickPlayer(String code, String hostId, String playerId) {
		GameRoom room = getRoomOrThrow(code);
		synchronized (room) {
			requireHost(room, hostId);
			if (hostId.equals(playerId)) {
				throw new ApiException(HttpStatus.BAD_REQUEST, "Не можна видалити себе з кімнати");
			}
			requirePlayer(room, playerId);
			room.players.remove(playerId);
			room.kickedPlayerIds.add(playerId);
			sessionToRoomAndPlayer.values().removeIf(ref -> ref[0].equals(code) && ref[1].equals(playerId));

			VotingStartedDto votingStarted = null;
			RoundResultDto roundResult = null;
			RoundState round = room.currentRound;
			if (round != null) {
				// The answer stays as a voting option once voting has started (other clients
				// already show it); before that it is simply dropped.
				if (room.status == RoomStatus.ANSWERING) {
					round.playerAnswers.remove(playerId);
					if (everyoneAnswered(room)) {
						votingStarted = beginVoting(room);
					}
				} else if (room.status == RoomStatus.VOTING) {
					round.votes.remove(playerId);
					if (everyoneVoted(room)) {
						roundResult = resolveRound(room);
					}
				}
			}
			return new KickResult(snapshot(room), votingStarted, roundResult);
		}
	}

	public record NextRoundResult(RoomSnapshot snapshot, boolean finished) {
	}

	public NextRoundResult nextRound(String code, String hostId) {
		GameRoom room = getRoomOrThrow(code);
		synchronized (room) {
			requireHost(room, hostId);
			if (room.status != RoomStatus.RESULTS) {
				throw new ApiException(HttpStatus.CONFLICT, "Зараз не можна перейти до наступного раунду");
			}
			if (room.roundNumber >= room.totalRounds) {
				finishGame(room);
				return new NextRoundResult(snapshot(room), true);
			}
			beginRound(room);
			return new NextRoundResult(snapshot(room), false);
		}
	}

	/**
	 * Starts another game in the same room once the previous one has finished: same players,
	 * scores carried over or reset to zero, and optionally another category / round count.
	 * Questions already asked in this room are not repeated while there are enough fresh ones.
	 */
	public RoomSnapshot startNewGame(String code, String hostId, boolean keepScores, String category, Integer rounds) {
		GameRoom room = getRoomOrThrow(code);
		synchronized (room) {
			requireHost(room, hostId);
			if (room.status != RoomStatus.FINISHED) {
				throw new ApiException(HttpStatus.CONFLICT, "Гра ще не завершилась");
			}
			String newCategory = category != null ? category : room.category;
			requireKnownCategory(newCategory);
			int requestedRounds = rounds != null ? rounds : room.totalRounds;
			requireRoundsInRange(requestedRounds);
			if (room.players.size() < 2) {
				throw new ApiException(HttpStatus.BAD_REQUEST, "Потрібно щонайменше 2 гравці");
			}

			List<Question> all = questionRepository.findByCategoryAndLanguage(newCategory, room.language);
			if (all.isEmpty()) {
				throw new ApiException(HttpStatus.CONFLICT, "Питання закінчились, недостатньо фактів у базі");
			}
			long fresh = all.stream().filter(q -> !room.usedQuestionIds.contains(q.getId())).count();
			if (fresh < requestedRounds) {
				// Not enough unseen questions for a full game: allow repeats rather than a short game.
				room.usedQuestionIds.clear();
			}

			cancelPhaseTimeout(room);
			room.category = newCategory;
			room.totalRounds = Math.max(1, Math.min(requestedRounds, all.size()));
			room.roundNumber = 0;
			room.currentRound = null;
			room.lastRoundResult = null;
			if (!keepScores) {
				room.players.values().forEach(p -> p.score = 0);
			}
			beginRound(room);
			return snapshot(room);
		}
	}

	public RoomSnapshot publicSnapshot(String code) {
		GameRoom room = getRoomOrThrow(code);
		synchronized (room) {
			return snapshot(room);
		}
	}

	// --- internal state transitions (caller must hold the room monitor) ---

	private boolean everyoneAnswered(GameRoom room) {
		long answered = room.currentRound.playerAnswers.keySet().stream().filter(room.players::containsKey).count();
		return answered >= connectedPlayers(room);
	}

	private boolean everyoneVoted(GameRoom room) {
		long voted = room.currentRound.votes.keySet().stream().filter(room.players::containsKey).count();
		return voted >= connectedPlayers(room);
	}

	private long connectedPlayers(GameRoom room) {
		return room.players.values().stream().filter(p -> p.connected).count();
	}

	private void beginRound(GameRoom room) {
		List<Question> pool = questionRepository.findByCategoryAndLanguage(room.category, room.language).stream()
				.filter(q -> !room.usedQuestionIds.contains(q.getId()))
				.toList();
		if (pool.isEmpty()) {
			throw new ApiException(HttpStatus.CONFLICT, "Питання закінчились, недостатньо фактів у базі");
		}
		Question question = pool.get(RANDOM.nextInt(pool.size()));
		room.usedQuestionIds.add(question.getId());
		room.currentRound = new RoundState(question);
		room.roundNumber++;
		room.status = room.hostless ? RoomStatus.ANSWERING : RoomStatus.TRAP;
		if (room.hostless) {
			schedulePhaseTimeout(room, ANSWER_TIMEOUT_SECONDS, RoomStatus.ANSWERING, room.roundNumber, () -> forceEndAnswering(room));
		}
	}

	/**
	 * Schedules a forced phase transition after the given delay, replacing any timer already
	 * pending for this room. The scheduled action only runs if the room is still in the exact
	 * phase/round it was scheduled for — if players finished early (or the room moved on for any
	 * other reason) by the time it fires, it's a no-op.
	 */
	private void schedulePhaseTimeout(GameRoom room, int seconds, RoomStatus expectedStatus, int expectedRound, Runnable onTimeout) {
		cancelPhaseTimeout(room);
		room.phaseDeadline = Instant.now().plusSeconds(seconds);
		room.phaseTimeoutTask = scheduler.schedule(() -> {
			synchronized (room) {
				if (room.status == expectedStatus && room.roundNumber == expectedRound) {
					onTimeout.run();
				}
			}
		}, seconds, TimeUnit.SECONDS);
	}

	private void cancelPhaseTimeout(GameRoom room) {
		if (room.phaseTimeoutTask != null) {
			room.phaseTimeoutTask.cancel(false);
			room.phaseTimeoutTask = null;
		}
		room.phaseDeadline = null;
	}

	private void forceEndAnswering(GameRoom room) {
		VotingStartedDto votingStarted = beginVoting(room);
		messagingTemplate.convertAndSend("/topic/room/" + room.code, snapshot(room));
		messagingTemplate.convertAndSend("/topic/room/" + room.code + "/voting", votingStarted);
	}

	private void forceEndVoting(GameRoom room) {
		RoundResultDto result = resolveRound(room);
		messagingTemplate.convertAndSend("/topic/room/" + room.code, snapshot(room));
		messagingTemplate.convertAndSend("/topic/room/" + room.code + "/results", result);
	}

	private VotingStartedDto beginVoting(GameRoom room) {
		RoundState round = room.currentRound;
		List<AnswerOption> options = new ArrayList<>();
		options.add(new AnswerOption(UUID.randomUUID().toString(), AnswerOption.CORRECT_OWNER, capitalize(round.question.getCorrectAnswer())));
		if (!room.hostless) {
			options.add(new AnswerOption(UUID.randomUUID().toString(), AnswerOption.HOST_OWNER, round.hostTrapAnswer));
		}
		round.playerAnswers.forEach((playerId, text) -> options.add(new AnswerOption(UUID.randomUUID().toString(), playerId, text)));
		java.util.Collections.shuffle(options, RANDOM);
		round.options = options;
		room.status = RoomStatus.VOTING;
		schedulePhaseTimeout(room, VOTE_TIMEOUT_SECONDS, RoomStatus.VOTING, room.roundNumber, () -> forceEndVoting(room));
		return new VotingStartedDto(options.stream().map(o -> new AnswerOptionDto(o.id, o.text)).toList());
	}

	private RoundResultDto resolveRound(GameRoom room) {
		cancelPhaseTimeout(room);
		RoundState round = room.currentRound;
		Map<String, Integer> deltas = new LinkedHashMap<>();
		room.players.keySet().forEach(id -> deltas.put(id, 0));

		Map<String, List<String>> votersByOption = new LinkedHashMap<>();
		round.options.forEach(o -> votersByOption.put(o.id, new ArrayList<>()));
		round.votes.forEach((voterId, optionId) -> votersByOption.get(optionId).add(voterId));

		for (Map.Entry<String, String> vote : round.votes.entrySet()) {
			String voterId = vote.getKey();
			String optionId = vote.getValue();
			AnswerOption option = round.options.stream().filter(o -> o.id.equals(optionId)).findFirst().orElseThrow();
			if (AnswerOption.CORRECT_OWNER.equals(option.ownerId)) {
				deltas.merge(voterId, 2, Integer::sum);
			} else if (AnswerOption.HOST_OWNER.equals(option.ownerId)) {
				deltas.merge(voterId, -1, Integer::sum);
			} else if (room.players.containsKey(option.ownerId)) {
				// A removed player's lie can still fool people, but they get no points for it.
				deltas.merge(option.ownerId, 1, Integer::sum);
			}
		}
		deltas.forEach((playerId, delta) -> {
			PlayerState player = room.players.get(playerId);
			if (player != null) {
				player.score += delta;
			}
		});

		List<RevealedOptionDto> revealed = round.options.stream()
				.map(o -> new RevealedOptionDto(
						o.id,
						o.text,
						authorLabel(room, o.ownerId),
						AnswerOption.CORRECT_OWNER.equals(o.ownerId),
						AnswerOption.HOST_OWNER.equals(o.ownerId),
						votersByOption.get(o.id).stream()
								.map(room.players::get)
								.filter(java.util.Objects::nonNull)
								.map(p -> p.displayName)
								.toList()))
				.toList();

		room.status = RoomStatus.RESULTS;
		boolean gameFinished = room.roundNumber >= room.totalRounds;
		RoundResultDto resultDto = new RoundResultDto(round.question.getFact(), revealed, deltas, scoreboard(room), gameFinished);
		room.lastRoundResult = resultDto;
		return resultDto;
	}

	private String authorLabel(GameRoom room, String ownerId) {
		if (AnswerOption.CORRECT_OWNER.equals(ownerId)) {
			return "Правильна відповідь";
		}
		if (AnswerOption.HOST_OWNER.equals(ownerId)) {
			return room.hostDisplayName + " (ведучий)";
		}
		PlayerState player = room.players.get(ownerId);
		return "Автор: " + (player != null ? player.displayName : "Гравець");
	}

	private void finishGame(GameRoom room) {
		room.status = RoomStatus.FINISHED;
		List<PlayerScoreDto> ranked = scoreboard(room);
		for (int i = 0; i < ranked.size(); i++) {
			PlayerScoreDto entry = ranked.get(i);
			PlayerState player = room.players.get(entry.id());
			if (player != null && player.registered) {
				gameHistoryRepository.save(GameHistoryEntry.builder()
						.userId(player.id)
						.roomCode(room.code)
						.displayName(player.displayName)
						.score(player.score)
						.placement(i + 1)
						.playerCount(room.players.size())
						.playedAt(Instant.now())
						.build());
			}
		}
	}

	private RoomSnapshot snapshot(GameRoom room) {
		String fact = room.currentRound != null ? room.currentRound.question.getFact() : null;
		int answered = room.currentRound != null ? room.currentRound.playerAnswers.size() : 0;
		int voted = room.currentRound != null ? room.currentRound.votes.size() : 0;
		return new RoomSnapshot(
				room.code,
				room.status.name(),
				room.roundNumber,
				room.totalRounds,
				room.hostDisplayName,
				room.hostless,
				fact,
				scoreboard(room),
				answered,
				voted,
				secondsRemaining(room),
				room.category);
	}

	private int secondsRemaining(GameRoom room) {
		if (room.phaseDeadline == null) {
			return 0;
		}
		return (int) Math.max(0, Duration.between(Instant.now(), room.phaseDeadline).toSeconds());
	}

	private List<PlayerScoreDto> scoreboard(GameRoom room) {
		return room.players.values().stream()
				.sorted(Comparator.comparingInt((PlayerState p) -> p.score).reversed())
				.map(p -> new PlayerScoreDto(p.id, p.displayName, p.score, p.connected))
				.toList();
	}

	private static void requireKnownCategory(String category) {
		if (!Question.CATEGORY_FACTS.equals(category) && !Question.CATEGORY_QUOTES.equals(category)) {
			throw new ApiException(HttpStatus.BAD_REQUEST, "Невідома категорія питань");
		}
	}

	private static void requireRoundsInRange(int rounds) {
		if (rounds < MIN_ROUNDS || rounds > MAX_ROUNDS) {
			throw new ApiException(HttpStatus.BAD_REQUEST,
					"Кількість раундів має бути від " + MIN_ROUNDS + " до " + MAX_ROUNDS);
		}
	}

	private void requireHost(GameRoom room, String playerId) {
		if (!room.hostId.equals(playerId)) {
			throw new ApiException(HttpStatus.FORBIDDEN, "Тільки ведучий може виконати цю дію");
		}
	}

	private PlayerState requirePlayer(GameRoom room, String playerId) {
		PlayerState player = room.players.get(playerId);
		if (player == null) {
			throw new ApiException(HttpStatus.FORBIDDEN, "Ви не є гравцем цієї кімнати");
		}
		return player;
	}

	private String requireNonBlank(String text) {
		if (text == null || text.isBlank()) {
			throw new ApiException(HttpStatus.BAD_REQUEST, "Відповідь не може бути порожньою");
		}
		String trimmed = text.trim();
		if (trimmed.length() > 120) {
			throw new ApiException(HttpStatus.BAD_REQUEST, "Відповідь занадто довга");
		}
		return capitalize(trimmed);
	}

	/** Displays every answer — typed or pre-seeded — starting with a capital letter, regardless of how it was entered/stored. */
	private static String capitalize(String text) {
		if (text == null || text.isEmpty()) {
			return text;
		}
		return Character.toUpperCase(text.charAt(0)) + text.substring(1);
	}

	GameRoom getRoomOrThrow(String code) {
		GameRoom room = rooms.get(code);
		if (room == null) {
			throw new ApiException(HttpStatus.NOT_FOUND, "Кімнату не знайдено");
		}
		return room;
	}

	private String generateUniqueCode() {
		String code;
		do {
			StringBuilder sb = new StringBuilder(5);
			for (int i = 0; i < 5; i++) {
				sb.append(CODE_ALPHABET.charAt(RANDOM.nextInt(CODE_ALPHABET.length())));
			}
			code = sb.toString();
		} while (rooms.containsKey(code));
		return code;
	}
}
