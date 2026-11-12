package org.jmouse.cad.libre.pcb;

import org.jmouse.cad.document.DocumentNode;
import org.jmouse.cad.document.MetaNode;
import org.jmouse.cad.exchange.imported.FootprintOutlines;
import org.jmouse.cad.exchange.imported.OutlineStretches;
import org.jmouse.cad.exchange.imported.RoundedPath;
import org.jmouse.cad.exchange.imported.SheetTracing;
import org.jmouse.cad.exchange.imported.UniqueIdentifiers;
import org.jmouse.cad.pcb.PcbModule;
import org.jmouse.cad.pcb.PcbVocabulary;
import org.jmouse.cad.pcb.drawing.PcbDrawing;
import org.jmouse.libre.pcb.LibrePcbAnchor;
import org.jmouse.libre.pcb.LibrePcbBoard;
import org.jmouse.libre.pcb.LibrePcbCircuit;
import org.jmouse.libre.pcb.LibrePcbGraphic;
import org.jmouse.libre.pcb.LibrePcbJunction;
import org.jmouse.libre.pcb.LibrePcbLayer;
import org.jmouse.libre.pcb.LibrePcbLibrary;
import org.jmouse.libre.pcb.LibrePcbOpening;
import org.jmouse.libre.pcb.LibrePcbPlaceholder;
import org.jmouse.libre.pcb.LibrePcbPoint;
import org.jmouse.libre.pcb.LibrePcbProject;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.TreeSet;

/**
 * A board of a project as a jM pcb, laid out from the circuit its sheets were read into. LibrePCB draws in millimetres with Y
 * upwards; a pcb is drawn from its top left with Y downwards. A device stands the same on both: mirrored, turned by the same
 * angle, moved ({@link LibrePcbBoard.DeviceOnBoard}, {@link PcbDrawing.Placement#placed}).
 */
final class LibrePcbBoardImport {

    private static final int PLACES = FootprintOutlines.PLACES;
    private static final String SUMMARY = "Imported from LibrePCB: ";
    private static final String COPPER = "copper";

    private final LibrePcbProject project;
    private final LibrePcbBoard board;
    private final LibrePcbSchematicImport.Converted circuit;
    private final LibrePcbFiling filing;
    private final RoundedPath round = new RoundedPath(PLACES);
    private final List<String> notes = new ArrayList<>();
    private final Map<LibrePcbPlaceholder, String> words;
    private double left;
    private double top;

    /** The pcb's document, and what it could not carry. */
    record Pcb(DocumentNode document, List<String> notes) {
    }

    private LibrePcbBoardImport(LibrePcbProject project, LibrePcbBoard board, LibrePcbSchematicImport.Converted circuit, LibrePcbFiling filing) {
        this.project = project;
        this.board = board;
        this.circuit = circuit;
        this.filing = filing;
        this.words = Map.of(LibrePcbPlaceholder.PROJECT, project.name(), LibrePcbPlaceholder.AUTHOR, project.author(),
                LibrePcbPlaceholder.VERSION, project.version(), LibrePcbPlaceholder.BOARD, board.name());
    }

    static Pcb read(LibrePcbProject project, LibrePcbBoard board, LibrePcbSchematicImport.Converted circuit, String file, String title, LibrePcbFiling filing) {
        LibrePcbBoardImport reading = new LibrePcbBoardImport(project, board, circuit, filing);
        reading.frame();
        List<PcbDrawing.Track> tracks = new ArrayList<>();
        List<PcbDrawing.Via> vias = new ArrayList<>();
        reading.copper(tracks, vias);
        PcbDrawing drawing = new PcbDrawing(circuit.circuit(), reading.rules(), reading.netClasses(), PcbDrawing.Sizes.NONE, List.of(), List.of(),
                2 + board.innerLayers(), reading.outline(), reading.placements(), tracks, List.of(), vias, reading.zones(), List.of(), List.of(),
                reading.texts(), reading.holes(), reading.shapes(), List.of());
        DocumentNode document = new DocumentNode(PcbModule.PCB, title, LibrePcbNames.pcb(title), null, 1);
        document.addExpression(MetaNode.filed(SUMMARY + file, null, filing.category(), filing.circuitTags()));
        drawing.lines().forEach(document::addExpression);
        return new Pcb(document, reading.notes);
    }

