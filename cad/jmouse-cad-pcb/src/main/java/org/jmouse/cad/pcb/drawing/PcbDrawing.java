package org.jmouse.cad.pcb.drawing;

import org.jmouse.cad.Point;
import org.jmouse.cad.pcb.PcbLayers;
import org.jmouse.cad.document.DocumentNode;
import org.jmouse.cad.drawing.ArrangedDrawing;
import org.jmouse.cad.drawing.Arrangement;
import org.jmouse.cad.drawing.DrawnGroup;
import org.jmouse.cad.drawing.DrawnGroups;
import org.jmouse.cad.drawing.StandingItem;
import org.jmouse.el.grammar.ClauseLineNode;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * A pcb as data an editor works on — the circuit it lays out, how it is made, its copper layers and edge, where each
 * part stands and the copper between them —, in millimetres, Y downwards. It is data; how it is sent is the product's.
 *
 * @param circuit      the slug of the circuit it lays out; empty for a pcb of no circuit
 * @param rules        how it is made — its maker's profile and what it changes of it —, where it says
 * @param netClasses   the classes its nets are routed by: each one's track width, clearance and via
 * @param sizes        the sizes it offers new copper in beside its classes'
 * @param teardrops    where its tracks are widened as they enter copper, and how far
 * @param pairs        its differential pairs: two nets routed side by side at one gap
 * @param copperLayers how many copper layers it has; 0 when it does not say
 * @param outline      its edge, as path data; empty for a pcb with none drawn yet
 * @param placements   where each part stands on it
 * @param tracks       its straight tracks
 * @param arcs         its tracks bent as arcs
 * @param vias         its vias
 * @param zones        its copper pours
 * @param keepouts     the areas where copper is not laid
 * @param dimensions   the measures drawn on it
 * @param texts        the words printed on it
 * @param holes        the holes drilled through it that are no part's
 * @param shapes       what is drawn on its layers that no part brought
 * @param outputs      what it is released as, each a set of formats written together
 * @param groups       the groups its items are chosen and moved with, each with its own state; a group only named by its members
 *                     is a free one
 */
