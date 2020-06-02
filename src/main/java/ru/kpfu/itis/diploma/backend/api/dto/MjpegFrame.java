package ru.kpfu.itis.diploma.backend.api.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class MjpegFrame {
    private String deviceId;
    private String dataB64;
}
