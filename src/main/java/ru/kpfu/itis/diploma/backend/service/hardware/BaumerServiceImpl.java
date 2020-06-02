package ru.kpfu.itis.diploma.backend.service.hardware;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.bytedeco.baumer.Buffer;
import org.bytedeco.baumer.BufferList;
import org.bytedeco.baumer.Device;
import org.bytedeco.baumer.DeviceList;
import org.bytedeco.baumer.Interface;
import org.bytedeco.baumer.InterfaceEventControl;
import org.bytedeco.baumer.InterfaceList;
import org.bytedeco.baumer.Node;
import org.bytedeco.baumer.NodeMap;
import org.bytedeco.baumer.PnPEvent;
import org.bytedeco.baumer.System;
import org.bytedeco.baumer.SystemList;
import org.bytedeco.baumer.global.baumer;
import org.bytedeco.javacpp.Pointer;
import org.jetbrains.annotations.NotNull;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Queue;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Consumer;

@Service
@Profile("prod")
@Log4j2
@RequiredArgsConstructor
public class BaumerServiceImpl implements BaumerService {
    private final ReentrantLock lock = new ReentrantLock(true);
    private final System system;
    private final PnPEventHandler pnpEventHandler = new PnPEventHandler();
    private final Queue<Consumer<CameraDevice>> connectListeners = new ArrayDeque<>();

    private Map<String, InterfaceEventControl> eventControlMap = new HashMap<>();

    public BaumerServiceImpl() {
        lock.lock();
        try {
            initDestructor();
            system = initUsb3System();
            initEventHandlers();
            updateDeviceList();
        } finally {
            lock.unlock();
        }
    }

