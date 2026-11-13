package org.jmouse.cad.table;

import org.jmouse.sheet.Sheet;

/**
 * 📋 Where a program reads a table by its slug — never a file of a jar —: the engine answers with the catalogue's table, its
 * curator's revision where one is kept, so a table saved applies at once; a program run without the engine reads the table as
 * it ships ({@link ShippedTables}). Asked each time a value is used, never read once and kept.
 */
@FunctionalInterface
public interface TableSource {

    /**
     * The table of this slug.
     *
     * @throws IllegalStateException when no table of this slug is shipped or kept: a program names only tables that ship
     */
    Sheet table(String slug);
}
