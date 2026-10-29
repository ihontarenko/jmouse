package org.jmouse.cad.store.library;

import org.jmouse.cad.document.DocumentInput;
import org.jmouse.cad.document.DocumentNode;
import org.jmouse.cad.document.MetaNode;
import org.jmouse.cad.jpa.catalogue.DocumentStore;
import org.jmouse.cad.library.CadLanguage;
import org.jmouse.cad.library.CadLibrary;
import org.jmouse.cad.simulation.SimulationModule;
import org.jmouse.cad.store.CadProperties;
import org.jmouse.cad.store.catalogue.CadCatalogue;
import org.jmouse.cad.store.catalogue.CadCatalogue.ShippedOutcome;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.ResourcePatternResolver;
import org.springframework.util.ResourceUtils;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * The documents that ship with the installation, written into the catalogue: jmouse-cad-library's own, and the
 * product's from {@code jmouse.cad.library.locations} — its categories, its circuits.
 *
 * <p>A category document declares a section, written when it is missing and never rewritten: a curator owns it after.
 * Every other document new to the catalogue is created from its file; one nobody has edited takes the file's newer text
 * as its next revision; one a curator has edited keeps the edit, and records that its file has a newer text. A file
 * whose text is the one already taken is not read at all, so a start where nothing moved reads nothing.</p>
 *
 * <p>A slug is shipped once: two sources shipping one address are refused, never resolved by whichever came last.</p>
 *
 * <p>Circuits are written last, since a circuit's drawing is checked against the parts it places; relations are indexed
 * once everything they may name exists.</p>
 */
public class ShippedLibrary {

    private static final Logger LOGGER = LoggerFactory.getLogger(ShippedLibrary.class);

    private static final String FILE_PATTERN = "**/*" + DocumentInput.FILE_SUFFIX;
    private static final String SEPARATOR    = "/";

    private final CadCatalogue            catalogue;
    private final ResourcePatternResolver resources;
    private final CadProperties           properties;
    private final CadLanguage             language;

    public ShippedLibrary(CadCatalogue catalogue, ResourcePatternResolver resources, CadProperties properties,
                          CadLanguage language) {
        this.language = language;
        this.catalogue = catalogue;
        this.resources = resources;
        this.properties = properties;
    }

    /**
     * One document as it ships: its text, where it came from, and the folder its file stands in below its location —
     * {@code components/passive} —, which a host may file it by.
     */
    public record ShippedDocument(String slug, String kind, String source, String origin, String folder) {
    }

    /** Every document that ships now, in the order they are found: the library's own, then the product's. */
    public List<ShippedDocument> documents() {
        return List.copyOf(shippedDocuments());
    }

    /**
     * What ships, as one checksum: each document's origin and the checksum of its text, in order. It changes when a file
     * is added, removed, moved or edited — what a host that imports on its own schedule runs the import again on.
     */
    public String checksum() {
        StringBuilder written = new StringBuilder();
        for (ShippedDocument document : shippedDocuments()) {
            written.append(document.origin()).append(' ').append(DocumentStore.checksumOf(document.source())).append('\n');
        }
        return DocumentStore.checksumOf(written.toString());
    }

    /** Writes what is missing or newer, and says what it wrote. */
    public String apply() {
        List<ShippedDocument> shipped = shippedDocuments();
        int categories = 0;
        for (ShippedDocument document : shipped) {
            if (document.kind().equals(SimulationModule.CATEGORY.word()) && seedCategory(document)) {
                categories++;
            }
        }
        Map<ShippedOutcome, Integer> outcomes = new EnumMap<>(ShippedOutcome.class);
        List<String> written = new ArrayList<>();
        for (ShippedDocument document : shipped) {
            if (!isCategory(document) && !isCircuit(document)) {
                write(document, outcomes, written);
            }
        }
        for (ShippedDocument document : shipped) {
            if (isCircuit(document)) {
                write(document, outcomes, written);
            }
        }
        written.forEach(catalogue::indexShippedRelations);
        String summary = categories + " categor" + (categories == 1 ? "y" : "ies") + " added, documents " + outcomes;
        LOGGER.info("📚 Shipped library: {}", summary);
        return summary;
    }

    /** The text a slug ships with, as its file writes it now; empty for a slug nothing ships. */
    public Optional<String> shippedSource(String slug) {
        return shippedDocuments().stream().filter(document -> document.slug().equals(slug)).findFirst()
                .map(ShippedDocument::source);
    }

    private void write(ShippedDocument document, Map<ShippedOutcome, Integer> outcomes, List<String> written) {
        if (catalogue.shippedUnchanged(document.slug(), DocumentStore.checksumOf(document.source()))) {
            outcomes.merge(ShippedOutcome.UNCHANGED, 1, Integer::sum);
            return;
        }
        ShippedOutcome outcome;
        try {
            outcome = catalogue.writeShipped(document.source(), document.origin(), document.folder());
        } catch (RuntimeException refused) {
            throw new IllegalStateException("The shipped document " + document.origin() + " is refused: "
                                            + refused.getMessage(), refused);
        }
        outcomes.merge(outcome, 1, Integer::sum);
        if (outcome == ShippedOutcome.CREATED || outcome == ShippedOutcome.REVISED) {
            written.add(document.slug());
        }
    }

