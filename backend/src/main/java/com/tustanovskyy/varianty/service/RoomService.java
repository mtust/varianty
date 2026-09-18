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

	public String createRoom(PlayerPrincipal host, boolean hostless, String category) {
		if (!QuestionSeeder.CATEGORY_FACTS.equals(category) && !QuestionSeeder.CATEGORY_QUOTES.equals(category)) {
			throw new ApiException(HttpStatus.BAD_REQUEST, "Невідома категорія питань");
		}
		String code = generateUniqueCode();
		GameRoom room = new GameRoom(code, host.playerId(), host.displayName(), !host.guest(), roundsPerGame, hostless, category);
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
			if (!room.hostless && room.hostId.equals(player.playerId())) {
				return snapshot(room);
			}
			if (room.status != RoomStatus.LOBBY && !room.players.containsKey(player.playerId())) {
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

	/** Returns the room code the disconnected session belonged to, or null if unknown. */
	public String handleSessionDisconnect(String sessionId) {
		String[] ref = sessionToRoomAndPlayer.remove(sessionId);
		if (ref == null) {
			return null;
		}
		markDisconnected(ref[0], ref[1]);
		return ref[0];
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

			long connectedPlayers = room.players.values().stream().filter(p -> p.connected).count();
			if (room.currentRound.playerAnswers.size() >= connectedPlayers) {
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

			long connectedPlayers = room.players.values().stream().filter(p -> p.connected).count();
			if (room.currentRound.votes.size() >= connectedPlayers) {
				RoundResultDto result = resolveRound(room);
				return new VoteResult(snapshot(room), result);
			}
			return new VoteResult(snapshot(room), null);
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

	public RoomSnapshot publicSnapshot(String code) {
		GameRoom room = getRoomOrThrow(code);
		synchronized (room) {
			return snapshot(room);
		}
	}

	// --- internal state transitions (caller must hold the room monitor) ---

	private void beginRound(GameRoom room) {
		List<Question> pool = questionRepository.findByCategory(room.category).stream()
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
			} else {
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
						votersByOption.get(o.id).stream().map(voterId -> room.players.get(voterId).displayName).toList()))
				.toList();

		room.status = RoomStatus.RESULTS;
		boolean gameFinished = room.roundNumber >= room.totalRounds;
		return new RoundResultDto(round.question.getFact(), revealed, deltas, scoreboard(room), gameFinished);
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
				secondsRemaining(room));
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
