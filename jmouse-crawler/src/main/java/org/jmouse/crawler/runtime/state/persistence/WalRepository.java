package org.jmouse.crawler.runtime.state.persistence;

public interface WalRepository<E> extends AutoCloseable {

    void append(E event);

    Iterable<E> readAll();

    void truncate();

    @Override
    default void close() { }

    default void flush() {}
}