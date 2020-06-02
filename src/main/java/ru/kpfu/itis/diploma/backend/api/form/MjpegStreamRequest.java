package ru.kpfu.itis.diploma.backend.api.form;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class MjpegStreamRequest {
    private String deviceId;
    private String streamId;
    private StreamType type;
}
