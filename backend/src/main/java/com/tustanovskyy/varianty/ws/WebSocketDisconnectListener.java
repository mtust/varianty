package com.tustanovskyy.varianty.ws;

import com.tustanovskyy.varianty.service.RoomService;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;

@Component
public class WebSocketDisconnectListener {

	private final RoomService roomService;
	private final SimpMessagingTemplate messagingTemplate;

	public WebSocketDisconnectListener(RoomService roomService, SimpMessagingTemplate messagingTemplate) {
		this.roomService = roomService;
		this.messagingTemplate = messagingTemplate;
	}

	@EventListener
	public void onDisconnect(SessionDisconnectEvent event) {
		StompHeaderAccessor accessor = StompHeaderAccessor.wrap(event.getMessage());
		String code = roomService.handleSessionDisconnect(accessor.getSessionId());
		if (code != null && roomService.exists(code)) {
			messagingTemplate.convertAndSend("/topic/room/" + code, roomService.publicSnapshot(code));
		}
	}
}
