package org.jmouse.crawler.runtime.state.checkpoint.frontier;

import org.jmouse.core.Verify;
import org.jmouse.core.bind.TypedValue;
import org.jmouse.core.mapping.Mapper;
import org.jmouse.crawler.api.Frontier;
import org.jmouse.crawler.api.ProcessingTask;
import org.jmouse.crawler.api.TaskId;
import org.jmouse.crawler.runtime.state.checkpoint.SnapshotRepository;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

public final class CheckpointingFrontier implements Frontier {

    public interface CheckpointPolicy {
        boolean shouldCheckpoint();
        static CheckpointPolicy never() { return () -> false; }
    }

    private final Frontier                             delegate;
    private final SnapshotRepository<FrontierSnapshot> snapshots;
    private final Mapper                               mapper;
    private final CheckpointPolicy                     policy;

    private final    List<Map<String, Object>> mirror   = new ArrayList<>();
    private volatile boolean                   restored = false;

    public CheckpointingFrontier(
            Frontier delegate,
            SnapshotRepository<FrontierSnapshot> snapshots,
            Mapper mapper,
            CheckpointPolicy policy,
            boolean restoreOnCreate
    ) {
        this.delegate  = Verify.nonNull(delegate, "delegate");
        this.snapshots = Verify.nonNull(snapshots, "snapshots");
        this.mapper    = Verify.nonNull(mapper, "mapper");
        this.policy    = Verify.nonNull(policy, "policy");

        if (restoreOnCreate) {
            restore();
        }
    }

    public void restore() {
        if (restored) {
            return;
        }

        mirror.clear();

        FrontierSnapshot snapshot = snapshots.load();

        if (snapshot != null && snapshot.tasks() != null) {
            mirror.addAll(snapshot.tasks());
        }

        for (Map<String, Object> raw : mirror) {
            delegate.offer(
                    mapper.map(raw, ProcessingTask.class)
            );
        }

        restored = true;
    }

    public void checkpoint() {
        snapshots.save(new FrontierSnapshot(List.copyOf(mirror)));
    }

    @Override
    public void offer(ProcessingTask task) {
        if (task == null) {
            return;
        }

        Map<String, Object> values = mapper.map(
                task,
                TypedValue.ofMap(String.class, Object.class).getType()
        );

        mirror.add(values);
        delegate.offer(task);

        if (policy.shouldCheckpoint()) {
            checkpoint();
        }
    }

    @Override
    public ProcessingTask poll() {
        ProcessingTask task = delegate.poll();

        if (task == null) {
            return null;
        }

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

    private static void removeById(List<Map<String, Object>> mirror, TaskId id) {
        if (id == null || mirror.isEmpty()) {
            return;
        }

        String expected = id.value();

        for (Iterator<Map<String, Object>> iterator = mirror.iterator(); iterator.hasNext(); ) {
            Map<String, Object> values = iterator.next();
            String actual = extractTaskIdValue(values);
            if (expected.equals(actual)) {
                iterator.remove();
                return;
            }
        }
    }

    /**
     * Supports both:
     * - id: "jMouse_..."
     * - id: { value: "jMouse_..." }
     */
    private static String extractTaskIdValue(Map<String, Object> task) {
        if (task == null) return null;

        Object id = task.get("id");
        if (id instanceof String s) {
            return s;
        }
        if (id instanceof Map<?, ?> m) {
            Object v = m.get("value");
            return (v instanceof String s) ? s : null;
        }
        return null;
    }
}
