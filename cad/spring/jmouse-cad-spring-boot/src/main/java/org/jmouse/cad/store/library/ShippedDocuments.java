package org.jmouse.cad.store.library;

import org.jmouse.cad.document.DocumentInput;
import org.jmouse.cad.document.DocumentNode;
import org.jmouse.cad.document.source.DocumentLocation;
import org.jmouse.cad.folder.DocumentStore;
import org.jmouse.cad.library.CadLanguage;
import org.jmouse.cad.library.CadLibrary;
import org.jmouse.cad.store.CadProperties;
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
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * The documents that ship with the installation, where they are: jmouse-cad-library's own, and the product's from
 * {@code jmouse.cad.library.locations} — read from their files once, kept here, and never copied into the database (ADR
 * 0001): the database holds their index alone, and their text is read from here. A file may hold several documents — a
 * file of categories —, each split out by the language.
 *
 * <p>A slug is shipped once: two files shipping one address are refused, never resolved by whichever came last.</p>
 */
public class ShippedDocuments {

    private static final Logger LOGGER = LoggerFactory.getLogger(ShippedDocuments.class);

    /** What the engine's own library is called where it ships a document. */
    public static final String ENGINE_LIBRARY = "jmouse-cad-library";

    private static final String FILE_PATTERN = "**/*" + DocumentInput.FILE_SUFFIX;
    private static final String SEPARATOR    = "/";

    /**
     * One document as it ships: its text, and where its file is.
     *
     * @param library what ships it — {@value #ENGINE_LIBRARY}, a product's location —
     * @param path    its file's path under what ships it — {@code components/passive/resistor.component.jm} —
     */
    public record ShippedDocument(String slug, String kind, String source, String library, String path) {

        /** Where its copy is held, as the source layer says it. */
        public DocumentLocation.Shipped location() {
            return new DocumentLocation.Shipped(library, path);
        }

        /** The folder its file stands in under what ships it — {@code components/passive} —; null at its root. */
        public String folder() {
            int last = path.lastIndexOf(SEPARATOR);
            return last <= 0 ? null : path.substring(0, last);
        }

        /** Where it is, as a message names it: {@code jmouse-cad-library:components/passive/resistor.component.jm}. */
        public String origin() {
            return library + ":" + path;
        }
    }

    private final ResourcePatternResolver resources;
    private final CadProperties           properties;
    private final CadLanguage             language;
    /**
     * What the installation's jars hold, read once: the documents read, by slug, and the place of every shipped file —
     * those read and those of a kind no running module reads alike.
     */
    private record Scanned(Map<String, ShippedDocument> bySlug, Set<String> places) {
    }

    private volatile Scanned scanned;

    public ShippedDocuments(ResourcePatternResolver resources, CadProperties properties, CadLanguage language) {
        this.resources = resources;
        this.properties = properties;
        this.language = language;
    }

    /** Every document that ships, in the order they are found: the library's own, then the product's. */
    public List<ShippedDocument> documents() {
        return List.copyOf(shipped().values());
    }

    /** The document a slug ships as; empty for a slug nothing ships. */
    public Optional<ShippedDocument> find(String slug) {
        return Optional.ofNullable(shipped().get(slug));
    }

    /**
     * What ships, as one checksum: each document's place and the checksum of its text, in order. It changes when a file
     * is added, removed, moved or edited — what a host that indexes on its own schedule runs again on.
     */
    public String checksum() {
        StringBuilder written = new StringBuilder();
        for (ShippedDocument document : shipped().values()) {
            written.append(document.origin()).append(' ').append(DocumentStore.checksumOf(document.source())).append('\n');
        }
        return DocumentStore.checksumOf(written.toString());
    }

    /**
     * Whether a file still ships at that place — read, or of a kind no running module reads —: what a catalogue's row of a
     * shipped document is kept by. A plugin that is off leaves its files shipping; only a file gone from the jars does not.
     */
    public boolean ships(String library, String path) {
        return scanned().places().contains(placeOf(library, path));
    }

    private static String placeOf(String library, String path) {
        return library + ":" + path;
    }

    /** The documents by slug. */
    private Map<String, ShippedDocument> shipped() {
        return scanned().bySlug();
    }

