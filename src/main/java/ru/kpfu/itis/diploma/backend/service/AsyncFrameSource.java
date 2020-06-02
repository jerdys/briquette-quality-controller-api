package ru.kpfu.itis.diploma.backend.service;

import ru.kpfu.itis.diploma.backend.service.hardware.Frame;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.ArrayDeque;
import java.util.Queue;
import java.util.concurrent.CompletableFuture;

public interface AsyncFrameSource {
    /**
     * Когда фрейм будет готов вызовет consumer, передаст в него полученный фрейм.
     * Метод вызовет consumer только 1 раз, для получения следующего фрейма метод нужно вызвать еще раз.
     * Все consumer вызываются одним потоком, поэтому обработчик должен выполняться быстро,
     * либо клонировать фрейм и продолжать выполнение в отдельном потоке.
     */
    default CompletableFuture<Frame> getFrameAsync() {
        return getFrameAsync(0);
    }

    /**
     * Когда фрейм будет готов вызовет consumer, передаст в него полученный фрейм.
     * Пропустит фреймы с timestamp <= skipTimestamp
     * Метод вызовет consumer только 1 раз, для получения следующего фрейма метод нужно вызвать еще раз.
     * Все consumer вызываются одним потоком, поэтому обработчик должен выполняться быстро,
     * либо клонировать фрейм и продолжать выполнение в отдельном потоке.
     */
    CompletableFuture<Frame> getFrameAsync(long skipTimestamp);

    default void onNext(Frame frame, Queue<FutureFrame> queue) {
        final FutureFrame[] consumers = queue.toArray(new FutureFrame[0]);
        Queue<FutureFrame> skipped = new ArrayDeque<>();
        queue.clear();
        for (final FutureFrame consumer : consumers) {
            if (consumer.getSkipTimestamp() > frame.getTimestamp()) {
                skipped.add(consumer);
            } else {
                consumer.complete(frame);
            }
        }
        queue.addAll(skipped);
    }

    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    class FutureFrame extends CompletableFuture<Frame> {
        private long skipTimestamp = 0;
    }
}
