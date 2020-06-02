package ru.kpfu.itis.diploma.backend.service;

import ru.kpfu.itis.diploma.backend.api.form.MjpegStreamRequest;
import ru.kpfu.itis.diploma.backend.api.form.StreamConfigurationForm;
import ru.kpfu.itis.diploma.backend.exception.NotFoundException;
import ru.kpfu.itis.diploma.backend.model.BriquetteSide;
import ru.kpfu.itis.diploma.backend.service.analyze.AnalyzeService;
import ru.kpfu.itis.diploma.backend.service.analyze.Analyzer;
import ru.kpfu.itis.diploma.backend.service.hardware.BaumerService;
import ru.kpfu.itis.diploma.backend.service.hardware.CameraDevice;
import ru.kpfu.itis.diploma.backend.service.hardware.Frame;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.bytedeco.baumer.Device;
import org.opencv.core.MatOfByte;
import org.opencv.imgcodecs.Imgcodecs;
import org.springframework.stereotype.Service;
import org.springframework.web.socket.BinaryMessage;
import org.springframework.web.socket.WebSocketSession;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

@Log4j2
@Service
@RequiredArgsConstructor
public class MjpegStreamingService {
    private final ExecutorService executorService;
    private final BaumerService baumerService;
    private final AnalyzeService analyzeService;
    private final ConcurrentHashMap<WebSocketSession, ConcurrentSkipListSet<StreamReceiver>> connections = new ConcurrentHashMap<>();

    public void startStreaming(WebSocketSession ws, MjpegStreamRequest request) {
        final StreamReceiver streamReceiver = StreamReceiver.create(executorService, ws, request, baumerService, analyzeService);
        connections.compute(
                ws,
                (k, v) -> {
                    if (v == null) {
                        v = new ConcurrentSkipListSet<>();
                    }
                    v.add(streamReceiver);
                    return v;
                }
        );
        streamReceiver.start();
    }

    public void stopStreaming(WebSocketSession ws, String streamId) {
        connections.computeIfPresent(
                ws,
                (k, v) -> {
                    v.stream()
                            .filter(streamReceiver -> streamReceiver.getStreamId().equals(streamId))
                            .forEach(StreamReceiver::stop);
                    if (v.isEmpty()) {
                        v = null;
                    }
                    return v;
                }
        );
    }

    public void stopStreaming(WebSocketSession ws) {
        final ConcurrentSkipListSet<StreamReceiver> receivers = connections.remove(ws);
        if (receivers != null) {
            receivers.forEach(StreamReceiver::stop);
        }
    }

    public void reconfigureTestStream(WebSocketSession ws, String  streamId, StreamConfigurationForm conf) {
        connections.computeIfPresent(
                ws,
                (k, v) -> {
                    v.stream()
                            .filter(streamReceiver -> streamReceiver.getStreamId().equals(streamId))
                            .filter(streamReceiver -> streamReceiver instanceof ReconfigurableStream)
                            .map(streamReceiver -> ((ReconfigurableStream) streamReceiver))
                            .forEach(reconfigurableStream -> reconfigurableStream.reconfigure(conf));
                    if (v.isEmpty()) {
                        v = null;
                    }
                    return v;
                }
        );
    }

    private static abstract class StreamReceiver implements Comparable<StreamReceiver> {
        protected AtomicInteger idSeq = new AtomicInteger(0);
        protected AtomicBoolean started = new AtomicBoolean(false);

        public static StreamReceiver create(ExecutorService executor,
                                            WebSocketSession ws,
                                            MjpegStreamRequest request,
                                            BaumerService baumerService,
                                            AnalyzeService analyzeService) {
            switch (request.getType()) {
                case DEBUG:
                    return new DebugStream(executor, ws, request, baumerService, analyzeService);
                case CONFIGURE:
                    return new ReconfigurableStream(executor, ws, request, baumerService);
                default:
                    return new RawCameraStream(executor, ws, request, baumerService);
            }
        }

        public String getId() {
            return getWebSocket().getId() + getStreamId() + idSeq.getAndIncrement();
        }

        public void start() {
            if (!started.getAndSet(true)) {
                requestNextFrame(0);
            }
        }

        protected abstract void requestNextFrame(long timestamp);

        public void stop() {
            started.set(false);
        }

        protected abstract String getStreamId();

        protected abstract WebSocketSession getWebSocket();

        protected abstract ExecutorService getExecutor();