    // ─── frame and how it is made ────────────────────────────────────────────────────────────────────────────────────

    /** Where the pcb starts: the edge's left and top, or the parts' where it draws none. */
    private void frame() {
        List<LibrePcbPoint> points = new ArrayList<>();
        for (LibrePcbGraphic graphic : edgeGraphics()) {
            switch (graphic) {
                case LibrePcbGraphic.Polygon polygon -> polygon.vertices().forEach(vertex -> points.add(vertex.at()));
                case LibrePcbGraphic.Circle circle -> {
                    double radius = circle.diameter() / 2;
                    points.add(new LibrePcbPoint(circle.centre().x() - radius, circle.centre().y() + radius));
                }
                case LibrePcbGraphic.Text ignored -> {
                }
            }
        }
        if (points.isEmpty()) {
            board.devices().forEach(device -> points.add(device.transform().at()));
            notes.add("The board draws no outline, so the pcb has none.");
        }
        left = points.stream().mapToDouble(LibrePcbPoint::x).min().orElse(0);
        top = points.stream().mapToDouble(LibrePcbPoint::y).max().orElse(0);
    }

    private Optional<PcbDrawing.Rules> rules() {
        Map<String, Double> changes = new LinkedHashMap<>();
        if (board.rules().traceWidth() > 0) {
            changes.put(PcbDrawing.RULE_TRACK_WIDTH, board.rules().traceWidth());
        }
        if (board.rules().viaDrill() > 0) {
            changes.put(PcbDrawing.RULE_VIA_DRILL, board.rules().viaDrill());
        }
        filing.pcbRuleNames().forEach((ours, theirs) -> Optional.ofNullable(board.rules().checks().get(theirs)).ifPresent(value -> changes.put(ours, value)));
        return changes.isEmpty() ? Optional.empty() : Optional.of(new PcbDrawing.Rules(filing.pcbRules(), changes));
    }

    /** Each class that says more than the board's rules do, with the nets it takes. */
    private List<PcbDrawing.NetClass> netClasses() {
        UniqueIdentifiers identifiers = new UniqueIdentifiers(filing.classOpening(), UniqueIdentifiers.UNLIMITED);
        List<PcbDrawing.NetClass> classes = new ArrayList<>();
        for (LibrePcbCircuit.NetClass netClass : project.circuit().netClasses().values()) {
            if (netClass.traceWidth().isEmpty() && netClass.clearance() <= 0) {
                continue;
            }
            TreeSet<String> nets = new TreeSet<>();
            project.circuit().nets().values().stream().filter(net -> net.netClass().equals(netClass.id())).forEach(net -> nets.add(circuit.nets().of(net.name())));
            if (!nets.isEmpty()) {
                classes.add(new PcbDrawing.NetClass(identifiers.of(netClass.name()), round.rounded(netClass.traceWidth().orElse(board.rules().traceWidth())),
                        round.rounded(netClass.clearance()), 0, 0, List.copyOf(nets)));
            }
        }
        return classes;
    }

    // ─── the edge ────────────────────────────────────────────────────────────────────────────────────────────────────

    private List<LibrePcbGraphic> edgeGraphics() {
        return board.graphics().stream().filter(graphic -> LibrePcbLayer.isEdge(graphic.layer())).toList();
    }

