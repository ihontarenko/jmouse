package org.jmouse.cad.store.library;

import org.jmouse.cad.document.DocumentStatus;
import org.jmouse.cad.store.CadRead;
import org.jmouse.cad.pcb.PcbModule;
import org.jmouse.cad.circuit.Circuit;
import org.jmouse.cad.component.Component;
import org.jmouse.cad.component.Drawing;
import org.jmouse.cad.component.Seat;
import org.jmouse.cad.document.DocumentOwner;
import org.jmouse.cad.document.MetaNode;
import org.jmouse.cad.document.Provenance;
import org.jmouse.cad.folder.DocumentStore;
import org.jmouse.cad.folder.StoredDocument;
import org.jmouse.cad.library.CadLanguage;
import org.jmouse.cad.library.CadStandards;
import org.jmouse.cad.library.DocumentIndex;
import org.jmouse.cad.packages.pcb.PcbRules;
import org.jmouse.cad.packages.footprint.LandPatternRules;
import org.jmouse.cad.schematic.drawing.SymbolDrawing;
import org.jmouse.cad.simulation.ComponentCompilation;
import org.jmouse.cad.simulation.EvaluatedParameter;
import org.jmouse.cad.simulation.ParameterSliders;
import org.jmouse.cad.simulation.PartRole;
import org.jmouse.cad.simulation.SimulatedComponent;
import org.jmouse.cad.simulation.SimulationLines;
import org.jmouse.cad.simulation.SimulationModule;
import org.jmouse.cad.store.authority.CadAuthority;
import org.jmouse.cad.store.authority.CadCapability;
import org.jmouse.cad.store.authority.CadPlace;
import org.jmouse.cad.store.authority.CadPlaces;
import org.jmouse.cad.store.catalogue.CadCatalogue;
import org.jmouse.cad.store.catalogue.CatalogueViews.DocumentSummary;
import org.jmouse.cad.store.catalogue.CatalogueViews.LocationView;
import org.jmouse.cad.store.catalogue.DocumentAccess;
import org.jmouse.cad.store.catalogue.DocumentReading;
import org.jmouse.cad.store.catalogue.ForeignDocuments;
import org.jmouse.cad.store.catalogue.ModelFacts;
import org.jmouse.cad.store.catalogue.PartDocuments;
import org.jmouse.cad.store.catalogue.PlaceSource;
import org.jmouse.cad.store.catalogue.ReadDocument;
import org.jmouse.cad.store.catalogue.StoredDocuments;
import org.jmouse.cad.store.problem.CadProblem;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * The library a sheet and a pcb are built from, as an editor asks it — {@code JMOUSE_CAD_API.md} §2.3: the components
 * a reader may place, one of them with its lineage applied and its drawings resolved, the symbols, portraits, frames and
 * footprints with their drawings, and the standards they are checked against.
 *
 * <p>What a list holds is what the reader may see: the shared catalogue's documents — its drafts for a curator — and,
 * when an owner is named, the documents of every owner of its place — a project's own and its workspace's —, once the
 * reader may read there.</p>
 */
public class PartLibrary {

    /** A component as a palette lists it. */
    public record ComponentReference(String reference, String title, String type, LocationView location, DocumentOwner owner,
                                     String status) {
    }

    /** A drawing a component is shown with: the document's slug, its code where it writes one, and its drawing. */
    public record DrawnPart(String slug, String code, Object drawing) {
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
     * @param role       what a part of it does in a circuit ({@link PartRole}): a source or an instrument is the bench's
     */
    public record ComponentView(SimulatedComponent component, List<String> lineage, List<DrawnPart> symbols,
                                DrawnPart portrait, List<Seat> footprints, PartFace face,
                                List<ParameterSliders.Slider> sheetParameters, PartRole role) {
    }

    /** A drawn document — a symbol, a portrait, a frame, a footprint — with its drawing. */
    public record DrawnDocument(String slug, String title, String summary, LocationView location, DocumentOwner owner,
                                String folder, String code, Provenance provenance, Object drawing) {
    }

    /** A component of a file read in place, compiled as one placed from it is: by the reference its circuits name it by. */
    public record ForeignComponent(String reference, ComponentView view) {
    }

    /**
     * What a file read in place draws its circuits' parts from: each of its components compiled, its parts found first
     * among its own documents; and what could not be — a component that does not compile — in words.
     */
    public record ForeignParts(List<ForeignComponent> components, List<String> notes) {
    }

    /** A footprint and the components seated on it that the reader may see. */
    public record FootprintDetail(DrawnDocument footprint, List<DocumentSummary> usedBy) {
    }

    private final DocumentStore  documents;
    private final DocumentAccess access;
    private final CadCatalogue   catalogue;
    private final CadAuthority   authority;
    private final CadPlaces      places;
    private final CadLanguage     language;
    private final DocumentReading reading;

