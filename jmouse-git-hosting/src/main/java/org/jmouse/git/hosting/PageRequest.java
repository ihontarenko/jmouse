package org.jmouse.git.hosting;

/**
 * One page of a listing.
 *
 * @param page the page number, from 1
 * @param size how many entries a page holds, 1 to 100 — the most either hosting serves at once
 */
public record PageRequest(int page, int size) {

    /** The largest page both hostings serve. */
    public static final int MAXIMUM_SIZE = 100;

    public PageRequest {
        if (page < 1 || size < 1 || size > MAXIMUM_SIZE) {
            throw new IllegalArgumentException("A page is numbered from 1 and holds 1 to " + MAXIMUM_SIZE + " entries.");
        }
    }

    /** The first page of this size. */
    public static PageRequest first(int size) {
        return new PageRequest(1, size);
    }

    /** The page after this one, of the same size. */
    public PageRequest next() {
        return new PageRequest(page + 1, size);
    }
}