    private boolean seedCategory(ShippedDocument shipped) {
        DocumentNode document = language.language().readOne(DocumentInput.text(shipped.origin(), shipped.source()));
        Optional<MetaNode> meta = document.block(MetaNode.class);
        Integer order = meta.flatMap(found -> found.text(MetaNode.ORDER)).map(ShippedLibrary::orderOf).orElse(null);
        return catalogue.seedCategory(document.slug(), document.title(),
                                      meta.flatMap(found -> found.text(MetaNode.DESCRIPTION)).orElse(null), order);
    }

    private static Integer orderOf(String written) {
        try {
            return Integer.valueOf(written.strip());
        } catch (NumberFormatException notWhole) {
            throw new IllegalStateException("A category's order is a whole number, not '" + written + "'", notWhole);
        }
    }

    private static boolean isCategory(ShippedDocument document) {
        return document.kind().equals(SimulationModule.CATEGORY.word());
    }

    private static boolean isCircuit(ShippedDocument document) {
        return document.kind().equals(SimulationModule.CIRCUIT.word());
    }

    // ── Where the documents come from ────────────────────────────────────────────────────────────────────────────

    private List<ShippedDocument> shippedDocuments() {
        List<ShippedDocument> shipped = new ArrayList<>();
        if (properties.library().includeEngine()) {
            CadLibrary library = CadLibrary.library();
            for (String slug : library.slugs()) {
                String path = library.path(slug).orElseThrow();
                shipped.addAll(split(library.text(path), "jmouse-cad-library:" + path, folderOf(path)));
            }
        }
        for (String location : properties.library().locations()) {
            shipped.addAll(located(location));
        }
        requireOneSource(shipped);
        return shipped;
    }

    /**
     * Refuses two shipped documents with one slug. The catalogue keeps one document per address, and taking either
     * would be a guess — worse, a different one on every start, each start revising the other back.
     */
    private static void requireOneSource(List<ShippedDocument> shipped) {
        Map<String, ShippedDocument> bySlug = new HashMap<>();
        List<String> twice = new ArrayList<>();
        for (ShippedDocument document : shipped) {
            ShippedDocument earlier = bySlug.putIfAbsent(document.slug(), document);
            if (earlier != null) {
                twice.add("'" + document.slug() + "' in " + earlier.origin() + " and " + document.origin());
            }
        }
        if (!twice.isEmpty()) {
            throw new IllegalStateException(twice.size() + " shipped document(s) are shipped twice; remove one of each: "
                                            + String.join("; ", twice.subList(0, Math.min(5, twice.size())))
                                            + (twice.size() > 5 ? "; …" : ""));
        }
    }

    /** Every document under one location, each filed under the folder its file stands in below the location. */
    private List<ShippedDocument> located(String location) {
        String root = location.endsWith(SEPARATOR) ? location : location + SEPARATOR;
        List<ShippedDocument> shipped = new ArrayList<>();
        try {
            String base = canonical(resources.getResource(root));
            for (Resource resource : resources.getResources(root + FILE_PATTERN)) {
                String address = canonical(resource);
                if (!address.startsWith(base)) {
                    throw new IllegalStateException(resource + " is not below " + location);
                }
                String relative = address.substring(base.length());
                String text = resource.getContentAsString(StandardCharsets.UTF_8);
                shipped.addAll(split(text, location + SEPARATOR + relative, folderOf(relative)));
            }
        } catch (IOException failure) {
            throw new UncheckedIOException("The shipped documents at " + location + " could not be read", failure);
        }
        return shipped;
    }

    /**
     * A resource's address in one spelling, so a file found by a pattern and the directory it was looked for under
     * compare: a file's as its path names it ({@code file:/C:/…}, never {@code file:C:/…}), anything else — a jar's
     * entry — as its class loader gives it.
     */
    private static String canonical(Resource resource) throws IOException {
        URL url = resource.getURL();
        if (ResourceUtils.isFileURL(url)) {
            return ResourceUtils.getFile(url).toURI().normalize().toString();
        }
        return url.toExternalForm();
    }

    /**
     * The documents of one file: the file's own text when it holds one, and each written by the language when it
     * holds several — a file of categories.
     */
    private List<ShippedDocument> split(String text, String origin, String folder) {
        List<DocumentNode> documents = language.language().read(DocumentInput.file(fileNameOf(origin), text));
        if (documents.size() == 1) {
            DocumentNode only = documents.getFirst();
            return List.of(new ShippedDocument(only.slug(), only.kind().word(), text, origin, folder));
        }
        return documents.stream()
                .map(document -> new ShippedDocument(document.slug(), document.kind().word(),
                                                     language.language().write(document), origin, folder))
                .toList();
    }

    private static String folderOf(String path) {
        int last = path.lastIndexOf(SEPARATOR);
        return last <= 0 ? null : path.substring(0, last);
    }

    private static String fileNameOf(String origin) {
        return origin.substring(origin.lastIndexOf(SEPARATOR) + 1);
    }
}
