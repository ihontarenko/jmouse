package org.jmouse.parallax.store.catalogue;

import org.jmouse.parallax.jpa.collection.ObjectCollection;
import org.jmouse.parallax.script.Block;
import org.jmouse.parallax.script.ExperienceDocument;
import org.jmouse.parallax.script.Kind;
import org.jmouse.parallax.RepresentationKind;
import org.jmouse.parallax.object.ObjectKind;
import org.jmouse.parallax.catalogue.ShippedDocumentHandler;
import org.jmouse.parallax.store.catalogue.ShippedCatalogue.ShippedObject;
import org.jmouse.parallax.store.catalogue.Catalogue.ShippedOutcome;
import org.jmouse.parallax.jpa.catalogue.Instrument;
import org.jmouse.parallax.jpa.catalogue.ObjectCategory;
import org.jmouse.parallax.jpa.catalogue.CategoryStore;
import org.jmouse.parallax.store.collection.Collections;
import org.jmouse.parallax.store.collection.Collections.SectionSeed;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * The shipped catalogue, from its {@code .jmx} files: sections, objects — the symbols schematics draw from among them —
 * collections and formula sets, instruments — and the documents of kinds Parallax leaves to the host, courses among
 * them, through each {@link ShippedDocumentHandler}.
 *
 * <p>An object new to the catalogue is created from its file. One nobody has edited takes the file's newer text as its
 * next revision; one a curator has edited keeps the edit, and the object records that its file has a newer text.
 * Sections, collections, courses and instruments are added when missing and never rewritten — an editor may have
 * changed them since.</p>
 *
 * <p>The host decides when it runs, and in which transaction — Parallax keeps it in a start-up step that runs again
 * when {@link #checksum()} moves.</p>
 */
public class CatalogueImport {

    private static final String FORMULA_SET_TAG = Catalogue.FORMULA_SET_TAG;

    private final Catalogue catalogue;
    private final CategoryStore categories;
    private final Collections collections;
    private final Instruments instruments;
    private final List<ShippedDocumentHandler> handlers;
    private final CatalogueProperties properties;

    /**
     * @param handlers the host's handlers for the kinds the catalogue reads but does not keep, in their order
     */
    public CatalogueImport(Catalogue catalogue, CategoryStore categories, Collections collections, Instruments instruments,
                           List<ShippedDocumentHandler> handlers, CatalogueProperties properties) {
        this.catalogue = catalogue;
        this.categories = categories;
        this.collections = collections;
        this.instruments = instruments;
        this.handlers = List.copyOf(handlers);
        this.properties = properties;
    }

    /** One checksum over every shipped file's path and text: what moves when the shipped catalogue changes. */
    public String checksum() {
        return ShippedCatalogue.load(properties.locations()).checksum();
    }

    /** Writes what is missing or newer, and says what it wrote. */
    public Imported apply() {
        ShippedCatalogue shipped = ShippedCatalogue.load(properties.locations());
        requireHandled(shipped.delegated());
        int categoriesWritten = seedCategories(shipped.categories());
        Map<ShippedOutcome, Integer> outcomes = seedObjects(shipped.objects());
        int collectionsWritten = seedCollections(shipped.collections());
        int handledWritten = 0;
        StringBuilder handled = new StringBuilder();
        for (ShippedDocumentHandler handler : handlers) {
            ShippedDocumentHandler.Applied applied = handler.apply(shipped.delegated().stream()
                    .filter(document -> handler.handles(document.kind())).toList());
            handledWritten += applied.written();
            handled.append(applied.summary()).append(", ");
        }
        int instrumentsWritten = seedInstruments(shipped.instruments());
        int objectsWritten = outcomes.getOrDefault(ShippedOutcome.CREATED, 0) + outcomes.getOrDefault(ShippedOutcome.REVISED, 0);
        return new Imported(categoriesWritten + objectsWritten + collectionsWritten + handledWritten + instrumentsWritten,
                "objects " + outcomes + ", " + categoriesWritten + " section(s), " + collectionsWritten
                        + " collection(s), " + handled + instrumentsWritten + " instrument(s)");
    }

    /** Every document of a kind Parallax leaves to the host has a handler, or nothing is written. */
    private void requireHandled(List<ExperienceDocument> delegated) {
        for (ExperienceDocument document : delegated) {
            if (handlers.stream().noneMatch(handler -> handler.handles(document.kind()))) {
                throw new IllegalStateException("The shipped catalogue does not read a " + document.kind().keyword());
            }
        }
    }

    // ── Sections ─────────────────────────────────────────────────────────────────────────────────────────────────

    private int seedCategories(List<ExperienceDocument> documents) {
        int written = 0;
        for (ExperienceDocument document : documents) {
            String code = codeOf(document.slug());
            if (!categories.exists(code)) {
                categories.save(new ObjectCategory(code, document.title(), document.meta().description(),
                        ShippedCatalogue.order(document.meta())));
                categories.flush();
                written++;
            }
        }
        return written;
    }

    // ── Objects ──────────────────────────────────────────────────────────────────────────────────────────────────

    /**
     * Components first, since a circuit compiles against the components it pins; then every other object, symbols
     * among them, and the relations once everything they may name exists — a component's {@code symbol uses} among them.
     */
    private Map<ShippedOutcome, Integer> seedObjects(List<ShippedObject> shippedObjects) {
        Map<ShippedOutcome, Integer> outcomes = new EnumMap<>(ShippedOutcome.class);
        List<String> slugs = new ArrayList<>();
        List<ShippedObject> components = shippedObjects.stream()
                .filter(shipped -> shipped.document().kind() == Kind.COMPONENT).toList();
        for (ShippedObject component : components) {
            write(component.source(), component.name(), outcomes, slugs);
        }
        for (ShippedObject shipped : shippedObjects) {
            if (shipped.document().kind() != Kind.COMPONENT) {
                write(shipped.source(), shipped.name(), outcomes, slugs);
            }
        }
        for (String slug : slugs) {
            catalogue.indexShippedRelations(slug);
        }
        return outcomes;
    }

    private void write(String source, String name, Map<ShippedOutcome, Integer> outcomes, List<String> slugs) {
        Catalogue.ShippedWrite written;
        try {
            written = catalogue.writeShipped(source, name);
        } catch (RuntimeException refused) {
            throw new IllegalStateException("The shipped object " + name + " is refused: "
                    + refused.getMessage(), refused);
        }
        outcomes.merge(written.outcome(), 1, Integer::sum);
        slugs.add(written.slug());
    }

    // ── Collections and formula sets ─────────────────────────────────────────────────────────────────────────────

    /** A collection tagged {@code formula-set} is a formula set; every other one is a shelf. */
    private int seedCollections(List<ExperienceDocument> documents) {
        int written = 0;
        for (ExperienceDocument document : documents) {
            ObjectCollection.Purpose purpose = document.meta().tags().contains(FORMULA_SET_TAG)
                    ? ObjectCollection.Purpose.FORMULA_SET : ObjectCollection.Purpose.SHELF;
            List<SectionSeed> sections = new ArrayList<>();
            for (Block block : document.blocks()) {
                if (block instanceof Block.Items items) {
                    sections.add(new SectionSeed(null, items.references()));
                } else if (block instanceof Block.Section section) {
                    sections.add(new SectionSeed(section.title(), section.references()));
                }
            }
            if (collections.seed(document.slug(), purpose, document.title(), document.meta().summary(),
                    document.meta().description(), sections, ShippedCatalogue.order(document.meta()))) {
                written++;
            }
        }
        return written;
    }

    // ── Instruments ──────────────────────────────────────────────────────────────────────────────────────────────

    private int seedInstruments(List<ExperienceDocument> documents) {
        int written = 0;
        for (ExperienceDocument document : documents) {
            Instrument.Section section = ShippedCatalogue.word(document.meta(), "section")
                    .map(word -> Instrument.Section.valueOf(word.toUpperCase(Locale.ROOT)))
                    .orElseThrow(() -> new IllegalStateException("Instrument '" + document.slug()
                            + "' says neither 'section visualizer' nor 'section lab'"));
            Set<ObjectKind> kinds = new LinkedHashSet<>();
            document.block(Block.ObjectKinds.class).ifPresent(block -> block.kinds()
                    .forEach(kind -> kinds.add(ObjectKind.valueOf(kind.toUpperCase(Locale.ROOT)))));
            List<RepresentationKind> representations = document.block(Block.Representations.class)
                    .map(block -> block.representations().stream()
                            .map(representation -> RepresentationKind.valueOf(representation.toUpperCase(Locale.ROOT)))
                            .toList())
                    .orElse(List.of());
            String opens = document.block(Block.Opens.class).map(Block.Opens::reference).orElse(null);
            Instruments.InstrumentDraft draft = new Instruments.InstrumentDraft(section, document.title(),
                    document.meta().summary(), document.meta().description(), kinds, representations, opens, true,
                    ShippedCatalogue.order(document.meta()));
            if (instruments.seed(codeOf(document.slug()), draft)) {
                written++;
            }
        }
        return written;
    }

    /** {@code linear-algebra} is filed under the code {@code LINEAR_ALGEBRA}; an instrument's code is made the same way. */
    private static String codeOf(String slug) {
        return slug.toUpperCase(Locale.ROOT).replace('-', '_');
    }

    /**
     * What one import wrote.
     *
     * @param written how many things were added or revised
     * @param summary the same in words, by kind: {@code objects {CREATED=3}, 2 section(s), …}
     */
    public record Imported(int written, String summary) {
    }
}
