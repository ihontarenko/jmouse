package org.jmouse.cad.store.exchange;

import org.jmouse.cad.settings.CadSettings;
import org.jmouse.cad.store.CadRead;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.function.BiFunction;
import java.util.function.Supplier;
import org.jmouse.cad.simulation.CatalogueDocuments;
import java.util.HashSet;
import org.jmouse.cad.document.DocumentInput;
import java.util.zip.ZipOutputStream;
import java.util.zip.ZipEntry;
import java.util.stream.Collectors;
import java.util.Set;
import java.nio.charset.StandardCharsets;
import java.io.UncheckedIOException;
import java.io.IOException;
import java.io.ByteArrayOutputStream;
import org.jmouse.cad.exchange.ExportedFile;
import org.jmouse.cad.exchange.DocumentFlattening;
import org.jmouse.cad.exchange.OutputJob;
import org.jmouse.cad.pcb.PcbModule;
import org.jmouse.cad.pcb.PcbVocabulary;
import org.jmouse.cad.pcb.drawing.PcbDrawing;
import java.util.HashMap;
import org.jmouse.cad.store.parts.FetchedFilesKeeper;
import org.jmouse.cad.store.files.DocumentFiles;
import org.jmouse.cad.document.part.FetchedFile;
import org.jmouse.cad.document.DocumentKind;
import org.jmouse.cad.document.DocumentNode;
import org.jmouse.cad.exchange.DocumentExport;
import org.jmouse.cad.pcb.checks.PcbFinding;
import org.jmouse.cad.document.FindingSeverity;
import org.jmouse.cad.library.CadStandards;
import org.jmouse.cad.exchange.pcb.PcbFabrication;
import org.jmouse.cad.exchange.pcb.PcbArtwork;
import org.jmouse.cad.exchange.pcb.ArtworkAreas;
import org.jmouse.cad.exchange.pcb.PartBodies;
import org.jmouse.cad.exchange.pcb.PcbSolid;
import org.jmouse.cad.exchange.pcb.CircuitBoards;
import org.jmouse.cad.pcb.layout.PcbLayout;
import org.jmouse.cad.pcb.geometry.PcbDrills;
import org.jmouse.cad.pcb.geometry.CopperNets;
import org.jmouse.cad.pcb.geometry.CopperShape;
import org.jmouse.cad.pcb.geometry.PadOutline;
import org.jmouse.cad.pcb.physics.CopperPaths;
import org.jmouse.cad.pcb.physics.CopperPhysics;
import org.jmouse.cad.pcb.physics.StackPhysics;
import org.jmouse.cad.pcb.physics.TrackStacks;
import org.jmouse.cad.pcb.routing.RoutingBoard;
import org.jmouse.cad.pcb.routing.ViaRows;
import org.jmouse.cad.pcb.drawing.NetRules;
import org.jmouse.cad.pcb.geometry.AreaOffsets;
import org.jmouse.cad.pcb.geometry.NetLengths;
import org.jmouse.cad.pcb.routing.RoutingSettings;
import org.jmouse.cad.pcb.drawing.FootprintDrawing;
import org.locationtech.jts.geom.Geometry;
import org.jmouse.strokefont.StrokeFont;
import org.jmouse.cad.exchange.sheet.SheetChecks;
import org.jmouse.cad.exchange.sheet.SheetFinding;
import org.jmouse.cad.simulation.SimulationModule;
import org.jmouse.cad.simulation.SimulationReading;
import org.jmouse.cad.exchange.ExportPurpose;
import org.jmouse.cad.exchange.DocumentImport;
import org.jmouse.cad.exchange.ExchangeFiles;
import org.jmouse.cad.exchange.FileEncoding;
import org.jmouse.cad.exchange.ExchangeRefusal;
import org.jmouse.cad.exchange.ExportedFiles;
import org.jmouse.cad.exchange.ImportedDocuments;
import org.jmouse.cad.document.DocumentOwner;
import org.jmouse.cad.folder.DocumentStore;
import org.jmouse.cad.folder.RevisionOrigin;
import org.jmouse.cad.folder.StoredDocument;
import org.jmouse.cad.library.CadLanguage;
import org.jmouse.cad.library.LicenceLedger;
import org.jmouse.cad.store.authority.CadAuthority;
import org.jmouse.cad.store.authority.CadCapability;
import org.jmouse.cad.store.authority.CadPlaces;
import org.jmouse.cad.store.catalogue.CadCatalogue;
import org.jmouse.cad.store.catalogue.StoredDocuments;
import org.jmouse.cad.store.licence.DocumentLicences;
import org.jmouse.cad.store.problem.CadProblem;
import org.jmouse.el.grammar.GrammarRefusal;
import org.jmouse.plugin.Extensions;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Documents to and from the formats the plugins brought — {@code JMOUSE_CAD_API.md} §2.6. A format only converts; what
 * every format shares is here: whose components a conversion may see, who may read and store, how a refusal is said, and
 * how what an import made is stored beside what the owner holds.
 */