    private String outline() {
        RoundedPath path = new RoundedPath(PLACES);
        List<OutlineStretches.Stretch> stretches = new ArrayList<>();
        for (LibrePcbGraphic graphic : edgeGraphics()) {
            switch (graphic) {
                case LibrePcbGraphic.Polygon polygon -> stretchesOf(polygon.vertices(), stretches);
                case LibrePcbGraphic.Circle circle -> {
                    PcbDrawing.Point centre = point(circle.centre());
                    path.circle(centre.x(), centre.y(), circle.diameter() / 2);
                }
                case LibrePcbGraphic.Text ignored -> {
                }
            }
        }
        OutlineStretches.draw(stretches, path, end -> notes.add("The outline does not close at " + end.x() + ", " + end.y() + " mm from the pcb's top left: it is drawn as it is."));
        return path.isEmpty() ? null : path.build().toSource();
    }

    private void stretchesOf(List<LibrePcbGraphic.Vertex> vertices, List<OutlineStretches.Stretch> stretches) {
        for (int index = 0; index + 1 < vertices.size(); index++) {
            LibrePcbGraphic.Vertex from = vertices.get(index);
            LibrePcbGraphic.Vertex to = vertices.get(index + 1);
            Optional<SheetTracing.Point> through = from.bends() ? Optional.of(traced(from.middleTowards(to))) : Optional.empty();
            stretches.add(new OutlineStretches.Stretch(traced(from.at()), traced(to.at()), through));
        }
    }

    private SheetTracing.Point traced(LibrePcbPoint at) {
        PcbDrawing.Point point = point(at);
        return new SheetTracing.Point(point.x(), point.y());
    }

    // ─── parts and copper ────────────────────────────────────────────────────────────────────────────────────────────

    private List<PcbDrawing.Placement> placements() {
        List<PcbDrawing.Placement> placements = new ArrayList<>();
        List<String> unplaced = new ArrayList<>();
        for (LibrePcbBoard.DeviceOnBoard device : board.devices()) {
            String id = circuit.partIds().get(device.id());
            if (id == null) {
                unplaced.add(device.id());
                continue;
            }
            PcbDrawing.Point at = point(device.transform().at());
            placements.add(new PcbDrawing.Placement(id, at.x(), at.y(), round.rounded(device.transform().rotation()),
                    device.transform().mirrored() ? PcbDrawing.BOTTOM : PcbDrawing.TOP, Optional.of(referenceOf(device, at))));
        }
        if (!unplaced.isEmpty()) {
            notes.add(unplaced.size() + " devices of the board are no part of the circuit, so they are not placed.");
        }
        return placements;
    }

    /**
     * Where a part's name stands and whether it is printed: on a layer the board's silkscreen is made of, or for the assembly
     * alone. LibrePCB draws each device's name where its author left it, or nowhere, and then nothing is printed.
     */
    private PcbDrawing.Reference referenceOf(LibrePcbBoard.DeviceOnBoard device, PcbDrawing.Point at) {
        Optional<LibrePcbGraphic.Text> name = device.texts().stream().filter(text -> LibrePcbPlaceholder.NAME.isAll(text.value())).findFirst();
        if (name.isEmpty()) {
            return new PcbDrawing.Reference(0, 0, 0, PcbVocabulary.FABRICATION);
        }
        PcbDrawing.Point stands = point(name.get().at());
        double dx = stands.x() - at.x();
        double dy = stands.y() - at.y();
        double radians = Math.toRadians(device.transform().rotation());
        // A placed point turned back: what {@link PcbDrawing.Placement#placed} turned, undone.
        double along = dx * Math.cos(radians) - dy * Math.sin(radians);
        double across = dx * Math.sin(radians) + dy * Math.cos(radians);
        return new PcbDrawing.Reference(round.rounded(device.transform().mirrored() ? -along : along), round.rounded(across),
                round.rounded(((name.get().rotation() - device.transform().rotation()) % 360 + 360) % 360),
                board.printedLayers().contains(name.get().layer()) ? PcbVocabulary.SILKSCREEN : PcbVocabulary.FABRICATION);
    }

