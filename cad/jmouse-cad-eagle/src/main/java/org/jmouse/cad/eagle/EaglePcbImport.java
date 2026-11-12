package org.jmouse.cad.eagle;

import org.jmouse.cad.pcb.PcbModule;
import org.jmouse.cad.pcb.drawing.PcbDrawing;
import org.jmouse.cad.document.DocumentNode;
import org.jmouse.cad.document.MetaNode;
import org.jmouse.cad.exchange.imported.FootprintOutlines;
import org.jmouse.cad.exchange.imported.OutlineStretches;
import org.jmouse.cad.exchange.imported.RoundedPath;
import org.jmouse.cad.exchange.imported.SheetTracing;
import org.jmouse.eagle.EagleBoard;
import org.jmouse.eagle.EagleGraphic;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.TreeSet;
import java.util.function.BiFunction;

/**
 * An Eagle pcb as a pcb document of the circuit its schematic became: its outline — the wires and circles of the
 * dimension layer, chained into closed loops —, each element placed where Eagle places it, its copper — every signal's
 * wires as tracks, its vias, its poured polygons as zones of its net. It is built as the pcb's data
 * ({@link PcbDrawing}), which writes its own lines.
 *
 * <p>Eagle draws in millimetres with Y upwards; a pcb is drawn from its top left with Y downwards. A part on the
 * bottom is turned and then mirrored ({@link EagleGeometry#turned}), so it stands turned the other way round on ours.</p>
 */
final class EaglePcbImport {

    private static final int PLACES = FootprintOutlines.PLACES;
    private static final String SUMMARY = "Imported from Eagle: ";

    private final EagleBoard pcb;
    private final EagleSchematicImport.Converted circuit;
    private final EagleLibraries filing;
    private final RoundedPath round = new RoundedPath(PLACES);
    private final List<String> notes = new ArrayList<>();
    /** Our copper layer each copper layer of Eagle's is, by Eagle's number. */
    private final Map<Integer, String> copper = new LinkedHashMap<>();
    private double left;
    private double top;

    private EaglePcbImport(EagleBoard pcb, EagleSchematicImport.Converted circuit, EagleLibraries filing) {
        this.pcb = pcb;
        this.circuit = circuit;
        this.filing = filing;
    }

    /** The pcb's document, and what it could not carry. */
    record Pcb(DocumentNode document, List<String> notes) {
    }

    static Pcb read(EagleBoard pcb, EagleSchematicImport.Converted circuit, String file, String title, EagleLibraries filing) {
        EaglePcbImport reading = new EaglePcbImport(pcb, circuit, filing);
        reading.frame();
        reading.layers();
        PcbDrawing drawing = new PcbDrawing(circuit.circuit(), Optional.empty(), List.of(), PcbDrawing.Sizes.NONE, List.of(),
                List.of(), reading.copper.size(), reading.outline(), reading.placements(), reading.tracks(), reading.arcs(),
                reading.vias(), reading.zones(), List.of(), List.of(), List.of(), List.of(), reading.shapes(), List.of());
        DocumentNode document = new DocumentNode(PcbModule.PCB, title, EagleNames.pcb(title), null, 1);
        document.addExpression(MetaNode.filed(SUMMARY + file, null, filing.category(), filing.circuitTags()));
        drawing.lines().forEach(document::addExpression);
        return new Pcb(document, reading.notes);
    }

    // ─── frame and layers ───────────────────────────────────────────────────────────────────────────

    /** Where the pcb starts: the outline's left and top, or the parts' where it draws none. */
    private void frame() {
        List<double[]> points = new ArrayList<>();
        for (EagleGraphic graphic : outlineGraphics()) {
            switch (graphic) {
                case EagleGraphic.Wire wire -> {
                    points.add(new double[] {wire.x1(), wire.y1()});
                    points.add(new double[] {wire.x2(), wire.y2()});
                }
                case EagleGraphic.Circle circle -> {
                    points.add(new double[] {circle.x() - circle.radius(), circle.y() + circle.radius()});
                    points.add(new double[] {circle.x() + circle.radius(), circle.y() - circle.radius()});
                }
                default -> {
                }
            }
        }
        if (points.isEmpty()) {
            pcb.elements().forEach(element -> points.add(new double[] {element.x(), element.y()}));
            notes.add("The pcb draws no outline on Eagle's dimension layer, so it has none.");
        }
        left = points.stream().mapToDouble(point -> point[0]).min().orElse(0);
        top = points.stream().mapToDouble(point -> point[1]).max().orElse(0);
    }

