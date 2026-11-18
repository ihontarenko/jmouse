package org.jmouse.cad.kicad;

import org.jmouse.cad.Point;
import org.jmouse.cad.pcb.drawing.Placement;
import org.jmouse.cad.pcb.drawing.Reference;
import org.jmouse.cad.pcb.drawing.Track;
import org.jmouse.cad.pcb.drawing.Arc;
import org.jmouse.cad.pcb.drawing.Via;
import org.jmouse.cad.pcb.drawing.Zone;
import org.jmouse.cad.pcb.drawing.Keepout;
import org.jmouse.cad.pcb.drawing.Dimension;
import org.jmouse.cad.pcb.drawing.Lettering;
import org.jmouse.cad.pcb.drawing.Shape;
import org.jmouse.cad.pcb.drawing.Hole;
import org.jmouse.cad.drawing.Arrangement;
import org.jmouse.cad.drawing.DrawnGroup;
import org.jmouse.cad.drawing.DrawnGroups;
import org.jmouse.kicad.pcb.KiCadArrangement;
import org.jmouse.kicad.pcb.KiCadLock;
import java.util.LinkedHashSet;
import java.util.Set;
import org.jmouse.cad.circuit.Circuit;
import org.jmouse.cad.document.DocumentNode;
import org.jmouse.cad.document.MetaNode;
import org.jmouse.cad.document.Provenance;
import org.jmouse.cad.exchange.imported.UniqueIdentifiers;
import org.jmouse.cad.exchange.pcb.PcbArtwork;
import org.jmouse.cad.exchange.pcb.PcbFabrication;
import org.jmouse.cad.pcb.PcbVocabulary;
import org.jmouse.cad.pcb.drawing.FootprintDrawing;
import org.jmouse.cad.pcb.drawing.PcbDrawing;
import org.jmouse.cad.pcb.geometry.CopperNets;
import org.jmouse.cad.pcb.geometry.PadOutline;
import org.jmouse.cad.pcb.geometry.PlacedDesignator;
import org.jmouse.cad.pcb.geometry.RoundedTrack;
import org.jmouse.cad.simulation.CatalogueDocuments;
import org.jmouse.kicad.KiCadLayer;
import org.jmouse.kicad.KiCadPoint;
import org.jmouse.kicad.footprint.KiCadFootprint;
import org.jmouse.kicad.footprint.KiCadFootprintGraphic;
import org.jmouse.kicad.footprint.KiCadModel;
import org.jmouse.kicad.footprint.KiCadPad;
import org.jmouse.kicad.footprint.KiCadPadShape;
import org.jmouse.kicad.footprint.KiCadPadType;
import org.jmouse.kicad.pcb.KiCadPcb;
import org.jmouse.kicad.pcb.KiCadPcbBuilder;
import org.jmouse.kicad.pcb.KiCadSide;
import org.jmouse.sexpression.node.SExpressionNumbers;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * A pcb as a KiCad board: the way back from {@link KiCadPcbImport}. Its edge on {@code Edge.Cuts}; each part's footprint,
 * drawn from our own drawing of it and named as KiCad knows it ({@link KiCadFootprintIds}), standing where the part stands —
 * on the back as KiCad keeps one there — and tied to its symbol in the schematic written beside it; the holes the pcb drills
 * of its own as footprints of a bare hole; its tracks with their rounded corners as arcs, its arcs and vias, each on the net
 * its copper reaches; its pours and keepouts as zones; its words, measures and drawings on their layers.
 *
 * <p>What KiCad keeps elsewhere is written there: copper left bare is an opening drawn on the mask, as KiCad opens one, and
 * how far the mask opens past a pad is the board's own setting. A pcb is drawn from its top left and KiCad's page has Y
 * downwards too, so the board is only moved onto the page. What a KiCad board cannot say is named in the notes.</p>
 */
public final class KiCadPcbExport {

    /**
     * What an export comes out as: the file's name and text, what the board could not say, and every footprint it stands,
     * by the library id it names: what the project's own library holds of them.
     */
    public record Exported(String fileName, String text, List<String> notes, Map<String, KiCadFootprint> footprints) {

        public Exported {
            notes = List.copyOf(notes);
            footprints = Collections.unmodifiableMap(new LinkedHashMap<>(footprints));
        }
    }