    /** Every trace of a segment, a track from one of its ends to the other; every via, open as it or the rules say. */
    private void copper(List<PcbDrawing.Track> tracks, List<PcbDrawing.Via> vias) {
        Map<String, LibrePcbBoard.DeviceOnBoard> devices = new HashMap<>();
        board.devices().forEach(device -> devices.put(device.id(), device));
        for (LibrePcbBoard.Segment segment : board.segments()) {
            for (LibrePcbBoard.Via via : segment.vias()) {
                PcbDrawing.Point at = point(via.at());
                boolean through = LibrePcbLayer.TOP_COPPER.equals(via.from()) && LibrePcbLayer.BOTTOM_COPPER.equals(via.to());
                vias.add(new PcbDrawing.Via(at.x(), at.y(), round.rounded(via.drill()), round.rounded(via.size()),
                        through ? null : copperOf(via.from()).orElse(null), through ? null : copperOf(via.to()).orElse(null), openOf(via)));
            }
            for (LibrePcbBoard.Trace trace : segment.traces()) {
                Optional<String> layer = copperOf(trace.layer());
                if (layer.isPresent()) {
                    tracks.add(new PcbDrawing.Track(layer.get(), round.rounded(trace.width()), 0,
                            List.of(point(where(trace.from(), segment, devices)), point(where(trace.to(), segment, devices)))));
                }
            }
        }
    }

    private String openOf(LibrePcbBoard.Via via) {
        boolean open = switch (via.exposure()) {
            case LibrePcbOpening.Off ignored -> false;
            case LibrePcbOpening.Clearance ignored -> true;
            case LibrePcbOpening.Automatic ignored -> via.drill() > board.rules().largestTented();
        };
        return open ? PcbVocabulary.VIA_OPEN_BOTH : "";
    }

    private LibrePcbPoint where(LibrePcbAnchor anchor, LibrePcbBoard.Segment segment, Map<String, LibrePcbBoard.DeviceOnBoard> devices) {
        return switch (anchor) {
            case LibrePcbAnchor.OnJunction junction -> segment.junctions().stream().filter(candidate -> candidate.id().equals(junction.junction()))
                    .findFirst().map(LibrePcbJunction::at).orElseThrow(() -> new IllegalArgumentException("A trace ends on a junction its segment does not have."));
            case LibrePcbAnchor.OnVia via -> segment.vias().stream().filter(candidate -> candidate.id().equals(via.via()))
                    .findFirst().map(LibrePcbBoard.Via::at).orElseThrow(() -> new IllegalArgumentException("A trace ends on a via its segment does not have."));
            case LibrePcbAnchor.OnPad pad -> padPoint(pad, devices);
            case LibrePcbAnchor.OnPin ignored -> throw new IllegalArgumentException("A trace of a board ends on a pin of a sheet.");
        };
    }

    private LibrePcbPoint padPoint(LibrePcbAnchor.OnPad anchor, Map<String, LibrePcbBoard.DeviceOnBoard> devices) {
        LibrePcbBoard.DeviceOnBoard device = devices.get(anchor.device());
        LibrePcbLibrary.Device kind = device == null ? null : project.library().devices().get(device.device());
        LibrePcbLibrary.Package found = kind == null ? null : project.library().packages().get(kind.packageId());
        if (found == null) {
            throw new IllegalArgumentException("A trace ends on a device the board does not have.");
        }
        return found.footprint(device.footprint()).flatMap(footprint -> footprint.pad(anchor.pad()))
                .map(pad -> device.transform().placed(pad.at()))
                .orElseThrow(() -> new IllegalArgumentException("A trace ends on a pad its device's footprint does not have."));
    }

