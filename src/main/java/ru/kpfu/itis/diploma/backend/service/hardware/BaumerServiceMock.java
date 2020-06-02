package ru.kpfu.itis.diploma.backend.service.hardware;

import ru.kpfu.itis.diploma.backend.service.AsyncFrameSource;
import lombok.Getter;
import lombok.SneakyThrows;
import lombok.extern.log4j.Log4j2;
import org.bytedeco.baumer.DataStreamList;
import org.bytedeco.baumer.Device;
import org.bytedeco.baumer.DeviceEventControl;
import org.bytedeco.baumer.Interface;
import org.bytedeco.baumer.Node;
import org.bytedeco.baumer.NodeMap;
import org.bytedeco.baumer.Str;
import org.bytedeco.javacpp.Pointer;
import org.bytedeco.javacv.FFmpegFrameGrabber;
import org.bytedeco.javacv.OpenCVFrameConverter;
import org.opencv.core.Mat;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Consumer;

@Log4j2
@Service
@Profile("dev")
public class BaumerServiceMock implements BaumerService {
    private final Map<String, CameraDeviceMock> deviceMap = new HashMap<>();

    public BaumerServiceMock() {
        for (int i = 0; i < 1; i++) {
            final String deviceId = "M0CK_" + i;
            final DeviceMock device = new DeviceMock(deviceId);
            final CameraDeviceMock frameCapture = new CameraDeviceMock(device);
            deviceMap.put(deviceId, frameCapture);
        }
    }

    @Override
    public Optional<CameraDevice> findDevice(String deviceId) {
        return Optional.ofNullable(deviceMap.get(deviceId));
    }

    @Override
    public List<CameraDevice> getDeviceList() {
        return new ArrayList<>(deviceMap.values());
    }

    @Override
    public void handleConnectedDevices(Consumer<CameraDevice> consumer) {
        for (CameraDevice device : deviceMap.values()) {
            try {
                consumer.accept(device);
            } catch (Throwable t) {
                log.debug("Exception in handler: {}", t.toString(), t);
            }
        }
    }

    private static class CameraDeviceMock extends CameraDevice implements AsyncFrameSource, AutoCloseable {
        private static final String VIDEO_FILE_NAME = "mock_top_view.mkv";
        private final OpenCVFrameConverter.ToOrgOpenCvCoreMat converter = new OpenCVFrameConverter.ToOrgOpenCvCoreMat();

        @SneakyThrows
        private CameraDeviceMock(DeviceMock device) {
            super(device, new ReentrantLock());
            final FFmpegFrameGrabber grabber = FFmpegFrameGrabber.createDefault(VIDEO_FILE_NAME);
            grabber.start();
            new Thread(() -> {
                try {
                    long tsOffset = 0;
                    while (true) {
                        org.bytedeco.javacv.Frame frame = grabber.grabImage();
                        long ts = frame.timestamp;
                        long t = System.currentTimeMillis();
                        while ((frame = grabber.grabImage()) != null) {
                            long dts = frame.timestamp - ts;
                            long dt = System.currentTimeMillis() - t;
                            try {
                                Thread.sleep(Math.max(0, dts / 1000 - dt));
                            } catch (InterruptedException ignore) {
                            }
                            ts = frame.timestamp;
                            onFrame(frame, tsOffset);
                            t = System.currentTimeMillis();
                        }
                        try {
                            Thread.sleep(1000);
                        } catch (InterruptedException ignore) {
                        }
                        grabber.restart();
                        tsOffset += ts;
                    }
                } catch (Throwable throwable) {
                    throwable.printStackTrace();
                } finally {
                    try {
                        grabber.release();
                    } catch (Throwable t) {
                        log.error(t.toString(), t);
                    }
                }
            }, "mock-camera-device").start();
            device.open();
        }