public record PcbDrawing(String circuit, Optional<Rules> rules, List<NetClass> netClasses, Sizes sizes, List<Teardrop> teardrops,
                           List<Pair> pairs, int copperLayers, String outline, List<Placement> placements, List<Track> tracks,
                           List<Arc> arcs, List<Via> vias, List<Zone> zones, List<Keepout> keepouts, List<Dimension> dimensions,
                           List<Lettering> texts, List<Hole> holes, List<Shape> shapes, List<Output> outputs, List<DrawnGroup> groups)
        implements ArrangedDrawing {

    public PcbDrawing {
        netClasses = listed(netClasses);
        sizes = sizes == null ? Sizes.NONE : sizes;
        teardrops = listed(teardrops);
        pairs = listed(pairs);
        placements = listed(placements);
        tracks = listed(tracks);
        arcs = listed(arcs);
        vias = listed(vias);
        zones = listed(zones);
        keepouts = listed(keepouts);
        dimensions = listed(dimensions);
        texts = listed(texts);
        holes = listed(holes);
        shapes = listed(shapes);
        outputs = listed(outputs);
        groups = listed(groups);
    }

    /** Its parts, tracks, arcs and vias as a lock looks at them: each named as a refusal says it, apart from how it stands. */
    @Override
    public List<StandingItem> standingItems() {
        List<StandingItem> items = new ArrayList<>();
        placements.forEach(placement -> items.add(new StandingItem(placement.printed(), placement.arranged(Arrangement.FREE), placement.arrangement())));
        tracks.forEach(track -> items.add(new StandingItem("the track on " + track.layer() + " from " + at(track.points().getFirst()),
                track.arranged(Arrangement.FREE), track.arrangement())));
        arcs.forEach(arc -> items.add(new StandingItem("the arc on " + arc.layer() + " from " + at(arc.from()), arc.arranged(Arrangement.FREE), arc.arrangement())));
        vias.forEach(via -> items.add(new StandingItem("the via at " + at(new Point(via.x(), via.y())), via.arranged(Arrangement.FREE), via.arrangement())));
        return List.copyOf(items);
    }

    /** A point as a refusal writes it: {@code 12.5, 40}. */
    private static String at(Point point) {
        return millimetres(point.x()) + ", " + millimetres(point.y());
    }

    private static String millimetres(double value) {
        return BigDecimal.valueOf(value).stripTrailingZeros().toPlainString();
    }

    /** Its groups, asked whether an item is held where it stands. */
    @Override
    public DrawnGroups drawnGroups() {
        return new DrawnGroups(groups);
    }

    /** The same pcb with other groups. */
    public PcbDrawing withGroups(List<DrawnGroup> others) {
        return new PcbDrawing(circuit, rules, netClasses, sizes, teardrops, pairs, copperLayers, outline, placements, tracks, arcs, vias,
                zones, keepouts, dimensions, texts, holes, shapes, outputs, others);
    }

    /** A list a pcb does not have is left out of what is written: absent is empty. */
    private static <T> List<T> listed(List<T> items) {
        return items == null ? List.of() : List.copyOf(items);
    }

    private static final String SLUG_ENDING = "-pcb";

    /** The slug a circuit's pcb has, beside the circuit: {@code amplifier-pcb}; a bench looks for it there. */
    public static String slugOf(String circuit) {
        return circuit + SLUG_ENDING;
    }

    /** How many copper layers a pcb has that does not say. */
    private static final int USUAL_COPPER_LAYERS = 2;

    /**
     * Its copper layers from the top down, by id — {@code top}, {@code inner-1} …, {@code bottom} —; a pcb of one layer
     * is copper on the bottom alone.
     */
    public List<String> copperLayerIds() {
        return PcbLayers.copperStackOf(copperLayers > 0 ? copperLayers : USUAL_COPPER_LAYERS);
    }


    /** The same pcb with other tracks. */
    public PcbDrawing withTracks(List<Track> others) {
        return new PcbDrawing(circuit, rules, netClasses, sizes, teardrops, pairs, copperLayers, outline, placements, others, arcs, vias,
                zones, keepouts, dimensions, texts, holes, shapes, outputs, groups);
    }

    /** The same pcb with other arcs. */
    public PcbDrawing withArcs(List<Arc> others) {
        return new PcbDrawing(circuit, rules, netClasses, sizes, teardrops, pairs, copperLayers, outline, placements, tracks, others, vias,
                zones, keepouts, dimensions, texts, holes, shapes, outputs, groups);
    }

    /** The same pcb with other vias. */
    public PcbDrawing withVias(List<Via> others) {
        return new PcbDrawing(circuit, rules, netClasses, sizes, teardrops, pairs, copperLayers, outline, placements, tracks, arcs, others,
                zones, keepouts, dimensions, texts, holes, shapes, outputs, groups);
    }

    /** The same pcb with other placements. */
    public PcbDrawing withPlacements(List<Placement> others) {
        return new PcbDrawing(circuit, rules, netClasses, sizes, teardrops, pairs, copperLayers, outline, others, tracks, arcs, vias,
                zones, keepouts, dimensions, texts, holes, shapes, outputs, groups);
    }

    /** The same pcb with one track run through other points. */
    public PcbDrawing withTrackPoints(int index, List<Point> points) {
        List<Track> changed = new ArrayList<>(tracks);
        changed.set(index, tracks.get(index).withPoints(points));
        return withTracks(changed);
    }

    /** Whether a via has copper on a layer: from its first layer to its last, the pcb's top and bottom unless it names them. */
    public boolean reaches(Via via, String layer) {
        List<String> layers = copperLayerIds();
        int from = via.from() == null || via.from().isBlank() ? 0 : layers.indexOf(via.from());
        int to = via.to() == null || via.to().isBlank() ? layers.size() - 1 : layers.indexOf(via.to());
        int depth = layers.indexOf(layer);
        return depth >= 0 && depth >= Math.min(from, to) && depth <= Math.max(from, to);
    }

    /**
     * The pcb as its document's lines, built on the grammars that read them, in the order a pcb is written.
     *
     * @throws IllegalArgumentException when a path does not read as path data
     */
    public List<ClauseLineNode> lines() {
        return PcbDocumentWriting.written(this);
    }

    /** The pcb a pcb document's lines make. */
    public static PcbDrawing of(DocumentNode pcb) {
        return PcbDocumentReading.read(pcb);
    }
}
