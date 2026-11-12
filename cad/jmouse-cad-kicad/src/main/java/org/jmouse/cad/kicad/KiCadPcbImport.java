package org.jmouse.cad.kicad;

import org.jmouse.cad.circuit.Circuit;
import org.jmouse.cad.pcb.PcbVocabulary;
import org.jmouse.cad.circuit.Part;
import org.jmouse.cad.document.DocumentNode;
import org.jmouse.cad.document.MetaNode;
import org.jmouse.cad.exchange.imported.FootprintOutlines;
import org.jmouse.cad.exchange.imported.ImportedNetNames;
import org.jmouse.cad.exchange.imported.OutlineStretches;
import org.jmouse.cad.exchange.imported.RoundedPath;
import org.jmouse.cad.exchange.imported.SheetTracing;
import org.jmouse.cad.exchange.imported.UniqueIdentifiers;
import org.jmouse.cad.pcb.PcbModule;
import org.jmouse.cad.pcb.drawing.PcbDrawing;
import org.jmouse.cad.simulation.CatalogueDocuments;
import org.jmouse.cad.simulation.ComponentFacts;
import org.jmouse.cad.simulation.SimulationReading;
import org.jmouse.cad.simulation.elaboration.SheetUnfolding;
import org.jmouse.kicad.KiCadLayer;
import org.jmouse.kicad.KiCadPoint;
import org.jmouse.kicad.KiCadProjectSettings;
import org.jmouse.kicad.footprint.KiCadFootprintGraphic;
import org.jmouse.kicad.pcb.KiCadPcb;
import org.jmouse.kicad.pcb.KiCadPcbFootprint;
import org.jmouse.kicad.pcb.KiCadSide;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.UnaryOperator;
import java.util.Set;
import java.util.TreeSet;

/**
 * A KiCad board as a pcb document of the design its schematics became: its edge, each footprint where KiCad stands it, its
 * copper, its pours and keepouts, its printed words and its measures. A footprint with a symbol is that part of the
 * unfolded design, found by the path KiCad keeps it under; one only the board has — a mounting hole — names its own
 * footprint. The pads' nets are the schematic's, as ever on a pcb: each is read beside what the board says, so a board
 * that does not agree with its schematic is said, not silently taken one way.
 *
 * <p>A pcb is drawn from its top left: KiCad's page frame is taken off, and both have Y downwards. A footprint on the back
 * is mirrored about X by KiCad and about Y by ours, which is the same footprint turned half a turn further.</p>
 */
public final class KiCadPcbImport {

    private static final int PLACES = FootprintOutlines.PLACES;
    private static final String SUMMARY = "Imported from KiCad: ";
    private static final String FRONT_COPPER = KiCadSide.FRONT.copper();
    private static final String BACK_COPPER = KiCadSide.BACK.copper();
    /** What KiCad names a net of one pad nothing else joins: it is on no net of the schematic to be agreed with. */

    private static final String PATH_SEPARATOR = "/";
    /** How many of a list a note names before it counts the rest. */
    private static final int NAMED_IN_A_NOTE = 8;

    private final KiCadPcb pcb;
    private final double lineSpacing;
    private final Map<String, String> identifiers;
    private final Map<String, Part> parts = new LinkedHashMap<>();
    private final ComponentFacts facts;
    private final KiCadLibraries filing;
    /** What the board's zones draw on no copper: windows in its mask and its paste. */
    private final List<PcbDrawing.Shape> areasOffCopper = new ArrayList<>();
    private final String ownPart;
    private final UniqueIdentifiers ownIdentifiers;
    private final RoundedPath round = new RoundedPath(PLACES);
    private final List<String> notes = new ArrayList<>();
    /** Our copper layer each of KiCad's is. */
    private final Map<String, String> copper = new LinkedHashMap<>();
    /** What a net only the board has is called on the pcb. */
    private final ImportedNetNames ownNets;
    /** The design's net each of the board's nets is, as their pads agree. */
    private final Map<String, String> netsOfBoard = new LinkedHashMap<>();
    private double left;
    private double top;

