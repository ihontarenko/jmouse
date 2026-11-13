package org.jmouse.cad.table;

import org.jmouse.cad.document.DocumentLanguage;
import org.jmouse.sheet.Sheet;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * The tables as a module ships them: {@code <slug>.table.jm} files in one resource folder of its jar, each read once by the table
 * language. What a program reads when no engine answers — a test, a tool —; the engine reads the same files as catalogue
 * documents and answers with its own {@link TableSource}.
 */
public final class ShippedTables implements TableSource {

    /** What the engine's own library is called where it ships a table. */
    public static final String ENGINE_LIBRARY = "jmouse-cad-library";

    private static final DocumentLanguage LANGUAGE = DocumentLanguage.of(new TableModule());

    private final String              library;
    private final ClassLoader         loader;
    private final String              folder;
    private final Map<String, Sheet> read = new ConcurrentHashMap<>();

    private ShippedTables(String library, ClassLoader loader, String folder) {
        this.library = library;
        this.loader = loader;
        this.folder = folder.endsWith("/") ? folder : folder + "/";
    }

    /**
     * The tables in a resource folder: {@code org/jmouse/cad/kicad/tables}.
     *
     * @param beside  a class of the jar that ships them, whose loader reads the folder
     * @param library what ships them, as a shipped document's location names it: {@code jmouse-cad-kicad}
     */
    public static ShippedTables in(Class<?> beside, String library, String folder) {
        return new ShippedTables(library, beside.getClassLoader(), folder);
    }

    /** What ships these tables: {@code jmouse-cad-kicad}. */
    public String library() {
        return library;
    }

    /** One of these tables, as the catalogue lists it. */
    public ShippedTable shipping(String slug) {
        return new ShippedTable(this, slug);
    }

    /** Where a table ships, as a resource path. */
    public String pathOf(String slug) {
        return folder + slug + TableModule.TABLE.fileSuffix();
    }

    @Override
    public Sheet table(String slug) {
        return read.computeIfAbsent(slug, wanted -> TableModule.sheetOf(LANGUAGE.readOne(text(wanted), pathOf(wanted))));
    }

    /** A table's text, as its module ships it. */
    public String text(String slug) {
        return resource(pathOf(slug));
    }

    private String resource(String path) {
        try (InputStream stream = loader.getResourceAsStream(path)) {
            if (stream == null) {
                throw new IllegalStateException("No table ships at '" + path + "'");
            }
            return new String(stream.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException unreadable) {
            throw new UncheckedIOException("The table at '" + path + "' cannot be read", unreadable);
        }
    }
}
