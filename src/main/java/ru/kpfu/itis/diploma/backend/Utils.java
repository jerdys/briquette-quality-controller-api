package ru.kpfu.itis.diploma.backend;

import java.io.ByteArrayOutputStream;
import java.io.PrintWriter;

public class Utils {
    public static double inBounds(double min, double max, Double value) {
        if (value == null) {
            value = 0.;
        }
        return Math.max(min, Math.min(max, value));
    }

    public static String stackTraceToString(Throwable t) {
        final ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        final PrintWriter writes = new PrintWriter(outputStream);
        t.printStackTrace(writes);
        return new String(outputStream.toByteArray());
    }
}