    private List<PcbDrawing.Zone> zones() {
        List<PcbDrawing.Zone> zones = new ArrayList<>();
        for (LibrePcbBoard.Plane plane : board.planes()) {
            Optional<String> layer = copperOf(plane.layer());
            LibrePcbCircuit.Net net = project.circuit().nets().get(plane.net());
            if (layer.isEmpty() || net == null) {
                notes.add("A plane on " + plane.layer() + " is on no net or no copper this reads, so it is left out.");
                continue;
            }
            List<LibrePcbGraphic.Vertex> closed = new ArrayList<>(plane.vertices());
            if (!closed.isEmpty() && !closed.getFirst().at().equals(closed.getLast().at())) {
                closed.add(closed.getFirst());
            }
            zones.add(new PcbDrawing.Zone(layer.get(), circuit.nets().of(net.name()), round.rounded(plane.clearance()), plane.priority(), pathOf(closed)));
        }
        return zones;
    }

    // ─── everything else it draws ────────────────────────────────────────────────────────────────────────────────────

    private List<PcbDrawing.Shape> shapes() {
        List<PcbDrawing.Shape> shapes = new ArrayList<>();
        int elsewhere = 0;
        for (LibrePcbGraphic graphic : board.graphics()) {
            if (LibrePcbLayer.isEdge(graphic.layer()) || graphic instanceof LibrePcbGraphic.Text) {
                continue;
            }
            Optional<String> layer = pcbLayerOf(graphic.layer());
            if (layer.isEmpty()) {
                elsewhere++;
                continue;
            }
            switch (graphic) {
                case LibrePcbGraphic.Polygon polygon -> shapes.add(new PcbDrawing.Shape(layer.get(), polygon.filled() ? 0 : round.rounded(polygon.width()), polygon.filled(), "", pathOf(polygon.vertices())));
                case LibrePcbGraphic.Circle circle -> {
                    RoundedPath path = new RoundedPath(PLACES);
                    PcbDrawing.Point centre = point(circle.centre());
                    path.circle(centre.x(), centre.y(), circle.diameter() / 2);
                    shapes.add(new PcbDrawing.Shape(layer.get(), circle.filled() ? 0 : round.rounded(circle.width()), circle.filled(), "", path.build().toSource()));
                }
                case LibrePcbGraphic.Text ignored -> {
                }
            }
        }
        if (elsewhere > 0) {
            notes.add(elsewhere + " graphics are on layers we do not read, so they are left out.");
        }
        return shapes;
    }

    private List<PcbDrawing.Text> texts() {
        List<PcbDrawing.Text> texts = new ArrayList<>();
        int elsewhere = 0;
        for (LibrePcbGraphic graphic : board.graphics()) {
            if (graphic instanceof LibrePcbGraphic.Text text) {
                Optional<String> layer = pcbLayerOf(text.layer());
                if (layer.isEmpty()) {
                    elsewhere++;
                    continue;
                }
                lettered(text, layer.get(), texts);
            }
        }
        board.devices().forEach(device -> devicesWords(device, texts));
        if (elsewhere > 0) {
            notes.add(elsewhere + " texts are on layers we do not read, so they are left out.");
        }
        return texts;
    }

    /**
     * What a device's own words print besides its name, which its placement says: a logo's value on the silkscreen. They
     * stand where the board says, what they stand for written in.
     */
    private void devicesWords(LibrePcbBoard.DeviceOnBoard device, List<PcbDrawing.Text> texts) {
        LibrePcbCircuit.Part part = project.circuit().parts().get(device.id());
        if (part == null) {
            return;
        }
        Map<LibrePcbPlaceholder, String> own = new EnumMap<>(LibrePcbPlaceholder.class);
        own.putAll(words);
        own.put(LibrePcbPlaceholder.NAME, part.name());
        own.put(LibrePcbPlaceholder.VALUE, part.value());
        for (LibrePcbGraphic.Text text : device.texts()) {
            if (!LibrePcbPlaceholder.NAME.isAll(text.value()) && board.printedLayers().contains(text.layer())) {
                String side = LibrePcbLayer.isBottom(text.layer()) ? PcbDrawing.BOTTOM : PcbDrawing.TOP;
                lettered(text.filled(own), PcbDrawing.layerOf(side, PcbVocabulary.SILKSCREEN), texts);
            }
        }
    }