    /** Eagle's copper layers in use, top first, as ours: {@code top}, {@code inner-1} …, {@code bottom}. */
    private void layers() {
        TreeSet<Integer> used = new TreeSet<>(List.of(EagleBoard.TOP, EagleBoard.BOTTOM));
        pcb.signals().forEach(signal -> {
            signal.wires().forEach(wire -> used.add(wire.layer()));
            signal.polygons().forEach(polygon -> used.add(polygon.layer()));
        });
        used.removeIf(layer -> layer < EagleBoard.TOP || layer > EagleBoard.BOTTOM);
        int inner = 0;
        for (int layer : used) {
            copper.put(layer, layer == EagleBoard.TOP ? PcbDrawing.TOP
                    : layer == EagleBoard.BOTTOM ? PcbDrawing.BOTTOM : PcbDrawing.innerCopper(++inner));
        }
    }

    private List<EagleGraphic> outlineGraphics() {
        return pcb.plain().stream().filter(graphic -> graphic.layer() == EagleBoard.DIMENSION).toList();
    }

    // ─── outline ────────────────────────────────────────────────────────────────────────────────────

    /** The dimension layer's wires chained end to end into closed loops, and its circles, as one path. */
    private String outline() {
        RoundedPath path = new RoundedPath(PLACES);
        List<OutlineStretches.Stretch> stretches = new ArrayList<>();
        for (EagleGraphic graphic : outlineGraphics()) {
            switch (graphic) {
                case EagleGraphic.Wire wire -> stretches.add(stretchOf(wire));
                case EagleGraphic.Circle circle -> {
                    double[] centre = placed(circle.x(), circle.y());
                    path.circle(centre[0], centre[1], circle.radius());
                }
                default -> {
                }
            }
        }
        OutlineStretches.draw(stretches, path, end -> notes.add("The outline does not close at " + end.x() + ", " + end.y()
                + " mm from the pcb's top left: it is drawn as it is."));
        return path.isEmpty() ? null : path.build().toSource();
    }

    private OutlineStretches.Stretch stretchOf(EagleGraphic.Wire wire) {
        Optional<SheetTracing.Point> through = Optional.empty();
        if (wire.curve() != 0) {
            double[] middle = EagleGeometry.arcMiddle(wire.x1(), wire.y1(), wire.x2(), wire.y2(), wire.curve());
            through = Optional.of(traced(middle[0], middle[1]));
        }
        return new OutlineStretches.Stretch(traced(wire.x1(), wire.y1()), traced(wire.x2(), wire.y2()), through);
    }

    private SheetTracing.Point traced(double x, double y) {
        double[] at = placed(x, y);
        return new SheetTracing.Point(at[0], at[1]);
    }

    // ─── parts and copper ───────────────────────────────────────────────────────────────────────────

    /** Each element of a part the circuit places, where Eagle places it; one the circuit does not hold is said. */
    private List<PcbDrawing.Placement> placements() {
        List<PcbDrawing.Placement> placements = new ArrayList<>();
        List<String> unplaced = new ArrayList<>();
        for (EagleBoard.Element element : pcb.elements()) {
            String id = circuit.partIds().get(element.name());
            if (id == null) {
                unplaced.add(element.name());
                continue;
            }
            double[] at = placed(element.x(), element.y());
            boolean bottom = element.rotation().mirrored();
            double rotation = bottom ? (360 - element.rotation().angle()) % 360 : element.rotation().angle();
            placements.add(new PcbDrawing.Placement(id, at[0], at[1], round.rounded(rotation),
                    bottom ? PcbDrawing.BOTTOM : PcbDrawing.TOP, Optional.empty()));
        }
        if (!unplaced.isEmpty()) {
            notes.add(String.join(", ", unplaced) + ": on the pcb, not on the circuit — a frame, a logo, a fiducial — so "
                    + "not placed.");
        }
        return placements;
    }

    /** Every straight wire of a signal on copper, a track of its own. */
    private List<PcbDrawing.Track> tracks() {
        List<PcbDrawing.Track> tracks = new ArrayList<>();
        for (EagleBoard.Signal signal : pcb.signals()) {
            for (EagleGraphic.Wire wire : signal.wires()) {
                String layer = copper.get(wire.layer());
                if (layer != null && wire.curve() == 0) {
                    tracks.add(new PcbDrawing.Track(layer, round.rounded(wire.width()), 0,
                            List.of(point(wire.x1(), wire.y1()), point(wire.x2(), wire.y2()))));
                }
            }
        }
        return tracks;
    }

    /** Every bent wire of a signal on copper, an arc through its middle. */
    private List<PcbDrawing.Arc> arcs() {
        List<PcbDrawing.Arc> arcs = new ArrayList<>();
        for (EagleBoard.Signal signal : pcb.signals()) {
            for (EagleGraphic.Wire wire : signal.wires()) {
                String layer = copper.get(wire.layer());
                if (layer != null && wire.curve() != 0) {
                    double[] middle = EagleGeometry.arcMiddle(wire.x1(), wire.y1(), wire.x2(), wire.y2(), wire.curve());
                    arcs.add(new PcbDrawing.Arc(layer, round.rounded(wire.width()), point(wire.x1(), wire.y1()),
                            point(middle[0], middle[1]), point(wire.x2(), wire.y2())));
                }
            }
        }
        return arcs;
    }

