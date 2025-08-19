package org.jmouse.web.security;

public class SimpleBreaker {

    private final int  failureThreshold;
    private final long openMillis;
    private       int  failures = 0;
    private       long openedAt = -1;

    public SimpleBreaker(int failureThreshold, long openMillis) {
        this.failureThreshold = failureThreshold;
        this.openMillis = openMillis;
    }

    public synchronized boolean allow() {
        if (openedAt < 0) {
            return true;
        }

        if (System.currentTimeMillis() - openedAt > openMillis) {
            openedAt = -1;
            failures = 0;
            return true;
        }

        return false;
    }

    public synchronized void recordSuccess() {
        failures = 0;
        openedAt = -1;
    }

    public synchronized void recordFailure() {
        if (++failures >= failureThreshold) {
            openedAt = System.currentTimeMillis();
        }
    }
}

