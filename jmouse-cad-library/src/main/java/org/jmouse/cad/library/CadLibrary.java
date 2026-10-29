package org.jmouse.cad.library;

import org.jmouse.cad.document.DocumentInput;
import org.jmouse.cad.document.DocumentLanguage;
import org.jmouse.cad.document.DocumentNode;
import org.jmouse.cad.simulation.CatalogueDocuments;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Properties;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentHashMap;

/**
 * The jmCAD library: every part a design may be made of, as documents of the jM language, found by slug. Its documents
 * are resources beside this class, listed by {@value #INDEX} — a jar's contents cannot be listed —, each entry
 * {@code slug = path}; a document is read once, when first asked for, and kept.
 */
public final class CadLibrary implements CatalogueDocuments {

    /** The list of the library's documents, beside this class. */
    public static final String INDEX = "library.properties";

    private static final CadLibrary LIBRARY = new CadLibrary();

    private final DocumentLanguage          language = CadLanguage.standard().language();
    private final Map<String, String>       paths;
    private final Map<String, DocumentNode> read = new ConcurrentHashMap<>();

    private CadLibrary() {
        this.paths = Collections.unmodifiableMap(index());
    }

    public static CadLibrary library() {
        return LIBRARY;
    }

    /** The slug of every document the library holds, in alphabetical order. */
    public List<String> slugs() {
        return List.copyOf(paths.keySet());
    }

    /** The resource a document is read from, relative to this class. */
    public Optional<String> path(String slug) {
        return Optional.ofNullable(paths.get(slug));
    }

    @Override
    public Optional<DocumentNode> find(String slug) {
        String path = paths.get(slug);
        if (path == null) {
            return Optional.empty();
        }
        return Optional.of(read.computeIfAbsent(slug, ignored -> language.readOne(DocumentInput.file(path, text(path)))));
    }

    /** The text of a document of the library, as it is written. */
    public String text(String path) {
        try (InputStream stream = CadLibrary.class.getResourceAsStream(path)) {
            if (stream == null) {
                throw new IllegalStateException("the library lists '" + path + "', which is not beside " + CadLibrary.class.getName());
            }
            return new String(stream.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException failure) {
            throw new UncheckedIOException(failure);
        }
    }

    private static Map<String, String> index() {
        Properties index = new Properties();
        try (InputStream stream = CadLibrary.class.getResourceAsStream(INDEX)) {
            if (stream == null) {
                throw new IllegalStateException("'" + INDEX + "' is not beside " + CadLibrary.class.getName());
            }
            index.load(new InputStreamReader(stream, StandardCharsets.UTF_8));
        } catch (IOException failure) {
            throw new UncheckedIOException(failure);
        }
        Map<String, String> paths = new TreeMap<>();
        index.stringPropertyNames().forEach(slug -> paths.put(slug, index.getProperty(slug)));
        return paths;
    }
}
