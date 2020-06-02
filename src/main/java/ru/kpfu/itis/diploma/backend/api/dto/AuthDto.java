package ru.kpfu.itis.diploma.backend.api.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class AuthDto {
    private ProfileDto profile;
    private String session;
}
