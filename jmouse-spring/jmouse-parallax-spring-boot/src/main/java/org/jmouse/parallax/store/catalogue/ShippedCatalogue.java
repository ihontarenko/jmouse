package org.jmouse.parallax.store.catalogue;

import org.jmouse.parallax.script.ExperienceDocument;
import org.jmouse.parallax.script.ExperienceReader;
import org.jmouse.parallax.script.Kind;
import org.jmouse.parallax.script.Meta;
import org.jmouse.parallax.support.ParallaxIdentifiers;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

/**
 * The catalogue the host ships: every {@code .jmx} file the {@code jmouse.parallax.catalogue.locations} patterns
 * find, read and sorted by what each document is.
 *
 * <p>An object is one file and one document — its text, comments included, is what is stored. The catalogue's own
 * documents — sections ({@code category}), collections and instruments — may share a file, and so may the documents
 * of kinds Parallax leaves to the host (a {@code course}), which go to a {@link ShippedDocumentHandler}. Logic families
 * and function libraries are not objects; {@code SharedDocuments} reads them where components need them.</p>
 *
 * @param objects     the files holding one object each, in the order of their paths
 * @param categories  every {@code category} document
 * @param collections every {@code collection} document
 * @param delegated   every catalogue document of a kind Parallax does not keep itself — a {@code course}
 * @param instruments every {@code instrument} document
 * @param checksum    one checksum over every file's path and text: what the start-up step compares
 */
public record ShippedCatalogue(List<ShippedObject> objects, List<ExperienceDocument> categories,
                               List<ExperienceDocument> collections, List<ExperienceDocument> delegated,
                               List<ExperienceDocument> instruments, String checksum) {

    /**
     * One object's file.
     *
     * @param name     its resource name — the location's root and its path under it, {@code catalogue/curves/circle.jmx}
     *                 — which names it in a refusal and is stored as where its text came from
     * @param source   the file's text, stored as the object's revision
     * @param document what the text was read as
     */
    public record ShippedObject(String name, String source, ExperienceDocument document) {
    }

    /** A file as it was found: the root of the location that found it, and its path under that root. */
    private record ShippedFile(String root, String path, String source) {
    }

    public ShippedCatalogue {
        objects = List.copyOf(objects);
        categories = List.copyOf(categories);
        collections = List.copyOf(collections);
        delegated = List.copyOf(delegated);
        instruments = List.copyOf(instruments);
    }

    /**
     * Reads every shipped file; a file that does not read, or an object file holding more than one document, fails. A host
     * reads the catalogue through it too — to file what it ships —, so the files are read one way only.
     */
    public static ShippedCatalogue load(List<String> locations) {
        List<ShippedObject> objects = new ArrayList<>();
        List<ExperienceDocument> categories = new ArrayList<>();
        List<ExperienceDocument> collections = new ArrayList<>();
        List<ExperienceDocument> delegated = new ArrayList<>();
        List<ExperienceDocument> instruments = new ArrayList<>();
        StringBuilder fingerprint = new StringBuilder("catalogue-jmx-v1\n");
        for (ShippedFile file : files(locations)) {
            String name = file.root() + file.path();
            fingerprint.append(file.path()).append('\n').append(ParallaxIdentifiers.sha256(file.source())).append('\n');
            List<ExperienceDocument> documents = ExperienceReader.read(file.source(), name);
            for (ExperienceDocument document : documents) {
                switch (document.kind().role()) {
                    case COMPUTABLE, DRAWING, COMPOSITION -> {
                        if (documents.size() != 1) {
                            throw new IllegalStateException(name + " holds " + documents.size()
                                    + " documents; an object is one file of its own");
                        }
                        objects.add(new ShippedObject(name, file.source(), document));
                    }
                    case CATALOGUE -> catalogueDocument(document, categories, collections, delegated, instruments);
                    case SHARED -> {
                        // A logic family or a function library: read by SharedDocuments where a component uses it.
                    }
                }
            }
        }
        return new ShippedCatalogue(objects, categories, collections, delegated, instruments,
                ParallaxIdentifiers.sha256(fingerprint.toString()));
    }

    /** {@code order 3}: where a section, collection, course or instrument stands among its kind; 0 when not written. */
    public static int order(Meta meta) {
        return meta.field("order").map(value -> switch (value) {
            case Meta.MetaValue.Amount amount -> (int) Math.round(amount.quantity().value());
            case Meta.MetaValue.Text text -> Integer.parseInt(text.text().strip());
            default -> throw new IllegalStateException("'order' is a number, not " + value);
        }).orElse(0);
    }

    /** A free meta field that is one word: {@code level intermediate}, {@code section lab}. */
    public static Optional<String> word(Meta meta, String field) {
        return meta.field(field).map(value -> switch (value) {
            case Meta.MetaValue.Text text -> text.text();
            case Meta.MetaValue.Names names when names.names().size() == 1 -> names.names().getFirst();
            default -> throw new IllegalStateException("'" + field + "' is one word, not " + value);
        });
    }

    private static void catalogueDocument(ExperienceDocument document, List<ExperienceDocument> categories,
                                          List<ExperienceDocument> collections, List<ExperienceDocument> delegated,
                                          List<ExperienceDocument> instruments) {
        if (document.kind() == Kind.CATEGORY) {
            categories.add(document);
        } else if (document.kind() == Kind.COLLECTION) {
            collections.add(document);
        } else if (document.kind() == Kind.INSTRUMENT) {
            instruments.add(document);
        } else {
            delegated.add(document);
        }
    }

    private static List<ShippedFile> files(List<String> locations) {
        PathMatchingResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();
        List<ShippedFile> files = new ArrayList<>();
        for (String location : locations) {
            String root = rootOf(location);
            try {
                for (Resource resource : resolver.getResources(location)) {
                    String url = resource.getURL().toString();
                    String path = url.substring(url.lastIndexOf(root) + root.length());
                    files.add(new ShippedFile(root, path, resource.getContentAsString(StandardCharsets.UTF_8)));
                }
            } catch (IOException failure) {
                throw new UncheckedIOException("The shipped catalogue cannot be read from " + location, failure);
            }
        }
        files.sort(Comparator.comparing(ShippedFile::path));
        return files;
    }

    /** {@code classpath*:catalogue/**}{@code /*.jmx} is rooted at {@code catalogue/}: the folder before any wildcard. */
    private static String rootOf(String location) {
        String pattern = location.substring(location.indexOf(':') + 1);
        int wildcard = pattern.length();
        for (char marker : new char[] {'*', '?', '{'}) {
            int found = pattern.indexOf(marker);
            if (found >= 0 && found < wildcard) {
                wildcard = found;
            }
        }
        String root = pattern.substring(0, pattern.lastIndexOf('/', wildcard) + 1);
        if (root.isEmpty()) {
            throw new IllegalStateException("A catalogue location names a folder before its wildcards: " + location);
        }
        return root;
    }
}
