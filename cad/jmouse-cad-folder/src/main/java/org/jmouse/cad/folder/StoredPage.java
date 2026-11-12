package org.jmouse.cad.folder;

import java.util.List;
import java.util.function.Function;

/**
 * 📄 One page of a listing and how many rows there are in all.
 *
 * <p>Bounds are the caller's to check: a store pages whatever it is asked to.</p>
 *
 * @param rows  the rows of the page, in the listing's order
 * @param page  the page number, from zero
 * @param size  the page size asked for
 * @param total how many rows the whole listing has
 * @param <T>   the row type
 */
public record StoredPage<T>(List<T> rows, int page, int size, long total) {

    public StoredPage {
        rows = List.copyOf(rows);
    }

    /** One page of a whole listing, already in its order. */
    public static <T> StoredPage<T> of(List<T> listing, int page, int size) {
        int from = Math.min(page * size, listing.size());
        return new StoredPage<>(listing.subList(from, Math.min(from + size, listing.size())), page, size, listing.size());
    }

    /** The same page with every row turned into something else. */
    public <R> StoredPage<R> map(Function<? super T, ? extends R> mapping) {
        return new StoredPage<>(rows.stream().<R>map(mapping).toList(), page, size, total);
    }
}