public class DocumentExchange {

    /**
     * A format as the list offers it: what it reads, what it writes.
     *
     * @param readsAlone those of {@code reads} read on their own, with nothing beside them sent
     * @param encoding   how its files travel to it: {@code TEXT}, or {@code BINARY} in Base64
     * @param readsAsLibrary those of {@code reads} that are a library: their holder and terms are asked of the reader
     */
    public record Format(String id, String title, List<String> reads, List<String> readsAlone, List<String> readsAsBytes,
                         FileEncoding encoding, List<String> readsAsLibrary, List<String> writes) {
    }

    /** What a read made, unstored: each document as jM text, the one it stands for first. */
    public record Read(List<ReadDocument> documents, List<String> notes) {
    }

    public record ReadDocument(String slug, String title, String kind, String source) {
    }

    /**
     * What an import stored.
     *
     * @param root    the slug of what the import stands for: a design's root circuit, or the one thing it made
     * @param created what was stored new, by slug
     * @param updated what an earlier import made, written over where this one says otherwise
     * @param reused  what the owner or the catalogue already held, kept as it is
     */
    public record Imported(String root, List<String> created, List<String> updated, List<String> reused,
                           List<String> notes) {
    }

    /** What a release of a document never stored is called. */
    private static final String UNSTORED = "unsaved";

    private final Extensions    extensions;
    private final CadCatalogue  catalogue;
    private final DocumentStore documents;
    private final CadAuthority  authority;
    private final CadPlaces     places;
    private final CadLanguage   language;
    private final FetchedFilesKeeper keeper;
    private final DocumentLicences licences;

    /**
     * @param files    where a document's files are kept; null for an installation that keeps none
     * @param licences what the licences of the works a written document carries let it leave as
     */
    public DocumentExchange(Extensions extensions, CadCatalogue catalogue, DocumentStore documents, CadAuthority authority,
                            CadPlaces places, CadLanguage language, DocumentFiles files, DocumentLicences licences) {
        this.extensions = extensions;
        this.catalogue = catalogue;
        this.documents = documents;
        this.authority = authority;
        this.places = places;
        this.language = language;
        this.keeper = new FetchedFilesKeeper(files);
        this.licences = licences;
    }

    /** Every format a plugin brought, read or written; none without a plugin. */
    public List<Format> formats() {
        Map<String, Format> formats = new LinkedHashMap<>();
        for (DocumentImport reading : extensions.contributions(DocumentImport.POINT)) {
            formats.put(reading.id(), new Format(reading.id(), reading.title(), reading.fileNames(), reading.fileNamesReadAlone(),
                    reading.fileNamesAsBytes(), reading.encoding(), reading.libraryFileNames(), List.of()));
        }
        for (DocumentExport writing : extensions.contributions(DocumentExport.POINT)) {
            if (!writing.available()) {
                continue;
            }
            List<String> kinds = kindsWrittenBy(writing);
            Format known = formats.get(writing.id());
            formats.put(writing.id(), known == null
                    ? new Format(writing.id(), writing.title(), List.of(), List.of(), List.of(), FileEncoding.TEXT, List.of(), kinds)
                    : new Format(known.id(), known.title(), known.reads(), known.readsAlone(), known.readsAsBytes(), known.encoding(), known.readsAsLibrary(), kinds));
        }
        return List.copyOf(formats.values());
    }

    /** Files read as documents and handed back, nothing stored: a netlist opened in an editor. */
    @CadRead
    public Read read(String format, ExchangeFiles files, DocumentOwner owner) {
        ImportedDocuments made = converted(() -> importOf(format).read(files, scope(owner)));
        List<ReadDocument> read = new ArrayList<>();
        read.add(readDocument(made.root()));
        for (DocumentNode document : allOf(made)) {
            if (document != made.root()) {
                read.add(readDocument(document));
            }
        }
        return new Read(read, made.notes());
    }

    /**
     * Files read as documents and stored as the owner's: what a design is built from first, each kept as it is where
     * something is already called so — an earlier import's untouched first revision excepted, which this one writes
     * over —, then the circuits, each written over where an earlier import of this owner made it.
     */
    @Transactional
    public Imported importInto(DocumentOwner owner, String format, ExchangeFiles files) {
        authority.require(CadCapability.OWNER_WRITE, places.requireOwner(owner));
        ImportedDocuments made = converted(() -> importOf(format).read(files, catalogue.scope(owner)));
        List<String> created = new ArrayList<>();
        List<String> updated = new ArrayList<>();
        List<String> reused = new ArrayList<>();
        DocumentOwner libraryOwner = libraryOwnerOf(owner);
        made.library().forEach(document -> storeLibraryDocument(owner, libraryOwner, document, created, updated, reused));
        made.designs().forEach(document -> storeDesign(owner, document, created, updated, reused));
        List<String> notes = new ArrayList<>(made.notes());
        keeper.keep(made.files(), filedDocuments(made)).ifPresent(notes::add);
        return new Imported(made.root().slug(), created, updated, reused, notes);
    }

