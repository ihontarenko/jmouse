package org.jmouse.cad.plugin.drawings;

import java.util.Locale;

/** What a mark on a sheet is part of: a format with layers keeps them apart. */
public enum SheetLayer {

    FRAME,
    OUTLINE,
    HOLES,
    DIMENSIONS,
    ARTWORK,
    TABLES,
    NOTES;

    /** The layer's name in a file. */
    public String word() {
        return name().toLowerCase(Locale.ROOT);
    }
}
