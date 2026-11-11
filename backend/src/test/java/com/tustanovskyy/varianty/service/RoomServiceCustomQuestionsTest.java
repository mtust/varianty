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

class RoomServiceCustomQuestionsTest {

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

	private String roomWithPlayers(boolean hostless, int rounds) {
		String code = service.createRoom(HOST, hostless, Question.CATEGORY_FACTS, Question.LANGUAGE_UK, rounds);
		service.join(code, ANNA);
		service.join(code, BOHDAN);
		return code;
	}

	@Test
	void customQuestionsAreAskedFirst() {
		String code = roomWithPlayers(false, 5);
		service.addCustomQuestion(code, HOST.playerId(), "  My cat is called ___ ", "мурчик");
		RoomService.CustomQuestionsResult result = service.addCustomQuestion(code, HOST.playerId(), "Second ___", "two");
		assertThat(result.customQuestions()).hasSize(2);
		assertThat(result.customQuestions().get(0).fact()).isEqualTo("My cat is called ___");
		assertThat(result.snapshot().customQuestionCount()).isEqualTo(2);

		RoomSnapshot started = service.startGame(code, HOST.playerId());
		assertThat(started.fact()).isEqualTo("My cat is called ___");
		assertThat(started.totalRounds()).isEqualTo(5);
		assertThat(started.customQuestionCount()).isEqualTo(1);
		assertThat(service.currentHostQuestion(code, HOST.playerId()).correctAnswer()).isEqualTo("Мурчик");
	}

	@Test
	void moreCustomQuestionsThanRoundsExtendTheGame() {
		String code = roomWithPlayers(false, 5);
		for (int i = 0; i < 7; i++) {
			service.addCustomQuestion(code, HOST.playerId(), "Q" + i + " ___", "a");
		}
		assertThat(service.startGame(code, HOST.playerId()).totalRounds()).isEqualTo(7);
	}

	@Test
	void removeAndValidation() {
		String code = roomWithPlayers(false, 5);
		String id = service.addCustomQuestion(code, HOST.playerId(), "Q ___", "a").customQuestions().get(0).id();
		assertThat(service.removeCustomQuestion(code, HOST.playerId(), id).customQuestions()).isEmpty();
		assertThatThrownBy(() -> service.addCustomQuestion(code, HOST.playerId(), " ", "a")).isInstanceOf(ApiException.class);
		assertThatThrownBy(() -> service.addCustomQuestion(code, ANNA.playerId(), "Q", "a")).isInstanceOf(ApiException.class);
		service.startGame(code, HOST.playerId());
		assertThatThrownBy(() -> service.addCustomQuestion(code, HOST.playerId(), "Q", "a")).isInstanceOf(ApiException.class);
	}

	@Test
	void hostlessRoomsCannotHaveCustomQuestions() {
		String code = roomWithPlayers(true, 5);
		assertThatThrownBy(() -> service.addCustomQuestion(code, HOST.playerId(), "Q", "a"))
				.isInstanceOf(ApiException.class)
				.hasMessage("Власні питання доступні лише в грі з ведучим");
	}
}
