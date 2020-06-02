package ru.kpfu.itis.diploma.backend.service.analyze;

import ru.kpfu.itis.diploma.backend.api.form.StreamConfigurationForm;
import ru.kpfu.itis.diploma.backend.model.BriquetteSide;
import ru.kpfu.itis.diploma.backend.service.AsyncFrameSource;
import ru.kpfu.itis.diploma.backend.service.analyze.util.Contour;
import ru.kpfu.itis.diploma.backend.service.hardware.CameraDevice;
import ru.kpfu.itis.diploma.backend.service.hardware.Frame;
import ru.kpfu.itis.diploma.backend.util.FpsCounter;
import lombok.extern.log4j.Log4j2;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfPoint;
import org.opencv.core.Point;
import org.opencv.core.Rect;
import org.opencv.core.Scalar;
import org.opencv.core.Size;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

import static ru.kpfu.itis.diploma.backend.service.analyze.util.AnalyzeUtils.*;
import static ru.kpfu.itis.diploma.backend.service.analyze.util.AnalyzeUtils.contours;
import static ru.kpfu.itis.diploma.backend.service.analyze.util.AnalyzeUtils.drawContour;
import static ru.kpfu.itis.diploma.backend.service.analyze.util.AnalyzeUtils.drawRects;
import static ru.kpfu.itis.diploma.backend.service.analyze.util.AnalyzeUtils.sobel;
import static org.opencv.imgproc.Imgproc.CHAIN_APPROX_SIMPLE;
import static org.opencv.imgproc.Imgproc.COLOR_BGR2GRAY;
import static org.opencv.imgproc.Imgproc.RETR_EXTERNAL;
import static org.opencv.imgproc.Imgproc.THRESH_BINARY;
import static org.opencv.imgproc.Imgproc.boundingRect;
import static org.opencv.imgproc.Imgproc.cvtColor;
import static org.opencv.imgproc.Imgproc.dilate;
import static org.opencv.imgproc.Imgproc.drawContours;
import static org.opencv.imgproc.Imgproc.erode;
import static org.opencv.imgproc.Imgproc.fillPoly;
import static org.opencv.imgproc.Imgproc.findContours;
import static org.opencv.imgproc.Imgproc.threshold;

@Log4j2
public class Analyzer implements AsyncFrameSource {
    private final CameraDevice capture;
    private final Consumer<BriquetteInfo> eventConsumer;
    private final BriquetteSide side;
    private final AtomicBoolean started = new AtomicBoolean(false);

    public Analyzer(CameraDevice capture, Consumer<BriquetteInfo> eventConsumer, BriquetteSide side) {
        this.capture = capture;
        this.eventConsumer = eventConsumer;
        this.side = side;
        start();
        log.info("Started analyzer(camId: {})", capture.getId());
    }

    public void start() {
        if (!started.getAndSet(true)) {
            try {
                capture.getFrameAsync()
                        .thenAccept(this::onFrame);
            } catch (Throwable t) {
                log.error("Failed to start analyze: {}", t.toString(), t);
                started.set(false);
                throw t;
            }
        }
    }

    public void stop() {
        started.set(false);
    }

    private final FpsCounter fps = new FpsCounter();

    private void onFrame(Frame frame) {
        try {
            analyze(frame);
            fps.newFrame();
            if (fps.framesCount() > 0 && fps.framesCount() % 1000 == 0) {
                log.debug(
                        "Analyze fps: {} (frames count: {})",
                        fps.fps(),
                        fps.framesCount()
                );
            }
        } finally {
            if (started.get()) {
                // TODO: 04.03.2020 remove 100_000_000
                capture.getFrameAsync(frame.getTimestamp() + 100_000_000).thenAccept(this::onFrame);
            }
        }
    }

    private final ConcurrentLinkedQueue<FutureFrame> debugConsumers = new ConcurrentLinkedQueue<>();

    @Override
    public CompletableFuture<Frame> getFrameAsync(long skipTimestamp) {
        final FutureFrame future = new FutureFrame(skipTimestamp);
        debugConsumers.add(future);
        return future;
    }

    private BriquetteInfo currentBriquette;
    private long emptyFramesCount;
    private long lastAnalyzeTime;