    private void initDestructor() {
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            lock.lock();
            try {
                releaseDevices();
                releaseInterfaces();
                releaseSystems();
            } finally {
                lock.unlock();
            }
        }));
    }

    private void releaseSystems() {
        try {
            if (system.isOpen()) {
                system.close();
            }
        } catch (Throwable t) {
            log.warn("Unable to close system {}: {}", system.getID(), t.toString());
        }
    }

    private void releaseInterfaces() {
        for (Interface i : iterate(system.getInterfaces())) {
            try {
                if (i.isOpen()) {
                    i.close();
                }
            } catch (Throwable t) {
                log.warn("Unable to close interface {}: {}", i.getID(), t.toString());
            }
        }
    }

    private void releaseDevices() {
        for (CameraDevice d : devices.values()) {
            try {
                d.close();
            } catch (Throwable t) {
                log.warn("Unable to close device {}: {}", d.getId(), t.toString());
            }
        }
    }

    private void initEventHandlers() {
        for (Interface i : iterate(system.getInterfaces())) {
            if (!i.isOpen()) {
                i.open();
            }
            final InterfaceEventControl eventControl = eventControlMap.computeIfAbsent(
                    i.getID().toString(),
                    s -> i.asInterfaceEventControl()
            );
            eventControl.registerPnPEvent(baumer.EventMode.EVENTMODE_EVENT_HANDLER);
            eventControl.registerPnPEventHandler(
                    null,
                    pnpEventHandler
            );
        }
    }

    private Map<String, CameraDevice> devices = new HashMap<>();

    private void onDeviceDisconnected(String serial) {
        log.info("Trying to release device sn:{}", serial);
        try {
            final Optional<CameraDevice> cameraDevice = devices.values()
                    .stream()
                    .filter(d -> d.getSerialNumber().equals(serial))
                    .findFirst();
            if (cameraDevice.isPresent()) {
                cameraDevice.get().close();
            }
            log.info("Device sn:{} released", serial);
        } catch (Throwable t){
            log.info("Unable to release disconnected device sn:{}", serial);
        }
    }

    private void updateDeviceList() {
        lock.lock();
        log.info("Updating device list");
        try {
            List<CameraDevice> disconnected = new ArrayList<>(devices.values());
            List<CameraDevice> connected = new ArrayList<>();
            for (Interface i : iterate(system.getInterfaces())) {
                for (Device dev : iterate(i.getDevices())) {
                    final String id = dev.getID().toString();
                    final CameraDevice wrapper = devices.get(id);
                    if (wrapper == null) {
                        final CameraDevice cameraDevice = new CameraDevice(dev, lock);
                        devices.put(id, cameraDevice);
                        connected.add(cameraDevice);
                    } else {
                        disconnected.remove(wrapper);
                    }
                }
            }
            for (final CameraDevice dev : disconnected) {
                log.info("Closing device {}", dev.getId());
                try {
                    dev.close();
                } catch (Throwable ignore) {
                }
                devices.remove(dev.getId());
            }
            fireConnectedEvents(connected, connectListeners);
        } finally {
            lock.unlock();
        }
    }

    private System initUsb3System() {
        for (System system : iterate(SystemList.getInstance())) {
            final String systemType = system.getTLType().toString();
            log.info(
                    "Found system name: '{}'; type: '{}'",
                    system.getFileName().toString(),
                    systemType
            );
            if (systemType.equals("U3V")) {
                log.info("System type is 'U3V'(USB3). Opening...");
                if (!system.isOpen()) {
                    system.open();
                    log.info("Opened USB3 system: {}", system.getDisplayName().toString());
                } else {
                    log.debug("System already opened");
                }
                return system;
            }
        }
        throw new IllegalStateException("No supported systems found");
    }

    @Override
    public Optional<CameraDevice> findDevice(String deviceId) {
        lock.lock();
        try {
            return Optional.ofNullable(devices.get(deviceId));
        } finally {
            lock.unlock();
        }
    }

    @Override
    public List<CameraDevice> getDeviceList() {
        lock.lock();
        try {
            return new ArrayList<>(devices.values());
        } finally {
            lock.unlock();
        }
    }

    @Override
    public void handleConnectedDevices(Consumer<CameraDevice> consumer) {
        lock.lock();
        try {
            fireConnectedEvents(new ArrayList<>(devices.values()), Collections.singletonList(consumer));
            connectListeners.add(consumer);
        } finally {
            lock.unlock();
        }
    }

    private void fireConnectedEvents(List<CameraDevice> devices, Collection<Consumer<CameraDevice>> consumers) {
        for (CameraDevice device : devices) {
            for (Consumer<CameraDevice> consumer : consumers) {
                try {
                    consumer.accept(device);
                } catch (Throwable t) {
                    log.debug("Exception in handler: {}", t.toString(), t);
                }
            }
        }
    }

    private final class PnPEventHandler extends org.bytedeco.baumer.PnPEventHandler {
        @Override
        public void call(Pointer callBackOwner, PnPEvent event) {
            if (event == null) {
                return;
            }
            log.info(
                    "device sn:{} - {}",
                    event.getSerialNumber(),
                    event.getPnPType().intern() == baumer.PnPType.PNPTYPE_DEVICEADDED
                            ? "connected"
                            : "disconnected"
            );
            if (event.getPnPType().intern() == baumer.PnPType.PNPTYPE_DEVICEREMOVED) {
                BaumerServiceImpl.this.onDeviceDisconnected(event.getSerialNumber().toString());
            } else {
                BaumerServiceImpl.this.updateDeviceList();
            }
        }
    }


    //
    //
    //
    //
    //   Utils
    //
    //
    //
    //


    public static SystemListIterable iterate(SystemList pList) {
        return new SystemListIterable(pList);
    }

    public static InterfaceListIterable iterate(InterfaceList pList) {
        return new InterfaceListIterable(pList);
    }

    public static DeviceListIterable iterate(DeviceList pList) {
        return new DeviceListIterable(pList);
    }

    public static BufferListIterable iterate(BufferList pList) {
        return new BufferListIterable(pList);
    }

    public static NodeMapIterable iterate(NodeMap pMap) {
        return new NodeMapIterable(pMap);
    }

    @RequiredArgsConstructor
    public static class NodeMapIterable implements Iterable<Node> {
        private final NodeMap pMap;

        @NotNull
        @Override
        public NodeMapIterator iterator() {
            return new NodeMapIterator(pMap);
        }
    }

    public static class NodeMapIterator implements Iterator<Node> {
        private final NodeMap.iterator iterator;
        private final NodeMap.iterator end;

        public NodeMapIterator(NodeMap pMap) {
            iterator = pMap.begin();
            end = pMap.end();
        }

        @Override
        public boolean hasNext() {
            return iterator.notEquals(end);
        }

        @Override
        public Node next() {
            try {
                return iterator.access().second().second();
            } finally {
                iterator.increment();
            }
        }
    }

    @RequiredArgsConstructor
    public static class SystemListIterable implements Iterable<System> {
        private final SystemList pList;

        @NotNull
        @Override
        public SystemListIterator iterator() {
            return new SystemListIterator(pList);
        }
    }

    @RequiredArgsConstructor
    public static class InterfaceListIterable implements Iterable<Interface> {
        private final InterfaceList pList;

        @NotNull
        @Override
        public InterfaceListIterator iterator() {
            return new InterfaceListIterator(pList);
        }
    }

    @RequiredArgsConstructor
    public static class DeviceListIterable implements Iterable<Device> {
        private final DeviceList pList;

        @NotNull
        @Override
        public DeviceListIterator iterator() {
            return new DeviceListIterator(pList);
        }
    }

    @RequiredArgsConstructor
    public static class BufferListIterable implements Iterable<Buffer> {
        private final BufferList pList;

        @NotNull
        @Override
        public BufferListIterator iterator() {
            return new BufferListIterator(pList);
        }
    }

    public static class SystemListIterator implements Iterator<System> {
        private final SystemList.iterator iterator;
        private final SystemList.iterator end;

        public SystemListIterator(SystemList pList) {
            pList.refresh();
            iterator = pList.begin();
            end = pList.end();
        }

        @Override
        public boolean hasNext() {
            return iterator.notEquals(end);
        }

        @Override
        public System next() {
            try {
                return iterator.access().second();
            } finally {
                iterator.increment();
            }
        }
    }

    public static class InterfaceListIterator implements Iterator<Interface> {
        private final InterfaceList.iterator iterator;
        private final InterfaceList.iterator end;

        public InterfaceListIterator(InterfaceList pList) {
            pList.refresh(100);
            iterator = pList.begin();
            end = pList.end();
        }

        @Override
        public boolean hasNext() {
            return iterator.notEquals(end);
        }

        @Override
        public Interface next() {
            try {
                return iterator.access().second();
            } finally {
                iterator.increment();
            }
        }
    }

    public static class DeviceListIterator implements Iterator<Device> {
        private final DeviceList.iterator iterator;
        private final DeviceList.iterator end;

        public DeviceListIterator(DeviceList pList) {
            pList.refresh(100);
            iterator = pList.begin();
            end = pList.end();
        }

        @Override
        public boolean hasNext() {
            return iterator.notEquals(end);
        }

        @Override
        public Device next() {
            try {
                return iterator.access().second();
            } finally {
                iterator.increment();
            }
        }
    }

    public static class BufferListIterator implements Iterator<Buffer> {
        private final BufferList.iterator iterator;
        private final BufferList.iterator end;

        public BufferListIterator(BufferList pList) {
            iterator = pList.begin();
            end = pList.end();
        }

        @Override
        public boolean hasNext() {
            return iterator.notEquals(end);
        }

        @Override
        public Buffer next() {
            try {
                return iterator.access().second();
            } finally {
                iterator.increment();
            }
        }
    }
}