    private static final String REFERENCE = "reference";
    private static final String VALUE = "value";
    private static final String EXTENSION = ".kicad_pcb";

    private final PcbFabrication fabrication;
    private final PcbDrawing pcb;
    private final KiCadLibraries libraries;
    private final KiCadFootprintIds footprintIds;
    private final KiCadFootprintExport footprints;
    private final KiCadPathGraphics graphics;
    private final Map<String, Double> rules;
    private final PcbArtwork.Settings artwork;
    private final Map<String, KiCadSchematicExport.Symbol> symbols;
    /** What KiCad calls each part, as the schematic beside it does. */
    private final Map<String, String> references;
    /** What KiCad calls each of our nets: the schematic's names; a net it names not keeps its own, node 0 KiCad's ground. */
    private final Map<String, String> kicadNets;
    private final CatalogueDocuments catalogue;
    private final CopperNets nets;
    private final Map<String, String> copper = new LinkedHashMap<>();
    private final List<String> notes = new ArrayList<>();
    /** Every footprint the board stands, by the library id it names. */
    private final Map<String, KiCadFootprint> standing = new LinkedHashMap<>();
    private final KiCadPcbBuilder board;
    private int foreignModels;

    private KiCadPcbExport(PcbFabrication fabrication, String title, Map<String, String> references, Map<String, KiCadSchematicExport.Symbol> symbols,
                           Map<String, String> kicadNets, CatalogueDocuments catalogue,
                           KiCadLibraryMap map, KiCadLibraries libraries, Map<String, Double> rules, PcbArtwork.Settings artwork) {
        this.fabrication = fabrication;
        this.pcb = fabrication.pcb();
        this.libraries = libraries;
        this.footprintIds = new KiCadFootprintIds(catalogue, map, libraries);
        this.footprints = new KiCadFootprintExport(libraries);
        this.graphics = new KiCadPathGraphics(libraries.areaStep());
        this.rules = rules;
        this.artwork = artwork;
        this.symbols = symbols;
        this.references = references;
        this.kicadNets = kicadNets;
        this.catalogue = catalogue;
        this.nets = fabrication.copperNets(new PadOutline(artwork.curveSteps(), artwork.cornerSteps()), artwork.curveSteps());
        List<String> ours = pcb.copperLayerIds();
        List<String> theirs = KiCadPcbBuilder.copperLayers(ours.size());
        for (int index = 0; index < ours.size(); index++) {
            copper.put(ours.get(index), theirs.get(index));
        }
        this.board = KiCadPcbBuilder.of(fabrication.name(), title, extent(), ours.size(), rules.get(PcbVocabulary.RULE_PCB_THICKNESS),
                artwork.letterStroke(), artwork.maskExpansion());
        declareGroups();
        notes.addAll(fabrication.notes());
    }

    /** Every group of the pcb told to the board before anything is put in it: those it declares, and those only named by members. */
    private void declareGroups() {
        DrawnGroups declared = pcb.drawnGroups();
        Set<String> named = new LinkedHashSet<>();
        pcb.groups().forEach(group -> named.add(group.name()));
        pcb.standingItems().forEach(item -> {
            if (item.arrangement().grouped()) {
                named.add(item.arrangement().group());
            }
        });
        named.forEach(name -> board.group(name, declared.named(name).map(DrawnGroup::locked).orElse(false) ? KiCadLock.LOCKED : KiCadLock.FREE));
    }

    /** How a thing of the pcb stands on KiCad's board: in its group, locked where it is locked itself. */
    private static KiCadArrangement kicadArrangement(Arrangement arrangement) {
        return new KiCadArrangement(arrangement.group(), arrangement.locked() ? KiCadLock.LOCKED : KiCadLock.FREE);
    }

