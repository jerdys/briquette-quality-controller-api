package ru.kpfu.itis.diploma.backend.api.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class StreamInviteDto {
    private String token;
}