    private void analyze(Frame frame) {
        Frame debug;
        if (!debugConsumers.isEmpty()) {
            debug = frame.clone();
        } else {
            debug = null;
        }
        try {
            Optional<Contour> contour = contours(frame.getBgr());
            if (contour.isPresent() && contour.get().corners.size() > 3) {
                if (currentBriquette == null && emptyFramesCount > 10) {
                    log.debug("New BriquetteInfo");
                    currentBriquette = new BriquetteInfo(AlgorithmParameters.TEST_A4_PAPER, side);
                }
                emptyFramesCount = 0;
                if (currentBriquette == null) {
                    if (debug != null) {
                        List<Rect> smudges = analyze(frame.getBgr(), contour.get(), debug.getBgr());
                        drawRects(debug.getBgr(), smudges, new Scalar(255, 255, 0), 1);
                        drawContour(debug.getBgr(), contour.get());
                    }
                    return;
                }
                log.trace("Analyze frame " + frame);
                List<Rect> smudges = analyze(frame.getBgr(), contour.get(), debug != null ? debug.getBgr() : null);
                if (debug != null) {
                    drawRects(debug.getBgr(), smudges, new Scalar(255, 255, 0), 1);
                    drawContour(debug.getBgr(), contour.get());
                }
                try {
                    currentBriquette.append(
                            frame,
                            contour.get(),
                            smudges
                    );
                    log.trace("Briq info updated");
                } catch (Exception e) {
                    log.warn(e.toString());
                    log.trace("Briq info released");
                    log.trace("New BriquetteInfo");
                    currentBriquette = new BriquetteInfo(AlgorithmParameters.TEST_A4_PAPER, side);
                    currentBriquette.append(frame, contour.get(), smudges);
                    log.trace("Briq info updated");
                }
                if (currentBriquette.isFinished()) {
                    log.trace("Briq finished");
                    onNewBriquetteInfo(currentBriquette);
                    log.trace("Briq finish event handled");
                    log.trace("Briq info released");
                    currentBriquette = null;
                }
            } else {
                emptyFramesCount++;
                if (emptyFramesCount > AlgorithmParameters.TEST_A4_PAPER.stitch.maxEmptyFrames
                        || System.currentTimeMillis() - lastAnalyzeTime > AlgorithmParameters.TEST_A4_PAPER.stitch.maxAnalyzePauseMillis) {
                    // log.debug("Too many empty frames");
                    if (currentBriquette != null) {
                        log.trace("Briq info released");
                        currentBriquette = null;
                        emptyFramesCount = 0;
                    }
                }
            }
        } finally {
            if (debug != null) {
                try {
                    onNext(debug, debugConsumers);
                } catch (Throwable ignore) {
                }
                debug.release();
            }
            lastAnalyzeTime = System.currentTimeMillis();
        }
    }

    private void onNewBriquetteInfo(BriquetteInfo currentBriquette) {
        if (eventConsumer != null) {
            try {
                eventConsumer.accept(currentBriquette);
            } catch (Throwable t) {
                log.warn("Unhandled exception in event consumer: {}", t.toString(), t);
            }
        }
    }

    private List<Rect> analyze(Mat img, Contour contour, Mat debugImg) {
        final Mat mask = new Mat(img.rows(), img.cols(), CvType.CV_8UC1, Scalar.all(0));
        final Mat clone = new Mat(img.rows(), img.cols(), img.type(), Scalar.all(0));
        final Mat gray = new Mat();
        try {
            // TODO: 16.12.2019 ouch
            final List<MatOfPoint> listOfMoP = Collections.singletonList(new MatOfPoint(contour.contour.toArray(new Point[0])));
            fillPoly(
                    mask,
                    listOfMoP,
                    Scalar.all(255)
            );
            img.copyTo(clone, mask);
            sobel(clone, clone, 1);

            // FIXME: 15.01.2020 края брикета - слепые зоны
            drawContours(
                    clone,
                    listOfMoP,
                    -1,
                    Scalar.all(0),
                    20
            );

            // cvtColor(clone, gray, COLOR_BGR2GRAY);
            // final Scalar mean = Core.mean(gray, mask);
            // final Core.MinMaxLocResult minMax = Core.minMaxLoc(gray, mask);
            // System.err.printf("%.4f\t%.4f%n", mean.val[0], minMax.maxVal);

            // Imgproc.calcHist();


            // Core.normalize(clone, clone, 0, 255, Core.NORM_MINMAX);
            if (debugImg != null) {
                clone.copyTo(debugImg);
            }
            cvtColor(clone, gray, COLOR_BGR2GRAY);
            threshold(gray, gray, 10, 255, THRESH_BINARY);

            dilate(gray, gray, 2, 2);
            erode(gray, gray, 1, 1);

            return findSmudges(
                    gray,
                    new Size(20, 20),
                    new Size(350, 350)
            );
        } finally {
            gray.release();
            clone.release();
            mask.release();
        }
    }

    private List<Rect> findSmudges(Mat gray, Size min, Size max) {
        final ArrayList<MatOfPoint> ctr = new ArrayList<>();
        final Mat hierarchy = new Mat();
        findContours(gray, ctr, hierarchy, RETR_EXTERNAL, CHAIN_APPROX_SIMPLE);
        List<Rect> smudges = new ArrayList<>();

        for (final MatOfPoint matOfPoint : ctr) {
            final Rect rect = boundingRect(matOfPoint);
            if (rect.width >= min.width && rect.height >= min.height
                    && rect.width < max.width && rect.height < max.height) {
                smudges.add(rect);
            }
            matOfPoint.release();
        }
        hierarchy.release();

        return smudges;
    }

    public void updateConf(StreamConfigurationForm conf, boolean immediately) {
        log.error("conf update not implemented yet");
        // conf.getHistogram().print(System.out);
    }
}