    /**
     * Words of several lines are as many texts, one under another, and a text stands middle on its point: each is moved down
     * its own line from where the format aligns the whole, turned with it.
     */
    private void lettered(LibrePcbGraphic.Text text, String layer, List<PcbDrawing.Text> texts) {
        List<String> lines = text.lines();
        double pitch = text.height() * filing.lineSpacing();
        double first = switch (text.across()) {
            case TOP -> text.height() / 2;
            case BOTTOM -> -text.height() / 2 - pitch * (lines.size() - 1);
            case MIDDLE -> -pitch * (lines.size() - 1) / 2;
        };
        double radians = Math.toRadians(text.rotation());
        for (int index = 0; index < lines.size(); index++) {
            double down = first + pitch * index;
            PcbDrawing.Point at = point(new LibrePcbPoint(text.at().x() + down * Math.sin(radians), text.at().y() - down * Math.cos(radians)));
            texts.add(new PcbDrawing.Text(layer, LibrePcbPlaceholder.filled(lines.get(index), words), at.x(), at.y(), round.rounded(text.height()),
                    round.rounded(text.rotation()), anchorOf(text)));
        }
    }

    private static String anchorOf(LibrePcbGraphic.Text text) {
        return switch (text.alignment()) {
            case LEFT -> PcbDrawing.START;
            case CENTRE -> PcbDrawing.MIDDLE;
            case RIGHT -> PcbDrawing.END;
        };
    }

    private List<PcbDrawing.Hole> holes() {
        List<PcbDrawing.Hole> holes = new ArrayList<>();
        for (LibrePcbLibrary.Hole hole : board.holes()) {
            PcbDrawing.Point at = point(hole.vertices().getFirst().at());
            holes.add(new PcbDrawing.Hole(at.x(), at.y(), round.rounded(hole.diameter()), 0));
            if (hole.vertices().size() > 1) {
                notes.add("A slot of the board is drilled as a round hole where it starts: a pcb's own holes are round.");
            }
        }
        return holes;
    }

    // ─── layers and points ───────────────────────────────────────────────────────────────────────────────────────────

    /** Our copper layer a LibrePCB one is. */
    private Optional<String> copperOf(String layer) {
        if (LibrePcbLayer.TOP_COPPER.equals(layer)) {
            return Optional.of(PcbDrawing.TOP);
        }
        if (LibrePcbLayer.BOTTOM_COPPER.equals(layer)) {
            return Optional.of(PcbDrawing.BOTTOM);
        }
        return LibrePcbLayer.innerCopper(layer).map(PcbDrawing::innerCopper);
    }

    /** Our layer a LibrePCB one is: copper by its place in the stack, the rest by what it is for and its side. */
    private Optional<String> pcbLayerOf(String layer) {
        Optional<String> copper = copperOf(layer);
        if (copper.isPresent()) {
            return copper;
        }
        return filing.layerOf(layer).filter(purpose -> !COPPER.equals(purpose)).map(purpose -> LibrePcbLayer.isTop(layer) ? PcbDrawing.layerOf(PcbDrawing.TOP, purpose)
                : LibrePcbLayer.isBottom(layer) ? PcbDrawing.layerOf(PcbDrawing.BOTTOM, purpose) : purpose);
    }

    private String pathOf(List<LibrePcbGraphic.Vertex> vertices) {
        RoundedPath path = new RoundedPath(PLACES);
        LibrePcbPaths.corners(vertices, path, at -> {
            PcbDrawing.Point point = point(at);
            return new double[] {point.x(), point.y()};
        });
        return path.build().toSource();
    }

    /** A point of the board, Y upwards, on the pcb: from its top left, Y downwards. */
    private PcbDrawing.Point point(LibrePcbPoint at) {
        return new PcbDrawing.Point(round.rounded(at.x() - left), round.rounded(top - at.y()));
    }
}
