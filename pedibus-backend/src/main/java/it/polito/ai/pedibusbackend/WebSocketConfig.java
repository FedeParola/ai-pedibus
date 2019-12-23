package it.polito.ai.pedibusbackend;

import it.polito.ai.pedibusbackend.security.jwt.JwtTokenProvider;
import org.springframework.beans.factory.annotation.Autowired;
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
import org.springframework.security.core.Authentication;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

@Configuration
@EnableWebSocketMessageBroker
@Order(Ordered.HIGHEST_PRECEDENCE + 99)
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {
    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    @Override
    //Configure message broker options
    public void configureMessageBroker(MessageBrokerRegistry config) {
        //Enable a simple message broker and configure one or more prefixes to filter destinations targeting the broker
        config.enableSimpleBroker("/topic");
        config.setApplicationDestinationPrefixes("/app");
    }

    @Override
    //Register STOMP endpoints mapping each to a specific URL and (optionally) enabling and configuring SockJS fallback options
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        registry.addEndpoint("/stomp-websocket")
                .setAllowedOrigins("*");
    }

    @Override
    public void configureClientInboundChannel(ChannelRegistration registration) {
        registration.interceptors(new ChannelInterceptor() {
            @Override
            public Message<?> preSend(Message<?> message, MessageChannel channel) {
                StompHeaderAccessor accessor =
                        MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);
                if (StompCommand.CONNECT.equals(accessor.getCommand())) {
                    String token = accessor.getLogin();
                    if (token != null && jwtTokenProvider.validateToken(token)) {
                        Authentication auth = jwtTokenProvider.getAuthentication(token);

                        if (auth != null) {
                            accessor.setUser(auth);
                        }
                    }
                }
                return message;
            }
        });
    }

}
