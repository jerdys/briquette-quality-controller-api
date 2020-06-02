package ru.kpfu.itis.diploma.backend.api.ws;

import com.fasterxml.jackson.databind.ObjectMapper;
import ru.kpfu.itis.diploma.backend.api.dto.OperatorEventDto;
import ru.kpfu.itis.diploma.backend.api.ws.configuration.WSHandler;
import ru.kpfu.itis.diploma.backend.service.analyze.AnalyzeService;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.log4j.Log4j2;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.AbstractWebSocketHandler;

import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

@Log4j2
@WSHandler(path = "/events/operator")
@RequiredArgsConstructor
public class OperatorWebSocket extends AbstractWebSocketHandler {
    private final ObjectMapper objectMapper;
    private final AnalyzeService analyzeService;
    private final ConcurrentHashMap<WebSocketSession, Operator> operators = new ConcurrentHashMap<>();

    @Override
    public void afterConnectionEstablished(WebSocketSession session) {
        session.setTextMessageSizeLimit(10 * 1024 * 1024);
        session.setBinaryMessageSizeLimit(10 * 1024 * 1024);
        analyzeService.subscribe(
                operators.computeIfAbsent(session, Operator::new)
        );
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        operators.computeIfPresent(
                session,
                (webSocketSession, operator) -> {
                    analyzeService.unsubscribe(operator);
                    return null;
                }
        );
    }


    @RequiredArgsConstructor
    private class Operator implements Consumer<OperatorEventDto> {
        private final WebSocketSession ws;

        @Override
        @SneakyThrows
        public void accept(OperatorEventDto eve) {
            synchronized (ws) {
                ws.sendMessage(new TextMessage(objectMapper.writeValueAsString(eve)));
            }
        }
    }
}