    /** The output jobs the plugins bring, those whose formats all run here. */
    public List<OutputJob> outputJobs() {
        Set<String> running = extensions.contributions(DocumentExport.POINT).stream().filter(DocumentExport::available).map(DocumentExport::id)
                .collect(Collectors.toSet());
        return extensions.contributions(OutputJob.POINT).stream().filter(job -> running.containsAll(job.formats())).toList();
    }

    /**
     * A document written in several formats at once, as one archive: a folder for each format, what each could not say in the
     * notes. It is called by its slug, its revision and the job; a text that is not the revision stored is called a draft of it.
     *
     * @param job which job: one of the document's own outputs, which comes first, or one this server offers; every format
     *            of it must write the document
     */
    @CadRead
    public ExportedFiles release(String source, String job, DocumentOwner owner) {
        DocumentNode document = read(source);
        List<String> formats = requireOutputJob(document, job).formats().stream().distinct().toList();
        List<ExportedFiles> written = formats.stream().map(format -> export(format, source, owner)).toList();
        String name = document.slug() + "-" + scope(owner).revisionOf(document).orElse(UNSTORED) + "-" + job;
        List<String> notes = new ArrayList<>();
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        try (ZipOutputStream archive = new ZipOutputStream(bytes)) {
            for (int index = 0; index < formats.size(); index++) {
                ExportedFiles files = written.get(index);
                files.notes().forEach(note -> notes.add(files.name() + ": " + note));
                for (ExportedFile file : files.files()) {
                    archive.putNextEntry(new ZipEntry(name + "/" + formats.get(index) + "/" + file.fileName()));
                    archive.write(file.content() != null ? file.content() : file.text().getBytes(StandardCharsets.UTF_8));
                    archive.closeEntry();
                }
            }
        } catch (IOException failed) {
            throw new UncheckedIOException("The archive of " + name + " could not be written", failed);
        }
        return ExportedFiles.binary(name + ".zip", bytes.toByteArray(), notes);
    }

    /**
     * What a pcb's checks find, its text as it stands — what the editor shows after a change, and what every output a
     * factory makes from asks first. A panel is checked as the pcb it is written as.
     */
    @CadRead
    public List<PcbFinding> checks(String source, DocumentOwner owner) {
        DocumentNode document = read(source);
        return converted(() -> {
            DocumentFlattening.Flattened flat = flattened(document, scope(owner));
            if (!flat.document().kind().equals(PcbModule.PCB)) {
                throw CadProblem.invalid("Checks are made of a pcb; this text holds a " + document.kind().word() + ".");
            }
            return findingsOf(flat.document(), flat.catalogue());
        });
    }

    /**
     * Everything the engine makes of a pcb that an editor draws: its copper worked out — which copper is on which net, its
     * zones poured, its tracks' drops — and what its checks find on that same copper.
     *
     * @param artwork  the pcb's artwork: its nets, pours and drops
     * @param masks    each outer side's mask and paste as made, by side
     * @param findings what its checks find
     * @param lengths  how long each net's copper runs, by net
     */
    public record PcbDerived(PcbArtwork artwork, Map<String, MaskAreas> masks, List<PcbFinding> findings, Map<String, NetLengths.Length> lengths) {
    }

    /**
     * A side's mask and paste as made: everything that opens the mask, the windows in it that are no pad's, via's or hole's —
     * where the copper is bare —, and the paste.
     */
    public record MaskAreas(Geometry openings, Geometry windows, Geometry paste) {
    }

    /**
     * A pcb as made, for a 3D view: the made pcb and the thicknesses it is raised by.
     *
     * @param thickness       the substrate's, by the pcb's rules
     * @param copperThickness the outer copper's, by the pcb's rules
     * @param tolerance       how far an area's outline may move when it is simplified for a view
     */
    public record MadePcb(double thickness, double copperThickness, PcbSolid solid, double tolerance) {
    }

