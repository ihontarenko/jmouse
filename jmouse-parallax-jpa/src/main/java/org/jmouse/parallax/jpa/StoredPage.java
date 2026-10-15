package org.jmouse.parallax.jpa;

import jakarta.persistence.TypedQuery;

import java.util.List;
import java.util.function.Function;

/**
 * 📄 One page of stored rows and how many there are in all.
 *
 * <p>The stores page by number and size and answer with this rather than a framework's page type, so the module needs
 * nothing beyond Jakarta Persistence. Bounds are the caller's to check: a store pages whatever it is asked to.</p>
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

    /**
     * 📄 Read one page of a query.
     *
     * @param rows  the listing query, ordered
     * @param count the count query over the same rows
     * @param page  the page number, from zero
     * @param size  the page size
     * @param <T>   the row type
     * @return the page
     */
    public static <T> StoredPage<T> of(TypedQuery<T> rows, TypedQuery<Long> count, int page, int size) {
        List<T> content = rows.setFirstResult(page * size).setMaxResults(size).getResultList();

        return new StoredPage<>(content, page, size, count.getSingleResult());
    }

    /**
     * 🔁 The same page with every row turned into something else.
     *
     * @param mapping what a row becomes
     * @param <R>     the new row type
     * @return the mapped page
     */
    public <R> StoredPage<R> map(Function<? super T, ? extends R> mapping) {
        return new StoredPage<>(rows.stream().<R>map(mapping).toList(), page, size, total);
    }
}
