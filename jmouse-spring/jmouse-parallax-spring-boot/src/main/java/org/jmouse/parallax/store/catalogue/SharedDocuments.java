package org.jmouse.parallax.store.catalogue;

import org.jmouse.parallax.script.ExperienceDocument;
import org.jmouse.parallax.script.ExperienceReader;
import org.jmouse.parallax.script.Kind;
import org.jmouse.parallax.object.ComponentLibrary;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * The documents components share rather than compute — logic families ({@code family cmos-hc}) and libraries of jMF
 * functions ({@code formulas "smooth-transitions"}) — read once, at start-up, from
 * {@code jmouse.parallax.catalogue.shared-document-locations}; a family's or a function library's slug resolves to
 * its source.
 *
 * <p>⚠️ They are not catalogue objects: nothing lists, edits or opens them on their own. When every document is stored
 * as text in the catalogue, they move there and this class goes.</p>
 */
public class SharedDocuments implements ComponentLibrary {

    private final Map<String, String> sources;

    public SharedDocuments(CatalogueProperties properties) {
        this.sources = load(properties.sharedDocumentLocations());
    }

    @Override
    public Optional<String> source(String reference) {
        return Optional.ofNullable(sources.get(reference));
    }

    private static Map<String, String> load(List<String> locations) {
        PathMatchingResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();
        Map<String, String> sources = new LinkedHashMap<>();
        for (String location : locations) {
            try {
                for (Resource resource : resolver.getResources(location)) {
                    String source = new String(resource.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
                    ExperienceDocument document = ExperienceReader.readOne(source, resource.getFilename());
                    if (document.kind() != Kind.FAMILY && document.kind() != Kind.FUNCTIONS) {
                        throw new IllegalStateException(resource.getFilename() + " is a " + document.kind().keyword()
                                + "; " + location + " holds families and function libraries");
                    }
                    if (sources.putIfAbsent(document.slug(), source) != null) {
                        throw new IllegalStateException("Two shared documents are called '" + document.slug() + "'");
                    }
                }
            } catch (IOException failure) {
                throw new UncheckedIOException("Cannot read the shared documents in " + location, failure);
            }
        }
        return Map.copyOf(sources);
    }
}