    /**
     * @param project   what the KiCad project is called: the file is named after it
     * @param title     what the board is called on its title block
     * @param references what KiCad calls each part, by designator, as the schematic written beside it does
     * @param symbols   the symbol each part is drawn as in the schematic written beside it, by designator
     * @param kicadNets what KiCad calls each net, as the schematic written beside it names them
     * @param catalogue the documents the pcb names — its circuit, the components, the footprints, the models
     * @param map       which KiCad footprint each of ours is, read backwards
     * @param rules     the values of the rules the pcb is made by
     * @param artwork   how every format draws what no document says: curves, a designator nothing places, the mask's opening
     */
    public static Exported of(PcbFabrication fabrication, String project, String title, Map<String, String> references,
                              Map<String, KiCadSchematicExport.Symbol> symbols,
                              Map<String, String> kicadNets, CatalogueDocuments catalogue, KiCadLibraryMap map, KiCadLibraries libraries,
                              Map<String, Double> rules, PcbArtwork.Settings artwork) {
        return new KiCadPcbExport(fabrication, title, references, symbols, kicadNets, catalogue, map, libraries, rules, artwork).export(project);
    }

    private Exported export(String project) {
        edge();
        parts();
        holes();
        copper();
        areas();
        drawings();
        noteWhatIsLeft();
        String fileName = project + EXTENSION;
        return new Exported(fileName, board.print(fileName), notes, standing);
    }

    // ─── frame and edge ─────────────────────────────────────────────────────────────────────────────

    /**
     * How far the pcb reaches: its edge exactly, as KiCad's graphics of it reach — the frame a board is read back in —, or
     * where its parts stand when it draws none.
     */
    private KiCadPcbBuilder.Extent extent() {
        List<KiCadPoint> points = new ArrayList<>();
        graphics.stroked(pcb.outline(), KiCadLayer.EDGE, libraries.edgeWidth()).forEach(graphic -> points.addAll(graphic.reach()));
        if (points.isEmpty()) {
            pcb.placements().forEach(placement -> points.add(new KiCadPoint(placement.x(), placement.y())));
        }
        if (points.isEmpty()) {
            return new KiCadPcbBuilder.Extent(0, 0, 0, 0);
        }
        return new KiCadPcbBuilder.Extent(points.stream().mapToDouble(KiCadPoint::x).min().orElseThrow(),
                points.stream().mapToDouble(KiCadPoint::y).min().orElseThrow(), points.stream().mapToDouble(KiCadPoint::x).max().orElseThrow(),
                points.stream().mapToDouble(KiCadPoint::y).max().orElseThrow());
    }

    private void edge() {
        if (pcb.outline() == null || pcb.outline().isBlank()) {
            notes.add("The pcb draws no edge, so the board has none on Edge.Cuts.");
            return;
        }
        graphics.stroked(pcb.outline(), KiCadLayer.EDGE, libraries.edgeWidth()).forEach(board::graphic);
    }

    // ─── parts ──────────────────────────────────────────────────────────────────────────────────────

    private void parts() {
        int unplaced = 0;
        List<String> withoutDrawing = new ArrayList<>();
        for (PcbFabrication.SeatedPart part : fabrication.parts()) {
            if (part.footprint().isEmpty() || part.drawing().isEmpty()) {
                withoutDrawing.add(part.designator());
                continue;
            }
            Placement placement = part.placement();
            FootprintDrawing drawing = part.drawing().get();
            String libraryId = footprintIds.of(part.footprint().get());
            Optional<KiCadSchematicExport.Symbol> symbol = placement.ownedByPcb() ? Optional.empty()
                    : Optional.ofNullable(symbols.get(part.designator()));
            Optional<String> path = symbol.map(KiCadSchematicExport.Symbol::path);
            if (!placement.ownedByPcb() && path.isEmpty()) {
                unplaced++;
            }
            String reference = references.getOrDefault(part.designator(), part.printed());
            KiCadFootprint footprint = footprints.footprintOf(nameOf(libraryId), titleOf(part.footprint().get()), drawing,
                    modelsOf(drawing), catalogue.find(part.footprint().get()).map(Provenance::of).orElse(Provenance.OURS));
            // KiCad holds a footprint's value to its symbol's: the schematic's words, where it places the part.
            String value = symbol.map(KiCadSchematicExport.Symbol::value).orElse(part.value());
            Map<String, String> padNets = new LinkedHashMap<>();
            part.padNets().forEach((pad, net) -> padNets.put(pad, kicadNet(net)));
            standing.putIfAbsent(libraryId, footprint);
            // On the board it carries its part's fields too, as KiCad copies a symbol's onto its footprint.
            KiCadFootprint placedFootprint = footprint.withProperties(symbol.map(KiCadSchematicExport.Symbol::fields).orElse(Map.of()));
            board.footprint(new KiCadPcbBuilder.PlacedFootprint(libraryId, placedFootprint, path, reference, value,
                    placement.onBottom() ? KiCadSide.BACK : KiCadSide.FRONT, point(placement.x(), placement.y()),
                    placement.onBottom() ? placement.rotation() - 180 : placement.rotation(), referenceField(part, drawing),
                    valueField(drawing), padNets, part.nothingToMount(), kicadArrangement(placement.arrangement())));
        }
        if (!withoutDrawing.isEmpty()) {
            notes.add(withoutDrawing.size() + " parts have no footprint to stand on and are left off the board: "
                    + String.join(", ", withoutDrawing) + ".");
        }
        if (unplaced > 0) {
            notes.add(unplaced + " parts are of sheets the schematic places as blocks, which the KiCad schematic does not draw yet: "
                    + "their footprints stand on the board tied to no symbol.");
        }
    }

