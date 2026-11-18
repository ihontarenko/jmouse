package org.jmouse.cad.panel;

import org.jmouse.cad.Point;
import org.jmouse.cad.pcb.drawing.NetClass;
import org.jmouse.cad.pcb.drawing.Teardrop;
import org.jmouse.cad.pcb.drawing.Placement;
import org.jmouse.cad.pcb.drawing.Reference;
import org.jmouse.cad.pcb.drawing.Track;
import org.jmouse.cad.pcb.drawing.Arc;
import org.jmouse.cad.pcb.drawing.Via;
import org.jmouse.cad.pcb.drawing.Zone;
import org.jmouse.cad.pcb.drawing.Keepout;
import org.jmouse.cad.pcb.drawing.Shape;
import org.jmouse.cad.pcb.drawing.Hole;
import org.jmouse.cad.circuit.Connection;
import org.jmouse.cad.circuit.Part;
import org.jmouse.cad.Setting;
import org.jmouse.cad.Text;
import org.jmouse.cad.document.DocumentKind;
import org.jmouse.cad.document.DocumentNode;
import org.jmouse.cad.document.MetaNode;
import org.jmouse.cad.exchange.DocumentFlattening;
import org.jmouse.cad.exchange.pcb.PcbFabrication;
import org.jmouse.cad.library.CadLanguage;
import org.jmouse.cad.library.CadStandards;
import org.jmouse.cad.pcb.PcbModule;
import org.jmouse.cad.pcb.drawing.PcbDrawing;
import org.jmouse.cad.simulation.CatalogueDocuments;
import org.jmouse.cad.simulation.CircuitLines;
import org.jmouse.cad.simulation.SimulationModule;
import org.jmouse.cad.simulation.SimulationReading;
import org.jmouse.cad.simulation.elaboration.SheetUnfolding;
import org.jmouse.el.grammar.ClauseBlockNode;
import org.locationtech.jts.geom.Envelope;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Supplier;

/**
 * A panel made into the one pcb it is: each board's copper, parts and holes moved to where it stands, its nets and parts told
 * apart from the same board's next copy, the frame and what separates the boards added. A circuit of all the boards' parts is
 * made beside it, because that is what a pcb's parts are read from, so every format of a pcb writes a panel as it stands.
 */
public final class PanelFlattening implements DocumentFlattening {

    /** The rule a pcb's thickness is in: boards of one panel are one sheet of material. */
    private static final String THICKNESS = "pcb-thickness";

    /** What separates a board's copy from the name it adds to its parts and nets. */
    private static final String COPY_MARK = "_";

    private static final String COPY = "B";

    /** The block a circuit holds its parts in. */
    private static final String PARTS_BLOCK = "parts";

    private final Supplier<PanelDefaults> defaults;
    /** The ways a pcb is made: what thickness a board names, read through the engine's tables. */
    private final CadStandards standards;

    /** @param defaults what a panel takes where its lines say nothing, as it stands when a panel is written */
    public PanelFlattening(Supplier<PanelDefaults> defaults, CadStandards standards) {
        this.defaults = defaults;
        this.standards = standards;
    }

    @Override
    public DocumentKind from() {
        return PanelModule.PANEL;
    }

    @Override
    public DocumentKind to() {
        return PcbModule.PCB;
    }

    @Override
    public Flattened flatten(DocumentNode document, CatalogueDocuments catalogue) {
        PanelDrawing panel = PanelDrawing.of(document);
        if (panel.boards().isEmpty()) {
            throw new IllegalArgumentException("The panel holds no board.");
        }
        Map<String, DocumentNode> pcbs = new LinkedHashMap<>();
        Map<String, PcbDrawing> drawings = new LinkedHashMap<>();
        for (PanelDrawing.Board board : panel.boards()) {
            DocumentNode pcb = pcbs.computeIfAbsent(board.pcb(), slug -> catalogue.find(slug).filter(found -> found.kind().equals(PcbModule.PCB))
                    .orElseThrow(() -> new IllegalArgumentException("The panel names '" + slug + "', which is no pcb the reader may see.")));
            drawings.computeIfAbsent(board.pcb(), ignored -> PcbDrawing.of(pcb));
        }
        requireOneMaterial(drawings);
        PanelLayout layout = PanelLayout.of(panel, drawings, defaults.get());
        return flattened(document, layout, pcbs, drawings, catalogue);
    }

