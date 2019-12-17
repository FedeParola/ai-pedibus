package it.polito.ai.pedibusbackend;

import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

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
                .withSockJS();
    }

}
