package org.jmouse.cad;

import java.util.List;
import java.util.stream.IntStream;

/**
 * The index a name is written with: a bus's bits — {@code q[0..7]} —, or a row's slots — {@code memory[256]}, slots 0
 * to 255. Each of them is a name of its own, the name and its number joined: {@code q_0}, {@code memory_255}.
 */
public record Index(int first, int last) {

    private static final String JOIN = "_";

    public Index {
        if (first < 0 || last < first) {
            throw new IllegalArgumentException("An index runs from a first to a later last, from 0: [" + first + ".." + last + "]");
        }
    }

    /** {@code [count]}: slots 0 to count − 1. */
    public static Index counted(int count) {
        if (count < 1) {
            throw new IllegalArgumentException("A row holds at least one slot, not " + count);
        }
        return new Index(0, count - 1);
    }

    public int size() {
        return last - first + 1;
    }

    /** The names of its members: {@code q_0} … {@code q_7}. */
    public List<String> names(String base) {
        return IntStream.rangeClosed(first, last).mapToObj(number -> base + JOIN + number).toList();
    }
}
