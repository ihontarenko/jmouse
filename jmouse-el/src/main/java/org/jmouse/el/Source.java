package org.jmouse.el;

/** Named read-only content view; token annotations do not change its content units. */
public interface Source {
    String getName();

    long contentLength();

    InputUnit unit();
}
