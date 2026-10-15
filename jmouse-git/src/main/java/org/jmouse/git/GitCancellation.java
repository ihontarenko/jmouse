package org.jmouse.git;

/**
 * A caller's request to stop an operation in flight — a scan that was cancelled, a shutdown. Polled between phases
 * and, while objects arrive and files are written, every time JGit asks whether to go on; so it must be cheap and
 * must not block. A signal that throws counts as cancelled.
 *
 * <p>A cancelled operation fails with {@link GitException.Reason#CANCELLED}. A remote listing in flight cannot be
 * interrupted and finishes within its timeout before the signal is seen.
 */
@FunctionalInterface
public interface GitCancellation {

    /** A signal that never cancels. */
    GitCancellation NONE = () -> false;

    /** Whether the caller wants the operation stopped. */
    boolean cancelled();

    /** Whether the signal asks to stop; a signal that throws is taken as asking to. */
    static boolean requested(GitCancellation cancellation) {
        try {
            return cancellation.cancelled();
        } catch (RuntimeException failure) {
            return true;
        }
    }
}