    /** The files read once, on the first question: an installation's jars do not change while it runs. */
    private Scanned scanned() {
        Scanned found = scanned;
        if (found == null) {
            synchronized (this) {
                found = scanned;
                if (found == null) {
                    found = read();
                    scanned = found;
                }
            }
        }
        return found;
    }

    private Scanned read() {
        List<ShippedDocument> documents = new ArrayList<>();
        Set<String> places = new HashSet<>();
        Set<String> kinds = new HashSet<>();
        language.modules().forEach(module -> module.kinds().forEach(kind -> kinds.add(kind.word())));
        int unread = 0;
        if (properties.library().includeEngine()) {
            CadLibrary library = CadLibrary.library();
            for (String slug : library.slugs()) {
                String path = library.path(slug).orElseThrow();
                places.add(placeOf(ENGINE_LIBRARY, path));
                if (!kinds.contains(kindOf(path))) {
                    unread++;
                    continue;
                }
                documents.addAll(split(library.text(path), ENGINE_LIBRARY, path));
            }
        }
        if (unread > 0) {
            LOGGER.info("📚 {} shipped document(s) are of a kind no running module reads — a plugin's that is off — and are left out.",
                        unread);
        }
        for (String location : properties.library().locations()) {
            documents.addAll(located(location));
        }
        documents.forEach(document -> places.add(placeOf(document.library(), document.path())));
        return new Scanned(bySlug(documents), Set.copyOf(places));
    }

    /** The kind a document's file says it is by its name: `soic-8-model.model.jm` is a model. */
    private static String kindOf(String path) {
        String name = path.substring(path.lastIndexOf(SEPARATOR) + 1);
        String withoutSuffix = name.substring(0, name.length() - DocumentInput.FILE_SUFFIX.length());
        return withoutSuffix.substring(withoutSuffix.lastIndexOf('.') + 1);
    }

    /**
     * The documents by slug, refusing two with one slug: the catalogue keeps one document per address, and taking either
     * would be a guess — worse, a different one on every start.
     */
    private static Map<String, ShippedDocument> bySlug(List<ShippedDocument> documents) {
        Map<String, ShippedDocument> bySlug = new LinkedHashMap<>();
        List<String> twice = new ArrayList<>();
        Map<String, ShippedDocument> seen = new HashMap<>();
        for (ShippedDocument document : documents) {
            ShippedDocument earlier = seen.putIfAbsent(document.slug(), document);
            if (earlier != null) {
                twice.add("'" + document.slug() + "' in " + earlier.origin() + " and " + document.origin());
                continue;
            }
            bySlug.put(document.slug(), document);
        }
        if (!twice.isEmpty()) {
            throw new IllegalStateException(twice.size() + " shipped document(s) are shipped twice; remove one of each: "
                                            + String.join("; ", twice.subList(0, Math.min(5, twice.size())))
                                            + (twice.size() > 5 ? "; …" : ""));
        }
        return bySlug;
    }

    /** Every document under one location, each placed by its file's path below the location. */
    private List<ShippedDocument> located(String location) {
        String root = location.endsWith(SEPARATOR) ? location : location + SEPARATOR;
        List<ShippedDocument> documents = new ArrayList<>();
        try {
            String base = canonical(resources.getResource(root));
            for (Resource resource : resources.getResources(root + FILE_PATTERN)) {
                String address = canonical(resource);
                if (!address.startsWith(base)) {
                    throw new IllegalStateException(resource + " is not below " + location);
                }
                documents.addAll(split(resource.getContentAsString(StandardCharsets.UTF_8), location, address.substring(base.length())));
            }
        } catch (IOException failure) {
            throw new UncheckedIOException("The shipped documents at " + location + " could not be read", failure);
        }
        return documents;
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
     * The documents of one file: the file's own text when it holds one, and each written by the language when it holds
     * several — a file of categories.
     */
    private List<ShippedDocument> split(String text, String library, String path) {
        List<DocumentNode> documents = language.language().read(DocumentInput.file(path.substring(path.lastIndexOf(SEPARATOR) + 1), text));
        if (documents.size() == 1) {
            DocumentNode only = documents.getFirst();
            return List.of(new ShippedDocument(only.slug(), only.kind().word(), text, library, path));
        }
        return documents.stream().map(document -> new ShippedDocument(document.slug(), document.kind().word(),
                language.language().write(document), library, path)).toList();
    }
}
