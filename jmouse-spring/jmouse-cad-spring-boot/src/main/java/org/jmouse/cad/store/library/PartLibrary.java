package org.jmouse.cad.store.library;

import org.jmouse.cad.board.BoardModule;
import org.jmouse.cad.circuit.Circuit;
import org.jmouse.cad.component.Component;
import org.jmouse.cad.component.Drawing;
import org.jmouse.cad.component.Seat;
import org.jmouse.cad.document.MetaNode;
import org.jmouse.cad.jpa.catalogue.DocumentOwner;
import org.jmouse.cad.jpa.catalogue.DocumentStore;
import org.jmouse.cad.jpa.catalogue.StoredDocument;
import org.jmouse.cad.library.CadStandards;
import org.jmouse.cad.packages.board.BoardRules;
import org.jmouse.cad.packages.footprint.LandPatternRules;
import org.jmouse.cad.schematic.SchematicModule;
import org.jmouse.cad.schematic.drawing.SymbolDrawing;
import org.jmouse.cad.simulation.ComponentCompilation;
import org.jmouse.cad.simulation.EvaluatedParameter;
import org.jmouse.cad.simulation.SimulatedComponent;
import org.jmouse.cad.simulation.SimulationLines;
import org.jmouse.cad.simulation.SimulationModule;
import org.jmouse.cad.store.authority.CadAuthority;
import org.jmouse.cad.store.authority.CadCapability;
import org.jmouse.cad.store.authority.CadPlaces;
import org.jmouse.cad.library.DocumentIndex;
import org.jmouse.cad.store.catalogue.CadCatalogue;
import org.jmouse.cad.store.catalogue.CatalogueViews.DocumentSummary;
import org.jmouse.cad.store.catalogue.CatalogueViews.Origin;
import org.jmouse.cad.store.catalogue.DocumentAccess;
import org.jmouse.cad.store.catalogue.ModelFacts;
import org.jmouse.cad.simulation.ParameterSliders;
import org.jmouse.cad.store.catalogue.ReadDocument;
import org.jmouse.cad.store.catalogue.StoredDocuments;
import org.jmouse.cad.store.problem.CadProblem;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * The library a sheet and a board are built from, as an editor asks it — {@code JMOUSE_CAD_API.md} §2.3: the components
 * a reader may place, one of them with its lineage applied and its drawings resolved, the symbols, portraits, frames and
 * footprints with their drawings, and the standards they are checked against.
 *
 * <p>What a list holds is what the reader may see: the shared catalogue's documents — its drafts for a curator — and,
 * when an owner is named, that owner's own, once the reader may read there.</p>
 */
public class PartLibrary {

    /** A component as a palette lists it. */
    public record ComponentReference(String reference, String title, String type, Origin origin, DocumentOwner owner,
                                     String status) {
    }

    /** A drawing a component is shown with: the document's slug and its drawing. */
    public record DrawnPart(String slug, Object drawing) {
    }

    /**
     * A component as a part of it is placed: compiled with its lineage applied, and every drawing it names resolved.
     *
     * @param lineage   from the most general base to itself
     * @param symbols   every symbol it may be drawn with, the default first
     * @param portrait  how it looks in the hand, or null
     * @param footprints every footprint its package lands on, the default first, with the pads each pin takes
     * @param face       how a part of it is drawn and read on a sheet
     * @param sheetParameters a sheet's sliders, each block of it setting its own; empty for a component
     */
    public record ComponentView(SimulatedComponent component, List<String> lineage, List<DrawnPart> symbols,
                                DrawnPart portrait, List<Seat> footprints, PartFace face,
                                List<ParameterSliders.Slider> sheetParameters) {
    }

    /** Where a drawing came from, as its text's {@code meta} says; a field not written is null. */
    public record Provenance(String source, String authors, String license) {
    }

    /** A drawn document — a symbol, a portrait, a frame, a footprint — with its drawing. */
    public record DrawnDocument(String slug, String title, String summary, Origin origin, DocumentOwner owner,
                                String folder, String code, Provenance provenance, Object drawing) {
    }

    /** A footprint and the components seated on it that the reader may see. */
    public record FootprintDetail(DrawnDocument footprint, List<DocumentSummary> usedBy) {
    }

    private final DocumentStore  documents;
    private final DocumentAccess access;
    private final CadCatalogue   catalogue;
    private final CadAuthority   authority;
    private final CadPlaces      places;

