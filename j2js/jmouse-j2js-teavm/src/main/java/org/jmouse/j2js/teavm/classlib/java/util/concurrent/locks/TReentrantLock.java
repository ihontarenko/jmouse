package org.jmouse.j2js.teavm.classlib.java.util.concurrent.locks;

import java.util.Date;
import java.util.concurrent.TimeUnit;

/**
 * {@code java.util.concurrent.locks.ReentrantLock} in the browser, where there is one thread: the lock is always free to the one
 * that asks, and counts how many times it is held, so it is released as often as it was taken — as the JDK's, for one thread.
 * Released more often than held, it refuses as the JDK's does.
 *
 * <p>Waiting on one of its conditions is refused: with one thread, nothing would ever signal it, and waiting would never end.</p>
 *
 * @author Ivan Hontarenko (Mr. Jerry Mouse)
 * @author ihontarenko@gmail.com
 */
public class TReentrantLock implements TLock {

    private final boolean fair;
    private       int     holds;

    public TReentrantLock() {
        this(false);
    }

    public TReentrantLock(boolean fair) {
        this.fair = fair;
    }

    @Override
    public void lock() {
        holds++;
    }

    @Override
    public void lockInterruptibly() {
        holds++;
    }

    @Override
    public boolean tryLock() {
        holds++;
        return true;
    }

    @Override
    public boolean tryLock(long time, TimeUnit unit) {
        holds++;
        return true;
    }

    @Override
    public void unlock() {
        if (holds == 0) {
            throw new IllegalMonitorStateException();
        }
        holds--;
    }

    @Override
    public TCondition newCondition() {
        return new SingleThreadCondition();
    }

    public int getHoldCount() {
        return holds;
    }

    public boolean isHeldByCurrentThread() {
        return holds > 0;
    }

    public boolean isLocked() {
        return holds > 0;
    }

    public final boolean isFair() {
        return fair;
    }

    public final boolean hasQueuedThreads() {
        return false;
    }

    public final int getQueueLength() {
        return 0;
    }

    @Override
    public String toString() {
        return super.toString() + (holds > 0 ? "[Locked by the page's thread]" : "[Unlocked]");
    }

    /** A condition with nothing to wait for: signals reach no one, and a wait is refused. */
    private static final class SingleThreadCondition implements TCondition {

        private static IllegalMonitorStateException refused() {
            return new IllegalMonitorStateException("A page has one thread: nothing would ever signal a wait");
        }

        @Override
        public void await() {
            throw refused();
        }

        @Override
        public void awaitUninterruptibly() {
            throw refused();
        }

        @Override
        public long awaitNanos(long nanosTimeout) {
            throw refused();
        }

        @Override
        public boolean await(long time, TimeUnit unit) {
            throw refused();
        }

        @Override
        public boolean awaitUntil(Date deadline) {
            throw refused();
        }

        @Override
        public void signal() {
        }

        @Override
        public void signalAll() {
        }
    }
}
