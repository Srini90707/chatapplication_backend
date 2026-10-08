package com.chatapp.websocket.config;

import com.chatapp.security.jwt.service.JwtService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

import java.util.Collections;
import java.util.List;

@Configuration
@EnableWebSocketMessageBroker
@Order(Ordered.HIGHEST_PRECEDENCE + 99)
@RequiredArgsConstructor
@Slf4j
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    private final JwtService jwtService;

    /**
     * 1. MESSAGE BROKER CONFIGURATION
     * Controls where messages are routed between clients and server.
     */
    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {
        // Broker channels to deliver messages back to subscribers:
        // - /user:  Private 1-to-1 chat messages (e.g. /user/{mobile}/queue/messages)
        // - /topic: Broadcast messages (e.g. user online/offline status, group chats)
        // - /queue: General point-to-point queues
        registry.enableSimpleBroker("/user", "/topic", "/queue");

        // Prefix for messages sent FROM client TO server (@MessageMapping methods in controllers)
        // Example: client sends to /app/chat.send -> handled by @MessageMapping("/chat.send")
        registry.setApplicationDestinationPrefixes("/app");

        // Prefix used for private 1-to-1 messaging via convertAndSendToUser(...)
        registry.setUserDestinationPrefix("/user");
    }

    /**
     * 2. STOMP WEBSOCKET ENDPOINTS
     * Defines the URL where mobile apps and browsers connect.
     */
    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        // Native WebSocket endpoint (used by mobile apps / React Native)
        registry.addEndpoint("/ws")
                .setAllowedOriginPatterns("*");

        // SockJS fallback endpoint (used by web browsers)
        registry.addEndpoint("/ws-sockjs")
                .setAllowedOriginPatterns("*")
                .withSockJS();
    }

    /**
     * 3. INBOUND CHANNEL SECURITY INTERCEPTOR
     * Authenticates the user during the STOMP connection handshake using JWT.
     */
    @Override
    public void configureClientInboundChannel(ChannelRegistration registration) {
        registration.interceptors(new ChannelInterceptor() {
            @Override
            public Message<?> preSend(Message<?> message, MessageChannel channel) {
                StompHeaderAccessor accessor =
                        MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);

                // Only intercept CONNECT command (initial handshake authentication)
                if (accessor != null && StompCommand.CONNECT.equals(accessor.getCommand())) {
                    authenticateStompConnection(accessor);
                }

                return message;
            }
        });
    }

    /**
     * Extracts and validates the JWT token from the STOMP CONNECT headers.
     */
    private void authenticateStompConnection(StompHeaderAccessor accessor) {
        String token = extractToken(accessor);

        if (token != null && jwtService.validateToken(token)) {
            String userNumber = jwtService.getNumberFromToken(token);

            // Create Spring Security Authentication with the user's phone number as principal
            Authentication auth = new UsernamePasswordAuthenticationToken(
                    userNumber,
                    null,
                    Collections.emptyList()
            );

            // Bind the authenticated user to this WebSocket session
            accessor.setUser(auth);
            log.info("WebSocket connected & authenticated for user: {}", userNumber);
            return;
        }

        // Fallback: Check STOMP login header
        String loginUser = accessor.getLogin();
        if (loginUser != null && !loginUser.isBlank()) {
            Authentication fallbackAuth = new UsernamePasswordAuthenticationToken(
                    loginUser.trim(),
                    null,
                    Collections.emptyList()
            );
            accessor.setUser(fallbackAuth);
            log.info("WebSocket connected with login fallback for user: {}", loginUser.trim());
        } else {
            log.warn("WebSocket CONNECT: unauthenticated anonymous session");
        }
    }

    /**
     * Extracts the JWT token from either 'Authorization: Bearer <token>' or STOMP passcode.
     */
    private String extractToken(StompHeaderAccessor accessor) {
        List<String> authHeaders = accessor.getNativeHeader("Authorization");
        if (authHeaders != null && !authHeaders.isEmpty()) {
            String header = authHeaders.get(0);
            if (header.startsWith("Bearer ")) {
                return header.substring(7);
            }
            return header;
        }

        // Fallback: Some mobile STOMP libraries pass JWT in the passcode field
        return accessor.getPasscode();
    }
}
