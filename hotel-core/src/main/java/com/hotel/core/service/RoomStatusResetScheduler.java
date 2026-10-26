package com.hotel.core.service;

import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.Objects;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Level;
import java.util.logging.Logger;

public final class RoomStatusResetScheduler implements AutoCloseable {

    private static final Logger LOGGER = Logger.getLogger(RoomStatusResetScheduler.class.getName());
    private static final long CHECK_INTERVAL_MINUTES = 1;

    private final RoomService roomService;
    private final Runnable onRoomsReset;
    private final ScheduledExecutorService executor;
    private final AtomicBoolean started = new AtomicBoolean();

    public RoomStatusResetScheduler(RoomService roomService, Runnable onRoomsReset) {
        this.roomService = Objects.requireNonNull(roomService, "Room service is required");
        this.onRoomsReset = Objects.requireNonNull(onRoomsReset, "Room reset callback is required");
        executor = Executors.newSingleThreadScheduledExecutor(task -> {
            Thread thread = new Thread(task, "dirty-room-status-reset");
            thread.setDaemon(true);
            return thread;
        });
    }

    public void start() {
        if (!started.compareAndSet(false, true)) {
            throw new IllegalStateException("Room status reset scheduler has already been started.");
        }
        executor.scheduleWithFixedDelay(
                this::resetExpiredRooms, 0, CHECK_INTERVAL_MINUTES, TimeUnit.MINUTES);
    }

    private void resetExpiredRooms() {
        try {
            int changedRooms = roomService.resetExpiredDirtyRooms(LocalDateTime.now());
            if (changedRooms > 0) {
                onRoomsReset.run();
            }
        } catch (SQLException exception) {
            LOGGER.log(Level.WARNING, "Unable to reset rooms that have been Dirty for one hour.", exception);
        } catch (RuntimeException exception) {
            LOGGER.log(Level.SEVERE, "Unexpected failure in the dirty-room reset task.", exception);
        }
    }

    @Override
    public void close() {
        executor.shutdownNow();
    }
}
