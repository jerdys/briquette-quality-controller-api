package ru.kpfu.itis.diploma.backend.service.hardware;

import ru.kpfu.itis.diploma.backend.Utils;
import ru.kpfu.itis.diploma.backend.service.AsyncFrameSource;
import ru.kpfu.itis.diploma.backend.service.hardware.model.DeviceExposureTimeProperty;
import ru.kpfu.itis.diploma.backend.util.FpsCounter;
import lombok.Getter;
import lombok.NonNull;
import lombok.extern.log4j.Log4j2;
import org.bytedeco.baumer.Buffer;
import org.bytedeco.baumer.BufferList;
import org.bytedeco.baumer.DataStream;
import org.bytedeco.baumer.DataStreamEventControl;
import org.bytedeco.baumer.DataStreamList;
import org.bytedeco.baumer.Device;
import org.bytedeco.baumer.NewBufferEventHandler;
import org.bytedeco.baumer.Node;
import org.bytedeco.baumer.Str;
import org.bytedeco.baumer.global.baumer;
import org.bytedeco.javacpp.BytePointer;
import org.bytedeco.javacpp.Pointer;
import org.jetbrains.annotations.Nullable;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.imgproc.Imgproc;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.locks.ReentrantLock;

@Log4j2
public class CameraDevice implements AsyncFrameSource, AutoCloseable {
    private static final int BUFFERS_COUNT = 2;

    private static final Str PIXEL_FORMAT_STR = new Str("PixelFormat");
    private static final Str BAYER_RG_8_PIXEL_FORMAT_STR = new Str("BayerRG8");
    private static final Str ACQUISITION_START_STR = new Str("AcquisitionStart");
    private static final Str EXPOSURE_TIME_STR = new Str("ExposureTime");
    private static final Str TRIGGER_MODE_STR = new Str("TriggerMode");
    private static final Str TRIGGER_SOURCE_STR = new Str("TriggerSource");
    private static final Str OFF_STR = new Str("Off");
    private static final Str ACQUISITION_ABORT_STR = new Str("AcquisitionAbort");
    private static final Str ACQUISITION_STOP_STR = new Str("AcquisitionStop");

    private static final Str PAYLOAD_IMAGE_STR = new Str(baumer.BGAPI2_PAYLOADTYPE_IMAGE);

    private final Device device;
    @Getter
    private final String id;
    @Getter
    private final String displayName;
    @Getter
    private final String serialNumber;
    @Getter
    private final String model;
    @Getter
    private final String type;
    private final String deviceName;
    private final ReentrantLock lock;

    private final ConcurrentLinkedQueue<FutureFrame> consumers = new ConcurrentLinkedQueue<>();
    private volatile boolean initialized = false;
    private volatile boolean closed = false;

    CameraDevice(Device device, ReentrantLock lock) {
        this.device = device;
        this.lock = lock;

        this.id = device.getID().toString();
        this.displayName = device.getDisplayName().toString();
        this.serialNumber = device.getSerialNumber().toString();
        this.model = device.getModel().toString();
        this.type = device.getTLType().toString();
        this.deviceName = String.format("sn:%s;id:%s", serialNumber, id);

        init();
    }

    void init() {
        if (initialized) {
            return;
        }
        lock.lock();
        try {
            if (initialized) {
                return;
            }
            log.debug("Initializing device '{}'#{}", deviceName, id);
            if (!device.isOpen()) {
                device.open();
            }
            setupDeviceParameters();
            final DataStream dataStream = initDataStream();

            initBufferHandler(dataStream);
            startAcquisition(dataStream);
            initialized = true;
        } catch (Throwable t) {
            log.error("Unable to initialize camera: {}", t.toString(), t);
            try {
                release();
            } catch (Throwable t1) {
                log.debug("Unable to release device after failed initialization: {}", t.toString());
            }
            throw t;
        } finally {
            lock.unlock();
        }
    }

    public DeviceExposureTimeProperty getExposureTime() {
        lock.lock();
        try {
            if (!device.isOpen()) {
                device.open();
            }
            final Node node = device.getRemoteNode(EXPOSURE_TIME_STR);
            return new DeviceExposureTimeProperty(
                    node.getDouble(),
                    node.getDoubleMin(),
                    node.getDoubleMax(),
                    node.getUnit().toString()
            );
        } finally {
            lock.unlock();
        }
    }

    public void setExposureTime(double value) {
        lock.lock();
        try {
            final DeviceExposureTimeProperty exposureTimeProperty = getExposureTime();
            final Node node = device.getRemoteNode(EXPOSURE_TIME_STR);
            node.setDouble(Utils.inBounds(exposureTimeProperty.getMin(), exposureTimeProperty.getMax(), value));
        } finally {
            lock.unlock();
        }
    }