        protected void sendImage(Frame frame) {
            if (started.getPlain() && getWebSocket().isOpen()) {
                MatOfByte encodedMat = new MatOfByte();
                try {
                    if (!Imgcodecs.imencode(".jpg", frame.getBgr(), encodedMat)) {
                        log.warn("Frame encoding failed");
                        return;
                    }
                    final byte[] encoded = encodedMat.toArray();
                    final byte[] streamIdBytes = getStreamId().getBytes();
                    final byte[] bytes = new byte[4 + streamIdBytes.length + encoded.length];
                    // deviceId.length
                    bytes[0] = (byte) (0xFF000000 & streamIdBytes.length >> 24);
                    bytes[1] = (byte) (0x00FF0000 & streamIdBytes.length >> 16);
                    bytes[2] = (byte) (0x0000FF00 & streamIdBytes.length >> 8);
                    bytes[3] = (byte) (0x000000FF & streamIdBytes.length);
                    // deviceId
                    System.arraycopy(streamIdBytes, 0, bytes, 4, streamIdBytes.length);
                    // jpg data
                    // TODO: 09.02.2020 copy to bytebuffer?
                    System.arraycopy(encoded, 0, bytes, 4 + streamIdBytes.length, encoded.length);

                    try {
                        // FIXME: 12.12.2019 sync
                        synchronized (getWebSocket()) {
                            getWebSocket().sendMessage(new BinaryMessage(bytes));
                        }
                    } catch (Throwable t) {
                        log.warn("Unable to send frame to client: {}", t.toString());
                    }
                } catch (Throwable t) {
                    log.error("Unable to handle frame: {}", t.toString(), t);
                } finally {
                    frame.release();
                    encodedMat.release();
                }
            }
        }

        protected void onFrame(Frame frame) {
            final Frame clone = frame.clone();
            try {
                getExecutor().execute(() -> {
                    try {
                        sendImage(clone);
                    } finally {
                        clone.release();
                    }
                    if (started.get() && getWebSocket().isOpen()) {
                        requestNextFrame(frame.getTimestamp());
                    }
                });
            } catch (Exception e) {
                clone.release();
            }
        }

        @Override
        public int compareTo(@NonNull MjpegStreamingService.StreamReceiver o) {
            return o.getId().compareTo(this.getId());
        }
    }

    @Getter
    private static class RawCameraStream extends StreamReceiver {
        @Getter(AccessLevel.PROTECTED)
        private final ExecutorService executor;
        @Getter(AccessLevel.PROTECTED)
        private final WebSocketSession webSocket;
        @Getter(AccessLevel.PROTECTED)
        private final String streamId;
        private final CameraDevice cameraDevice;
        private final MjpegStreamRequest request;

        public RawCameraStream(@NonNull ExecutorService executor,
                               @NonNull WebSocketSession ws,
                               @NonNull MjpegStreamRequest request,
                               @NonNull BaumerService baumerService) {
            this.executor = executor;
            this.webSocket = ws;
            this.request = request;
            this.streamId = request.getStreamId();
            this.cameraDevice = baumerService.findDevice(request.getDeviceId())
                    .orElseThrow(() -> new NotFoundException(Device.class, request.getDeviceId()));
        }

        @Override
        protected void requestNextFrame(long timestamp) {
            cameraDevice.getFrameAsync(timestamp).thenAccept(this::onFrame);
        }
    }

    @Getter
    private static class DebugStream extends StreamReceiver {
        @Getter(AccessLevel.PROTECTED)
        private final ExecutorService executor;
        @Getter(AccessLevel.PROTECTED)
        private final WebSocketSession webSocket;
        @Getter(AccessLevel.PROTECTED)
        private final String streamId;
        private final MjpegStreamRequest request;
        private final Analyzer analyzer;

        public DebugStream(@NonNull ExecutorService executor,
                           @NonNull WebSocketSession ws,
                           @NonNull MjpegStreamRequest request,
                           @NonNull BaumerService baumerService,
                           @NonNull AnalyzeService analyzeService) {
            this.executor = executor;
            this.request = request;
            this.webSocket = ws;
            this.streamId = request.getStreamId();
            CameraDevice cameraDevice = baumerService.findDevice(request.getDeviceId())
                    .orElseThrow(() -> new NotFoundException(Device.class, request.getDeviceId()));
            this.analyzer = analyzeService.analyzer(cameraDevice);
        }

        @Override
        protected void requestNextFrame(long timestamp) {
            analyzer.getFrameAsync(timestamp).thenAccept(this::onFrame);
        }
    }

    @Getter
    private static class ReconfigurableStream extends StreamReceiver {
        @Getter(AccessLevel.PROTECTED)
        private final ExecutorService executor;
        @Getter(AccessLevel.PROTECTED)
        private final WebSocketSession webSocket;
        @Getter(AccessLevel.PROTECTED)
        private final String streamId;
        private final CameraDevice cameraDevice;
        private final MjpegStreamRequest request;
        private final Analyzer analyzer;

        public ReconfigurableStream(@NonNull ExecutorService executor,
                                    @NonNull WebSocketSession ws,
                                    @NonNull MjpegStreamRequest request,
                                    @NonNull BaumerService baumerService) {
            this.executor = executor;
            this.request = request;
            this.webSocket = ws;
            this.streamId = request.getStreamId();
            this.cameraDevice = baumerService.findDevice(request.getDeviceId())
                    .orElseThrow(() -> new NotFoundException(Device.class, request.getDeviceId()));
            // FIXME: 04.03.2020 set correct side
            this.analyzer = new Analyzer(cameraDevice, null, BriquetteSide.TOP);
        }

        public void reconfigure(StreamConfigurationForm conf) {
            analyzer.updateConf(conf, true);
        }

        @Override
        public void stop() {
            super.stop();
            analyzer.stop();
        }

        @Override
        protected void requestNextFrame(long timestamp) {
            analyzer.getFrameAsync(timestamp).thenAccept(this::onFrame);
        }
    }
}