        private void onFrame(org.bytedeco.javacv.Frame javacvFrame, long tsOffset) {
            List<FutureFrame> skipped = new ArrayList<>();
            Frame frame = null;
            try {
                final FutureFrame[] consumers = this.consumers.toArray(new FutureFrame[0]);
                if (consumers.length == 0) {
                    return;
                }
                this.consumers.removeAll(Arrays.asList(consumers));
                frame = toFrame(javacvFrame, tsOffset);
                for (FutureFrame future : consumers) {
                    if (frame.getTimestamp() <= future.getSkipTimestamp()) {
                        skipped.add(future);
                    } else {
                        future.complete(frame);
                    }
                }
            } finally {
                consumers.addAll(skipped);
                if (frame != null) {
                    frame.release();
                }
            }
        }

        private Frame toFrame(org.bytedeco.javacv.Frame frame, long tsOffset) {
            final Mat mat = converter.convert(frame);
            return new Frame(mat, (frame.timestamp + tsOffset) * 1000);
        }

        @Override
        protected void init() {
        }

        private final ConcurrentLinkedQueue<FutureFrame> consumers = new ConcurrentLinkedQueue<>();

        @Override
        public CompletableFuture<Frame> getFrameAsync(long skipTimestamp) {
            final FutureFrame future = new FutureFrame(skipTimestamp);
            consumers.add(future);
            return future;
        }

        @Override
        public void close() throws Exception {
            super.close();
        }
    }

    private static class DeviceMock extends Device {
        @Getter
        private final Str ID;
        @Getter
        private final Str name = new Str("Mock");
        @Getter
        private final Str vendor = new Str("Test");
        @Getter
        private final Str model = new Str("Mock");
        @Getter
        private final Str serialNumber;
        @Getter
        private final Str TLType = new Str("U3V");
        @Getter
        private final Str displayName = new Str("Device Mock Object");

        public DeviceMock(String id) {
            super(null);
            this.ID = new Str(id);
            this.serialNumber = new Str(id + "_sn");
        }

        @Override
        public DeviceEventControl asDeviceEventControl() {
            throw new UnsupportedOperationException();
        }

        private volatile boolean opened;

        @Override
        public void open() {
            opened = true;
        }

        @Override
        public void openExclusive() {
            opened = true;
        }

        @Override
        public void openReadOnly() {
            opened = true;
        }

        @Override
        public void close() {
            opened = false;
        }

        @Override
        public boolean isOpen() {
            return opened;
        }

        @Override
        public void startStacking(boolean bReplaceMode) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void writeStack() {
            throw new UnsupportedOperationException();
        }

        @Override
        public DataStreamList getDataStreams() {
            throw new UnsupportedOperationException();
        }

        @Override
        public Str getAccessStatus() {
            throw new UnsupportedOperationException();
        }

        @Override
        public long getPayloadSize() {
            throw new UnsupportedOperationException();
        }

        @Override
        public Node getRemoteNode(Str name) {
            throw new UnsupportedOperationException();
        }

        @Override
        public NodeMap getRemoteNodeTree() {
            throw new UnsupportedOperationException();
        }

        @Override
        public NodeMap getRemoteNodeList() {
            throw new UnsupportedOperationException();
        }

        @Override
        public Str getRemoteConfigurationFile() {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean isUpdateModeAvailable() {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean isUpdateModeActive() {
            throw new UnsupportedOperationException();
        }

        @Override
        public void setUpdateMode(boolean bActive, Str pcCustomKey) {
            throw new UnsupportedOperationException();
        }

        @Override
        public Node getUpdateNode(Str name) {
            throw new UnsupportedOperationException();
        }

        @Override
        public NodeMap getUpdateNodeTree() {
            throw new UnsupportedOperationException();
        }

        @Override
        public NodeMap getUpdateNodeList() {
            throw new UnsupportedOperationException();
        }

        @Override
        public Str getUpdateConfigurationFile() {
            throw new UnsupportedOperationException();
        }

        @Override
        public Interface getParent() {
            throw new UnsupportedOperationException();
        }

        @Override
        public Pointer getReserved() {
            throw new UnsupportedOperationException();
        }

        @Override
        public Node getNode(Str name) {
            throw new UnsupportedOperationException();
        }

        @Override
        public NodeMap getNodeTree() {
            throw new UnsupportedOperationException();
        }

        @Override
        public NodeMap getNodeList() {
            throw new UnsupportedOperationException();
        }
    }
}
