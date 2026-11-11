package org.jmouse.cad.kicad;

import java.util.Optional;

/**
 * What a drawing or a component converted from KiCad writes as its source ({@code meta { source … }}), and how it is
 * read back: {@code KiCad symbol Device:R}, {@code KiCad footprint Package_SO:SOIC-8_3.9x4.9mm_P1.27mm} — followed, for
 * a footprint of a project's own library, by {@value #PROJECT_LIBRARY}. The one place both are spelled: the export reads
 * the KiCad id back from it, and a shelf files the drawing under its KiCad library.
 */
public final class KiCadSources {

    private static final String SYMBOL = "KiCad symbol ";
    private static final String FOOTPRINT = "KiCad footprint ";
    private static final String PROJECT_LIBRARY = ", from the project's own library";

    private KiCadSources() {
    }

    /** @param libraryId KiCad's {@code LIBRARY:NAME} */
    public static String symbol(String libraryId) {
        return SYMBOL + libraryId;
    }

    /**
     * @param libraryId KiCad's {@code LIBRARY:NAME}
     * @param installed whether it is one of KiCad's installed libraries, not a project's own
     */
    public static String footprint(String libraryId, boolean installed) {
        return FOOTPRINT + libraryId + (installed ? "" : PROJECT_LIBRARY);
    }

    /** The KiCad symbol's {@code LIBRARY:NAME} a source names; empty for a source that names none. */
    public static Optional<String> symbolIdOf(String source) {
        return idAfter(SYMBOL, source);
    }

    /** The KiCad footprint's {@code LIBRARY:NAME} a source names; empty for a source that names none. */
    public static Optional<String> footprintIdOf(String source) {
        return idAfter(FOOTPRINT, source);
    }

    /** The KiCad library a symbol's or a footprint's source names — {@code Package_SO}; empty for one from elsewhere. */
    public static Optional<String> libraryOf(String source) {
        return symbolIdOf(source).or(() -> footprintIdOf(source))
                .filter(libraryId -> libraryId.indexOf(':') > 0)
                .map(libraryId -> libraryId.substring(0, libraryId.indexOf(':')));
    }

    private static Optional<String> idAfter(String prefix, String source) {
        if (source == null || !source.startsWith(prefix)) {
            return Optional.empty();
        }
        String written = source.substring(prefix.length());
        if (written.endsWith(PROJECT_LIBRARY)) {
            written = written.substring(0, written.length() - PROJECT_LIBRARY.length());
        }
        written = written.strip();
        return written.isEmpty() ? Optional.empty() : Optional.of(written);
    }
}
