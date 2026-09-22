package org.jmouse.grabber;

import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

/**
 * 📊 One run, while it is happening and after it has finished.
 *
 * <p>Two things live here. The <b>figures</b>, which a listener reports and a handler may read, and
 * the <b>stop switch</b> — so a run can be ended by something other than a handler's verdict: a
 * timer, a signal, a caller who has seen enough.</p>
 *
 * <p>Everything on it is thread-safe, because the whole point of the figures is that several workers
 * are contributing to them at once.</p>
 */
public final class GrabRun {

    private final Instant startedAt = Instant.now();

    private final AtomicLong queued  = new AtomicLong();
    private final AtomicLong fetched = new AtomicLong();
    private final AtomicLong skipped = new AtomicLong();
    private final AtomicLong failed  = new AtomicLong();
    private final AtomicLong retried = new AtomicLong();
    private final AtomicLong emitted = new AtomicLong();

    private final AtomicBoolean         stopped      = new AtomicBoolean();
    private final AtomicReference<String> stopReason = new AtomicReference<>();
    private final AtomicReference<Instant> finishedAt = new AtomicReference<>();

    /**
     * 🛑 Ends the run. Everything in flight finishes; nothing new starts.
     */
    public void stop(String reason) {
        if (stopped.compareAndSet(false, true)) {
            stopReason.set(reason);
        }
    }

    public boolean isStopped() {
        return stopped.get();
    }

    /**
     * 🛑 Why it was stopped, or {@code null} when it was not.
     */
    public String stopReason() {
        return stopReason.get();
    }

    /**
     * ⏱️ How long the run has been going, or how long it took.
     */
    public Duration elapsed() {
        Instant end = finishedAt.get();
        return Duration.between(startedAt, end == null ? Instant.now() : end);
    }

    public Instant startedAt() {
        return startedAt;
    }

    public long queued() {
        return queued.get();
    }

    public long fetched() {
        return fetched.get();
    }

    public long skipped() {
        return skipped.get();
    }

    public long failed() {
        return failed.get();
    }

    public long retried() {
        return retried.get();
    }

    public long emitted() {
        return emitted.get();
    }

    void countQueued() {
        queued.incrementAndGet();
    }

    void countFetched() {
        fetched.incrementAndGet();
    }

    void countSkipped() {
        skipped.incrementAndGet();
    }

    void countFailed() {
        failed.incrementAndGet();
    }

    void countRetried() {
        retried.incrementAndGet();
    }

    void countEmitted() {
        emitted.incrementAndGet();
    }

    void markFinished() {
        finishedAt.compareAndSet(null, Instant.now());
    }

    @Override
    public String toString() {
        return "GrabRun[queued=" + queued() + " fetched=" + fetched() + " emitted=" + emitted()
                + " skipped=" + skipped() + " failed=" + failed() + " in " + elapsed().toMillis() + "ms"
                + (isStopped() ? " stopped: " + stopReason() : "") + "]";
    }

}