    private KiCadPcbImport(KiCadPcb pcb, Map<String, String> identifiers, String ground, Circuit design, CatalogueDocuments documents,
                           KiCadLibraries filing) {
        this.pcb = pcb;
        // One name is one net in KiCad, on the board as on the sheets: the board's net of the reference ground is node 0.
        this.ownNets = new ImportedNetNames(ground, KiCadNetNames::local);
        this.filing = filing;
        this.ownPart = filing.pcbOwnPart();
        this.ownIdentifiers = new UniqueIdentifiers(filing.pcbOwnPart(), UniqueIdentifiers.UNLIMITED);
        this.lineSpacing = filing.textLineSpacing();
        this.identifiers = identifiers;
        this.facts = new ComponentFacts(documents);
        design.parts().forEach(part -> {
            parts.put(part.reference(), part);
            ownIdentifiers.of(part.reference());
        });
    }

    /** The pcb's document, and what it could not carry. */
    public record Pcb(DocumentNode document, List<String> notes) {

        public Pcb {
            notes = List.copyOf(notes);
        }
    }

    /**
     * @param settings    what the board's project file says of how it is made
     * @param file        the board's file, which the pcb says it was imported from
     * @param slug        the pcb's slug
     * @param circuit     the slug of the design's root sheet, which the pcb lays out
     * @param identifiers the id each symbol's and each sheet's uuid became in the sheets imported
     * @param ground      the KiCad net the sheets made node 0
     * @param documents   the design as imported and what it names, as the pcb will find them
     * @throws IllegalArgumentException when the design does not unfold
     */
    public static Pcb read(KiCadPcb pcb, KiCadProjectSettings settings, String file, String slug, String circuit, String title,
                           Map<String, String> identifiers, String ground, CatalogueDocuments documents, KiCadLibraries filing) {
        DocumentNode root = documents.find(circuit).orElseThrow(() -> new IllegalArgumentException(
                "The board lays out " + circuit + ", which was not made."));
        Circuit design = SheetUnfolding.of(SimulationReading.circuit(root), documents);
        KiCadPcbImport reading = new KiCadPcbImport(pcb, identifiers, ground, design, documents, filing);
        reading.frame();
        reading.layers();
        List<PcbDrawing.Placement> placements = reading.placements();
        List<PcbDrawing.Zone> zones = new ArrayList<>();
        List<PcbDrawing.Keepout> keepouts = new ArrayList<>();
        reading.areas(zones, keepouts);
        PcbDrawing drawing = new PcbDrawing(circuit, rulesOf(settings, filing), reading.netClasses(settings, filing), sizesOf(settings), List.of(), List.of(),
                reading.copper.size(), reading.outline(), placements, reading.tracks(), reading.arcs(), reading.vias(), zones,
                keepouts, reading.dimensions(), reading.texts(), List.of(), reading.shapes(), List.of());
        reading.noteWhatIsLeft();
        DocumentNode document = new DocumentNode(PcbModule.PCB, title, slug, null, 1);
        document.addExpression(MetaNode.filed(SUMMARY + file, null, filing.category(), filing.circuitTags()));
        drawing.lines().forEach(document::addExpression);
        return new Pcb(document, reading.notes);
    }

    // ─── how it is made ─────────────────────────────────

    /** The rules it was laid out to: the usual way of making a pcb, each value as the project sets it. */
    private static Optional<PcbDrawing.Rules> rulesOf(KiCadProjectSettings settings, KiCadLibraries filing) {
        if (settings.usual().isEmpty() && settings.rules().isEmpty()) {
            return Optional.empty();
        }
        Map<String, Double> changes = new LinkedHashMap<>();
        settings.usual().ifPresent(usual -> {
            changes.put(PcbDrawing.RULE_CLEARANCE, usual.clearance());
            changes.put(PcbDrawing.RULE_TRACK_WIDTH, usual.trackWidth());
            changes.put(PcbDrawing.RULE_VIA_DRILL, usual.viaDrill());
            changes.put(PcbDrawing.RULE_VIA_DIAMETER, usual.viaDiameter());
        });
        filing.pcbRuleNames().forEach((ours, theirs) -> Optional.ofNullable(settings.rules().get(theirs))
                .ifPresent(value -> changes.put(ours, value)));
        return Optional.of(new PcbDrawing.Rules(filing.pcbRules(), changes));
    }

    private static PcbDrawing.Sizes sizesOf(KiCadProjectSettings settings) {
        return new PcbDrawing.Sizes(settings.trackWidths(), settings.viaSizes().stream()
                .map(size -> new PcbDrawing.ViaSize(size.drill(), size.diameter())).toList());
    }