    public PartLibrary(DocumentStore documents, DocumentAccess access, CadCatalogue catalogue, CadAuthority authority,
                       CadPlaces places) {
        this.documents = documents;
        this.access = access;
        this.catalogue = catalogue;
        this.authority = authority;
        this.places = places;
    }

    /** Every component the reader may place: the catalogue's, and the owner's own when one is named. */
    @Transactional(readOnly = true)
    public List<ComponentReference> components(DocumentOwner owner) {
        StoredDocuments scope = catalogue.scope(owner);
        return visible(SimulationModule.COMPONENT.word(), owner).stream()
                .map(document -> new ComponentReference(document.getSlug(), document.getTitle(), typeOf(scope, document),
                                                        origin(document), document.owner().orElse(null),
                                                        document.getStatus().name()))
                .toList();
    }

    /**
     * One component, by slug or {@code slug@revision}, compiled as a part of it runs — or a circuit, as it is placed on
     * another sheet as a block: its ports its pins, its parameters what each block sets.
     *
     * @param owner the place it is asked from; null for the place it is in — a slug names one document, and an owner's
     *              document is read, as always, only by a reader who may read that owner's place
     */
    @Transactional(readOnly = true)
    public ComponentView component(String reference, DocumentOwner owner) {
        DocumentOwner place = owner != null ? owner : placeOf(reference);
        requireOwner(place);
        StoredDocuments scope = catalogue.scope(place);
        ReadDocument read = scope.resolve(reference).filter(this::readable)
                .orElseThrow(() -> CadProblem.notFound("The component"));
        Object model = read.model().orElse(null);
        if (model instanceof Circuit sheet) {
            return sheetView(sheet);
        }
        if (!(model instanceof Component component)) {
            throw CadProblem.invalid("'" + reference + "' is a " + read.index().kind() + ", not a component.");
        }
        SimulatedComponent compiled;
        try {
            compiled = ComponentCompilation.compile(component, scope);
        } catch (IllegalArgumentException | IllegalStateException refused) {
            throw new CadProblem.Unprocessable("component-refused", refused.getMessage());
        }
        DrawnPart portrait = drawings(component.portrait(), scope).stream().findFirst().orElse(null);
        PartFace face;
        try {
            face = PartFace.of(component, compiled.lineage(),
                               portrait != null && portrait.drawing() instanceof SymbolDrawing drawn ? drawn : null,
                               compiled.parameters().stream().map(EvaluatedParameter::name).collect(Collectors.toSet()));
        } catch (IllegalArgumentException refused) {
            throw new CadProblem.Unprocessable("component-refused", refused.getMessage());
        }
        return new ComponentView(compiled, lineage(component, scope), drawings(component.symbol(), scope), portrait,
                                 component.seats(), face, List.of());
    }

    /** A circuit as a block of it is placed: drawn as a sheet's frame, its letters its own when its text names them. */
    private static ComponentView sheetView(Circuit sheet) {
        String designator = sheet.head().meta().get(MetaNode.DESIGNATOR);
        PartFace face = new PartFace(designator, null, List.of(), Map.of(), null, List.of(), List.of(), Map.of(), List.of());
        return new ComponentView(SimulatedComponent.sheet(sheet), List.of(sheet.head().slug()), List.of(), null, List.of(),
                                 face, ModelFacts.slidersOf(sheet));
    }

    @Transactional(readOnly = true)
    public List<DrawnDocument> symbols(DocumentOwner owner) {
        return drawn(SchematicModule.SYMBOL.word(), owner);
    }

    @Transactional(readOnly = true)
    public List<DrawnDocument> portraits(DocumentOwner owner) {
        return drawn(SchematicModule.PORTRAIT.word(), owner);
    }

    @Transactional(readOnly = true)
    public List<DrawnDocument> frames(DocumentOwner owner) {
        return drawn(SchematicModule.FRAME.word(), owner);
    }

    @Transactional(readOnly = true)
    public List<DrawnDocument> footprints(DocumentOwner owner) {
        return drawn(BoardModule.FOOTPRINT.word(), owner);
    }

    /** A footprint the reader may see, and the components that name it as theirs. */
    @Transactional(readOnly = true)
    public FootprintDetail footprint(String slug) {
        StoredDocument document = access.requireReadable(slug);
        if (!document.getKind().equals(BoardModule.FOOTPRINT.word())) {
            throw CadProblem.notFound("The footprint");
        }
        return new FootprintDetail(drawnDocument(catalogue.scope(document.owner().orElse(null)), document),
                                   catalogue.usedBy(document.getId(), SimulationLines.FOOTPRINT.word()));
    }