    /** Where a part's reference prints, in its footprint's frame: where its line moved it, else its footprint's field, else above it. */
    private KiCadPcbBuilder.Field referenceField(PcbFabrication.SeatedPart part, FootprintDrawing drawing) {
        Placement placement = part.placement();
        Optional<FootprintDrawing.Field> field = fieldOf(drawing, REFERENCE);
        PlacedDesignator placed = PlacedDesignator.of(placement, Optional.of(drawing), artwork.designator());
        if (placement.reference().isPresent()) {
            Reference moved = placement.reference().get();
            return new KiCadPcbBuilder.Field(new KiCadPoint(moved.x(), moved.y()), moved.rotation(), footprints.fieldLayer(placed.layer()),
                    placed.height(), false, KiCadFootprintDrawing.alongOf(placed.anchor()));
        }
        if (field.isPresent()) {
            FootprintDrawing.Field found = field.get();
            return new KiCadPcbBuilder.Field(new KiCadPoint(found.x(), found.y()), found.rotation(), footprints.fieldLayer(found.layer()),
                    found.height(), false, KiCadFootprintDrawing.alongOf(found.anchor()));
        }
        return new KiCadPcbBuilder.Field(partFrame(placement, placed.x(), placed.y()), placed.rotation() - placement.rotation(),
                footprints.fieldLayer(placed.layer()), placed.height(), false);
    }

    /** Where a part's value prints: its footprint's field; a footprint that prints none keeps it hidden on its origin. */
    private KiCadPcbBuilder.Field valueField(FootprintDrawing drawing) {
        return fieldOf(drawing, VALUE).map(found -> new KiCadPcbBuilder.Field(new KiCadPoint(found.x(), found.y()), found.rotation(),
                footprints.fieldLayer(found.layer()), found.height(), false, KiCadFootprintDrawing.alongOf(found.anchor()))).orElseGet(() -> new KiCadPcbBuilder.Field(new KiCadPoint(0, 0), 0,
                footprints.fieldLayer(PcbVocabulary.FABRICATION), artwork.designator().least(), true));
    }

    private static Optional<FootprintDrawing.Field> fieldOf(FootprintDrawing drawing, String subject) {
        return drawing.fields().stream().filter(field -> field.subject().equals(subject)).findFirst();
    }

    /** A point of the pcb in a part's own frame: {@link Placement#placed} undone. */
    private static KiCadPoint partFrame(Placement placement, double x, double y) {
        double radians = Math.toRadians(-placement.rotation());
        double across = x - placement.x();
        double down = y - placement.y();
        double mirroredX = across * Math.cos(radians) + down * Math.sin(radians);
        double footprintY = -across * Math.sin(radians) + down * Math.cos(radians);
        return new KiCadPoint(placement.onBottom() ? -mirroredX : mirroredX, footprintY);
    }