    private void setupDeviceParameters() {
        setParam(TRIGGER_MODE_STR, OFF_STR);
        setParam(TRIGGER_SOURCE_STR, OFF_STR);
        setParam(PIXEL_FORMAT_STR, BAYER_RG_8_PIXEL_FORMAT_STR);
    }

    private void setParam(Str nodeName, Str value) {
        try {
            final Node node = this.device.getRemoteNode(nodeName);
            final Str currentVal = node.getValue();
            if (currentVal.notEquals(value)) {
                node.setString(value);
                log.debug("{}: {}", nodeName.toString(), value);
            } else {
                log.debug("{}: {}", nodeName.toString(), currentVal);
            }
        } catch (Throwable t) {
            log.warn(
                    "Unable to set parameter {} value to {}: {}",
                    nodeName.toString(),
                    value.toString(),
                    t.toString()
            );
        }
    }

    private DataStream initDataStream() {
        final DataStreamList dataStreams = device.getDataStreams();
        dataStreams.refresh();
        log.info("Found {} data streams on device {}", dataStreams.size(), deviceName);
        if (dataStreams.size() == 0) {
            // FIXME: 10/24/19
            throw new RuntimeException("No dataStreams found on device");
        }

        final DataStream dataStream = dataStreams.begin().access().second();
        if (!dataStream.isOpen()) {
            dataStream.open();
        }
        final String dataStreamId = dataStream.getID().toString();
        log.info("Using data stream '{}'", dataStreamId);
        initBuffers(dataStream);
        return dataStream;
    }

    private void startAcquisition(DataStream dataStream) {
        dataStream.startAcquisitionContinuous();
        this.device.getRemoteNode(ACQUISITION_START_STR).execute();
    }

    private final FpsCounter fps = new FpsCounter();
    private NewBufferEventHandler handler;
    @SuppressWarnings("FieldCanBeLocal")
    private DataStreamEventControl eventControl;

    private void initBufferHandler(DataStream dataStream) {
        eventControl = dataStream.asDataStreamEventControl();
        eventControl.registerNewBufferEvent(baumer.EVENTMODE_EVENT_HANDLER);

        if (handler == null) {
            handler = new NewBufferEventHandler() {
                @Override
                public void call(Pointer p, Buffer buffer) {
                    try {
                        if (buffer == null || buffer.getIsIncomplete()) {
                            return;
                        }
                        if (buffer.getPayloadType().equals(PAYLOAD_IMAGE_STR)) {
                            fps.newFrame();
                            if (fps.framesCount() > 0 && fps.framesCount() % 1000 == 0) {
                                log.debug(
                                        "Camera fps: {} (frames count: {})",
                                        fps.fps(),
                                        fps.framesCount()
                                );
                            }
                            onBufferHandled(buffer);
                        }
                    } catch (Throwable t) {
                        log.error(t.toString(), t);
                    } finally {
                        try {
                            if (buffer != null && !buffer.getIsQueued()) {
                                buffer.queueBuffer();
                            }
                        } catch (Throwable t) {
                            log.warn("Unable to queue buffer: {}", t.toString());
                        }
                    }
                }
            };
        }
        eventControl.registerNewBufferEventHandler(dataStream, handler);
    }

    private void onBufferHandled(Buffer buffer) {
        final Mat mat = toMat(buffer);
        final Mat bgr = new Mat();
        List<FutureFrame> skipped = new ArrayList<>();
        try {
            final FutureFrame[] consumers = this.consumers.toArray(new FutureFrame[0]);
            if (consumers.length == 0) {
                return;
            }
            /*
            camera's pixel format BayerRG corresponds to BayerBG in OpenCV
            because for cam Bayer pattern is components from the 0 and 1 rows and 0 and 1 cols,
            but OpenCV uses 1 and 2 rows and 1 and 2 cols:
                0 1 2 3 4 ...
              0 R G R G R
              1 G B G B G
              2 R G R G R
              3 G B G B G
              .           .
              .             .
            */
            Imgproc.demosaicing(mat, bgr, Imgproc.COLOR_BayerBG2BGR);
            this.consumers.removeAll(Arrays.asList(consumers));
            final Frame frame = new Frame(bgr, buffer.getTimestamp());
            for (FutureFrame future : consumers) {
                if (buffer.getTimestamp() <= future.getSkipTimestamp()) {
                    skipped.add(future);
                } else {
                    future.complete(frame);
                }
            }
        } finally {
            consumers.addAll(skipped);
            mat.release();
            bgr.release();
            buffer.queueBuffer();
        }
    }

    /**
     * to prevent GC
     */
    @SuppressWarnings("MismatchedQueryAndUpdateOfCollection")
    private final List<Buffer> nativeBuffers = new ArrayList<>();