    /**
     * What the engine makes of a pcb, its text as it stands: the one answer the pcb editor draws its copper from and shows
     * its checks from, worked out once — never a second time in the editor.
     */
    @CadRead
    public PcbDerived derived(String source, DocumentOwner owner) {
        DocumentNode document = requirePcb(source);
        return converted(() -> {
            DocumentFlattening.Flattened flat = flattened(document, scope(owner));
            PcbFabrication fabrication = PcbFabrication.of(flat.document(), flat.catalogue());
            Map<String, Double> values = CadStandards.pcbRules().valuesOf(fabrication.pcb().rules());
            PcbArtwork artwork = PcbArtwork.of(fabrication, values, ARTWORK, SILKSCREEN_FONT);
            ArtworkAreas areas = new ArtworkAreas(artwork.outlines(), ARTWORK.areas().steps());
            Map<String, MaskAreas> masks = new LinkedHashMap<>();
            for (String side : List.of(PcbDrawing.TOP, PcbDrawing.BOTTOM)) {
                masks.put(side, new MaskAreas(areas.areaOf(artwork.maskOpenings(side)), areas.areaOf(artwork.maskWindows(side)),
                        areas.areaOf(artwork.paste(side))));
            }
            Map<String, NetLengths.Length> lengths = NetLengths.of(fabrication.pcb(), artwork.nets(), values.getOrDefault(PcbVocabulary.RULE_PCB_THICKNESS, 0.0));
            return new PcbDerived(artwork, masks, artwork.checks(values), lengths);
        });
    }

    /** What copper is, as the engine measures a pcb's copper: data beside the physics, read once. */
    private static final CopperPhysics COPPER_PHYSICS = CadSettings.read(CopperPhysics.SECTION);

    /** What a pcb's stack means for signals and heat: data beside the physics, read once. */
    private static final TrackStacks TRACK_STACKS = new TrackStacks(CadSettings.read(StackPhysics.SECTION));

    /**
     * The copper between two points of a pcb, its text as it stands: its resistance, the drop and the heat at a current, and
     * which piece is too thin for it within the allowed rise — or why there is no path, in words.
     */
    @CadRead
    public CopperPaths.Answer copperPath(String source, DocumentOwner owner, CopperPaths.Probe from, CopperPaths.Probe to, double current,
                                         double temperatureRise) {
        DocumentNode document = requirePcb(source);
        return converted(() -> {
            DocumentFlattening.Flattened flat = flattened(document, scope(owner));
            PcbFabrication fabrication = PcbFabrication.of(flat.document(), flat.catalogue());
            Map<String, Double> values = CadStandards.pcbRules().valuesOf(fabrication.pcb().rules());
            CopperNets nets = fabrication.copperNets(new PadOutline(ARTWORK.curveSteps(), ARTWORK.cornerSteps()), ARTWORK.curveSteps());
            return CopperPaths.between(fabrication.pcb(), nets, values, from, to, current, temperatureRise, COPPER_PHYSICS, ARTWORK.curveSteps());
        });
    }

    /**
     * What a pcb's stack means on one of its layers: the stack a track there stands in, its impedance at a width, the widths
     * that give the impedances asked about, each pair's impedance, and how warm the watts spent make the whole pcb.
     *
     * @param standing   the layer's stack, or why a track there has no plane
     * @param impedance  a track's at the width asked about; empty without a plane
     * @param widths     the width giving each impedance asked about, empty where none searched does
     * @param pairs      each pair's differential impedance, by its name
     * @param warmth     kelvin the pcb warms by; empty where it has no outline to give heat off from
     */
    public record StackAnswer(TrackStacks.Standing standing, Optional<TrackStacks.Impedance> impedance, Map<Double, Optional<Double>> widths,
                              Map<String, Double> pairs, Optional<Double> warmth) {
    }

    /**
     * What a pcb's stack means on a layer, its text as it stands.
     *
     * @param height       the reader's own height to the plane, where they say it
     * @param permittivity the reader's own permittivity, where they say it
     * @param impedances   the impedances asked about
     * @param power        the watts spent on the pcb
     */
    @CadRead
    public StackAnswer stack(String source, String layer, double width, Optional<Double> height, Optional<Double> permittivity,
                             List<Double> impedances, double power) {
        DocumentNode document = requirePcb(source);
        return converted(() -> {
            PcbDrawing pcb = PcbDrawing.of(document);
            Map<String, Double> values = CadStandards.pcbRules().valuesOf(pcb.rules());
            TrackStacks.Standing standing = TRACK_STACKS.stackOf(pcb.copperLayerIds(), values, layer, height, permittivity);
            Optional<Double> warmth = CopperShape.ofPath(pcb.outline(), true, 0, ARTWORK.curveSteps())
                    .map(edge -> TRACK_STACKS.warmth(power, edge.box().right() - edge.box().left(), edge.box().bottom() - edge.box().top()));
            if (!(standing instanceof TrackStacks.Stands stands)) {
                return new StackAnswer(standing, Optional.empty(), Map.of(), Map.of(), warmth);
            }
            Map<Double, Optional<Double>> widths = new LinkedHashMap<>();
            impedances.forEach(impedance -> widths.put(impedance, TRACK_STACKS.widthFor(impedance, stands.stack())));
            Map<String, Double> pairs = new LinkedHashMap<>();
            pcb.pairs().forEach(pair -> pairs.put(pair.name(), TRACK_STACKS.pairImpedance(pair.width(), pair.gap(), stands.stack())));
            return new StackAnswer(standing, Optional.of(TRACK_STACKS.impedanceOf(width, stands.stack())), widths, pairs, warmth);
        });
    }

