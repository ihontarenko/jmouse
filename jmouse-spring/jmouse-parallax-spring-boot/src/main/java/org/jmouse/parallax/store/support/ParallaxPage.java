package org.jmouse.parallax.store.support;

import org.jmouse.parallax.jpa.StoredPage;
import org.jmouse.parallax.problem.ParallaxProblem;

import java.util.List;
import java.util.function.Function;

/**
 * One page of a Parallax listing, and the bounds every listing accepts. Its wire shape, {@code {items, page, size,
 * total}}, is the one every listing of the host answers with too — the interface reads one shape from both.
 */
public record ParallaxPage<T>(List<T> items, int page, int size, long total) {

    public static final int MAXIMUM_SIZE = 200;

    /** Refuses a page number or size outside what every listing accepts. */
    public static void requireBounds(int page, int size) {
        if (page < 0 || page > 100_000) {
            throw ParallaxProblem.invalid("A page number must be between 0 and 100000.");
        }
        if (size < 1 || size > MAXIMUM_SIZE) {
            throw ParallaxProblem.invalid("A page size must be between 1 and " + MAXIMUM_SIZE + ".");
        }
    }

    /** A page the Parallax stores answered with, its rows mapped to what the listing shows. */
    public static <S, T> ParallaxPage<T> of(StoredPage<S> page, Function<S, T> mapping) {
        return new ParallaxPage<>(page.rows().stream().map(mapping).toList(), page.page(), page.size(), page.total());
    }
}