    /** Every via: its ring as Eagle draws it, or as the design rules give it; through the pcb unless it says less. */
    private List<PcbDrawing.Via> vias() {
        List<PcbDrawing.Via> vias = new ArrayList<>();
        for (EagleBoard.Signal signal : pcb.signals()) {
            for (EagleBoard.Via via : signal.vias()) {
                double[] at = placed(via.x(), via.y());
                double diameter = via.diameter() > 0 ? via.diameter() : filing.ringedDiameter(via.drill());
                String[] extent = via.extent() == null ? new String[0] : via.extent().split("-");
                String from = extent.length == 2 ? copper.get(Integer.parseInt(extent[0].strip())) : null;
                String to = extent.length == 2 ? copper.get(Integer.parseInt(extent[1].strip())) : null;
                boolean through = PcbDrawing.TOP.equals(from) && PcbDrawing.BOTTOM.equals(to);
                vias.add(new PcbDrawing.Via(at[0], at[1], round.rounded(via.drill()), round.rounded(diameter),
                        through ? null : from, through ? null : to));
            }
        }
        return vias;
    }

    /** Every polygon a signal pours, a zone of its net on its layer. */
    private List<PcbDrawing.Zone> zones() {
        List<PcbDrawing.Zone> zones = new ArrayList<>();
        for (EagleBoard.Signal signal : pcb.signals()) {
            for (EagleGraphic.Polygon polygon : signal.polygons()) {
                String layer = copper.get(polygon.layer());
                if (layer == null) {
                    continue;
                }
                RoundedPath path = new RoundedPath(PLACES);
                EagleGeometry.polygon(polygon, path, this::placed);
                if (!path.isEmpty()) {
                    zones.add(new PcbDrawing.Zone(layer, circuit.nets().of(signal.name()), 0, 0, path.build().toSource()));
                }
            }
        }
        return zones;
    }

    // ─── geometry ───────────────────────────────────────────────────────────────────────────────────

    /** A point of Eagle's pcb, millimetres with Y upwards, on ours from its top left, Y downwards, to a micrometre. */
    /** What the pcb draws on its layers besides its edge, none of it a signal's: a window in the mask, a logo on the silkscreen, copper of no net. */
    private List<PcbDrawing.Shape> shapes() {
        FootprintOutlines drawn = new FootprintOutlines();
        BiFunction<Double, Double, double[]> place = this::placed;
        for (EagleGraphic graphic : pcb.plain()) {
            Optional<String> layer = layerOfDrawing(graphic.layer());
            if (layer.isEmpty()) {
                continue;
            }
            switch (graphic) {
                case EagleGraphic.Wire wire -> EagleGeometry.wire(wire, drawn.path(layer.get(), wire.width(), false), place);
                case EagleGraphic.Circle circle -> {
                    double[] centre = placed(circle.x(), circle.y());
                    drawn.path(layer.get(), circle.width(), circle.width() == 0).circle(centre[0], centre[1], circle.radius());
                }
                case EagleGraphic.Rectangle rectangle -> EagleGeometry.rectangle(rectangle, drawn.path(layer.get(), 0, true), place);
                case EagleGraphic.Polygon polygon -> EagleGeometry.polygon(polygon, drawn.path(layer.get(), polygon.width(), true), place);
                case EagleGraphic.Text ignored -> {
                }
            }
        }
        return drawn.outlines().stream().map(outline -> new PcbDrawing.Shape(outline.layer(), outline.filled() ? 0 : outline.width(), outline.filled(), "",
                outline.path().toSource())).toList();
    }

    /** Our layer of the pcb one of Eagle's is: its copper by its place in the stack; the rest by what it is for, Eagle numbering a front layer odd and its back's the next. */
    private Optional<String> layerOfDrawing(int layer) {
        if (copper.containsKey(layer)) {
            return Optional.of(copper.get(layer));
        }
        return filing.layerOf(String.valueOf(layer)).filter(purpose -> layer > EagleBoard.BOTTOM)
                .map(purpose -> PcbDrawing.layerOf(layer % 2 == 1 ? PcbDrawing.TOP : PcbDrawing.BOTTOM, purpose));
    }

    private double[] placed(double x, double y) {
        return new double[] {round.rounded(x - left), round.rounded(top - y)};
    }

    private PcbDrawing.Point point(double x, double y) {
        double[] at = placed(x, y);
        return new PcbDrawing.Point(at[0], at[1]);
    }
}