    public PartLibrary(DocumentStore documents, DocumentAccess access, CadCatalogue catalogue, CadAuthority authority,
                       CadPlaces places, CadLanguage language, DocumentReading reading) {
        this.documents = documents;
        this.reading = reading;
        this.language = language;
        this.access = access;
        this.catalogue = catalogue;
        this.authority = authority;
        this.places = places;
    }

    /** Every component the reader may place: the catalogue's, and those of the owner's place when one is named. */
    @CadRead
    public List<ComponentReference> components(DocumentOwner owner) {
        StoredDocuments scope = catalogue.scope(owner);
        String kind = SimulationModule.COMPONENT.word();
        List<PlaceSource.PlacedDocument> placed = placedOf(scope, kind);
        return java.util.stream.Stream.concat(
                placed.stream().map(document -> new ComponentReference(document.read().index().slug(), document.read().index().title(),
                        document.read().document().type().orElse(null), LocationView.of(document.location()), owner,
                        DocumentStatus.PUBLISHED.name())),
                besidePlaced(visible(kind, owner), placed).stream()
                        .map(document -> new ComponentReference(document.getSlug(), document.getTitle(), typeOf(scope, document),
                                                                location(document), document.owner().orElse(null),
                                                                document.getStatus().name())))
                .toList();
    }

    /**
     * One component, by slug or {@code slug@revision}, compiled as a part of it runs — or a circuit, as it is placed on
     * another sheet as a block: its ports its pins, its parameters what each block sets.
     *
     * @param owner the place it is asked from; null for the place it is in — a slug names one document, and an owner's
     *              document is read, as always, only by a reader who may read that owner's place
     */
    @CadRead
    public ComponentView component(String reference, DocumentOwner owner) {
        DocumentOwner place = owner != null ? owner : placeOf(reference);
        requireOwner(place);
        StoredDocuments scope = catalogue.scope(place);
        ReadDocument read = scope.resolve(reference).filter(found -> scope.isPlaced(found) || readable(found))
                .orElseThrow(() -> CadProblem.notFound("The component"));
        return componentView(reference, read, scope);
    }

    /**
     * The components of a file read in place — a KiCad design's —, each compiled as a part of it is placed, the documents
     * it names found among the file's own before the catalogue's, as the reader sees it. Nothing is stored.
     *
     * @param sources the file's documents, as jM
     */
    @CadRead
    public ForeignParts foreignParts(List<String> sources) {
        List<String> notes = new ArrayList<>();
        List<ReadDocument> read = new ArrayList<>();
        for (String source : sources) {
            try {
                read.add(reading.read(source, "a document of the file"));
            } catch (CadProblem refused) {
                notes.add(refused.getMessage());
            }
        }
        ForeignDocuments scope = new ForeignDocuments(read, catalogue.scope(null));
        List<ForeignComponent> components = new ArrayList<>();
        for (ReadDocument document : scope.documents()) {
            if (!(document.model().orElse(null) instanceof Component)) {
                continue;
            }
            String reference = document.index().slug();
            try {
                components.add(new ForeignComponent(reference, componentView(reference, document, scope)));
            } catch (CadProblem refused) {
                notes.add(reference + ": " + refused.getMessage());
            }
        }
        return new ForeignParts(components, notes);
    }

    /** A component read, compiled with its lineage applied and its drawings resolved where the scope finds them. */
    private ComponentView componentView(String reference, ReadDocument read, PartDocuments scope) {
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
                                 seatsOf(component, scope), face, List.of(), compiled.role());
    }

    /**
     * The footprints a component sits on, each named by its slug: a footprint its text names by code is served by the slug
     * the scope finds for it, so what reads a seat — a pcb, the interface — knows slugs alone. One the scope finds no
     * document for is served as written.
     */
    private static List<Seat> seatsOf(Component component, PartDocuments scope) {
        return component.seats().stream()
                .map(seat -> new Seat(scope.slugOf(seat.footprint()).orElse(seat.footprint()), seat.pads()))
                .toList();
    }

    /** A circuit as a block of it is placed: drawn as a sheet's frame, its letters its own when its text names them. */
    private static ComponentView sheetView(Circuit sheet) {
        String designator = sheet.head().meta().get(MetaNode.DESIGNATOR);
        PartFace face = new PartFace(designator, null, List.of(), Map.of(), null, List.of(), List.of(), Map.of(), List.of());
        return new ComponentView(SimulatedComponent.sheet(sheet), List.of(sheet.head().slug()), List.of(), null, List.of(),
                                 face, ModelFacts.slidersOf(sheet), PartRole.DISSIPATING);
    }

    /**
     * The documents of one kind the reader may see — the catalogue's and the owner's place's —, each with its model and
     * where it is held: one shelf of the library, whichever module brings the kind.
     *
     * @throws CadProblem when no module of the language brings that kind
     */
    @CadRead
    public List<DrawnDocument> shelf(String kind, DocumentOwner owner) {
        if (language.modules().stream().flatMap(module -> module.kinds().stream()).noneMatch(known -> known.word().equals(kind))) {
            throw CadProblem.notFound("The kind of document '" + kind + "'");
        }
        return drawn(kind, owner);
    }