    /**
     * A zone of a pcb stitched to its net's pours on the other layers, its text as it stands: through vias on the pcb's grid of a
     * pitch, where both pours lie under a via's whole pad, clear of everything else — or why none can stand, in words.
     */
    @CadRead
    public ViaRows.Placed stitching(String source, DocumentOwner owner, int zone, double pitch, Optional<PcbDrawing.ViaSize> via) {
        return viaRowsOf(source, owner).stitching(zone, pitch, via);
    }

    /**
     * A track of a pcb fenced by a net's through vias down each side of it, its text as it stands: a pitch apart, as near as the
     * gaps allow, where two layers pour the net — or why none can stand, in words.
     */
    @CadRead
    public ViaRows.Placed fence(String source, DocumentOwner owner, int track, String net, double pitch, Optional<PcbDrawing.ViaSize> via) {
        return viaRowsOf(source, owner).fence(track, net, pitch, via);
    }

    /** What rows of vias are made over: a pcb's zones poured as its artwork pours them, and its copper's nets. */
    private ViaRows viaRowsOf(String source, DocumentOwner owner) {
        DocumentNode document = requirePcb(source);
        return converted(() -> {
            DocumentFlattening.Flattened flat = flattened(document, scope(owner));
            PcbFabrication fabrication = PcbFabrication.of(flat.document(), flat.catalogue());
            Map<String, Double> values = CadStandards.pcbRules().valuesOf(fabrication.pcb().rules());
            PcbArtwork artwork = PcbArtwork.of(fabrication, values, ARTWORK, SILKSCREEN_FONT);
            return new ViaRows(fabrication.pcb(), artwork.nets(), artwork.pours(), new NetRules(fabrication.pcb(), values),
                    new AreaOffsets(ARTWORK.arcError()), ARTWORK.curveSteps());
        });
    }

    /** How a new pcb is laid out from its circuit: data beside the layout, read once. */
    private static final PcbLayout LAYOUT = CadSettings.read(PcbLayout.SECTION);

    /**
     * A new pcb for a circuit, laid out by the engine from the circuit as it is drawn: its parts where the sheet has them, apart,
     * on the grid, the bench's nets on a header, the edge round everything and the ground poured. No copper is laid.
     *
     * @param circuit the circuit's slug, read as the reader may see it
     */
    @CadRead
    public PcbDrawing newPcb(String circuit, DocumentOwner owner) {
        StoredDocuments documents = scope(owner);
        DocumentNode document = documents.find(circuit).filter(found -> found.kind().equals(SimulationModule.CIRCUIT))
                .orElseThrow(() -> CadProblem.notFound("circuit " + circuit));
        return converted(() -> CircuitBoards.laidOut(document, documents, LAYOUT));
    }

    /** A text that holds a pcb, or refused where it holds another kind. */
    private DocumentNode requirePcb(String source) {
        DocumentNode document = read(source);
        if (!document.kind().equals(PcbModule.PCB)) {
            throw CadProblem.invalid("This is asked of a pcb; this text holds a " + document.kind().word() + ".");
        }
        return document;
    }


    /**
     * A pcb as made, its text as it stands: what a 3D view of it draws — the substrate, each side's copper, finish, laminate,
     * paste and silkscreen, each part's body —, the same made pcb the 3D files are raised from.
     */
    @CadRead
    public MadePcb made(String source, DocumentOwner owner) {
        DocumentNode document = requirePcb(source);
        return converted(() -> {
            DocumentFlattening.Flattened flat = flattened(document, scope(owner));
            PcbFabrication fabrication = PcbFabrication.of(flat.document(), flat.catalogue());
            Map<String, Double> values = CadStandards.pcbRules().valuesOf(fabrication.pcb().rules());
            PcbArtwork artwork = PcbArtwork.of(fabrication, values, ARTWORK, SILKSCREEN_FONT);
            PcbSolid solid = PcbSolid.of(artwork, new ArtworkAreas(artwork.outlines(), ARTWORK.areas().steps()),
                    PcbDrills.of(fabrication.pcb(), fabrication.footprints()), ARTWORK.areas().steps(), new PartBodies(ARTWORK.curveSteps()));
            return new MadePcb(values.get(PcbVocabulary.RULE_PCB_THICKNESS), values.get(PcbVocabulary.RULE_COPPER_THICKNESS), solid, ARTWORK.areas().tolerance());
        });
    }


    /** How the router lays copper: data beside it, read once. */
    private static final RoutingSettings ROUTING = CadSettings.read(RoutingSettings.SECTION);

