package org.jmouse.git;

import org.eclipse.jgit.lib.ProgressMonitor;

import java.nio.file.Path;
import java.util.Optional;

/**
 * Enforces a call's deadline, size ceiling and the caller's cancellation while JGit works. JGit asks
 * {@link #isCancelled()} as each object arrives and as files are written, so the signal is polled then, and the
 * directory is measured — at most every quarter second; the transfer stops the moment one of them says so.
 *
 * <p>Measurement is per object, so a single object larger than what is left of the ceiling is only caught once it
 * has arrived; the check after the operation and the deadline bound that case.
 */
final class BoundedTransferMonitor implements ProgressMonitor {

    private static final long MEASUREMENT_INTERVAL_NANOSECONDS = 250_000_000L;

    private final Path              directory;
    private final long              maximumBytes;
    private final long              deadlineNanoseconds;
    private final GitCancellation   cancellation;
    private boolean                 cancellationHonoured = true;
    private long                    lastMeasurementNanoseconds;
    private boolean                 measured;
    private GitException.Reason     refusal;

    BoundedTransferMonitor(Path directory, GitLimits limits, GitCancellation cancellation) {
        this.directory = directory;
        this.maximumBytes = limits.maximumBytes();
        this.deadlineNanoseconds = System.nanoTime() + limits.timeout().toNanos();
        this.cancellation = cancellation;
    }

    /** Throws when the deadline has passed or the caller cancelled — between phases, where JGit is not asking. */
    void requireContinuing() {
        if (System.nanoTime() - deadlineNanoseconds > 0) {
            refusal = GitException.Reason.TIMED_OUT;
        } else if (cancellationHonoured && GitCancellation.requested(cancellation)) {
            refusal = GitException.Reason.CANCELLED;
        }
        if (refusal != null) {
            throw new GitException(refusal);
        }
    }

    /** From here on the caller's signal is no longer polled: what follows must run to the end to stay consistent. */
    void stopHonouringCancellation() {
        cancellationHonoured = false;
    }

    /** The reason this monitor stopped the transfer, if it did. */
    Optional<GitException> refusal() {
        if (refusal == null) {
            return Optional.empty();
        }
        return Optional.of(new GitException(refusal));
    }

    @Override
    public boolean isCancelled() {
        if (refusal != null) {
            return true;
        }
        long now = System.nanoTime();
        if (now - deadlineNanoseconds > 0) {
            refusal = GitException.Reason.TIMED_OUT;
            return true;
        }
        if (cancellationHonoured && GitCancellation.requested(cancellation)) {
            refusal = GitException.Reason.CANCELLED;
            return true;
        }
        if (!measured || now - lastMeasurementNanoseconds >= MEASUREMENT_INTERVAL_NANOSECONDS) {
            measured = true;
            lastMeasurementNanoseconds = now;
            if (CheckoutDirectory.bytesOf(directory) > maximumBytes) {
                refusal = GitException.Reason.TOO_LARGE;
                return true;
            }
        }
        return false;
    }

    @Override
    public void start(int totalTasks) {
    }

    @Override
    public void beginTask(String title, int totalWork) {
    }

    @Override
    public void update(int completed) {
    }

    @Override
    public void endTask() {
    }

    @Override
    public void showDuration(boolean enabled) {
    }
}
