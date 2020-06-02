package ru.kpfu.itis.diploma.backend.api.controller;

import ru.kpfu.itis.diploma.backend.Utils;
import ru.kpfu.itis.diploma.backend.api.dto.ExceptionDto;
import ru.kpfu.itis.diploma.backend.exception.BadRequestException;
import ru.kpfu.itis.diploma.backend.exception.ForbiddenException;
import ru.kpfu.itis.diploma.backend.exception.NotFoundException;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class ExceptionController {
    @ExceptionHandler(BadRequestException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ExceptionDto badRequest(BadRequestException e) {
        return new ExceptionDto(
                400,
                e.getMessage(),
                e.toString() + "\n" + Utils.stackTraceToString(e)
        );
    }

    @ExceptionHandler(NotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public ExceptionDto notFound(NotFoundException e) {
        return new ExceptionDto(
                404,
                e.getMessage(),
                e.toString() + "\n" + Utils.stackTraceToString(e)
        );
    }

    @ExceptionHandler(ForbiddenException.class)
    @ResponseStatus(HttpStatus.FORBIDDEN)
    public ExceptionDto forbidden(ForbiddenException e) {
        return new ExceptionDto(
                403,
                e.getMessage(),
                e.toString() + "\n" + Utils.stackTraceToString(e)
        );
    }

    @ExceptionHandler(Throwable.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public ExceptionDto internalError(Throwable t) {
        return new ExceptionDto(
                500,
                t.getMessage(),
                t.toString() + "\n" + Utils.stackTraceToString(t)
        );
    }
}
