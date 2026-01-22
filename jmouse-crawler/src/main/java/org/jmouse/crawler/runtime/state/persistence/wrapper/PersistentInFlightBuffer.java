package org.jmouse.crawler.runtime.state.persistence.wrap;

import org.jmouse.core.Verify;
import org.jmouse.crawler.api.InFlightBuffer;
import org.jmouse.crawler.api.ProcessingTask;
import org.jmouse.crawler.api.TaskId;
import org.jmouse.crawler.runtime.state.persistence.SnapshotPolicy;
import org.jmouse.crawler.runtime.state.persistence.SnapshotRepository;
import org.jmouse.crawler.runtime.state.persistence.WalRepository;
import org.jmouse.crawler.runtime.state.persistence.events.InFlightEvent;
import org.jmouse.crawler.runtime.state.persistence.snapshot.InFlightSnapshot;

import java.time.Clock;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class PersistentInFlightBuffer implements InFlightBuffer {

    private final InFlightBuffer                       delegate;
    private final WalRepository<InFlightEvent>         wal;
    private final SnapshotRepository<InFlightSnapshot> snapshots;
    private final SnapshotPolicy                       snapshotPolicy;
    private final Clock                                clock;

    /**
     * Mirror is the single source of truth for restore/checkpoint.
     * Keep deterministic order for snapshots (LinkedHashMap).
     */
    private final Map<TaskId, ProcessingTask> mirror = new LinkedHashMap<>();

    public PersistentInFlightBuffer(
            InFlightBuffer delegate,
            WalRepository<InFlightEvent> wal,
            SnapshotRepository<InFlightSnapshot> snapshots,
            SnapshotPolicy snapshotPolicy,
            Clock clock
    ) {
        this(delegate, wal, snapshots, snapshotPolicy, clock, false);
    }

    public PersistentInFlightBuffer(
            InFlightBuffer delegate,
            WalRepository<InFlightEvent> wal,
            SnapshotRepository<InFlightSnapshot> snapshots,
            SnapshotPolicy snapshotPolicy,
            Clock clock,
            boolean restoreOnCreate
    ) {
        this.delegate = Verify.nonNull(delegate, "delegate");
        this.wal = Verify.nonNull(wal, "wal");
        this.snapshots = Verify.nonNull(snapshots, "snapshots");
        this.snapshotPolicy = Verify.nonNull(snapshotPolicy, "snapshotPolicy");
        this.clock = Verify.nonNull(clock, "clock");

        if (restoreOnCreate) {
            restore();
        }
    }

    /**
     * Restore state from snapshot + WAL replay.
     *
     * <p>Important: assumes delegate is NEW/EMPTY.</p>
     */
    public synchronized void restore() {
        mirror.clear();

        InFlightSnapshot snapshot = snapshots.load();
        for (ProcessingTask task : snapshot.tasks()) {
            if (task != null && task.id() != null) {
                mirror.put(task.id(), task);
            }
        }

        for (InFlightEvent event : wal.readAll()) {
            if (event == null) continue;

            if (event instanceof InFlightEvent.Put e) {
                ProcessingTask task = e.task();
                if (task != null && task.id() != null) {
                    mirror.put(task.id(), task);
                }
                continue;
            }

            if (event instanceof InFlightEvent.Remove e) {
                mirror.remove(e.taskId());
            }
        }

        // Fill delegate from mirror
        for (ProcessingTask task : mirror.values()) {
            delegate.put(task);
        }
    }

    @Override
    public synchronized void put(ProcessingTask task) {
        if (task == null || task.id() == null) {
            return;
        }

        // 1) persist intent
        wal.append(InFlightEvent.put(task));

        // 2) update mirror
        mirror.put(task.id(), task);

        // 3) apply to delegate
        delegate.put(task);

        // 4) maybe checkpoint
        if (snapshotPolicy.shouldCheckpoint()) {
            checkpoint();
        }
    }

    @Override
    public synchronized void remove(TaskId id) {
        if (id == null) {
            return;
        }

        wal.append(InFlightEvent.remove(id));
        mirror.remove(id);
        delegate.remove(id);

        if (snapshotPolicy.shouldCheckpoint()) {
            checkpoint();
        }
    }

    @Override
    public synchronized List<ProcessingTask> drainAll() {
        List<ProcessingTask> drained = delegate.drainAll();

        // Keep mirror consistent with drainAll semantics (it empties the buffer).
        mirror.clear();

        // This is a strong state transition; it is reasonable to checkpoint immediately.
        checkpoint();

        return drained;
    }

    @Override
    public synchronized int size() {
        return delegate.size();
    }

    /**
     * Write a snapshot of the current mirror and reset WAL.
     */
    public synchronized void checkpoint() {
        snapshots.save(new InFlightSnapshot(List.copyOf(mirror.values())));

        // WAL can be truncated after snapshot.
        wal.truncate();

        // If your WAL supports flush, this is a good time to flush.
        wal.flush();
    }
}