    /** The models of KiCad's own model libraries that show a footprint's body, by the file KiCad names them with. */
    private List<KiCadModel> modelsOf(FootprintDrawing drawing) {
        List<KiCadModel> models = new ArrayList<>();
        for (FootprintDrawing.Model model : drawing.models()) {
            Optional<String> place = catalogue.find(model.reference()).flatMap(document -> document.block(MetaNode.class))
                    .flatMap(meta -> meta.text(MetaNode.SOURCE)).flatMap(KiCadSources::modelPathOf);
            if (place.isEmpty()) {
                foreignModels++;
                continue;
            }
            models.add(new KiCadModel(libraries.modelFile().formatted(place.get()), axes(model.offset()), axes(model.scale()),
                    axes(model.rotation()), model.hidden()));
        }
        return models;
    }

    private static KiCadModel.Axes axes(List<Double> values) {
        return new KiCadModel.Axes(values.get(0), values.get(1), values.get(2));
    }

    private static String nameOf(String libraryId) {
        int colon = libraryId.indexOf(':');
        return colon < 0 ? libraryId : libraryId.substring(colon + 1);
    }

    private String titleOf(String footprint) {
        return footprintIds.document(footprint).map(DocumentNode::title).orElse("");
    }

    /** Each hole the pcb drills of its own, as the footprint of one bare or plated hole KiCad stands a mounting hole on. */
    private void holes() {
        UniqueIdentifiers references = new UniqueIdentifiers(libraries.pcbOwnPart(), UniqueIdentifiers.UNLIMITED);
        fabrication.parts().forEach(part -> references.of(part.designator()));
        for (Hole hole : pcb.holes()) {
            boolean plated = hole.ring() > 0;
            double size = plated ? hole.ring() : hole.drill();
            String name = libraries.holeFootprint().formatted(SExpressionNumbers.written(hole.drill()));
            KiCadPad pad = new KiCadPad("", plated ? KiCadPadType.THROUGH_HOLE : KiCadPadType.UNPLATED_HOLE, KiCadPadShape.CIRCLE,
                    new KiCadPoint(0, 0), 0, size, size, Optional.of(new double[] {hole.drill(), hole.drill()}), 0,
                    List.of(KiCadLayer.ALL_COPPER, KiCadLayer.ALL_MASK), Optional.empty(), Optional.empty(), List.of());
            KiCadFootprint footprint = new KiCadFootprint(name, "", List.of(), Map.of(), List.of(pad), List.of(), List.of(), List.of());
            String reference = references.of(libraries.pcbOwnPart());
            KiCadPcbBuilder.Field hidden = new KiCadPcbBuilder.Field(new KiCadPoint(0, 0), 0,
                    footprints.fieldLayer(PcbVocabulary.FABRICATION), artwork.designator().least(), true);
            standing.putIfAbsent(libraries.ownLibrary() + ":" + name, footprint);
            board.footprint(new KiCadPcbBuilder.PlacedFootprint(libraries.ownLibrary() + ":" + name, footprint, Optional.empty(), reference,
                    name, KiCadSide.FRONT, point(hole.x(), hole.y()), 0, hidden, hidden, Map.of(), true, KiCadArrangement.FREE));
        }
    }

    // ─── copper ─────────────────────────────────────────────────────────────────────────────────────