    private Flattened flattened(DocumentNode document, PanelLayout layout, Map<String, DocumentNode> pcbs,
                                Map<String, PcbDrawing> drawings, CatalogueDocuments catalogue) {
        String circuitSlug = document.slug() + "-circuit";
        Merged merged = new Merged();
        List<String> notes = new ArrayList<>(layout.notes());
        for (PanelLayout.Copy copy : layout.copies()) {
            PcbFabrication board = PcbFabrication.of(pcbs.get(copy.pcb()), catalogue);
            String prefix = COPY + copy.number() + COPY_MARK;
            merged.addPcb(drawings.get(copy.pcb()), copy.motion(), prefix);
            merged.addParts(boardParts(pcbs.get(copy.pcb()), catalogue), board, prefix);
            board.notes().forEach(note -> notes.add(copy.pcb() + ": " + note));
        }
        PcbDrawing first = drawings.values().iterator().next();
        merged.holes.addAll(layout.holes());
        merged.shapes.addAll(layout.shapes());
        merged.texts.addAll(layout.texts());
        PcbDrawing flat = new PcbDrawing(circuitSlug, first.rules(), merged.netClasses, first.sizes(), merged.teardrops(drawings), List.of(), first.copperLayers(),
                layout.outline(), merged.placements, merged.tracks, merged.arcs, merged.vias, merged.zones, merged.keepouts, List.of(), merged.texts,
                merged.holes, merged.shapes, List.of(), List.of());
        DocumentNode pcb = new DocumentNode(PcbModule.PCB, document.title(), document.slug(), null, 1);
        document.getExpressions().stream().filter(MetaNode.class::isInstance).forEach(pcb::addExpression);
        flat.lines().forEach(pcb::addExpression);
        DocumentNode circuit = new DocumentNode(SimulationModule.CIRCUIT, document.title(), circuitSlug, null, 1);
        CircuitLines lines = new CircuitLines(CadLanguage.standard().language(), circuitSlug);
        if (!merged.parts.isEmpty()) {
            ClauseBlockNode parts = new ClauseBlockNode(PARTS_BLOCK, null);
            parts.open();
            merged.parts.forEach(part -> parts.addExpression(lines.part(part)));
            circuit.addExpression(parts);
        }
        return new Flattened(pcb, new WithCircuit(catalogue, circuit, document), notes);
    }

    /** The parts a board's circuit holds, its sheets unfolded, by reference. */
    private static Map<String, Part> boardParts(DocumentNode pcb, CatalogueDocuments catalogue) {
        String slug = PcbDrawing.of(pcb).circuit();
        DocumentNode circuit = catalogue.find(slug).orElseThrow();
        Map<String, Part> parts = new HashMap<>();
        SheetUnfolding.of(SimulationReading.circuit(circuit), catalogue).parts().forEach(part -> parts.put(part.reference(), part));
        return parts;
    }

    /** Boards of one panel are one sheet of material: one count of copper layers, one thickness. */
    private void requireOneMaterial(Map<String, PcbDrawing> drawings) {
        Double thickness = null;
        Integer copper = null;
        for (Map.Entry<String, PcbDrawing> entry : drawings.entrySet()) {
            Double own = standards.pcbRules().valuesOf(entry.getValue().rules()).get(THICKNESS);
            int layers = entry.getValue().copperLayers();
            if (copper != null && copper != layers) {
                throw new IllegalArgumentException("'" + entry.getKey() + "' has " + layers + " copper layers and the panel's other boards " + copper
                                                   + ": a panel is one sheet of material.");
            }
            if (thickness != null && own != null && Math.abs(thickness - own) > 1e-9) {
                throw new IllegalArgumentException("'" + entry.getKey() + "' is " + own + " mm thick and the panel's other boards " + thickness
                                                   + " mm: a panel is one sheet of material.");
            }
            copper = layers;
            thickness = own;
        }
    }

    /** What the boards' copies come to, together. */
    private static final class Merged {

        private final List<NetClass> netClasses = new ArrayList<>();
        private final List<Placement> placements = new ArrayList<>();
        private final List<Track> tracks = new ArrayList<>();
        private final List<Arc> arcs = new ArrayList<>();
        private final List<Via> vias = new ArrayList<>();
        private final List<Zone> zones = new ArrayList<>();
        private final List<Keepout> keepouts = new ArrayList<>();
        private final List<org.jmouse.cad.pcb.drawing.Lettering> texts = new ArrayList<>();
        private final List<Hole> holes = new ArrayList<>();
        private final List<Shape> shapes = new ArrayList<>();
        private final List<Part> parts = new ArrayList<>();