    /**
     * What the router knows of a pcb besides where its copper runs, its text as it stands: each part's footprint and the nets
     * its pads are on, the values its rules give, the footprint a 0 Ω link stands on, and how the engine measures. Read once for
     * a pcb; every layout an editor asks the router about stands on it.
     */
    @CadRead
    public RoutingBoard routingBoard(String source, DocumentOwner owner) {
        DocumentNode document = requirePcb(source);
        return converted(() -> {
            DocumentFlattening.Flattened flat = flattened(document, scope(owner));
            PcbFabrication fabrication = PcbFabrication.of(flat.document(), flat.catalogue());
            Map<String, Optional<String>> named = new LinkedHashMap<>();
            Map<String, Map<String, String>> partNets = new LinkedHashMap<>();
            for (PcbFabrication.SeatedPart part : fabrication.parts()) {
                named.put(part.designator(), part.footprint());
                partNets.put(part.designator(), part.padNets());
            }
            String link = ROUTING.autorouting().link().footprint();
            Map<String, FootprintDrawing> ownFootprints = new LinkedHashMap<>();
            flat.catalogue().find(link).map(FootprintDrawing::of).ifPresent(drawing -> ownFootprints.put(link, drawing));
            return new RoutingBoard(fabrication.footprints(), named, partNets, CadStandards.pcbRules().valuesOf(fabrication.pcb().rules()),
                    ownFootprints, ARTWORK.forChecks(), new PadOutline(ARTWORK.curveSteps(), ARTWORK.cornerSteps()), ROUTING);
        });
    }

    /**
     * What a sheet's electrical checks find, its text as it stands: what the bench shows after a change. The parts are read
     * as the reader may see them; one whose component is hidden from the reader is left out, never guessed at.
     */
    @CadRead
    public List<SheetFinding> sheetChecks(String source, DocumentOwner owner) {
        DocumentNode document = read(source);
        if (!document.kind().equals(SimulationModule.CIRCUIT)) {
            throw CadProblem.invalid("A sheet's checks are made of a circuit; this text holds a " + document.kind().word() + ".");
        }
        return converted(() -> SheetChecks.of(SimulationReading.circuit(document), scope(owner), PIN_RULES));
    }

    /** Which pins may not share a net: the sheet checks' rules, which ship in the exchange's jar, read once. */
    private static final SheetChecks.PinRules PIN_RULES = CadSettings.read(SheetChecks.SECTION);

    /** The checks' findings of a pcb, held to the values its rules give, its copper worked out as its artwork is. */
    private static List<PcbFinding> findingsOf(DocumentNode pcb, CatalogueDocuments catalogue) {
        PcbFabrication fabrication = PcbFabrication.of(pcb, catalogue);
        return fabrication.checks(ARTWORK, CadStandards.pcbRules().valuesOf(fabrication.pcb().rules()));
    }

    /**
     * Refuses to write a factory's files of a pcb its checks find an error in: a narrow gap, a short, a track off the edge
     * would be made as drawn. What only warns is written; the editor shows it.
     */
    private static void requirePassing(DocumentNode pcb, CatalogueDocuments catalogue, String format) {
        List<PcbFinding> errors = findingsOf(pcb, catalogue).stream().filter(finding -> finding.severity() == FindingSeverity.ERROR).toList();
        if (errors.isEmpty()) {
            return;
        }
        String named = errors.stream().limit(NAMED_ERRORS).map(PcbFinding::message).collect(Collectors.joining("; "));
        throw CadProblem.invalid(format + " is not written: the pcb fails " + errors.size() + (errors.size() == 1 ? " check" : " checks")
                + " a factory would refuse it for — " + named + (errors.size() > NAMED_ERRORS ? "; and " + (errors.size() - NAMED_ERRORS) + " more" : "")
                + ". Correct the pcb, then write it again.");
    }

    /** How many of a pcb's errors a refusal names; the rest are counted. */
    private static final int NAMED_ERRORS = 5;

    /** How the checks measure: the artwork's settings, which ship in the exchange's jar, read once. */
    private static final PcbArtwork.Settings ARTWORK = CadSettings.read(PcbArtwork.SECTION);

    /** The letters a pcb's silkscreen is written in, as its Gerber writes them: read once. */
    private static final StrokeFont SILKSCREEN_FONT = StrokeFont.newStroke();

    /** The job a release names: the document's own by that name, otherwise one offered. */
    private OutputJob requireOutputJob(DocumentNode document, String job) {
        return ownOutputs(document).stream().filter(output -> output.id().equals(job)).findFirst()
                .or(() -> outputJobs().stream().filter(offered -> offered.id().equals(job)).findFirst())
                .orElseThrow(() -> CadProblem.notFound("An output called '" + job + "'"));
    }

