package org.jmouse.crawler.runtime.state.persistence.wrap;

import org.jmouse.core.Verify;
import org.jmouse.crawler.api.*;
import org.jmouse.crawler.runtime.state.persistence.SnapshotPolicy;
import org.jmouse.crawler.runtime.state.persistence.SnapshotRepository;
import org.jmouse.crawler.runtime.state.persistence.WALRepository;
import org.jmouse.crawler.runtime.state.persistence.events.InFlightEvent;
import org.jmouse.crawler.runtime.state.persistence.snapshot.InFlightSnapshot;

import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

public final class PersistentInFlightBuffer implements InFlightBuffer {

    private final InFlightBuffer                       delegate;
    private final WALRepository<InFlightEvent>         wal;
    private final SnapshotRepository<InFlightSnapshot> snapshots;
    private final SnapshotPolicy                       snapshotPolicy;
    private final Clock                                clock;

    private final    AtomicLong operationsSinceSnapshot = new AtomicLong(0);
    private volatile Instant    lastSnapshotAt          = null;

    public PersistentInFlightBuffer(
            InFlightBuffer delegate,
            WALRepository<InFlightEvent> wal,
            SnapshotRepository<InFlightSnapshot> snapshots,
            SnapshotPolicy snapshotPolicy,
            Clock clock
    ) {
        this.delegate = Verify.nonNull(delegate, "delegate");
        this.wal = Verify.nonNull(wal, "wal");
        this.snapshots = Verify.nonNull(snapshots, "snapshots");
        this.snapshotPolicy = Verify.nonNull(snapshotPolicy, "snapshotPolicy");
        this.clock = Verify.nonNull(clock, "clock");
    }

    @Override
    public void put(ProcessingTask task) {
        delegate.put(task);
        wal.append(InFlightEvent.put(task));
        onOperation();
    }

    @Override
    public void remove(TaskId id) {
        delegate.remove(id);
        wal.append(InFlightEvent.remove(id));
        onOperation();
    }

    @Override
    public List<ProcessingTask> drainAll() {
        return delegate.drainAll();
    }

    @Override
    public int size() {
        return delegate.size();
    }

    private void onOperation() {
        long ops = operationsSinceSnapshot.incrementAndGet();
        Instant now = clock.instant();
        if (snapshotPolicy.shouldSnapshot(ops, now, lastSnapshotAt)) {
            // TODO: snapshot requires reading internal state. For now, caller (bootstrapper) will snapshot.
            operationsSinceSnapshot.set(0);
            lastSnapshotAt = now;
            wal.flush();
        }
    }
}
