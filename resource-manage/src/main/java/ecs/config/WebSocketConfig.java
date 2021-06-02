package ecs.config;

import ecs.handler.BinaryHandler;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

/**
 * @author linjinbao66@gmail.com
 * @date 2020/6/7
 */
@Configuration
@EnableWebSocket
public class WebSocketConfig implements WebSocketConfigurer {
    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry webSocketHandlerRegistry) {
        webSocketHandlerRegistry
                .addHandler(binaryHandler(), "/binaryHandler")
                .setAllowedOrigins("*");
    }

    private WebSocketHandler binaryHandler(){
        return new BinaryHandler();
    }
}