    /** Each class of the project with the nets of the board it takes; a class no net of the board is in is not said. */
    private List<PcbDrawing.NetClass> netClasses(KiCadProjectSettings settings, KiCadLibraries filing) {
        Map<String, Set<String>> nets = new LinkedHashMap<>();
        netsOfBoard.forEach((boardNet, net) -> settings.classOf(boardNet).ifPresent(
                netClass -> nets.computeIfAbsent(netClass, ignored -> new TreeSet<>()).add(net)));
        return settings.classes().stream().filter(netClass -> nets.containsKey(netClass.name()))
                .map(netClass -> new PcbDrawing.NetClass(new UniqueIdentifiers(filing.pcbClassOpening(), UniqueIdentifiers.UNLIMITED).of(netClass.name()), netClass.trackWidth(), netClass.clearance(),
                        netClass.viaDrill(), netClass.viaDiameter(), List.copyOf(nets.get(netClass.name())))).toList();
    }

    // ─── frame and layers ───────────────────────────────

    /** Where the pcb starts: its edge's left and top, or where its footprints stand if it draws none. */
    private void frame() {
        List<KiCadPoint> points = new ArrayList<>();
        edgeGraphics().forEach(graphic -> points.addAll(graphic.reach()));
        if (points.isEmpty()) {
            pcb.footprints().forEach(footprint -> points.add(footprint.at()));
            notes.add("The board draws no edge on Edge.Cuts, so the pcb has none.");
        }
        left = points.stream().mapToDouble(KiCadPoint::x).min().orElse(0);
        top = points.stream().mapToDouble(KiCadPoint::y).min().orElse(0);
    }

    /** Its copper layers, front first, as ours: {@code top}, {@code inner-1} …, {@code bottom}. */
    private void layers() {
        int inner = 0;
        for (String layer : pcb.copper()) {
            if (layer.equals(FRONT_COPPER)) {
                copper.put(layer, PcbDrawing.TOP);
            } else if (layer.equals(BACK_COPPER)) {
                copper.put(layer, PcbDrawing.BOTTOM);
            } else {
                copper.put(layer, PcbDrawing.innerCopper(++inner));
            }
        }
    }

    /** What draws the edge: the board's own lines on it, and those of its footprints, each where it stands on the board. */
    private List<KiCadFootprintGraphic> edgeGraphics() {
        List<KiCadFootprintGraphic> graphics = new ArrayList<>(pcb.graphics().stream().filter(graphic -> graphic.layer().equals(KiCadLayer.EDGE)).toList());
        for (KiCadPcbFootprint footprint : pcb.footprints()) {
            footprint.footprint().graphics().stream().filter(graphic -> graphic.layer().equals(KiCadLayer.EDGE))
                    .map(graphic -> onBoard(graphic, footprint)).forEach(graphics::add);
        }
        return graphics;
    }

    private static KiCadFootprintGraphic onBoard(KiCadFootprintGraphic graphic, KiCadPcbFootprint footprint) {
        return mapped(graphic, footprint::onBoard);
    }

    /** The same graphic with every point of it moved; a rectangle is no longer one, once it may be turned. */
    private static KiCadFootprintGraphic mapped(KiCadFootprintGraphic graphic, UnaryOperator<KiCadPoint> moved) {
        KiCadFootprintGraphic kept = graphic instanceof KiCadFootprintGraphic.Rectangle box
                ? new KiCadFootprintGraphic.Polygon(cornersOf(box), box.layer(), box.width(), box.filled()) : graphic;
        return kept.moved(moved, kept.layer());
    }

    private static List<KiCadPoint> cornersOf(KiCadFootprintGraphic.Rectangle box) {
        return List.of(box.start(), new KiCadPoint(box.end().x(), box.start().y()), box.end(),
                new KiCadPoint(box.start().x(), box.end().y()));
    }

    // ─── edge ───────────────────────────────────────────

