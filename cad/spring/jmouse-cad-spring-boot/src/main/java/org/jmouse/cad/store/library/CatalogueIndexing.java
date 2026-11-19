package org.jmouse.cad.store.library;

import org.jmouse.cad.document.DocumentInput;
import org.jmouse.cad.document.DocumentNode;
import org.jmouse.cad.folder.DocumentStore;
import org.jmouse.cad.folder.StoredDocument;
import org.jmouse.cad.simulation.CategoryDocument;
import org.jmouse.cad.simulation.SimulationModule;
import org.jmouse.cad.store.catalogue.CadCatalogue;
import org.jmouse.cad.store.catalogue.CadCatalogue.ShippedOutcome;
import org.jmouse.cad.store.catalogue.DocumentReading;
import org.jmouse.cad.store.library.ShippedDocuments.ShippedDocument;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * The catalogue's index, made on every start from the files behind it: the documents libraries ship, read in place,
 * and the library folder's, its files as they stand. Nothing of it is kept anywhere else.
 *
 * <p>Categories first, each after the one it stands under — every other document is filed in one —; circuits last,
 * since a circuit's drawing is checked against the parts it places; relations once everything they may name exists.</p>
 *
 * <p>A file is the truth: one whose text the catalogue would refuse to write — put in the folder by hand, filed in what is
 * no category — is indexed all the same and says why it is refused; no file stops the catalogue being made.</p>
 */
public class CatalogueIndexing {

    private static final Logger LOGGER = LoggerFactory.getLogger(CatalogueIndexing.class);

    private final CadCatalogue     catalogue;
    private final ShippedDocuments shipped;
    private final DocumentStore    folder;
    private final DocumentReading  reading;

    public CatalogueIndexing(CadCatalogue catalogue, ShippedDocuments shipped, DocumentStore folder, DocumentReading reading) {
        this.catalogue = catalogue;
        this.shipped = shipped;
        this.folder = folder;
        this.reading = reading;
    }

    /** One document to index: shipped in a jar, or held in the library folder. */
    private sealed interface Indexed permits Shipped, Held {

        String slug();

        String kind();
    }

    private record Shipped(ShippedDocument document) implements Indexed {

        public String slug() {
            return document.slug();
        }

        public String kind() {
            return document.kind();
        }
    }

    private record Held(StoredDocument document, String kind, CategoryDocument category) implements Indexed {

        public String slug() {
            return document.getSlug();
        }
    }

    /** Indexes everything, and says what it indexed: every text read once, however many passes ask it. */
    public String index() {
        return reading.readingEachOnce(this::indexEverything);
    }

    private String indexEverything() {
        List<Indexed> all = new ArrayList<>();
        // The library folder's first: a document written there holds its address against one a jar ships.
        for (StoredDocument held : folder.open()) {
            all.add(heldOf(held));
        }
        shipped.documents().forEach(document -> all.add(new Shipped(document)));
        Map<ShippedOutcome, Integer> outcomes = new EnumMap<>(ShippedOutcome.class);
        List<String> indexed = new ArrayList<>();
        List<Indexed> tree = categoriesParentsFirst(all);
        for (Indexed category : tree) {
            index(category, outcomes, indexed);
        }
        for (Indexed document : all) {
            if (!tree.contains(document) && !isCircuit(document)) {
                index(document, outcomes, indexed);
            }
        }
        for (Indexed document : all) {
            if (isCircuit(document)) {
                index(document, outcomes, indexed);
            }
        }
        catalogue.indexRelations(indexed);
        String summary = indexed.size() + " documents indexed — shipped " + outcomes + ", the library folder's "
                + all.stream().filter(Held.class::isInstance).count();
        LOGGER.info("📚 Catalogue: {}", summary);
        return summary;
    }

    /**
     * A document of the library folder, its kind and — for a category — the category it says it is, read from its head;
     * a text that does not read is what its place says — its file's name its kind —, and the catalogue says why it is
     * refused when it indexes it.
     */
    private Held heldOf(StoredDocument held) {
        try {
            DocumentNode read = documentOf(folder.heldText(held), held.getSlug());
            CategoryDocument category = read.kind().equals(SimulationModule.CATEGORY) ? CategoryDocument.of(read) : null;
            return new Held(held, read.kind().word(), category);
        } catch (RuntimeException unreadable) {
            return new Held(held, held.getKind(), null);
        }
    }

    /**
     * A text as the language reads it: the catalogue's reading — kept, so its index reads it no more —; one whose model is refused
     * by its grammar alone, which is all a kind and a category ask.
     */
    private DocumentNode documentOf(String source, String origin) {
        try {
            return reading.read(source, origin).document();
        } catch (RuntimeException modelRefused) {
            return reading.language().language().readOne(DocumentInput.text(origin, source));
        }
    }

    private void index(Indexed document, Map<ShippedOutcome, Integer> outcomes, List<String> indexed) {
        switch (document) {
            case Shipped shippedDocument -> {
                ShippedOutcome outcome = catalogue.indexShipped(shippedDocument.document());
                outcomes.merge(outcome, 1, Integer::sum);
                if (outcome == ShippedOutcome.CREATED) {
                    indexed.add(document.slug());
                }
            }
            case Held held -> {
                catalogue.indexHeld(held.document());
                indexed.add(document.slug());
            }
        }
    }

    /**
     * The categories that make a tree, as it reads, each after the one it stands under. One standing under what is no
     * category, or under itself, is left with the other documents: indexed after the tree, refused with its reason.
     */
    private List<Indexed> categoriesParentsFirst(List<Indexed> all) {
        Map<String, Indexed> bySlug = new LinkedHashMap<>();
        Map<String, CategoryDocument> categories = new LinkedHashMap<>();
        for (Indexed document : all) {
            categoryOf(document).filter(ignored -> !bySlug.containsKey(document.slug())).ifPresent(category -> {
                bySlug.put(document.slug(), document);
                categories.put(category.slug(), category);
            });
        }
        List<CategoryDocument> tree = categories.values().stream()
                .filter(category -> CategoryDocument.ringOrOrphan(category, categories::get).isEmpty()).toList();
        return CategoryDocument.parentsFirst(tree).stream().map(category -> bySlug.get(category.slug())).toList();
    }

    /** The category a document says it is: none for another kind, or for a text that does not read. */
    private Optional<CategoryDocument> categoryOf(Indexed document) {
        return switch (document) {
            case Held held -> Optional.ofNullable(held.category());
            case Shipped shippedDocument -> {
                if (!isCategoryKind(document)) {
                    yield Optional.empty();
                }
                try {
                    yield Optional.of(CategoryDocument.of(documentOf(shippedDocument.document().source(), shippedDocument.document().origin())));
                } catch (RuntimeException unreadable) {
                    yield Optional.empty();
                }
            }
        };
    }

    private static boolean isCategoryKind(Indexed document) {
        return document.kind().equals(SimulationModule.CATEGORY.word());
    }

    private static boolean isCircuit(Indexed document) {
        return document.kind().equals(SimulationModule.CIRCUIT.word());
    }
}
