package com.tustanovskyy.varianty.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.tustanovskyy.varianty.domain.dto.room.RoomSnapshot;
import com.tustanovskyy.varianty.domain.entity.Question;
import com.tustanovskyy.varianty.exception.ApiException;
import com.tustanovskyy.varianty.repository.GameHistoryRepository;
import com.tustanovskyy.varianty.repository.QuestionRepository;
import com.tustanovskyy.varianty.security.PlayerPrincipal;
import java.util.List;
import java.util.stream.IntStream;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.messaging.simp.SimpMessagingTemplate;

class RoomServiceAnswerTimeTest {

	private static final PlayerPrincipal HOST = new PlayerPrincipal("host", "Host", true);
	private static final PlayerPrincipal ANNA = new PlayerPrincipal("anna", "Anna", true);
	private static final PlayerPrincipal BOHDAN = new PlayerPrincipal("bohdan", "Bohdan", true);

	private RoomService service;

	@BeforeEach
	void setUp() {
		QuestionRepository questions = mock(QuestionRepository.class);
		List<Question> builtIn = IntStream.range(0, 30)
				.mapToObj(i -> Question.builder().id("q" + i).fact("Fact " + i + " ___").correctAnswer("a" + i)
						.category(Question.CATEGORY_FACTS).language(Question.LANGUAGE_UK).build())
				.toList();
		when(questions.findByCategoryAndLanguage(anyString(), anyString())).thenReturn(builtIn);
		service = new RoomService(questions, mock(GameHistoryRepository.class), mock(SimpMessagingTemplate.class), 10);
	}

	@AfterEach
	void tearDown() {
		service.shutdown();
	}

	@Test
	void defaultsToFortyFiveSeconds() {
		String code = service.createRoom(HOST, true, Question.CATEGORY_FACTS, Question.LANGUAGE_UK, 5);
		service.join(code, ANNA);
		RoomSnapshot started = service.startGame(code, HOST.playerId());
		assertThat(started.answerSeconds()).isEqualTo(RoomService.DEFAULT_ANSWER_SECONDS);
		assertThat(started.secondsRemaining()).isBetween(44, 45);
	}

	@Test
	void hostPicksAnswerTime() {
		String code = service.createRoom(HOST, true, Question.CATEGORY_FACTS, Question.LANGUAGE_UK, 5, 90, false);
		service.join(code, ANNA);
		RoomSnapshot started = service.startGame(code, HOST.playerId());
		assertThat(started.status()).isEqualTo("ANSWERING");
		assertThat(started.answerSeconds()).isEqualTo(90);
		assertThat(started.secondsRemaining()).isBetween(89, 90);
	}

	@Test
	void rejectsAnswerTimeOutOfRange() {
		assertThatThrownBy(() -> service.createRoom(HOST, false, Question.CATEGORY_FACTS, Question.LANGUAGE_UK, 5, 5, false))
				.isInstanceOf(ApiException.class);
		assertThatThrownBy(() -> service.createRoom(HOST, false, Question.CATEGORY_FACTS, Question.LANGUAGE_UK, 5, 600, false))
				.isInstanceOf(ApiException.class);
	}

	@Test
	void roomWithHostKeepsAnswerTime() {
		String code = service.createRoom(HOST, false, Question.CATEGORY_FACTS, Question.LANGUAGE_UK, 5, 30, false);
		service.join(code, ANNA);
		service.join(code, BOHDAN);
		assertThat(service.startGame(code, HOST.playerId()).answerSeconds()).isEqualTo(30);
	}

	@Test
	void manualTimerHasNoCountdownAndHostEndsPhases() {
		String code = service.createRoom(HOST, false, Question.CATEGORY_FACTS, Question.LANGUAGE_UK, 5, null, true);
		service.join(code, ANNA);
		service.join(code, BOHDAN);
		service.startGame(code, HOST.playerId());
		RoomSnapshot answering = service.submitHostTrap(code, HOST.playerId(), "trap");
		assertThat(answering.status()).isEqualTo("ANSWERING");
		assertThat(answering.manualTimer()).isTrue();
		assertThat(answering.secondsRemaining()).isZero();

		service.submitAnswer(code, ANNA.playerId(), "lie");
		RoomService.AnswerResult voting = service.endAnswering(code, HOST.playerId());
		assertThat(voting.snapshot().status()).isEqualTo("VOTING");
		assertThat(voting.snapshot().secondsRemaining()).isZero();
		assertThat(voting.votingStarted().options()).hasSize(3);

		RoomService.VoteResult results = service.endVoting(code, HOST.playerId());
		assertThat(results.snapshot().status()).isEqualTo("RESULTS");
		assertThat(results.roundResult()).isNotNull();
	}

	@Test
	void onlyTheHostEndsPhasesAndNeverInHostlessRooms() {
		String code = service.createRoom(HOST, false, Question.CATEGORY_FACTS, Question.LANGUAGE_UK, 5, null, true);
		service.join(code, ANNA);
		service.join(code, BOHDAN);
		service.startGame(code, HOST.playerId());
		service.submitHostTrap(code, HOST.playerId(), "trap");
		assertThatThrownBy(() -> service.endAnswering(code, ANNA.playerId())).isInstanceOf(ApiException.class);
		assertThatThrownBy(() -> service.endVoting(code, HOST.playerId())).isInstanceOf(ApiException.class);

		String hostless = service.createRoom(HOST, true, Question.CATEGORY_FACTS, Question.LANGUAGE_UK, 5, null, true);
		service.join(hostless, ANNA);
		RoomSnapshot started = service.startGame(hostless, HOST.playerId());
		assertThat(started.manualTimer()).isFalse();
		assertThat(started.secondsRemaining()).isPositive();
		assertThatThrownBy(() -> service.endAnswering(hostless, HOST.playerId())).isInstanceOf(ApiException.class);
	}
}
