package com.hotel.desktop.roomMonitoring;

import com.hotel.core.model.RoomMonitoringSnapshot;
import com.hotel.core.service.RoomService;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ExecutionException;
import java.util.function.Consumer;
import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;

public final class RoomMonitoringDataSource {

    private final RoomService roomService;
    private final List<Consumer<State>> listeners = new ArrayList<>();
    private State state = new State(null, false, null, null);
    private SwingWorker<RoomMonitoringSnapshot, Void> worker;
    private boolean refreshPending;

    public RoomMonitoringDataSource(RoomService roomService) {
        this.roomService = Objects.requireNonNull(roomService, "roomService");
    }

    public void addListener(Consumer<State> listener) {
        Objects.requireNonNull(listener, "listener");
        runOnEdt(() -> {
            listeners.add(listener);
            listener.accept(state);
        });
    }

    public void refresh() {
        runOnEdt(() -> {
            if (worker != null) {
                refreshPending = true;
                return;
            }

            state = new State(state.snapshot, true, null, state.lastUpdated);
            notifyListeners();
            worker = new SwingWorker<>() {
                @Override
                protected RoomMonitoringSnapshot doInBackground() throws Exception {
                    return roomService.getMonitoringSnapshot();
                }

                @Override
                protected void done() {
                    RoomMonitoringSnapshot snapshot = null;
                    Throwable error = null;
                    try {
                        snapshot = get();
                    } catch (InterruptedException exception) {
                        Thread.currentThread().interrupt();
                        error = exception;
                    } catch (ExecutionException exception) {
                        error = exception.getCause() == null ? exception : exception.getCause();
                    }

                    worker = null;
                    if (error == null) {
                        state = new RoomMonitoringDataSource.State(
                                snapshot, false, null, LocalDateTime.now());
                    } else {
                        state = new RoomMonitoringDataSource.State(null, false, error, null);
                    }
                    notifyListeners();

                    if (refreshPending) {
                        refreshPending = false;
                        refresh();
                    }
                }
            };
            worker.execute();
        });
    }

    private void notifyListeners() {
        for (Consumer<State> listener : new ArrayList<>(listeners)) {
            listener.accept(state);
        }
    }

    private static void runOnEdt(Runnable action) {
        if (SwingUtilities.isEventDispatchThread()) {
            action.run();
        } else {
            SwingUtilities.invokeLater(action);
        }
    }

    public static final class State {

        private final RoomMonitoringSnapshot snapshot;
        private final boolean loading;
        private final Throwable error;
        private final LocalDateTime lastUpdated;

        private State(
                RoomMonitoringSnapshot snapshot,
                boolean loading,
                Throwable error,
                LocalDateTime lastUpdated) {
            this.snapshot = snapshot;
            this.loading = loading;
            this.error = error;
            this.lastUpdated = lastUpdated;
        }

        public RoomMonitoringSnapshot getSnapshot() {
            return snapshot;
        }

        public boolean isLoading() {
            return loading;
        }

        public Throwable getError() {
            return error;
        }

        public LocalDateTime getLastUpdated() {
            return lastUpdated;
        }
    }
}