    private void copper() {
        for (int index = 0; index < pcb.tracks().size(); index++) {
            Track track = pcb.tracks().get(index);
            String net = nets.trackNets().get(index).map(this::kicadNet).orElse("");
            String layer = copperOf(track.layer());
            // Copper left bare is the mask opened along it, as wide as the track and the mask's own opening past copper.
            Optional<String> bare = track.bare() ? maskOf(track.layer()) : Optional.empty();
            double opened = track.width() + 2 * artwork.maskExpansion();
            for (RoundedTrack.Piece piece : RoundedTrack.of(track)) {
                switch (piece) {
                    case RoundedTrack.Run run -> {
                        for (int point = 1; point < run.points().size(); point++) {
                            KiCadPoint start = point(run.points().get(point - 1));
                            KiCadPoint end = point(run.points().get(point));
                            // A leg of no length between two others adds no copper; a run of one such leg is a dot of copper.
                            if (start.sameAs(end) && run.points().size() > 2) {
                                continue;
                            }
                            board.track(start, end, track.width(), layer, net, kicadArrangement(track.arrangement()));
                            bare.ifPresent(mask -> board.graphic(new KiCadFootprintGraphic.Line(start, end, mask, opened, false)));
                        }
                    }
                    case RoundedTrack.Bend bend -> {
                        board.arc(point(bend.from()), point(bend.through()), point(bend.to()), track.width(), layer, net, kicadArrangement(track.arrangement()));
                        bare.ifPresent(mask -> board.graphic(new KiCadFootprintGraphic.Arc(point(bend.from()), point(bend.through()),
                                point(bend.to()), mask, opened, false)));
                    }
                }
            }
        }
        for (int index = 0; index < pcb.arcs().size(); index++) {
            Arc arc = pcb.arcs().get(index);
            board.arc(point(arc.from()), point(arc.through()), point(arc.to()), arc.width(), copperOf(arc.layer()),
                    nets.arcNets().get(index).map(this::kicadNet).orElse(""), kicadArrangement(arc.arrangement()));
            if (arc.bare()) {
                maskOf(arc.layer()).ifPresent(mask -> board.graphic(new KiCadFootprintGraphic.Arc(point(arc.from()), point(arc.through()),
                        point(arc.to()), mask, arc.width() + 2 * artwork.maskExpansion(), false)));
            }
        }
        List<String> layers = pcb.copperLayerIds();
        for (int index = 0; index < pcb.vias().size(); index++) {
            Via via = pcb.vias().get(index);
            String from = via.from() == null || via.from().isBlank() ? layers.getFirst() : via.from();
            String to = via.to() == null || via.to().isBlank() ? layers.getLast() : via.to();
            board.via(point(via.x(), via.y()), via.diameter(), via.drill(), copperOf(from), copperOf(to),
                    nets.viaNets().get(index).map(this::kicadNet).orElse(""), kicadArrangement(via.arrangement()));
            // KiCad tents its vias board-wide; one left open on a side is an opening drawn on that side's mask, as wide as its own would be.
            for (String side : List.of(PcbVocabulary.TOP, PcbVocabulary.BOTTOM)) {
                if (via.opensMaskOn(side)) {
                    String mask = maskLayer(side);
                    KiCadPoint centre = point(via.x(), via.y());
                    double radius = via.diameter() / 2 + artwork.maskExpansion();
                    board.graphic(new KiCadFootprintGraphic.Circle(centre, new KiCadPoint(centre.x() + radius, centre.y()), mask, 0, true));
                }
            }
        }
    }

    // ─── areas ──────────────────────────────────────────────────────────────────────────────────────

    private void areas() {
        double clearance = rules.get(PcbVocabulary.RULE_CLEARANCE);
        double leastWidth = rules.get(PcbVocabulary.RULE_LEAST_TRACK_WIDTH);
        double thermalGap = rules.get(PcbVocabulary.RULE_THERMAL_GAP);
        double thermalSpoke = rules.get(PcbVocabulary.RULE_THERMAL_SPOKE_WIDTH);
        for (Zone zone : pcb.zones()) {
            for (List<KiCadPoint> outline : graphics.outlines(zone.path())) {
                board.area(new KiCadPcbBuilder.Area(KiCadPcbBuilder.AreaKind.POUR, kicadNet(zone.net()), List.of(copperOf(zone.layer())),
                        zone.clearance() > 0 ? zone.clearance() : clearance, zone.priority(), leastWidth, thermalGap, thermalSpoke, outline));
            }
            if (zone.bare()) {
                maskOf(zone.layer()).ifPresent(mask -> graphics.filled(zone.path(), mask).forEach(board::graphic));
            }
        }
        for (Keepout keepout : pcb.keepouts()) {
            List<String> layers = keepout.layers().stream().filter(copper::containsKey).map(copper::get).toList();
            if (layers.isEmpty()) {
                continue;
            }
            for (List<KiCadPoint> outline : graphics.outlines(keepout.path())) {
                board.area(new KiCadPcbBuilder.Area(KiCadPcbBuilder.AreaKind.KEEPOUT, "", layers, 0, 0, leastWidth, thermalGap,
                        thermalSpoke, outline));
            }
        }
    }

    // ─── words, measures, drawings ──────────────────────────────────────────────────────────────────

