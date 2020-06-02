package ru.kpfu.itis.diploma.backend.api.ws;

import com.fasterxml.jackson.databind.ObjectMapper;
import ru.kpfu.itis.diploma.backend.api.dto.OperatorEventDto;
import ru.kpfu.itis.diploma.backend.api.dto.SmudgeDescriptionDto;
import ru.kpfu.itis.diploma.backend.api.ws.configuration.WSHandler;
import ru.kpfu.itis.diploma.backend.model.BriquetteSide;
import ru.kpfu.itis.diploma.backend.service.StatisticsService;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.AbstractWebSocketHandler;

import javax.annotation.PostConstruct;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

@Log4j2
@WSHandler(path = "/events/operator/test")
@RequiredArgsConstructor
public class TestOperatorWebSocket extends AbstractWebSocketHandler {
    private final ObjectMapper objectMapper;
    private final StatisticsService statisticsService;
    private final ScheduledExecutorService executorService = Executors.newSingleThreadScheduledExecutor();

    private final ConcurrentSkipListSet<WebSocketSession> sessions =
            new ConcurrentSkipListSet<>(Comparator.comparing(WebSocketSession::getId));

    @PostConstruct
    private void init() {
        executorService.scheduleWithFixedDelay(
                () -> {
                    if (!sessions.isEmpty()) {
                        sessions.forEach(
                                ws -> {
                                    OperatorEventDto dto = OperatorEventDto.builder()
                                            .time(System.currentTimeMillis())
                                            .briquetteDescription(buildTestAnomalies())
                                            .statistics(statisticsService.getTodayStatistics())
                                            .build();

                                    try {
                                        synchronized (ws){
                                            ws.sendMessage(new TextMessage(
                                                    objectMapper.writeValueAsString(dto)
                                            ));
                                        }
                                    } catch (IOException e) {
                                        log.warn("Unable to send event: {}", e.toString());
                                    }
                                }
                        );
                    }
                },
                3, 3, TimeUnit.SECONDS
        );
    }

    private final Random random = new Random();

    private Map<BriquetteSide, List<SmudgeDescriptionDto>> buildTestAnomalies() {
        int magicNumber = 4;
        if (random.nextBoolean()) {
            statisticsService.incrementNormal();
            return Collections.emptyMap();
        } else {
            final int count = (int) Math.sqrt(random.nextInt(magicNumber * magicNumber));
            Map<BriquetteSide, List<SmudgeDescriptionDto>> anomalies = new HashMap<>();
            final BriquetteSide[] sides = BriquetteSide.values();
            for (int i = 0; i < magicNumber - count; i++) {
                final BriquetteSide side = sides[random.nextInt(sides.length)];
                List<SmudgeDescriptionDto> list = anomalies.computeIfAbsent(side, k -> new ArrayList<>());
                list.add(buildTestAnomaly());
            }
            statisticsService.incrementDefective();
            return anomalies;
        }
    }

    private SmudgeDescriptionDto buildTestAnomaly() {
        final int minRadius = random.nextInt(20);
        return SmudgeDescriptionDto.builder()
                .minRadius(minRadius)
                .maxRadius(minRadius * 2f)
                .x(random.nextFloat())
                .y(random.nextFloat())
                .build();
    }

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        sessions.add(session);
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
        sessions.remove(session);
    }
}
