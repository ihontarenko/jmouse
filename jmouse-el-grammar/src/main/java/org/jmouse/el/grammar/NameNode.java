package org.jmouse.el.grammar;

import java.util.Optional;

/**
 * A name a line opens with or holds, and the index written against it: {@code R1}; {@code memory[256]}, a row of 256;
 * {@code q[0..7]}, the bits 0 to 7 of a bus. It is a word as written, with what its brackets say read apart.
 */
public class NameNode extends WordNode {

    private final String  base;
    private final Integer first;
    private final Integer last;

    /**
     * @param written the name as written, its index and all
     * @param first   the index's first or only number, or null for a name without one
     * @param last    the last of a range — {@code [0..7]} —, or null for a count or none
     */
    public NameNode(String written, String base, Integer first, Integer last) {
        super(written, false);
        this.base = base;
        this.first = first;
        this.last = last;
    }

    /** The name without its index: {@code q} for {@code q[0..7]}. */
    public String base() {
        return base;
    }

    public boolean indexed() {
        return first != null;
    }

    /** {@code [0..7]}: the first and last of a range. */
    public Optional<int[]> range() {
        return last == null ? Optional.empty() : Optional.of(new int[]{first, last});
    }

    /** {@code [256]}: how many. */
    public Optional<Integer> count() {
        return first != null && last == null ? Optional.of(first) : Optional.empty();
    }
}
