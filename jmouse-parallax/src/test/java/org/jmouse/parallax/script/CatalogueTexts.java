package org.jmouse.parallax.script;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

/**
 * Every jMX text the language is held to: this module's catalogue and test resources — its own —, and every directory
 * named in the system property {@code jmx.catalogues} (separated by {@code ;}) — a product's catalogue lives outside this
 * repository.
 */
final class CatalogueTexts {

    private static final String EXTRA_CATALOGUES = "jmx.catalogues";

    /** This module's own texts, each by its path from the module. */
    static final List<Path> OWN_ROOTS = List.of(Path.of("src/main/resources/catalogue"), Path.of("src/test/resources"));

    private CatalogueTexts() {
    }

    /** Each text by its path, in the order of the roots and, within one, of the paths: the module's own, then the products'. */
    static Map<String, String> documents() {
        Map<String, String> documents = new LinkedHashMap<>(own());
        documents.putAll(products());
        return documents;
    }

    /** This module's own texts: what its snapshots are recorded from. */
    static Map<String, String> own() {
        return read(OWN_ROOTS);
    }

    /** The texts of the products named in {@code jmx.catalogues}: their data, held to the language but never recorded here. */
    static Map<String, String> products() {
        List<Path> roots = new ArrayList<>();
        for (String root : System.getProperty(EXTRA_CATALOGUES, "").split(";")) {
            if (!root.isBlank()) {
                roots.add(Path.of(root.strip()));
            }
        }
        return read(roots);
    }

    private static Map<String, String> read(List<Path> roots) {
        Map<String, String> documents = new LinkedHashMap<>();
        for (Path root : roots) {
            if (!Files.isDirectory(root)) {
                continue;
            }
            try (Stream<Path> files = Files.walk(root)) {
                for (Path file : files.filter(path -> path.toString().endsWith(".jmx")).sorted().toList()) {
                    documents.put(file.toString().replace('\\', '/'), Files.readString(file, StandardCharsets.UTF_8));
                }
            } catch (IOException unreadable) {
                throw new UncheckedIOException(unreadable);
            }
        }
        return documents;
    }
}
