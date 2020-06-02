package ru.kpfu.itis.diploma.backend.service.analyze.util;

import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfPoint;
import org.opencv.core.MatOfPoint2f;
import org.opencv.core.Point;
import org.opencv.core.Rect;
import org.opencv.core.RotatedRect;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;
import org.opencv.imgproc.Moments;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

import static java.lang.Math.abs;
import static java.lang.Math.atan;
import static java.lang.Math.atan2;
import static java.lang.Math.cos;
import static java.lang.Math.pow;
import static java.lang.Math.sin;
import static java.lang.Math.sqrt;
import static org.opencv.imgproc.Imgproc.CHAIN_APPROX_SIMPLE;
import static org.opencv.imgproc.Imgproc.COLOR_RGB2GRAY;
import static org.opencv.imgproc.Imgproc.COLOR_RGB2HSV;
import static org.opencv.imgproc.Imgproc.Canny;
import static org.opencv.imgproc.Imgproc.MARKER_TILTED_CROSS;
import static org.opencv.imgproc.Imgproc.RETR_LIST;
import static org.opencv.imgproc.Imgproc.THRESH_BINARY;
import static org.opencv.imgproc.Imgproc.THRESH_OTSU;
import static org.opencv.imgproc.Imgproc.approxPolyDP;
import static org.opencv.imgproc.Imgproc.arcLength;
import static org.opencv.imgproc.Imgproc.contourArea;
import static org.opencv.imgproc.Imgproc.cvtColor;
import static org.opencv.imgproc.Imgproc.drawContours;
import static org.opencv.imgproc.Imgproc.drawMarker;
import static org.opencv.imgproc.Imgproc.fillPoly;
import static org.opencv.imgproc.Imgproc.findContours;
import static org.opencv.imgproc.Imgproc.minAreaRect;
import static org.opencv.imgproc.Imgproc.rectangle;
import static org.opencv.imgproc.Imgproc.resize;
import static org.opencv.imgproc.Imgproc.threshold;

public class AnalyzeUtils {

    public static void erode(Mat src, Mat dst, int radius, int iterations) {
        final Mat kern = Imgproc.getStructuringElement(
                Imgproc.CV_SHAPE_ELLIPSE,
                new Size(radius * 2 + 1, radius * 2 + 1)
        );
        Imgproc.erode(src, dst, kern, new Point(-1, -1), iterations);
        kern.release();
    }

    public static void dilate(Mat src, Mat dst, int radius, int iterations) {
        final Mat kern = Imgproc.getStructuringElement(
                Imgproc.CV_SHAPE_ELLIPSE,
                new Size(radius * 2 + 1, radius * 2 + 1)
        );
        Imgproc.dilate(src, dst, kern, new Point(-1, -1), iterations);
        kern.release();
    }

    public static void lut(Function<Integer, Double> func, Mat img) {
        Mat lookUpTable = new Mat(1, 256, CvType.CV_8U);
        byte[] lookUpTableData = new byte[(int) (lookUpTable.total() * lookUpTable.channels())];
        for (int i = 0; i < lookUpTable.cols(); i++) {
            lookUpTableData[i] = saturate(func.apply(i));
        }
        lookUpTable.put(0, 0, lookUpTableData);
        Core.LUT(img, lookUpTable, img);
        Core.normalize(img, img, 0, 255, Core.NORM_MINMAX);
        lookUpTable.release();
    }

    public static byte saturate(double val) {
        return (byte) Math.round(Math.min(255, Math.max(val, 0)));
    }

    public static Point getCenter(List<Point> contour) {
        final Moments m = Imgproc.moments(new MatOfPoint(contour.toArray(new Point[0])));
        return new Point(m.m10 / m.m00, m.m01 / m.m00);
    }

    public static MatOfPoint contourWithMaxArea(List<MatOfPoint> filterContours) {
        if (filterContours.isEmpty()) {
            return null;
        }
        MatOfPoint max = filterContours.get(0);
        double maxArea = Imgproc.contourArea(max);
        for (int i = 1; i < filterContours.size(); i++) {
            final MatOfPoint contour = filterContours.get(i);
            double area = Imgproc.contourArea(contour);
            if (area > maxArea) {
                max = contour;
                maxArea = area;
            }
        }
        return max;
    }

