package org.jmouse.grabber;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * 📤 Where the things a run finds go.
 *
 * <p>A {@link Consumer} is enough for most callers, so the builder's {@code into(...)} takes one. The
 * interface exists so that a batching sink, a file sink or one that closes something at the end is a
 * swap rather than a wrapper around a lambda.</p>
 */
public interface ItemSink extends AutoCloseable {

    /**
     * 📤 A sink that hands each item to this consumer.
     */
    static ItemSink of(Consumer<Object> consumer) {
        return consumer::accept;
    }

    /**
     * 🕳️ A sink that discards everything — for a run whose handlers do their own storing.
     */
    static ItemSink discarding() {
        return item -> {
        };
    }

    /**
     * 📚 A sink that keeps everything, for a smoke or a small run.
     */
    static Collecting collecting() {
        return new Collecting();
    }

    void accept(Object item);

    @Override
    default void close() {
    }

    /**
     * 📚 Keeps every item, in order.
     */
    final class Collecting implements ItemSink {

        private final List<Object> items = new ArrayList<>();

        @Override
        public synchronized void accept(Object item) {
            items.add(item);
        }

        public synchronized List<Object> items() {
            return List.copyOf(items);
        }

        /**
         * 📚 Everything of this type, cast — for the run whose sink took one kind of thing.
         */
        public synchronized <T> List<T> itemsOf(Class<T> type) {
            return items.stream().filter(type::isInstance).map(type::cast).toList();
        }

        public synchronized int size() {
            return items.size();
        }
    }

}
