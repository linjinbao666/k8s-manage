package ama.handler;

import ama.log.LoggerDisruptorQueue;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

/**
 * 制作镜像过程中日志输出
 */
@Slf4j
public class BuildLogHandler extends TextWebSocketHandler {

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception{
        while (!LoggerDisruptorQueue.queue.isEmpty()){
            String log = LoggerDisruptorQueue.getLog();
            session.sendMessage(new TextMessage("hello"));
            session.sendMessage(new TextMessage(log));
        }
    }
}