    /** The edge as one path: its loose lines and arcs chained into loops, its closed shapes as they are. */
    private String outline() {
        RoundedPath path = new RoundedPath(PLACES);
        List<OutlineStretches.Stretch> stretches = new ArrayList<>();
        for (KiCadFootprintGraphic graphic : edgeGraphics()) {
            switch (graphic) {
                case KiCadFootprintGraphic.Line line -> stretches.add(
                        new OutlineStretches.Stretch(traced(line.start()), traced(line.end()), Optional.empty()));
                case KiCadFootprintGraphic.Arc arc -> stretches.add(
                        new OutlineStretches.Stretch(traced(arc.start()), traced(arc.end()), Optional.of(traced(arc.middle()))));
                case KiCadFootprintGraphic.Curve curve -> stretches.add(OutlineStretches.Stretch.curve(traced(curve.start()),
                        traced(curve.first()), traced(curve.second()), traced(curve.end())));
                case KiCadFootprintGraphic.Rectangle box -> closedShape(cornersOf(box), path);
                case KiCadFootprintGraphic.Polygon polygon -> closedShape(polygon.points(), path);
                case KiCadFootprintGraphic.Circle circle -> {
                    SheetTracing.Point centre = traced(circle.center());
                    path.circle(centre.x(), centre.y(), circle.radius());
                }
            }
        }
        OutlineStretches.draw(stretches, path, end -> notes.add("The edge does not close at " + end.x() + ", " + end.y()
                + " mm from the pcb's top left: it is drawn as it is."));
        return path.isEmpty() ? null : path.build().toSource();
    }

    private void closedShape(List<KiCadPoint> corners, RoundedPath path) {
        SheetTracing.Point start = traced(corners.getFirst());
        path.moveTo(start.x(), start.y());
        corners.subList(1, corners.size()).stream().map(this::traced).forEach(corner -> path.lineTo(corner.x(), corner.y()));
        path.close();
    }

    // ─── footprints ─────────────────────────────────────

    /** Every footprint: each with a symbol a part of the design, and those only the board has named once the parts are. */
    private List<PcbDrawing.Placement> placements() {
        List<PcbDrawing.Placement> placements = new ArrayList<>();
        Set<String> disagreeing = new TreeSet<>();
        List<KiCadPcbFootprint> own = new ArrayList<>();
        Set<String> lostSymbols = new TreeSet<>();
        for (KiCadPcbFootprint footprint : pcb.footprints()) {
            Optional<String> designator = designatorOf(footprint);
            if (designator.isEmpty()) {
                if (footprint.ofSchematic()) {
                    lostSymbols.add(footprint.reference());
                }
                own.add(footprint);
                continue;
            }
            Part part = parts.get(designator.get());
            readNets(footprint, part, disagreeing);
            String shown = footprint.reference();
            placements.add(placed(footprint, designator.get(), shown.equals(designator.get()) ? Optional.empty() : Optional.of(shown),
                    Optional.empty(), Map.of()));
        }
        // After the parts: a pour names the nets those parts carry, and a mounting hole's net is one of them.
        for (KiCadPcbFootprint footprint : own) {
            String designator = ownIdentifiers.of(footprint.reference().isBlank() ? ownPart : footprint.reference());
            Optional<String> shown = footprint.reference().equals(designator) || footprint.reference().isBlank()
                    ? Optional.empty() : Optional.of(footprint.reference());
            placements.add(placed(footprint, designator, shown, Optional.of(KiCadFootprintDrawing.codeOf(footprint.libraryId())),
                    netsOfOwn(footprint)));
        }
        if (!lostSymbols.isEmpty()) {
            // A reference proves nothing here: a schematic drawn anew reuses them for other parts.
            notes.add(lostSymbols.size() + " footprints stand for symbols the schematic no longer has (" + named(lostSymbols) + "): the "
                    + "board is an older drawing than its schematic. They are laid out as the board has them, on the board's nets; "
                    + "update the board from the schematic in KiCad to tie them to it.");
        }
        if (!disagreeing.isEmpty()) {
            notes.add("The board and its schematic disagree on " + disagreeing.size() + " pads, the schematic is taken: "
                    + named(disagreeing) + ". Update the board from the schematic in KiCad to see them.");
        }
        return placements;
    }

