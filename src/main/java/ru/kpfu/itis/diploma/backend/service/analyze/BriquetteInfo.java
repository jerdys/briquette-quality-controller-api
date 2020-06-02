package ru.kpfu.itis.diploma.backend.service.analyze;

import ru.kpfu.itis.diploma.backend.Application;
import ru.kpfu.itis.diploma.backend.model.BriquetteSide;
import ru.kpfu.itis.diploma.backend.model.SideReport;
import ru.kpfu.itis.diploma.backend.model.SmudgeInfo;
import ru.kpfu.itis.diploma.backend.service.analyze.util.AnalyzeUtils;
import ru.kpfu.itis.diploma.backend.service.analyze.util.Contour;
import ru.kpfu.itis.diploma.backend.service.hardware.Frame;
import lombok.Builder;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.jetbrains.annotations.Nullable;
import org.opencv.core.Mat;
import org.opencv.core.MatOfInt;
import org.opencv.core.MatOfPoint;
import org.opencv.core.Point;
import org.opencv.core.Rect;
import org.opencv.imgproc.Imgproc;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import static java.lang.Math.PI;
import static java.lang.Math.abs;
import static java.lang.Math.cos;
import static java.lang.Math.max;
import static java.lang.Math.min;
import static java.lang.Math.sin;

@Log4j2
@RequiredArgsConstructor
public class BriquetteInfo {
    private final AlgorithmParameters parameters;
    @Getter
    private final BriquetteSide side;
    @Getter
    private final ArrayList<FrameMeta> keyFrames = new ArrayList<>();
    /**
     * pixels per nanosecond
     */
    @Getter
    private double avgSpeed = 0;
    private double direction = 0;
    @Getter
    private boolean finished = false;
    @Getter
    private List<SmudgeInfo> summarySmudges;

    public void append(Frame frame,
                       Contour contour,
                       List<Rect> smudges) {
        if (finished) {
            throw new IllegalStateException("This briquette already finished");
        }
        final FrameMeta frameMeta = meta(frame, contour, smudges);
        if (!frameMeta.isEdge && keyFrames.isEmpty()) {
            return;
        }
        if (keyFrames.isEmpty()) {
            System.err.println("start");
        }
        System.err.println("add");
        keyFrames.add(frameMeta);
        if (keyFrames.size() < parameters.stitch.minFrames) {
            return;
        }
        if (frameMeta.isEdge) {
            updateSpeed();
            if (isEndOfBriq(frameMeta)) {
                if (!isValidFirstAndLastEdges()) {
                    throw new IllegalStateException("Invalid first and last frames");
                }
                System.err.println("finish");
                finished = true;
                // TODO: 20.12.2019 filter
                this.summarySmudges = smudges();
            }
        }
    }

    // по сути isValidFirstAndLastEdges и isEndOfBriq делают практически одно и то же, надо объединить
    private boolean isValidFirstAndLastEdges() {
        final FrameMeta firstFrame = keyFrames.get(0);
        final FrameMeta lastFrame = keyFrames.get(keyFrames.size() - 1);
        final Point firstFrameVector = new Point(
                firstFrame.edgeCenter.x - firstFrame.center.x,
                firstFrame.edgeCenter.y - firstFrame.center.y
        );
        final Point lastFrameVector = new Point(
                lastFrame.edgeCenter.x - lastFrame.center.x,
                lastFrame.edgeCenter.y - lastFrame.center.y
        );
        double firstFrameVectorAngle = Math.atan2(firstFrameVector.y, firstFrameVector.x);
        double lastFrameVectorAngle = Math.atan2(lastFrameVector.y, lastFrameVector.x);
        firstFrameVectorAngle = firstFrameVectorAngle < 0 ? (2 * PI + firstFrameVectorAngle) : firstFrameVectorAngle;
        lastFrameVectorAngle = lastFrameVectorAngle < 0 ? (2 * PI + lastFrameVectorAngle) : lastFrameVectorAngle;
        // 2 * PI < directionPositive <=  0
        double directionPositive = direction < 0 ? (2 * PI + direction) : direction;

        return abs(firstFrameVectorAngle - directionPositive) < PI / 6
                && abs(abs(lastFrameVectorAngle - directionPositive) - PI) < PI / 6;
    }