    public static double area(RotatedRect rotatedRect) {
        final Point[] points = new Point[4];
        rotatedRect.points(points);
        return distance(points[0], points[1]) * distance(points[1], points[2]);
    }

    public static Mat channel(Mat img, int ch) {
        List<Mat> channels = new ArrayList<>();
        try {
            Core.split(img, channels);
            Mat gray = channels.get(ch);
            channels.remove(gray);
            return gray;
        } finally {
            channels.forEach(Mat::release);
        }
    }

    public static MatOfPoint toMOP(List<Point> points) {
        return new MatOfPoint(points.toArray(new Point[0]));
    }

    public static Contour scaleContour(Contour contour, double scaleRate) {
        return new Contour(
                scaleContour(contour.contour, scaleRate),
                scaleContour(contour.corners, scaleRate)
        );
    }

    public static List<Point> scaleContour(MatOfPoint contourMop, double scaleRate) {
        return scaleContour(contourMop.toList(), scaleRate);
    }

    public static List<Point> scaleContour(List<Point> points, double scaleRate) {
        final List<Point> scaled = new ArrayList<>(points.size());
        for (final Point point : points) {
            scaled.add(new Point(point.x * scaleRate, point.y * scaleRate));
        }
        return scaled;
    }

    public static List<Point> extendContour(List<Point> contour, double extDist) {
        List<Point> extendedContour = new ArrayList<>();
        Point center = getCenter(contour);

        for (final Point point : contour) {
            double tan = abs(center.y - point.y) / abs(center.x - point.x);
            double angle = atan(tan);
            double sin = sin(angle);
            double cos = cos(angle);
            int symX = (center.x > point.x) ? -1 : 1;
            int symY = (center.y > point.y) ? -1 : 1;
            double x = point.x + symX * extDist * cos;
            double y = point.y + symY * extDist * sin;
            extendedContour.add(new Point(x, y));
        }
        return extendedContour;
    }

    public static void sobel(Mat in, Mat out, int ksize) {
        final Mat gradX = new Mat();
        final Mat gradY = new Mat();
        try {
            Imgproc.Sobel(in, gradX, in.depth(), 1, 0, ksize);
            Core.convertScaleAbs(gradX, gradX);
            Imgproc.Sobel(in, gradY, in.depth(), 0, 1, ksize);
            Core.convertScaleAbs(gradY, gradY);
            Core.addWeighted(gradX, 0.5, gradY, 0.5, 0, out);
        } finally {
            gradX.release();
            gradY.release();
        }
    }

    public static double distance(Point leftPoint, Point rightPoint) {
        return sqrt(pow(abs(leftPoint.x - rightPoint.x), 2) + pow(abs(leftPoint.y - rightPoint.y), 2));
    }

    public static Point getEdgeCenter(Point p0, Point p1, Contour contour) {
        return new Point((p0.x + p1.x) / 2, (p0.y + p1.y) / 2);
    }

    public static double angle(Point vectorStart, Point vectorEnd) {
        Point vector = new Point(vectorEnd.x - vectorStart.x, vectorEnd.y - vectorStart.y);
        return atan2(vector.y, vector.x);
    }

    public static void drawContour(Mat img, Contour contour) {
        drawContours(
                img,
                Collections.singletonList(new MatOfPoint(contour.contour.toArray(new Point[0]))),
                -1,
                new Scalar(0, 255, 0),
                1
        );
        for (final Point corner : contour.corners) {
            drawMarker(img, corner, new Scalar(255, 0, 0), MARKER_TILTED_CROSS, 7, 2);
        }
    }

    public static void drawRects(Mat img, List<Rect> rects, Scalar color, int thickness) {
        rects.forEach(rect -> rectangle(img, rect, color, thickness));
    }