    /** The part of the design a footprint stands for: its symbol, under the blocks of the sheets it is placed through. */
    private Optional<String> designatorOf(KiCadPcbFootprint footprint) {
        if (footprint.path().isEmpty()) {
            return Optional.empty();
        }
        List<String> steps = Arrays.stream(footprint.path().get().split(PATH_SEPARATOR)).filter(step -> !step.isEmpty()).toList();
        if (steps.isEmpty()) {
            return Optional.empty();
        }
        List<String> blocks = new ArrayList<>();
        for (String step : steps.subList(0, steps.size() - 1)) {
            String block = identifiers.get(step);
            if (block == null) {
                return Optional.empty();
            }
            blocks.add(block);
        }
        return Optional.ofNullable(identifiers.get(steps.getLast())).map(part -> SheetUnfolding.partName(blocks, part))
                .filter(parts::containsKey);
    }


    private PcbDrawing.Placement placed(KiCadPcbFootprint footprint, String designator, Optional<String> shown,
                                        Optional<String> ownFootprint, Map<String, String> nets) {
        boolean back = footprint.side() == KiCadSide.BACK;
        double turned = back ? footprint.angle() + 180 : footprint.angle();
        Optional<PcbDrawing.Reference> reference = footprint.lettering().map(lettering -> new PcbDrawing.Reference(
                round.rounded(lettering.at().x()), round.rounded(lettering.at().y()), round.rounded(normalized(lettering.angle()))));
        return new PcbDrawing.Placement(designator, round.rounded(footprint.at().x() - left), round.rounded(footprint.at().y() - top),
                round.rounded(normalized(turned)), back ? PcbDrawing.BOTTOM : PcbDrawing.TOP, reference, shown, ownFootprint, nets);
    }

    /** Which design's net each of the board's nets is, as a part's pads say; where the two part ways, it is written down. */
    private void readNets(KiCadPcbFootprint footprint, Part part, Set<String> disagreeing) {
        Optional<String> slug = Optional.ofNullable(part.footprint()).filter(written -> !written.isBlank())
                .or(() -> facts.footprintOf(part.component()));
        Map<String, String> designed = facts.padNets(part, slug);
        footprint.padNets().forEach((pad, boardNet) -> {
            String net = designed.get(pad);
            if (net == null || boardNet.startsWith(KiCadNetNames.UNCONNECTED)) {
                return;
            }
            String known = netsOfBoard.putIfAbsent(boardNet, net);
            if (known != null && !known.equals(net)) {
                disagreeing.add(footprint.reference() + "." + pad);
            }
        });
    }

    /** The net each pad of a footprint only the board has is on: the design's where the design's parts carry it, else the board's own. */
    private Map<String, String> netsOfOwn(KiCadPcbFootprint footprint) {
        Map<String, String> nets = new LinkedHashMap<>();
        footprint.padNets().forEach((pad, net) -> {
            if (!net.startsWith(KiCadNetNames.UNCONNECTED)) {
                nets.put(pad, netOfBoard(net));
            }
        });
        return nets;
    }

    /** A net of the board as the pcb calls it: the design's, or one of its own for a net no part of the design is on. */
    private String netOfBoard(String net) {
        String designed = netsOfBoard.get(net);
        return designed != null ? designed : ownNets.of(net);
    }

    // ─── copper ─────────────────────────────────────────

    private List<PcbDrawing.Track> tracks() {
        List<PcbDrawing.Track> tracks = new ArrayList<>();
        for (KiCadPcb.Track track : pcb.tracks()) {
            Optional.ofNullable(copper.get(track.layer())).ifPresent(layer -> tracks.add(new PcbDrawing.Track(layer,
                    round.rounded(track.width()), 0, List.of(point(track.start()), point(track.end())))));
        }
        return tracks;
    }

    private List<PcbDrawing.Arc> arcs() {
        List<PcbDrawing.Arc> arcs = new ArrayList<>();
        for (KiCadPcb.Arc arc : pcb.arcs()) {
            Optional.ofNullable(copper.get(arc.layer())).ifPresent(layer -> arcs.add(new PcbDrawing.Arc(layer,
                    round.rounded(arc.width()), point(arc.start()), point(arc.middle()), point(arc.end()))));
        }
        return arcs;
    }

    /** Every via: through the pcb unless its layers say less. */
    private List<PcbDrawing.Via> vias() {
        List<PcbDrawing.Via> vias = new ArrayList<>();
        for (KiCadPcb.Via via : pcb.vias()) {
            PcbDrawing.Point at = point(via.at());
            String from = via.layers().isEmpty() ? null : copper.get(via.layers().getFirst());
            String to = via.layers().isEmpty() ? null : copper.get(via.layers().getLast());
            boolean through = PcbDrawing.TOP.equals(from) && PcbDrawing.BOTTOM.equals(to);
            vias.add(new PcbDrawing.Via(at.x(), at.y(), round.rounded(via.drill()), round.rounded(via.diameter()),
                    through ? null : from, through ? null : to, openOf(via.tenting())));
        }
        return vias;
    }

