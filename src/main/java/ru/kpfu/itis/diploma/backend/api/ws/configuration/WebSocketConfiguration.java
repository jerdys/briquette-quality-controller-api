package ru.kpfu.itis.diploma.backend.api.ws.configuration;

import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

import java.util.Map;

@Configuration
@EnableWebSocket
@RequiredArgsConstructor
public class WebSocketConfiguration implements WebSocketConfigurer {
    private final ApplicationContext context;

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry webSocketHandlerRegistry) {
        final Map<String, Object> handlerBeans = context.getBeansWithAnnotation(WSHandler.class);
        for (final Map.Entry<String, Object> entry : handlerBeans.entrySet()) {
            final WSHandler annotation = context.findAnnotationOnBean(entry.getKey(), WSHandler.class);
            if (annotation != null) {
                final String path = annotation.path();
                final String[] origins = annotation.allowedOrigins();
                final WebSocketHandler handler = (WebSocketHandler) entry.getValue();
                webSocketHandlerRegistry.addHandler(
                        handler,
                        path
                ).setAllowedOrigins(origins);
            }
        }
    }
}
