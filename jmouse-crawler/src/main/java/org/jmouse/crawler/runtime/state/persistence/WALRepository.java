package org.jmouse.crawler.runtime.state.persistence;

import java.io.Closeable;

public interface WALRepository<E> extends Closeable {

    void append(E event);

    Iterable<E> readAll();

    void flush();

    void rotate();

    @Override
    void close();
}