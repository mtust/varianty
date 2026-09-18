package com.tustanovskyy.varianty.security;

import org.springframework.lang.NonNull;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.stereotype.Component;

@Component
public class StompAuthChannelInterceptor implements ChannelInterceptor {

	private final JwtService jwtService;

	public StompAuthChannelInterceptor(JwtService jwtService) {
		this.jwtService = jwtService;
	}

	@Override
	public Message<?> preSend(@NonNull Message<?> message, @NonNull MessageChannel channel) {
		StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);
		if (accessor != null && StompCommand.CONNECT.equals(accessor.getCommand())) {
			String header = accessor.getFirstNativeHeader("Authorization");
			if (header != null && header.startsWith("Bearer ")) {
				PlayerPrincipal principal = jwtService.parse(header.substring(7));
				accessor.setUser(new StompPrincipal(principal.playerId(), principal.displayName(), principal.guest()));
			} else {
				throw new IllegalArgumentException("Missing Authorization header on STOMP CONNECT");
			}
		}
		return message;
	}
}
