package org.jmouse.cad.table;

import org.jmouse.sheet.Sheet;

import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;

/**
 * One table as a program uses it: asked of its source each time, and read to what it says — a filing, a set of rules — only
 * when the source answers with another table than the last, so a revision saved applies at once and an unchanged table is
 * not read again. The source keeps the table of the revision; this keeps only what the last one read to.
 *
 * @param <T> what the table reads to
 */
public final class ReadTable<T> {

    /** A table, and what it read to. */
    private record Read<T>(Sheet table, T value) {
    }

    private final TableSource              tables;
    private final String                   slug;
    private final Function<Sheet, T>       reading;
    private final AtomicReference<Read<T>> last = new AtomicReference<>();

    /**
     * @param slug    the table's address in the catalogue
     * @param reading what the table reads to: {@code PcbRules::of}
     */
    public ReadTable(TableSource tables, String slug, Function<Sheet, T> reading) {
        this.tables = tables;
        this.slug = slug;
        this.reading = reading;
    }

    /** What the table reads to now. */
    public T current() {
        Sheet table = tables.table(slug);
        Read<T> read = last.get();
        if (read != null && read.table() == table) {
            return read.value();
        }
        Read<T> fresh = new Read<>(table, reading.apply(table));
        last.set(fresh);
        return fresh.value();
    }
}