        void addPcb(PcbDrawing pcb, BoardMotion motion, String prefix) {
            for (NetClass netClass : pcb.netClasses()) {
                netClasses.add(new NetClass(prefix + netClass.name(), netClass.width(), netClass.clearance(), netClass.drill(), netClass.diameter(),
                        netClass.nets().stream().map(net -> prefix + net).toList()));
            }
            pcb.placements().forEach(placement -> placements.add(placed(placement, motion, prefix)));
            pcb.tracks().forEach(track -> tracks.add(track.withPoints(track.points().stream().map(motion::point).toList())));
            pcb.arcs().forEach(arc -> arcs.add(new Arc(arc.layer(), arc.width(), motion.point(arc.from()), motion.point(arc.through()), motion.point(arc.to()), arc.arrangement(), arc.finish())));
            for (Via via : pcb.vias()) {
                Point at = motion.point(new Point(via.x(), via.y()));
                vias.add(new Via(at.x(), at.y(), via.drill(), via.diameter(), via.from(), via.to(), via.open()));
            }
            pcb.zones().forEach(zone -> zones.add(new Zone(zone.layer(), netOf(zone.net(), prefix), zone.clearance(), zone.priority(), motion.path(zone.path()), zone.finish())));
            pcb.keepouts().forEach(keepout -> keepouts.add(new Keepout(keepout.layers(), motion.path(keepout.path()))));
            for (org.jmouse.cad.pcb.drawing.Lettering text : pcb.texts()) {
                Point at = motion.point(new Point(text.x(), text.y()));
                texts.add(new org.jmouse.cad.pcb.drawing.Lettering(text.layer(), text.text(), at.x(), at.y(), text.size(), text.rotation() + motion.rotation(), text.anchor()));
            }
            for (Hole hole : pcb.holes()) {
                Point at = motion.point(new Point(hole.x(), hole.y()));
                holes.add(new Hole(at.x(), at.y(), hole.drill(), hole.ring()));
            }
            pcb.shapes().forEach(shape -> shapes.add(new Shape(shape.layer(), shape.width(), shape.filled(), netOf(shape.net(), prefix), motion.path(shape.path()))));
        }

        void addParts(Map<String, Part> boardParts, PcbFabrication board, String prefix) {
            for (PcbFabrication.SeatedPart seated : board.parts()) {
                Part part = boardParts.get(seated.designator());
                if (part == null) {
                    continue;
                }
                List<Connection> connections = part.connections().stream()
                        .map(connection -> new Connection(connection.pin(), prefix + connection.net())).toList();
                // Its value worked out on its own board: the parameters of its circuit are not the panel's.
                List<Setting> settings = seated.value().isBlank() ? List.of() : List.of(new Setting(Part.VALUE, new Text(seated.value())));
                parts.add(new Part(prefix + part.reference(), part.component(), connections, settings, part.symbol(), part.footprint(), part.pads()));
            }
        }

        /** The teardrops every board asks: they are rules of the sheet, not of one board. */
        List<Teardrop> teardrops(Map<String, PcbDrawing> drawings) {
            return drawings.values().stream().flatMap(pcb -> pcb.teardrops().stream()).distinct().toList();
        }

        private static Placement placed(Placement placement, BoardMotion motion, String prefix) {
            Point at = motion.point(new Point(placement.x(), placement.y()));
            Optional<Reference> reference = placement.reference().map(moved -> {
                Point there = motion.point(new Point(moved.x(), moved.y()));
                return new Reference(there.x(), there.y(), moved.rotation() + motion.rotation(), moved.layer());
            });
            Map<String, String> nets = new LinkedHashMap<>();
            placement.nets().forEach((pad, net) -> nets.put(pad, prefix + net));
            // Each copy prints what the board prints; its name on the panel only tells the copies apart.
            return new Placement(prefix + placement.designator(), at.x(), at.y(), placement.rotation() + motion.rotation(), placement.side(), placement.reference(),
                    Optional.of(placement.printed()), placement.footprint(), nets);
        }

        private static String netOf(String net, String prefix) {
            return net == null || net.isEmpty() ? net : prefix + net;
        }
    }

    /** The reader's catalogue, with the circuit the panel was made with, and the panel's own revision. */
    private record WithCircuit(CatalogueDocuments catalogue, DocumentNode circuit, DocumentNode panel) implements CatalogueDocuments {

        @Override
        public Optional<DocumentNode> find(String slug) {
            return slug.equals(circuit.slug()) ? Optional.of(circuit) : catalogue.find(slug);
        }

        @Override
        public Optional<String> revisionOf(DocumentNode document) {
            return catalogue.revisionOf(panel);
        }
    }

    /** For a test, or a view: the extent the panel comes to. */
    public Envelope extentOf(DocumentNode document, CatalogueDocuments catalogue) {
        Map<String, PcbDrawing> drawings = new LinkedHashMap<>();
        PanelDrawing panel = PanelDrawing.of(document);
        panel.boards().forEach(board -> drawings.computeIfAbsent(board.pcb(), slug -> PcbDrawing.of(catalogue.find(slug).orElseThrow())));
        return PanelLayout.of(panel, drawings, defaults.get()).extent();
    }
}
