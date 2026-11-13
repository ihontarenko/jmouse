package org.jmouse.cad.table;

import org.jmouse.plugin.Contributions;

/**
 * 📋 A table a module ships, as the catalogue lists it: a document of the library its module's plugin brings, so a table read
 * by an importer, an exporter or a simulator is a page of the library whoever owns it — KiCad's filing by the KiCad plugin,
 * SPICE's devices by a simulator's. The engine's own library lists its tables beside its other documents.
 *
 * @param tables where its module ships its tables, and what ships them
 * @param slug   the table's address
 */
public record ShippedTable(ShippedTables tables, String slug) {

    /** Where a plugin brings the tables of its module. */
    public static final Contributions<ShippedTable> POINT = new Contributions<>("cad.library.tables", ShippedTable.class);

    /** What ships it, as a document's location names it: {@code jmouse-cad-kicad}. */
    public String library() {
        return tables.library();
    }

    /** Its file's path under what ships it: {@code org/jmouse/cad/kicad/tables/kicad-libraries.table.jm}. */
    public String path() {
        return tables.pathOf(slug);
    }

    /** Its text, as its module ships it. */
    public String text() {
        return tables.text(slug);
    }
}