    /** Each area: a pour for each of its copper layers, a keepout on all of them. */
    private void areas(List<PcbDrawing.Zone> zones, List<PcbDrawing.Keepout> keepouts) {
        int beside = 0;
        int netless = 0;
        for (KiCadPcb.Zone zone : pcb.zones()) {
            List<String> layers = copperLayersOf(zone.layers());
            // Off copper: a window in the mask or the paste, which is simply an area on that layer. One zone may be both, the copper and its window.
            List<String> offCopper = zone.layers().stream().filter(layer -> copperLayersOf(List.of(layer)).isEmpty()).toList();
            offCopper.stream().map(this::pcbLayerOf).flatMap(Optional::stream).forEach(layer -> zone.outlines().forEach(outline ->
                    areasOffCopper.add(new PcbDrawing.Shape(layer, 0, true, "", pathOf(outline)))));
            if (layers.isEmpty()) {
                beside += offCopper.stream().map(this::pcbLayerOf).allMatch(Optional::isEmpty) ? 1 : 0;
                continue;
            }
            for (List<KiCadPoint> outline : zone.outlines()) {
                String path = pathOf(outline);
                if (zone.kind() == KiCadPcb.ZoneKind.KEEPOUT) {
                    keepouts.add(new PcbDrawing.Keepout(layers, path));
                    continue;
                }
                Optional<String> net = netOfZone(zone);
                if (net.isEmpty()) {
                    netless++;
                    continue;
                }
                layers.forEach(layer -> zones.add(new PcbDrawing.Zone(layer, net.get(), round.rounded(zone.clearance()),
                        zone.priority(), path)));
            }
        }
        if (beside > 0) {
            notes.add(beside + " areas are on layers we do not read, so they are left out.");
        }
        if (netless > 0) {
            notes.add(netless + " pours are on no net, so they are left out.");
        }
    }

    private Optional<String> netOfZone(KiCadPcb.Zone zone) {
        return zone.net().isBlank() ? Optional.empty() : Optional.of(netOfBoard(zone.net()));
    }

    /** The layers an area names, as ours; KiCad writes all of them, or both outer ones, in one word. */
    private List<String> copperLayersOf(List<String> layers) {
        Set<String> named = new LinkedHashSet<>();
        for (String layer : layers) {
            if (layer.equals(KiCadLayer.ALL_COPPER)) {
                named.addAll(copper.values());
            } else if (layer.equals(KiCadLayer.BOTH_SIDES_COPPER)) {
                named.add(PcbDrawing.TOP);
                named.add(PcbDrawing.BOTTOM);
            } else if (copper.containsKey(layer)) {
                named.add(copper.get(layer));
            }
        }
        return List.copyOf(named);
    }

    private String pathOf(List<KiCadPoint> outline) {
        RoundedPath path = new RoundedPath(PLACES);
        closedShape(outline, path);
        return path.build().toSource();
    }

    // ─── words and measures ─────────────────────────────

    /** Our layer of the pcb a KiCad layer is: copper by its place in the stack, the rest by what it is for and its side. */
    private Optional<String> pcbLayerOf(String kicad) {
        if (copper.containsKey(kicad)) {
            return Optional.of(copper.get(kicad));
        }
        return filing.layerOf(kicad).map(purpose -> KiCadLayer.isFront(kicad) ? PcbDrawing.layerOf(PcbDrawing.TOP, purpose)
                : KiCadLayer.isBack(kicad) ? PcbDrawing.layerOf(PcbDrawing.BOTTOM, purpose) : purpose);
    }

    /** Which side's mask a via is left bare on, in our word; nothing for one covered on both. */
    private static String openOf(KiCadPcb.Tenting tenting) {
        if (tenting.front() && tenting.back()) {
            return "";
        }
        return !tenting.front() && !tenting.back() ? PcbVocabulary.VIA_OPEN_BOTH : tenting.front() ? PcbDrawing.BOTTOM : PcbDrawing.TOP;
    }

