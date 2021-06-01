package ama.config;

import ama.handler.BuildLogHandler;
import ama.handler.PodLogHandler;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.config.annotation.*;

@Configuration
@EnableWebSocket
@EnableWebSocketMessageBroker
public class WebSocketConfig extends AbstractWebSocketMessageBrokerConfigurer implements WebSocketConfigurer {
    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        registry.addEndpoint("/log")
                .setAllowedOrigins("*").withSockJS();
    }

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(podLogHandler(),"/podLog2").setAllowedOrigins("*")
                .addHandler(buildLogHandler(),"/buildLog3").setAllowedOrigins("*");
    }

    private WebSocketHandler podLogHandler() {
        return new PodLogHandler();
    }
    
    private WebSocketHandler buildLogHandler(){
        return new BuildLogHandler();
    }
}