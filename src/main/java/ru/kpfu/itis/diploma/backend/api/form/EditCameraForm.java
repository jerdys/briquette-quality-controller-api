package ru.kpfu.itis.diploma.backend.api.form;

import ru.kpfu.itis.diploma.backend.model.BriquetteSide;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

@Data
@NoArgsConstructor
public class EditCameraForm {
    @NotNull
    @Size(max = 20)
    private String deviceId;
    @NotNull
    @Size(max = 20)
    private String deviceSerial;

    private String displayName;
    @NotNull
    private BriquetteSide position;
    private Double exposureTime;
}
