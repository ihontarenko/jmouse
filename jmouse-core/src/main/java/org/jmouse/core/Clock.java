package org.jmouse.core;

@FunctionalInterface
public interface Clock {

    long nanoTime();

    static Clock system() {
        return System::nanoTime;
    }

}