    private void initBuffers(DataStream dataStream) {
        log.debug("Initializing buffers for stream '{}'", dataStream.getID().toString());
        BufferList bufferList = dataStream.getBufferList();
        log.debug("{} buffers already allocated. Allocating {} new buffers", bufferList.size(), BUFFERS_COUNT);
        Buffer pBuffer = null;

        for (int i = 0; i < BUFFERS_COUNT; i++) {
            pBuffer = new Buffer();
            nativeBuffers.add(pBuffer);
            bufferList.add(pBuffer);
        }
        log.debug(
                "Announced {} buffers using {}bytes",
                bufferList.getAnnouncedCount(),
                pBuffer.getMemSize() * bufferList.getAnnouncedCount()
        );

        nativeBuffers.forEach(Buffer::queueBuffer);
        log.debug("Queued {} buffers", bufferList.getQueuedCount());
    }

    // TODO: 11/1/19 auto run this method
    private void release() {
        lock.lock();
        try {
            closed = true;
            initialized = false;
            log.debug("Releasing device {}", deviceName);
            try {
                stopDevice();
                log.debug("Device {} stopped", deviceName);
            } catch (Exception e) {
                log.warn("Unable to stop device {}: {}", deviceName, e.toString());
            }
            try {
                final DataStreamList dataStreams = device.getDataStreams();
                release(dataStreams);
            } catch (Exception e) {
                log.warn(
                        "Unable to release dataStreams on device {}: {}",
                        deviceName,
                        e.toString()
                );
            }
            try {
                if (device.isOpen()) {
                    device.close();
                }
                log.debug("Device {} released", deviceName);
            } catch (Exception e) {
                log.warn("Unable to close device {}: {}", deviceName, e.toString());
            }
        } finally {
            lock.unlock();
        }
    }

    private void release(@Nullable DataStreamList dataStreams) {
        if (dataStreams != null && dataStreams.size() > 0) {
            for (DataStreamList.iterator iterator = dataStreams.begin(); !iterator.equals(dataStreams.end()); iterator.increment()) {
                final DataStream dataStream = iterator.access().second();
                release(dataStream);
            }
        }
    }

    private void release(@NonNull DataStream dataStream) {
        final String dataStreamId = dataStream.getID().toString();
        final BufferList bufferList = dataStream.getBufferList();
        log.debug("Stopping acquisition");
        dataStream.abortAcquisition();
        dataStream.stopAcquisition();
        log.debug("Releasing data stream {}: {} buffers", dataStreamId, bufferList.size());
        bufferList.discardAllBuffers();
        log.debug("Data stream {} stopped, buffers discarded", dataStreamId);
        while (bufferList.size() > 0) {
            final Buffer buffer = bufferList.begin().access().second();
            try {
                bufferList.revokeBuffer(buffer);
                buffer.deallocate();
                nativeBuffers.remove(buffer);
            } catch (Exception e) {
                log.error("Unable to release buffer {}: {}", buffer, e.toString());
            }
        }
        log.debug("Buffers released");
        dataStream.close();
        log.debug("Data stream {} released", dataStreamId);
    }

    private void stopDevice() {
        if (device.getRemoteNodeList().getNodePresent(ACQUISITION_ABORT_STR)) {
            device.getRemoteNode(ACQUISITION_ABORT_STR).execute();
            log.debug("Device {} acquisition aborted", deviceName);
        }

        device.getRemoteNode(ACQUISITION_STOP_STR).execute();
        log.debug("Device {} acquisition stopped", deviceName);
    }

    /**
     * Все обработчики выполняются последовательно.
     * consumer будет вызван в потоке, работающем с камерой.
     * Если буффер планируется долго обрабатывать - копируй его и обрабатывай в другом потоке.
     */
    public CompletableFuture<Frame> getFrameAsync(long skipTimestamp) {
        if (!initialized) {
            if (closed) {
                // FIXME: 04.03.2020 Все останавливается, если камера по какой-то причине отключилась и вернулась в строй
                throw new IllegalStateException("Device closed");
            }
            init();
        }
        final FutureFrame future = new FutureFrame(skipTimestamp);
        consumers.add(future);
        return future;
    }

    @Override
    public void close() throws Exception {
        release();
    }

    private Mat toMat(Buffer buffer) {
        return new Mat(
                (int) buffer.getHeight(),
                (int) buffer.getWidth(),
                CvType.CV_8UC1,
                new BytePointer(new Pointer(buffer
                        .getMemPtr()
                        .position(0)
                        .capacity(buffer.getSizeFilled())
                        .limit(buffer.getSizeFilled())
                )).asByteBuffer()
        );
    }
}
