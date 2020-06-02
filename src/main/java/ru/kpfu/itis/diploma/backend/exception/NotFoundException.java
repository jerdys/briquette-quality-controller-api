package ru.kpfu.itis.diploma.backend.exception;

public class NotFoundException extends RuntimeException {
    private final Class<?> entity;
    private final Object id;

    public NotFoundException(Class<?> entity, Object id) {
        super("Entity " + entity + " '" + id + "' not found.");
        this.entity = entity;
        this.id = id;
    }
}
