package com.tustanovskyy.varianty.config;

import com.tustanovskyy.varianty.security.StompAuthChannelInterceptor;
import org.springframework.context.annotation.Configuration;
import org.springframework.lang.NonNull;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

	private final StompAuthChannelInterceptor stompAuthChannelInterceptor;

	public WebSocketConfig(StompAuthChannelInterceptor stompAuthChannelInterceptor) {
		this.stompAuthChannelInterceptor = stompAuthChannelInterceptor;
	}

	@Override
	public void registerStompEndpoints(@NonNull StompEndpointRegistry registry) {
		// Plain WebSocket for native clients (Expo/React Native)...
		registry.addEndpoint("/ws").setAllowedOriginPatterns("*");
		// ...and a SockJS fallback for browser-based testing/dev tools.
		registry.addEndpoint("/ws").setAllowedOriginPatterns("*").withSockJS();
	}

	@Override
	public void configureMessageBroker(@NonNull MessageBrokerRegistry registry) {
		registry.enableSimpleBroker("/topic", "/queue");
		registry.setApplicationDestinationPrefixes("/app");
		registry.setUserDestinationPrefix("/user");
	}

	@Override
	public void configureClientInboundChannel(@NonNull ChannelRegistration registration) {
		registration.interceptors(stompAuthChannelInterceptor);
	}
}
