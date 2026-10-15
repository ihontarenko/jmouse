package org.jmouse.telegram.management;

import java.util.List;

/**
 * One page of a listing, with what a table needs to draw its pager.
 *
 * @param items  this page
 * @param total  how many match in all
 * @param offset where this page starts
 * @param limit  the page size that was applied
 */
public record ManagementPage<T>(List<T> items, long total, int offset, int limit) {

    public ManagementPage {
        items = List.copyOf(items);
    }
}