    /** A graphic of the board in the frame of ours: from the edge's corner, as all of a pcb is. */
    private KiCadFootprintGraphic framed(KiCadFootprintGraphic graphic) {
        return mapped(graphic, position -> {
            PcbDrawing.Point ours = point(position);
            return new KiCadPoint(ours.x(), ours.y());
        });
    }

    /** What the board draws on its layers besides its copper and its edge. A filled graphic is an area, the rest are lines. */
    private List<PcbDrawing.Shape> shapes() {
        List<PcbDrawing.Shape> shapes = new ArrayList<>(areasOffCopper);
        int elsewhere = 0;
        for (KiCadFootprintGraphic graphic : pcb.graphics()) {
            if (graphic.layer().equals(KiCadLayer.EDGE)) {
                continue;
            }
            Optional<String> layer = pcbLayerOf(graphic.layer());
            if (layer.isEmpty()) {
                elsewhere++;
                continue;
            }
            shapes.add(new PcbDrawing.Shape(layer.get(), graphic.filled() ? 0 : round.rounded(graphic.width()), graphic.filled(), "",
                    KiCadFootprintDrawing.pathOf(framed(graphic))));
        }
        if (elsewhere > 0) {
            notes.add(elsewhere + " graphics are on layers we do not read, so they are left out.");
        }
        return shapes;
    }

    private List<PcbDrawing.Text> texts() {
        List<PcbDrawing.Text> texts = new ArrayList<>();
        int elsewhere = 0;
        for (KiCadPcb.Text text : pcb.texts()) {
            String layer = pcbLayerOf(text.layer()).orElse(null);
            if (layer == null) {
                elsewhere++;
                continue;
            }
            String anchor = switch (text.along()) {
                case LEFT -> PcbDrawing.START;
                case RIGHT -> PcbDrawing.END;
                case CENTRE -> PcbDrawing.MIDDLE;
            };
            // A line of a pcb's words is one line of its text: words of several lines are as many texts, one under another.
            List<String> lines = text.text().lines().toList();
            double pitch = text.height() * lineSpacing;
            double first = switch (text.across()) {
                case TOP -> text.height() / 2;
                case BOTTOM -> -text.height() / 2 - pitch * (lines.size() - 1);
                case MIDDLE -> -pitch * (lines.size() - 1) / 2;
            };
            for (int index = 0; index < lines.size(); index++) {
                PcbDrawing.Point at = point(across(text, first + pitch * index));
                texts.add(new PcbDrawing.Text(layer, lines.get(index), at.x(), at.y(), round.rounded(text.height()),
                        round.rounded(normalized(text.angle())), anchor));
            }
        }
        if (elsewhere > 0) {
            notes.add(elsewhere + " texts are on layers we do not read, so they are left out.");
        }
        return texts;
    }

    /** A point this far down a text's lines from where it stands, turned as the text is. */
    private static KiCadPoint across(KiCadPcb.Text text, double down) {
        double radians = Math.toRadians(text.angle());
        return new KiCadPoint(text.at().x() + down * Math.sin(radians), text.at().y() + down * Math.cos(radians));
    }

    private List<PcbDrawing.Dimension> dimensions() {
        return pcb.dimensions().stream().map(dimension -> new PcbDrawing.Dimension(point(dimension.start()), point(dimension.end()),
                round.rounded(dimension.height()))).toList();
    }

    /** What the file holds and a pcb has no place for, said once. */
    private void noteWhatIsLeft() {
        if (pcb.images() > 0) {
            notes.add(pcb.images() + " pictures on the board are left out: a pcb holds none.");
        }
    }

    // ─── geometry ───────────────────────────────────────

    private PcbDrawing.Point point(KiCadPoint point) {
        return new PcbDrawing.Point(round.rounded(point.x() - left), round.rounded(point.y() - top));
    }

    private SheetTracing.Point traced(KiCadPoint point) {
        return new SheetTracing.Point(round.rounded(point.x() - left), round.rounded(point.y() - top));
    }

    private static double normalized(double degrees) {
        return ((degrees % 360) + 360) % 360;
    }

    private static String named(Set<String> names) {
        List<String> listed = names.stream().limit(NAMED_IN_A_NOTE).toList();
        int rest = names.size() - listed.size();
        return String.join(", ", listed) + (rest > 0 ? " and " + rest + " more" : "");
    }

}
