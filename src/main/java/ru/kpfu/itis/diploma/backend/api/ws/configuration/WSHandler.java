package ru.kpfu.itis.diploma.backend.api.ws.configuration;

import org.springframework.stereotype.Component;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Component
public @interface WSHandler {
    String path();

    String[] allowedOrigins() default {"*"};
}
