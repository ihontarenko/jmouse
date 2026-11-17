package org.jmouse.j2js.teavm.classlib.java.util.concurrent.locks;

import java.util.concurrent.TimeUnit;

/**
 * {@code java.util.concurrent.locks.Lock} in the browser, as the JDK declares it.
 *
 * @author Ivan Hontarenko (Mr. Jerry Mouse)
 * @author ihontarenko@gmail.com
 */
public interface TLock {

    void lock();

    void lockInterruptibly() throws InterruptedException;

    boolean tryLock();

    boolean tryLock(long time, TimeUnit unit) throws InterruptedException;

    void unlock();

    TCondition newCondition();
}