    private static List<OutputJob> ownOutputs(DocumentNode document) {
        if (!document.kind().equals(PcbModule.PCB)) {
            return List.of();
        }
        return PcbDrawing.of(document).outputs().stream()
                .map(output -> new OutputJob(output.name(), output.title(), output.formats())).toList();
    }

    /** The document as it is written: itself, or what a plugin makes of its kind. */
    private DocumentFlattening.Flattened flattened(DocumentNode document, CatalogueDocuments catalogue) {
        return extensions.contributions(DocumentFlattening.POINT).stream().filter(flattening -> flattening.from().equals(document.kind())).findFirst()
                .map(flattening -> flattening.flatten(document, catalogue))
                .orElseGet(() -> new DocumentFlattening.Flattened(document, catalogue, List.of()));
    }

    /** Something made from a document's text as it is written, read for its owner as an export is. */
    @CadRead
    public <T> T written(String source, DocumentOwner owner, BiFunction<DocumentNode, CatalogueDocuments, T> making) {
        DocumentNode document = read(source);
        return converted(() -> {
            DocumentFlattening.Flattened flat = flattened(document, scope(owner));
            return making.apply(flat.document(), flat.catalogue());
        });
    }

    /** What a text comes to as it is written, in the language, with what the making says. */
    public record FlattenedText(String source, List<String> notes) {
    }

    /** A document's text as it is written: a panel's as the one pcb it makes. */
    @CadRead
    public FlattenedText flattened(String source, DocumentOwner owner) {
        DocumentNode document = read(source);
        return converted(() -> {
            DocumentFlattening.Flattened flat = flattened(document, scope(owner));
            return new FlattenedText(language.language().write(flat.document()), flat.notes());
        });
    }

    /** The kinds a format writes: its own, and each that is written as one of them. */
    private List<String> kindsWrittenBy(DocumentExport writing) {
        Set<DocumentKind> kinds = new HashSet<>(writing.kinds());
        extensions.contributions(DocumentFlattening.POINT).stream().filter(flattening -> writing.kinds().contains(flattening.to()))
                .forEach(flattening -> kinds.add(flattening.from()));
        return kinds.stream().map(DocumentKind::word).sorted().toList();
    }

    /**
     * The works of others a document carries — itself, when it is borrowed, and what it is built from —, each with what its licence
     * answers for passing the document on: what an export of it may do, and owes.
     */
    @CadRead
    public List<DocumentLicences.BorrowedWork> borrowed(String source, DocumentOwner owner) {
        DocumentNode document = read(source);
        return converted(() -> {
            DocumentFlattening.Flattened flat = flattened(document, scope(owner));
            return licences.worksOf(licences.passedOn(flat.document(), flat.catalogue()));
        });
    }

    /** A document's text written in a format, as the licences of the works it carries let it leave. */
    @CadRead
    public ExportedFiles export(String format, String source, DocumentOwner owner) {
        DocumentExport writing = exportOf(format);
        DocumentNode document = read(source);
        return converted(() -> {
            DocumentFlattening.Flattened flat = flattened(document, scope(owner));
            if (!writing.kinds().contains(flat.document().kind())) {
                throw CadProblem.invalid(writing.title() + " is written from " + writing.kinds().stream().map(DocumentKind::word)
                        .sorted().toList() + "; this text holds a " + document.kind().word() + ".");
            }
            if (writing.purpose() == ExportPurpose.FABRICATION && flat.document().kind().equals(PcbModule.PCB)) {
                requirePassing(flat.document(), flat.catalogue(), writing.title());
            }
            LicenceLedger ledger = licences.passedOn(flat.document(), flat.catalogue());
            ExportedFiles written = licences.credited(writing.write(flat.document(), flat.catalogue()), ledger);
            List<String> notes = new ArrayList<>(flat.notes());
            notes.addAll(written.notes());
            return new ExportedFiles(written.name(), written.files(), notes);
        });
    }

    /**
     * Who holds what a design is built from: the outermost place the reader may write. The same footprint serves every
     * project of a workspace, and a slug names one document, so held by the first project it would be out of the next
     * one's sight. Its circuits and its pcb stay the project's own.
     */
    private DocumentOwner libraryOwnerOf(DocumentOwner owner) {
        DocumentOwner outermost = places.requireOwner(owner).chain().getFirst();
        return authority.permits(CadCapability.OWNER_WRITE, places.requireOwner(outermost)) ? outermost : owner;
    }

    /** The documents an import's files go with, as they are now stored, by slug. */
    private Map<String, String> filedDocuments(ImportedDocuments made) {
        Map<String, String> filed = new HashMap<>();
        made.files().stream().map(FetchedFile::document).distinct()
                .forEach(slug -> documents.findBySlug(slug).ifPresent(stored -> filed.put(slug, stored.getId())));
        return filed;
    }

