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
 * Every jMX text the language is held to: this module's catalogue and test resources, and every directory named in the
 * system property {@code jmx.catalogues} (separated by {@code ;}) — a product's catalogue lives outside this repository.
 */
final class CatalogueTexts {

    private static final String EXTRA_CATALOGUES = "jmx.catalogues";

    private CatalogueTexts() {
    }

    /** Each text by its path, in the order of the roots and, within one, of the paths. */
    static Map<String, String> documents() {
        List<Path> roots = new ArrayList<>(List.of(Path.of("src/main/resources/catalogue"), Path.of("src/test/resources")));
        String extra = System.getProperty(EXTRA_CATALOGUES, "");
        for (String root : extra.split(";")) {
            if (!root.isBlank()) {
                roots.add(Path.of(root.strip()));
            }
        }
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
