package com.tustanovskyy.varianty.ws;

import com.tustanovskyy.varianty.domain.dto.room.AnswerPayload;
import com.tustanovskyy.varianty.domain.dto.room.ErrorDto;
import com.tustanovskyy.varianty.domain.dto.room.RoomSnapshot;
import com.tustanovskyy.varianty.domain.dto.room.TrapPayload;
import com.tustanovskyy.varianty.domain.dto.room.VotePayload;
import com.tustanovskyy.varianty.exception.ApiException;
import com.tustanovskyy.varianty.security.StompPrincipal;
import com.tustanovskyy.varianty.service.RoomService;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.MessageExceptionHandler;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;

@Controller
public class GameWebSocketController {

	private final RoomService roomService;
	private final SimpMessagingTemplate messagingTemplate;

	public GameWebSocketController(RoomService roomService, SimpMessagingTemplate messagingTemplate) {
		this.roomService = roomService;
		this.messagingTemplate = messagingTemplate;
	}

	@MessageMapping("/room/{code}/join")
	public void join(@DestinationVariable String code, StompPrincipal principal, @Header("simpSessionId") String sessionId) {
		roomService.trackSession(sessionId, code, principal.playerId());
		RoomSnapshot snapshot = roomService.join(code, toPrincipal(principal));
		broadcast(code, snapshot);
	}

	@MessageMapping("/room/{code}/start")
	public void start(@DestinationVariable String code, StompPrincipal principal) {
		RoomSnapshot snapshot = roomService.startGame(code, principal.playerId());
		broadcast(code, snapshot);
		if (!snapshot.hostless()) {
			messagingTemplate.convertAndSendToUser(
					principal.playerId(), "/queue/host-question", roomService.currentHostQuestion(code, principal.playerId()));
		}
	}

	@MessageMapping("/room/{code}/host-trap")
	public void hostTrap(@DestinationVariable String code, StompPrincipal principal, @Payload TrapPayload payload) {
		RoomSnapshot snapshot = roomService.submitHostTrap(code, principal.playerId(), payload.text());
		broadcast(code, snapshot);
	}

	@MessageMapping("/room/{code}/answer")
	public void answer(@DestinationVariable String code, StompPrincipal principal, @Payload AnswerPayload payload) {
		RoomService.AnswerResult result = roomService.submitAnswer(code, principal.playerId(), payload.text());
		broadcast(code, result.snapshot());
		if (result.votingStarted() != null) {
			messagingTemplate.convertAndSend("/topic/room/" + code + "/voting", result.votingStarted());
		}
	}

	@MessageMapping("/room/{code}/vote")
	public void vote(@DestinationVariable String code, StompPrincipal principal, @Payload VotePayload payload) {
		RoomService.VoteResult result = roomService.submitVote(code, principal.playerId(), payload.optionId());
		broadcast(code, result.snapshot());
		if (result.roundResult() != null) {
			messagingTemplate.convertAndSend("/topic/room/" + code + "/results", result.roundResult());
		}
	}

	@MessageMapping("/room/{code}/next-round")
	public void nextRound(@DestinationVariable String code, StompPrincipal principal) {
		RoomService.NextRoundResult result = roomService.nextRound(code, principal.playerId());
		broadcast(code, result.snapshot());
		if (!result.finished() && result.snapshot().status().equals("TRAP")) {
			messagingTemplate.convertAndSendToUser(
					principal.playerId(), "/queue/host-question", roomService.currentHostQuestion(code, principal.playerId()));
		}
	}

	@MessageExceptionHandler(ApiException.class)
	public void handleApiException(ApiException ex, StompPrincipal principal) {
		messagingTemplate.convertAndSendToUser(principal.playerId(), "/queue/errors", new ErrorDto(ex.getMessage()));
	}

	private void broadcast(String code, RoomSnapshot snapshot) {
		messagingTemplate.convertAndSend("/topic/room/" + code, snapshot);
	}

	private com.tustanovskyy.varianty.security.PlayerPrincipal toPrincipal(StompPrincipal principal) {
		return new com.tustanovskyy.varianty.security.PlayerPrincipal(principal.playerId(), principal.displayName(), principal.guest());
	}
}
