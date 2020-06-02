package ru.kpfu.itis.diploma.backend.api.form;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class StreamConfigurationForm {
    private HistogramForm histogram;
}