    private boolean isEndOfBriq(FrameMeta curFrame) {
        final Point curFrameVector = new Point(
                curFrame.edgeCenter.x - curFrame.center.x,
                curFrame.edgeCenter.y - curFrame.center.y
        );
        double firstFrameAngle = direction < 0 ? (2 * PI + direction) : direction;
        double curFrameAngle = Math.atan2(curFrameVector.y, curFrameVector.x);
        curFrameAngle = curFrameAngle < 0 ? (2 * PI + curFrameAngle) : curFrameAngle;

        return abs(abs(firstFrameAngle - curFrameAngle) - PI) < PI / 4;
    }

    /**
     * Возвращает список точек, относительно левого верхнего угла брикета
     * в абсолютных значениях(левая верхняя точка - [0;0], правая нижняя - [1;1])
     * Вдоль оси x - ширина брикета(по стороне, перпендикулярной направлению движения брикета)
     * <pre>
     *                      ________________v(1,1)
     *      Направление    |                |
     *     <------------   |                |
     *       движения      |________________|
     *                     ^(0,0)
     * </pre>
     */
    public List<SmudgeInfo> smudges() {
        final FrameMeta firstFrame = keyFrames.get(0);
        final double briqWidth = AnalyzeUtils.distance(
                firstFrame.edgeCorners.get(0),
                firstFrame.edgeCorners.get(1)
        );
        // FIXME: 20.12.2019 only situations where edge is smaller side of briq face is supported
        //  and only TOP view supported
        //  can try to use edge from last frame to detect height
        final double briqHeight = briqWidth * parameters.briquetteSize.length / parameters.briquetteSize.depth;
        final double targetDirection = PI;
        final List<SmudgeInfo> smudges = new ArrayList<>();
        for (final FrameMeta frameMeta : keyFrames) {
            for (final Rect rect : frameMeta.smudges) {
                final long dt = frameMeta.timestamp - firstFrame.timestamp;
                final double dx = dt * avgSpeed * cos(direction);
                final double dy = dt * avgSpeed * sin(direction);

                double x = rect.x - dx - firstFrame.edgeCenter.x;
                double y = rect.y - dy - firstFrame.edgeCenter.y;
                double width = (rect.width + rect.height) / 2.;
                double height = (rect.width + rect.height) / 2.;

                final double rotation = targetDirection - direction;
                final double newX = x * cos(rotation) + y * sin(rotation);
                final double newY = y * cos(rotation) + x * sin(rotation);

                x = min(1, max(0, -newX) / briqHeight);
                y = min(1, max(0, newY + briqWidth / 2) / briqWidth);

                width /= briqWidth;
                height /= briqHeight;

                smudges.add(new SmudgeInfo(
                        x,
                        y,
                        width,
                        height,
                        targetDirection
                ));
            }
        }
        return smudges;
    }

    private double roundDirection(double direction) {
        direction = direction < 0 ? (2 * PI + direction) : direction;
        if (abs(direction - PI / 2) < PI / 4) {
            return PI / 2;
        } else if (abs(direction - PI) < PI / 4) {
            return PI;
        } else if (abs(direction - PI * 3 / 4) < PI / 4) {
            return PI * 3 / 4;
        } else {
            return 0;
        }
    }

    private FrameMeta meta(Frame frame, Contour contour, List<Rect> smudges) {
        final Point center = AnalyzeUtils.getCenter(contour.contour);

        List<Point> edgeCorners = contour.corners
                .stream()
                .filter(point -> filterBorders(point, frame))
                .collect(Collectors.toList());

        Point edgeCenter = null;
        if (edgeCorners.size() > 2) {
            edgeCorners = findEdge(edgeCorners, frame);
        }
        if (edgeCorners.size() == 2) {
            edgeCenter = AnalyzeUtils.getEdgeCenter(edgeCorners.get(0), edgeCorners.get(1), contour);
        }
        final Mat image = frame.getBgr().clone();
        final FrameMeta meta = FrameMeta.builder()
                .width(frame.getWidth())
                .height(frame.getHeight())
                .time(LocalDateTime.now())
                .timestamp(frame.getTimestamp())
                .smudges(smudges)
                .isEdge(edgeCenter != null)
                .center(center)
                .edgeCenter(edgeCenter)
                .edgeCorners(edgeCorners)
                .contour(contour)
                .image(image)
                .build();
        Application.CLEANER.register(
                meta,
                image::release
        );
        return meta;
    }

