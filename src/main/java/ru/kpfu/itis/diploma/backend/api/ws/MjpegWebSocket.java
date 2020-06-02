package ru.kpfu.itis.diploma.backend.api.ws;

import com.fasterxml.jackson.databind.ObjectMapper;
import ru.kpfu.itis.diploma.backend.api.form.MjpegStreamControlForm;
import ru.kpfu.itis.diploma.backend.api.ws.configuration.WSHandler;
import ru.kpfu.itis.diploma.backend.service.MjpegStreamingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

@Log4j2
@WSHandler(path = "/mjpeg")
@RequiredArgsConstructor
public class MjpegWebSocket extends TextWebSocketHandler {
    private final MjpegStreamingService service;
    private final ObjectMapper objectMapper;

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        session.setTextMessageSizeLimit(10 * 1024 * 1024);
        session.setBinaryMessageSizeLimit(10 * 1024 * 1024);
    }

    @Override
    protected void handleTextMessage(WebSocketSession ws, TextMessage message) throws Exception {
        try {
            final MjpegStreamControlForm form = objectMapper.readValue(
                    message.getPayload(),
                    MjpegStreamControlForm.class
            );
            // TODO: 10/28/19 handle exceptions
            if (form.getAction() == MjpegStreamControlForm.Action.START) {
                service.startStreaming(ws, form.toStreamRequest());
            } else if (form.getAction() == MjpegStreamControlForm.Action.STOP) {
                service.stopStreaming(ws, form.getStreamId());
            } else if (form.getAction() == MjpegStreamControlForm.Action.RECONFIGURE) {
                service.reconfigureTestStream(ws, form.getStreamId(), form.getConfiguration());
            }
        } catch (Throwable t) {
            log.warn(t.toString(), t);
        }
    }

    @Override
    public void afterConnectionClosed(WebSocketSession ws, CloseStatus status) throws Exception {
        log.debug("mjpeg disconnected: {}", status);
        service.stopStreaming(ws);
    }
}
