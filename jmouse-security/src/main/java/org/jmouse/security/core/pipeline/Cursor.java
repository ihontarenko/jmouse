package org.jmouse.security.core.pipeline;

public interface Cursor<E> {
    Step next(E input) throws Exception;
}