package ru.kpfu.itis.diploma.backend.service.analyze.util;

import lombok.RequiredArgsConstructor;
import org.opencv.core.Point;

import java.util.List;

@RequiredArgsConstructor
public  class Contour {
    public final List<Point> contour;
    public final List<Point> corners;
}