    /** The land pattern rules the library's footprints are generated with. */
    public LandPatternRules footprintRules() {
        return CadStandards.landPatternRules();
    }

    /** The ways a printed circuit board can be made. */
    public BoardRules boardRules() {
        return CadStandards.boardRules();
    }

    // ── What a reader may see ────────────────────────────────────────────────────────────────────────────────────

    /** The documents of one kind the reader may see: the catalogue's, then the owner's, each by title. */
    private List<StoredDocument> visible(String kind, DocumentOwner owner) {
        requireOwner(owner);
        List<StoredDocument> found = new ArrayList<>(documents.findByKind(kind).stream()
                .filter(StoredDocument::isCatalogue).filter(access::canRead).toList());
        if (owner != null) {
            found.addAll(documents.owned(owner, kind));
        }
        found.sort(Comparator.comparing(StoredDocument::getTitle, String.CASE_INSENSITIVE_ORDER));
        return found;
    }

    /** The owner whose place a referenced document is in; null for the catalogue's, or for none. */
    private DocumentOwner placeOf(String reference) {
        return documents.findBySlug(DocumentIndex.slugOf(reference)).flatMap(StoredDocument::owner).orElse(null);
    }

    private void requireOwner(DocumentOwner owner) {
        if (owner != null) {
            authority.require(CadCapability.OWNER_READ, places.requireOwner(owner));
        }
    }

    /** A catalogue document as the reader may see it; an owner's one is reached only through its own scope. */
    private boolean readable(ReadDocument read) {
        return documents.findBySlug(read.index().slug())
                .filter(document -> document.owner().isPresent() || access.canRead(document))
                .isPresent();
    }

    // ── Views ────────────────────────────────────────────────────────────────────────────────────────────────────

    private List<DrawnDocument> drawn(String kind, DocumentOwner owner) {
        StoredDocuments scope = catalogue.scope(owner);
        return visible(kind, owner).stream().map(document -> drawnDocument(scope, document)).toList();
    }

    /** A drawn document; one whose text no longer reads is listed without its drawing, never dropped from the list. */
    private static DrawnDocument drawnDocument(StoredDocuments scope, StoredDocument document) {
        ReadDocument read;
        try {
            read = scope.head(document);
        } catch (CadProblem unreadable) {
            return new DrawnDocument(document.getSlug(), document.getTitle(), document.getSummary(), origin(document),
                                     document.owner().orElse(null), document.getFolder(), null,
                                     new Provenance(null, null, null), null);
        }
        Optional<MetaNode> meta = read.document().block(MetaNode.class);
        return new DrawnDocument(document.getSlug(), document.getTitle(), document.getSummary(), origin(document),
                                 document.owner().orElse(null), document.getFolder(),
                                 meta.flatMap(found -> found.text(MetaNode.CODE)).orElse(null),
                                 new Provenance(meta.flatMap(found -> found.text(MetaNode.SOURCE)).orElse(null),
                                                meta.flatMap(found -> found.text(MetaNode.AUTHORS)).orElse(null),
                                                meta.flatMap(found -> found.text(MetaNode.LICENSE)).orElse(null)),
                                 read.model().orElse(null));
    }

    /** The drawings a {@code symbol} or {@code portrait} line names, each resolved; one the catalogue lacks is left out. */
    private static List<DrawnPart> drawings(Drawing drawing, StoredDocuments scope) {
        if (drawing == null) {
            return List.of();
        }
        return drawing.uses().stream()
                .map(slug -> scope.resolve(slug).map(read -> new DrawnPart(read.index().slug(), read.model().orElse(null))))
                .flatMap(Optional::stream)
                .toList();
    }

    /** From the most general base to the component itself; a base the catalogue lacks ends the walk. */
    private static List<String> lineage(Component component, StoredDocuments scope) {
        List<String> lineage = new ArrayList<>();
        Set<String> met = new LinkedHashSet<>();
        Component current = component;
        while (current != null && met.add(current.head().slug())) {
            lineage.addFirst(current.head().slug());
            current = current.bases().isEmpty() ? null : scope.resolve(current.bases().getFirst())
                    .flatMap(ReadDocument::model).filter(Component.class::isInstance).map(Component.class::cast)
                    .orElse(null);
        }
        return lineage;
    }

    private static String typeOf(StoredDocuments scope, StoredDocument document) {
        try {
            return scope.head(document).document().type().orElse(null);
        } catch (CadProblem unreadable) {
            return null;
        }
    }

    private static Origin origin(StoredDocument document) {
        return document.isCatalogue() ? Origin.CATALOGUE : Origin.OWNED;
    }
}
