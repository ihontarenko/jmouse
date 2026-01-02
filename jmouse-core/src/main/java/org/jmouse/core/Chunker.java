package org.jmouse.core;

import java.util.*;

final public class Chunker {

    private Chunker() {}

    public static <T, Inner extends Collection<T>,
            Outer extends Collection<Inner>> Outer split(
            Collection<T> source,
            int chunkSize,
            Class<Inner> innerType,
            Class<Outer> outerType
    ) {
        Objects.requireNonNull(source, "source must not be null");
        Objects.requireNonNull(innerType, "innerType must not be null");
        Objects.requireNonNull(outerType, "outerType must not be null");

        if (chunkSize <= 0) {
            throw new IllegalArgumentException("chunkSize must be > 0");
        }
        if (source.isEmpty()) {
            return CollectionFactory.createCollection(outerType, 0);
        }

        int   capacity = (source.size() + chunkSize - 1) / chunkSize;
        Outer chunks   = CollectionFactory.createCollection(outerType, capacity);

        Iterator<T> it = source.iterator();
        while (it.hasNext()) {
            Inner chunk = CollectionFactory.createCollection(innerType, Math.min(chunkSize, source.size()));
            for (int i = 0; i < chunkSize && it.hasNext(); i++) {
                chunk.add(it.next());
            }
            chunks.add(chunk);
        }

        return chunks;
    }

    public static <T, C extends Collection<T>> Collection<C> split(
            Collection<T> source,
            int chunkSize,
            Class<C> collectionType
    ) {
        return split(source, chunkSize, collectionType, List.class);
    }
}
