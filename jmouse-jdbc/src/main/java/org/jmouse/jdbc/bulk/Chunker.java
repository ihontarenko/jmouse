package org.jmouse.jdbc.bulk;

import org.jmouse.core.Contract;

import java.util.ArrayList;
import java.util.List;

public final class Chunker {

    private Chunker() {}

    public static <T> List<List<T>> split(List<T> input, int chunkSize) {
        if (input == null || input.isEmpty()) {
            return List.of();
        }

        Contract.state(chunkSize <= 0, "chunk-size must be > 0");

        int           size   = input.size();
        List<List<T>> chunks = new ArrayList<>((size + chunkSize - 1) / chunkSize);

        for (int i = 0; i < size; i += chunkSize) {
            chunks.add(input.subList(i, Math.min(i + chunkSize, size)));
        }

        return chunks;
    }
}