    /** A footprint the reader may see, and the components that name it as theirs. */
    @CadRead
    public FootprintDetail footprint(String slug) {
        StoredDocument document = access.requireReadable(slug);
        if (!document.getKind().equals(PcbModule.FOOTPRINT.word())) {
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
    public PcbRules pcbRules() {
        return CadStandards.pcbRules();
    }

    // ── What a reader may see ────────────────────────────────────────────────────────────────────────────────────

    /**
     * The documents of one kind the reader may see: the catalogue's, then those of every owner of the owner's place — a
     * project's own and its workspace's —, each by title.
     */
    private List<StoredDocument> visible(String kind, DocumentOwner owner) {
        CadPlace place = requireOwner(owner);
        List<StoredDocument> found = new ArrayList<>(documents.findByKind(kind).stream()
                .filter(StoredDocument::isCatalogue).filter(access::canRead).toList());
        for (DocumentOwner seen : place.chain()) {
            found.addAll(documents.owned(seen, kind));
        }
        found.sort(Comparator.comparing(StoredDocument::getTitle, String.CASE_INSENSITIVE_ORDER));
        return found;
    }

    /** The owner whose place a referenced document is in; null for the catalogue's, or for none. */
    private DocumentOwner placeOf(String reference) {
        return documents.findBySlug(DocumentIndex.slugOf(reference)).flatMap(StoredDocument::owner).orElse(null);
    }

    /** The owner's place, once the reader may read there; the installation for no owner. */
    private CadPlace requireOwner(DocumentOwner owner) {
        if (owner == null) {
            return CadPlace.installation();
        }
        CadPlace place = places.requireOwner(owner);
        authority.require(CadCapability.OWNER_READ, place);
        return place;
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
        List<PlaceSource.PlacedDocument> placed = placedOf(scope, kind);
        return java.util.stream.Stream.concat(placed.stream().map(document -> drawnDocument(document, owner)),
                besidePlaced(visible(kind, owner), placed).stream().map(document -> drawnDocument(scope, document))).toList();
    }

    /** What the place holds besides the store of one kind. */
    private static List<PlaceSource.PlacedDocument> placedOf(StoredDocuments scope, String kind) {
        return scope.placed().stream().filter(document -> kind.equals(document.read().index().kind())).toList();
    }

    /** The store's, without those a document held besides it stands in front of: an address names one document here. */
    private static List<StoredDocument> besidePlaced(List<StoredDocument> stored, List<PlaceSource.PlacedDocument> placed) {
        Set<String> addresses = placed.stream().map(document -> document.read().index().slug()).collect(Collectors.toSet());
        return stored.stream().filter(document -> !addresses.contains(document.getSlug())).toList();
    }

    /** A drawn document held besides the store. */
    private static DrawnDocument drawnDocument(PlaceSource.PlacedDocument placed, DocumentOwner owner) {
        ReadDocument read = placed.read();
        Optional<MetaNode> meta = read.document().block(MetaNode.class);
        return new DrawnDocument(read.index().slug(), read.index().title(), read.index().summary(), LocationView.of(placed.location()),
                                 owner, null, read.index().code(), meta.map(Provenance::of).orElse(Provenance.OURS),
                                 read.model().orElse(null));
    }

    /** A drawn document; one whose text no longer reads is listed without its drawing, never dropped from the list. */
    private static DrawnDocument drawnDocument(StoredDocuments scope, StoredDocument document) {
        ReadDocument read;
        try {
            read = scope.head(document);
        } catch (CadProblem unreadable) {
            return new DrawnDocument(document.getSlug(), document.getTitle(), document.getSummary(), location(document),
                                     document.owner().orElse(null), document.getFolder(), null,
                                     new Provenance(null, null, null), null);
        }
        Optional<MetaNode> meta = read.document().block(MetaNode.class);
        return new DrawnDocument(document.getSlug(), document.getTitle(), document.getSummary(), location(document),
                                 document.owner().orElse(null), document.getFolder(),
                                 meta.flatMap(found -> found.text(MetaNode.CODE)).orElse(null),
                                 meta.map(Provenance::of).orElse(Provenance.OURS),
                                 read.model().orElse(null));
    }

    /** The drawings a {@code symbol} or {@code portrait} line names, each resolved; one the catalogue lacks is left out. */
    private static List<DrawnPart> drawings(Drawing drawing, PartDocuments scope) {
        if (drawing == null) {
            return List.of();
        }
        return drawing.uses().stream()
                .map(slug -> scope.resolve(slug).map(read -> new DrawnPart(read.index().slug(), read.index().code(), read.model().orElse(null))))
                .flatMap(Optional::stream)
                .toList();
    }

    /** From the most general base to the component itself; a base the catalogue lacks ends the walk. */
    private static List<String> lineage(Component component, PartDocuments scope) {
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

    private static LocationView location(StoredDocument document) {
        return LocationView.of(document);
    }
}