    public static Optional<Contour> contours(Mat img) {
        // FIXME: 27.12.2019 magic numbers
        Mat rgb = new Mat();
        Mat hsv = new Mat();
        Mat gray = null;
        try {
            final double scaleRate = 0.4;
            final double resizedWidth = img.width() * scaleRate;
            final double resizedHeight = img.height() * scaleRate;
            resize(img, rgb, new Size(resizedWidth, resizedHeight));

            cvtColor(rgb, hsv, COLOR_RGB2HSV);
            gray = channel(hsv, 1);

            dilate(gray, gray, 2, 1);
            erode(gray, gray, 2, 1);

            final double otsu = threshold(gray, gray, 0, 255, THRESH_BINARY | THRESH_OTSU);

            Canny(
                    gray,
                    gray,
                    otsu * 0.33,
                    otsu * 0.66,
                    3,
                    false
            );

            dilate(gray, gray, 2, 1);
            erode(gray, gray, 2, 1);

            rectangle(
                    gray,
                    new Point(0, 0),
                    new Point(gray.width() - 1, gray.height() - 1),
                    Scalar.all(255),
                    1
            );
            dilate(gray, gray, 1, 1);
            Core.bitwise_not(gray, gray);

            resize(rgb, img, img.size());

            final List<MatOfPoint> ctrs = new ArrayList<>();
            final Mat hierarchy = new Mat();
            findContours(
                    gray,
                    ctrs,
                    hierarchy,
                    RETR_LIST,
                    CHAIN_APPROX_SIMPLE
            );
            hierarchy.release();

            final int imgArea = gray.width() * gray.height();
            final Mat grayFinal = gray;
            final List<Contour> filteredContours = ctrs.stream()
                    // довольно тупой способ: просто смотрим чтоб
                    // контур содержал точки в обеих половинах изображения
                    .filter(mop -> containPointsInOppositeHalvesOfImage(mop, grayFinal))
                    // FIXME: 26.12.2019 magic number (imgMat.height())
                    .filter(mop -> longEnoughArc(mop, grayFinal.height()))
                    .filter(mop -> bigEnoughArea(mop, imgArea))
                    .map(contour -> {
                        final List<Point> points = contour.toList();
                        return new Contour(points, corners(points, 0.05 * scaleRate * img.width()));
                    })
                    .filter(contour -> contour.corners.size() == 4)
                    .collect(Collectors.toList());
            if (filteredContours.isEmpty()) {
                return Optional.empty();
            }
            final Contour contour = contourByColor(filteredContours, rgb, Scalar.all(155), Scalar.all(255));
            // хз надо ли, но на всякий случай
            ctrs.forEach(Mat::release);

            return Optional.of(scaleContour(contour, 1 / scaleRate));
        } finally {
            rgb.release();
            if (gray != null) {
                gray.release();
            }
            hsv.release();
        }
    }

    public static List<Point> corners(List<Point> curve, double epsilon) {
        final MatOfPoint2f curveMat = new MatOfPoint2f(curve.toArray(new Point[0]));
        final MatOfPoint2f corners = new MatOfPoint2f();
        try {
            approxPolyDP(curveMat, corners, epsilon, true);
            curveMat.release();
            return corners.toList();
        } finally {
            curveMat.release();
            corners.release();
        }
    }

    public static Optional<Contour> contours_(Mat img) {
        // FIXME: 16.12.2019 magic numbers
        Mat rgb = new Mat();
        Mat gray = new Mat();
        try {
            double scaleRate = 0.3;
            double resizedWidth = img.width() * scaleRate;
            double resizedHeight = img.height() * scaleRate;
            resize(img, rgb, new Size(resizedWidth, resizedHeight));
            erode(rgb, rgb, 4, 1);
            dilate(rgb, rgb, 4, 1);

            lut(i -> (Math.atan((i - 100) / 20.0) + Math.PI / 2) * 255 / Math.PI, rgb);
            cvtColor(rgb, gray, COLOR_RGB2GRAY);
            threshold(gray, gray, 30, 255, THRESH_BINARY);

            List<MatOfPoint> ctrs = new ArrayList<>();
            Mat hierarchy = new Mat();
            findContours(
                    gray,
                    ctrs,
                    hierarchy,
                    RETR_LIST,
                    CHAIN_APPROX_SIMPLE
            );
            final List<MatOfPoint> filteredContours = filterContours(ctrs, gray);
            final MatOfPoint contourMop = contourWithMaxArea(filteredContours);
            if (contourMop == null) {
                // на всякий случай
                ctrs.forEach(Mat::release);
                return Optional.empty();
            }
            List<Point> scaledContour = scaleContour(contourMop, 1 / scaleRate);
            // на всякий случай
            ctrs.forEach(Mat::release);
            if (scaledContour.size() < 3) {
                return Optional.empty();
            }
            List<Point> contours = new ArrayList<>(scaledContour);

            final MatOfPoint2f curve = new MatOfPoint2f(scaledContour.toArray(new Point[0]));
            // final double arcLength = arcLength(curve, true);
            MatOfPoint2f corners = new MatOfPoint2f();
            approxPolyDP(curve, corners, 0.05 * img.width(), true);
            curve.release();
            final Contour result = new Contour(contours, corners.toList());
            corners.release();
            return Optional.of(result);
        } finally {
            rgb.release();
            gray.release();
        }
    }