    private List<Point> findEdge(List<Point> edgeCorners, Frame frame) {
        final MatOfInt hull = new MatOfInt();
        Imgproc.convexHull(
                new MatOfPoint(edgeCorners.toArray(new Point[0])),
                hull
        );
        final List<Point> extraPoints = Arrays.stream(hull.toArray())
                .mapToObj(edgeCorners::get)
                .filter(point -> filterBorders(point, frame))
                .collect(Collectors.toList());
        return maxDistNeighbours(extraPoints);
    }

    private boolean filterBorders(Point point, Frame frame) {
        return point.x > frame.getWidth() * parameters.stitch.minDistanceFromBorderAbs
                && point.x < frame.getWidth() * (1 - parameters.stitch.minDistanceFromBorderAbs)
                && point.y > frame.getHeight() * parameters.stitch.minDistanceFromBorderAbs
                && point.y < frame.getHeight() * (1 - parameters.stitch.minDistanceFromBorderAbs);
    }

    private List<Point> maxDistNeighbours(List<Point> points) {
        if (points.size() < 2) {
            return points;
        }
        double maxDist = AnalyzeUtils.distance(points.get(0), points.get(1));
        Point[] result = new Point[]{points.get(0), points.get(1)};
        for (int i = 1; i < points.size(); i++) {
            final double distance = AnalyzeUtils.distance(points.get(i), points.get((i + 1) % points.size()));
            if (distance > maxDist) {
                maxDist = distance;
                result[0] = points.get(i);
                result[1] = points.get((i + 1) % points.size());
            }
        }
        return Arrays.asList(result);
    }

    private void updateSpeed() {
        // TODO: 17.12.2019 скорость считается только по первым брикетам, где была видна граница
        if (keyFrames.size() < 2) {
            throw new IllegalStateException("Not enough frames");
        }
        final FrameMeta first = keyFrames.get(0);
        FrameMeta last = keyFrames.get(1);
        if (!(first.isEdge && last.isEdge)) {
            throw new IllegalStateException("Not enough frames with visible edge");
        }
        for (int i = 2; i < keyFrames.size(); i++) {
            final FrameMeta frame = keyFrames.get(i);
            if (frame.isEdge) {
                last = frame;
            } else {
                break;
            }
        }
        avgSpeed = AnalyzeUtils.distance(
                first.edgeCenter,
                last.edgeCenter
        ) / (last.timestamp - first.timestamp);
        direction = AnalyzeUtils.angle(first.edgeCenter, last.edgeCenter);
    }

    @Nullable
    public LocalDateTime getFirstFrameTime() {
        if (keyFrames.isEmpty()) {
            return null;
        } else {
            return keyFrames.get(0).time;
        }
    }

    @Nullable
    public LocalDateTime getLastFrameTime() {
        if (keyFrames.isEmpty()) {
            return null;
        } else {
            return keyFrames.get(keyFrames.size() - 1).time;
        }
    }

    public SideReport getReport() {
        return SideReport.builder()
                .side(side)
                .avgSpeed(avgSpeed)
                .direction(direction)
                .firstFrameTime(getFirstFrameTime())
                .lastFrameTime(getLastFrameTime())
                .smudges(getSummarySmudges())
                .frames(Collections.emptyList()) // FIXME: 04.03.2020
                .rawSmudges(Collections.emptyList()) // FIXME: 04.03.2020
                .build();
    }

    @Override
    public String toString() {
        return getSide() + "{" +
                "finished:" + finished + ", " +
                "avgSpeed:" + avgSpeed + ", " +
                "direction:" + direction + ", " +
                "frames_count:" + keyFrames.size() + ", " +
                "smudges_count:" + (summarySmudges == null ? "na" : summarySmudges.size()) + ", " +
                "first_frame_time:" + getFirstFrameTime() + ", " +
                "last_frame_time:" + getLastFrameTime() +
                "}";
    }

    @Builder
    @Getter
    @RequiredArgsConstructor
    public static class FrameMeta {
        private final int width;
        private final int height;
        private final LocalDateTime time;
        /**
         * Frame timestamp from camera, nanos
         */
        private final long timestamp;
        private final List<Rect> smudges;
        /**
         * True, if 3 edges of side are visible
         */
        private final boolean isEdge;
        /**
         * center of briquette
         */
        private final Point center;
        private final Point edgeCenter;
        private final List<Point> edgeCorners;
        private final Contour contour;
        private final Mat image;
    }
}
