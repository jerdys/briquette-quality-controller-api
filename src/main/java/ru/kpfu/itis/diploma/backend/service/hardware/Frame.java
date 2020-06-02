package ru.kpfu.itis.diploma.backend.service.hardware;

import lombok.Data;
import lombok.extern.log4j.Log4j2;
import org.opencv.core.Mat;

@Data
@Log4j2
public class Frame {
    /**
     * Внимание!!
     * Для всех фреймов с одной камеры используется один буффер,
     * который будет очищен после вызова всех обработчиков
     * Если сохранять буффер надолго - надо копировать его
     */
    private final Mat bgr;
    private final long timestamp;

    public int getWidth() {
        return bgr.width();
    }

    public int getHeight() {
        return bgr.height();
    }

    public Frame clone() {
        return new Frame(
                this.bgr.clone(),
                this.timestamp
        );
    }

    public void release() {
        this.bgr.release();
    }
}
