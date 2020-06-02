package ru.kpfu.itis.diploma.backend.api.form;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.PrintStream;
import java.util.Arrays;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class HistogramForm {
    private int[] r;
    private int[] g;
    private int[] b;

    public void print(PrintStream out) {
        out.println("R: " + Arrays.toString(r));
        out.println("G: " + Arrays.toString(g));
        out.println("B: " + Arrays.toString(b));
    }
}
