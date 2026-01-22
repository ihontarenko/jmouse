package org.jmouse.crawler.runtime.state.persistence.wrapper;

import org.jmouse.core.Verify;
import org.jmouse.crawler.api.Frontier;
import org.jmouse.crawler.api.ProcessingTask;
import org.jmouse.crawler.api.TaskId;
import org.jmouse.crawler.runtime.state.persistence.SnapshotPolicy;
import org.jmouse.crawler.runtime.state.persistence.SnapshotRepository;
import org.jmouse.crawler.runtime.state.persistence.WalRepository;
import org.jmouse.crawler.runtime.state.persistence.snapshot.FrontierSnapshot;
import org.jmouse.crawler.runtime.state.persistence.wal.StateEvent;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;

public final class PersistentFrontier implements Frontier {

    private final Frontier delegate;

    private final WalRepository<StateEvent>            walRepository;
    private final SnapshotRepository<FrontierSnapshot> snapshotRepository;
    private final SnapshotPolicy                       policy;

    private final Deque<ProcessingTask> mirror = new ArrayDeque<>();

    private volatile boolean restored = false;

    public PersistentFrontier(
            Frontier delegate,
            SnapshotRepository<FrontierSnapshot> snapshotRepository,
            WalRepository<StateEvent> walRepository,
            SnapshotPolicy policy,
            boolean restoreIfNeeded
    ) {
        this.delegate = Verify.nonNull(delegate, "delegate");
        this.snapshotRepository = Verify.nonNull(snapshotRepository, "snapshotRepo");
        this.walRepository = Verify.nonNull(walRepository, "wal");
        this.policy = Verify.nonNull(policy, "policy");

        if (restoreIfNeeded) {
            restore();
        }
    }

    /**
     * Restore state from snapshot + WAL into mirror, then repopulate delegate.
     *
     * <p>Contract: delegate is expected to be NEW/EMPTY when restore() is invoked.</p>
     */
    public void restore() {
        if (restored) {
            return;
        }

        mirror.clear();

        FrontierSnapshot snapshot = snapshotRepository.load();

        if (snapshot != null && snapshot.tasks() != null) {
            mirror.addAll(snapshot.tasks());
        }

        for (StateEvent event : walRepository.readAll()) {
            apply(event);
        }

        // repopulate delegate
        for (ProcessingTask task : mirror) {
            delegate.offer(task);
        }

        restored = true;
    }

    @Override
    public void offer(ProcessingTask task) {
        if (task != null) {
            walRepository.append(new StateEvent.FrontierOffered(task));
            mirror.addLast(task);
            delegate.offer(task);

            if (policy.shouldCheckpoint()) {
                checkpoint();
            }
        }
    }

    @Override
    public ProcessingTask poll() {
        ProcessingTask task = delegate.poll();

        if (task == null) {
            return null;
        }

        walRepository.append(new StateEvent.FrontierPolled(task.id()));

        removeById(mirror, task.id());

        if (policy.shouldCheckpoint()) {
            checkpoint();
        }

        return task;
    }

    @Override
    public int size() {
        return delegate.size();
    }

    public void checkpoint() {
        snapshotRepository.save(new FrontierSnapshot(List.copyOf(mirror)));
        walRepository.truncate();
    }

    private void apply(StateEvent event) {
        if (event == null) return;

        if (event instanceof StateEvent.FrontierOffered(ProcessingTask task)) {
            if (task != null) {
                mirror.addLast(task);
            }
            return;
        }

        if (event instanceof StateEvent.FrontierPolled(TaskId taskId)) {
            removeById(mirror, taskId);
        }
    }

    private static void removeById(Deque<ProcessingTask> deque, TaskId taskId) {
        if (taskId != null && !deque.isEmpty()) {
            for (var iterator = deque.iterator(); iterator.hasNext(); ) {
                ProcessingTask processingTask = iterator.next();
                if (taskId.equals(processingTask.id())) {
                    iterator.remove();
                    return;
                }
            }
        }
    }
}
