package ru.kpfu.itis.diploma.backend.api.form;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.validation.constraints.NotNull;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class MjpegStreamControlForm {
    private String deviceId;
    /**
     * Уникальный для клиена идентификатор стрима.
     * Перед каждым отправленным кадром будет содержаться эта строка
     */
    @NotNull
    private String streamId;
    @NotNull
    private Action action;
    private StreamType type;
    private StreamConfigurationForm configuration;

    public MjpegStreamRequest toStreamRequest() {
        return new MjpegStreamRequest(
                deviceId,
                streamId,
                type
        );
    }

    public enum Action {
        START, STOP, RECONFIGURE
    }
}