    public static List<MatOfPoint> filterContours(List<MatOfPoint> contours, Mat imgMat) {
        final int imgArea = imgMat.width() * imgMat.height();
        return contours.stream()
                // довольно тупой способ: просто смотрим чтоб
                // контур содержал точки в обеих половинах изображения
                .filter(mop -> containPointsInOppositeHalvesOfImage(mop, imgMat))
                // FIXME: 26.12.2019 magic number (imgMat.height())
                .filter(mop -> longEnoughArc(mop, imgMat.height()))
                .filter(mop -> bigEnoughArea(mop, imgArea))
                .collect(Collectors.toList());
    }

    private static boolean bigEnoughArea(MatOfPoint mop, int imgArea) {
        final MatOfPoint2f curve = new MatOfPoint2f(mop.toArray());
        final RotatedRect rotatedRect = minAreaRect(curve);
        curve.release();
        final double contourArea = contourArea(mop);
        final double rotatedRectArea = area(rotatedRect);
        // FIXME: 26.12.2019 magic numbers
        return abs(contourArea - rotatedRectArea) < rotatedRectArea * 0.2 && abs(contourArea - imgArea) > imgArea * 0.01;
    }

    private static boolean longEnoughArc(MatOfPoint mop, int minArc) {
        final MatOfPoint2f curve = new MatOfPoint2f(mop.toArray());
        final double arcLength = arcLength(curve, true);
        curve.release();
        return arcLength > minArc;
    }

    private static boolean containPointsInOppositeHalvesOfImage(MatOfPoint mop, Mat img) {
        boolean in_left_zone = false;
        boolean in_right_zone = false;
        boolean in_top_zone = false;
        boolean in_bottom_zone = false;
        final int width = img.width();
        final int height = img.height();
        for (final Point point : mop.toArray()) {
            in_left_zone = in_left_zone || (point.x < width / 2.0 - width * 0.1);
            in_right_zone = in_right_zone || (point.x > width / 2.0 + width * 0.1);
            in_top_zone = in_top_zone || (point.y < height / 2.0 - height * 0.1);
            in_bottom_zone = in_bottom_zone || (point.y > height / 2.0 + height * 0.1);
            if (in_left_zone && in_right_zone || in_top_zone && in_bottom_zone) {
                return true;
            }
        }
        return false;
    }

    public static Contour contourByColor(List<Contour> contours, Mat imgMat, Scalar min, Scalar max) {
        if (contours == null || contours.isEmpty()) {
            return null;
        } else if (contours.size() == 1) {
            return contours.get(0);
        }
        Mat scaled = new Mat();
        Mat mask = null;
        Mat range = null;
        try {
            final double scaleRate = 0.3;
            // FIXME: 27.02.2020 спорный момент
            resize(imgMat, scaled, new Size(imgMat.width() * scaleRate, imgMat.height() * scaleRate));
            mask = new Mat(scaled.rows(), scaled.cols(), CvType.CV_8U);
            range = new Mat(scaled.rows(), scaled.cols(), CvType.CV_8U, Scalar.all(0));

            Contour maxMatch = contours.get(0);
            double maxMatchSize = 0;

            for (final Contour contour : contours) {
                final List<Point> points = contour.contour;
                mask.setTo(Scalar.all(0));
                fillPoly(
                        mask,
                        Collections.singletonList(toMOP(scaleContour(points, scaleRate))),
                        Scalar.all(255)
                );
                // FIXME: 26.12.2019 contour area faster?
                final int maskSize = Core.countNonZero(mask);
                Core.inRange(scaled, min, max, range);
                Core.bitwise_and(mask, range, mask);
                final int nonZero = Core.countNonZero(mask);
                final double nonZeroRate = 1.0 * nonZero / maskSize;
                if (nonZeroRate > maxMatchSize) {
                    maxMatch = contour;
                    maxMatchSize = nonZeroRate;
                }
            }
            return maxMatch;
        } finally {
            scaled.release();
            if (mask != null) {
                mask.release();
            }
            if (range != null) {
                range.release();
            }
        }
    }
}