    private void storeLibraryDocument(DocumentOwner owner, DocumentOwner libraryOwner, DocumentNode made, List<String> created,
                                      List<String> updated, List<String> reused) {
        String source = language.language().write(made);
        Optional<StoredDocument> existing = documents.findBySlug(made.slug());
        if (existing.isEmpty()) {
            catalogue.createOwned(libraryOwner, source, RevisionOrigin.IMPORTED);
            created.add(made.slug());
            return;
        }
        StoredDocument stored = existing.get();
        // Reused unseen, the design would name what it cannot read, and say so only at the first part that does.
        if (!catalogue.scope(owner).visible(stored)) {
            throw CadProblem.conflict("A document is already called '" + made.slug() + "' and is held where this design "
                                      + "cannot see it: import it there, or remove that one.");
        }
        // The import's own as long as nobody has written it since: an import may refresh it, a person's revision it keeps.
        boolean untouched = stored.owner().map(libraryOwner::equals).orElse(false) && catalogue.leftByAnImport(stored);
        if (!untouched) {
            reused.add(made.slug());
            return;
        }
        writeOver(stored, source, made.slug(), updated, reused);
    }

    private void storeDesign(DocumentOwner owner, DocumentNode design, List<String> created, List<String> updated,
                             List<String> reused) {
        String source = language.language().write(design);
        Optional<StoredDocument> existing = documents.findBySlug(design.slug());
        if (existing.isEmpty()) {
            catalogue.createOwned(owner, source, RevisionOrigin.IMPORTED);
            created.add(design.slug());
            return;
        }
        StoredDocument stored = existing.get();
        if (!stored.owner().map(owner::equals).orElse(false)) {
            throw CadProblem.conflict("A document is already called '" + design.slug() + "' and is not this owner's: "
                                      + "rename what is imported.");
        }
        writeOver(stored, source, design.slug(), updated, reused);
    }

    /** An unchanged text writes nothing: the document is as this import would make it. */
    private void writeOver(StoredDocument stored, String source, String slug, List<String> updated, List<String> reused) {
        int before = stored.getRevision();
        int after = catalogue.update(stored.getId(), source, stored.getVersion(), RevisionOrigin.IMPORTED).sourceRevision();
        (after == before ? reused : updated).add(slug);
    }

    private DocumentImport importOf(String format) {
        return extensions.contributions(DocumentImport.POINT).stream().filter(reading -> reading.id().equals(format))
                .findFirst().orElseThrow(() -> CadProblem.notFound("A format that reads '" + format + "'"));
    }

    private DocumentExport exportOf(String format) {
        return extensions.contributions(DocumentExport.POINT).stream().filter(DocumentExport::available)
                .filter(writing -> writing.id().equals(format))
                .findFirst().orElseThrow(() -> CadProblem.notFound("A format that writes '" + format + "'"));
    }

    /** The components the reader may see: the catalogue's, and the owner's once the reader may read there. */
    private StoredDocuments scope(DocumentOwner owner) {
        if (owner == null) {
            return catalogue.scope(null);
        }
        authority.require(CadCapability.OWNER_READ, places.requireOwner(owner));
        return catalogue.scope(owner);
    }

    private ReadDocument readDocument(DocumentNode document) {
        return new ReadDocument(document.slug(), document.title(), document.kind().word(), language.language().write(document));
    }

    private static List<DocumentNode> allOf(ImportedDocuments made) {
        List<DocumentNode> all = new ArrayList<>(made.library());
        all.addAll(made.designs());
        return all;
    }

    private DocumentNode read(String source) {
        if (source == null || source.isBlank()) {
            throw CadProblem.invalid("A document is its jM text: send it as 'source'.");
        }
        try {
            return language.language().readOne(DocumentInput.text("the document", source));
        } catch (GrammarRefusal refused) {
            throw CadProblem.invalidDocument(refused.detail(), refused.line(), refused.column());
        }
    }

    /** A conversion, its refusals as the problems every route answers with. */
    private static final Logger LOGGER = LoggerFactory.getLogger(DocumentExchange.class);

    private static <T> T converted(Supplier<T> conversion) {
        try {
            return conversion.get();
        } catch (ExchangeRefusal refused) {
            throw CadProblem.invalidDocument(refused.detail(), refused.line(), refused.column());
        } catch (GrammarRefusal refused) {
            throw CadProblem.invalidDocument(refused.detail(), refused.line(), refused.column());
        } catch (IllegalArgumentException | IllegalStateException refused) {
            // A text the engine cannot make anything of is refused in words; what refused it — a reading, or a library underneath
            // the engine, as JTS refusing an area — is kept for whoever looks into why.
            LOGGER.debug("Conversion refused: {}", refused.getMessage(), refused);
            throw new CadProblem.Unprocessable("conversion-refused", refused.getMessage());
        }
    }
}