    private void drawings() {
        for (Lettering text : pcb.texts()) {
            Optional<String> layer = kicadLayerOf(text.layer());
            if (layer.isEmpty()) {
                notes.add("The words \"" + text.text() + "\" are on " + text.layer() + ", a layer KiCad has no place for; left out.");
                continue;
            }
            KiCadPcb.Along along = switch (text.anchor()) {
                case PcbVocabulary.TEXT_START -> KiCadPcb.Along.LEFT;
                case PcbVocabulary.TEXT_END -> KiCadPcb.Along.RIGHT;
                default -> KiCadPcb.Along.CENTRE;
            };
            board.text(text.text(), point(text.x(), text.y()), text.rotation(), layer.get(), text.size(),
                    new KiCadPcbBuilder.Justification(along, KiCadPcb.Across.MIDDLE));
        }
        String measures = libraries.kicadLayerOf(PcbVocabulary.DOCUMENTATION, false).orElseThrow();
        for (Dimension dimension : pcb.dimensions()) {
            board.dimension(point(dimension.from()), point(dimension.to()), dimension.offset(), measures);
        }
        int netted = 0;
        for (Shape shape : pcb.shapes()) {
            Optional<String> layer = kicadLayerOf(shape.layer());
            if (layer.isEmpty()) {
                notes.add("A shape on " + shape.layer() + ", a layer KiCad has no place for, is left out.");
                continue;
            }
            if (!shape.net().isEmpty()) {
                netted++;
            }
            (shape.filled() ? graphics.filled(shape.path(), layer.get()) : graphics.stroked(shape.path(), layer.get(), shape.width()))
                    .forEach(board::graphic);
        }
        if (netted > 0) {
            notes.add(netted + " shapes of copper carry a net, which a drawing on a KiCad board does not: KiCad's checks see them as "
                    + "copper of no net.");
        }
    }

    private void noteWhatIsLeft() {
        if (foreignModels > 0) {
            notes.add(foreignModels + " 3D models are this library's own, not KiCad's: KiCad shows those parts without a body.");
        }
        if (!pcb.teardrops().isEmpty()) {
            notes.add("The pcb's teardrops are not written: KiCad adds its own from its board setup.");
        }
        if (!pcb.pairs().isEmpty()) {
            notes.add(pcb.pairs().size() + " differential pairs are not written: KiCad pairs nets by their names ending in _P and _N.");
        }
    }

    /** A net as KiCad calls it: the schematic's name for it; else its own, node 0 being KiCad's ground. */
    private String kicadNet(String net) {
        String named = kicadNets.get(net);
        if (named != null) {
            return named;
        }
        return net.equals(Circuit.GROUND) ? KiCadNetNames.KICAD_GROUND : net;
    }

    // ─── layers and points ──────────────────────────────────────────────────────────────────────────

    private String copperOf(String layer) {
        String found = copper.get(layer);
        if (found == null) {
            throw new IllegalArgumentException("The pcb names copper on '" + layer + "', which is none of its layers " + copper.keySet() + ".");
        }
        return found;
    }

    /** A layer of the pcb as KiCad names it: copper by its place in the stack, the rest by its side and what it is for. */
    private Optional<String> kicadLayerOf(String layer) {
        if (copper.containsKey(layer)) {
            return Optional.of(copper.get(layer));
        }
        for (String side : List.of(PcbVocabulary.TOP, PcbVocabulary.BOTTOM)) {
            String opening = side + "-";
            if (layer.startsWith(opening)) {
                return libraries.kicadLayerOf(layer.substring(opening.length()), PcbVocabulary.BOTTOM.equals(side));
            }
        }
        return libraries.kicadLayerOf(layer, false);
    }

    /** The mask over a copper layer of the outside; none over an inner one. */
    private Optional<String> maskOf(String layer) {
        return PcbVocabulary.TOP.equals(layer) || PcbVocabulary.BOTTOM.equals(layer) ? Optional.of(maskLayer(layer)) : Optional.empty();
    }

    private String maskLayer(String side) {
        return (PcbVocabulary.BOTTOM.equals(side) ? KiCadSide.BACK : KiCadSide.FRONT).layer(KiCadLayer.MASK_SUFFIX);
    }

    private static KiCadPoint point(Point point) {
        return new KiCadPoint(point.x(), point.y());
    }

    private static KiCadPoint point(double x, double y) {
        return new KiCadPoint(x, y);
    }
}
