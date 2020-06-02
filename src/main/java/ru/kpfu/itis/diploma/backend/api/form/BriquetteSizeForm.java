package ru.kpfu.itis.diploma.backend.api.form;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class BriquetteSizeForm {
    private int length;
    private int lengthTolerance;
    private int depth;
    private int depthTolerance;
    private int height;
    private int heightTolerance;
}
