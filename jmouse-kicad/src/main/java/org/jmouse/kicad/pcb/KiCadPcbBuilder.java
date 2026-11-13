package org.jmouse.kicad.pcb;

import static org.jmouse.kicad.KiCadKeyword.*;

import org.jmouse.kicad.KiCadKeyword;
import org.jmouse.kicad.KiCadLayer;
import org.jmouse.kicad.KiCadPoint;
import org.jmouse.kicad.footprint.KiCadFootprint;
import org.jmouse.kicad.footprint.KiCadFootprintGraphic;
import org.jmouse.kicad.footprint.KiCadModel;
import org.jmouse.kicad.footprint.KiCadPad;
import org.jmouse.sexpression.SExpressionDocument;
import org.jmouse.sexpression.SExpressionPrinter;
import org.jmouse.sexpression.node.SExpressionAtom;
import org.jmouse.sexpression.node.SExpressionList;
import org.jmouse.sexpression.node.SExpressionNode;
import org.jmouse.sexpression.node.SExpressionString;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.UnaryOperator;

/**
 * A board built as KiCad's tree and printed as a {@code .kicad_pcb} KiCad 8 and newer open — what {@link KiCadPcb} reads
 * back. Its layers, its nets, each footprint as a library holds it stood where it stands (on the back mirrored and on the
 * back's layers, as KiCad keeps it), its copper, its pours and keepouts, its words, measures and drawings are added in the
 * board's own frame — millimetres, Y downwards —, and the whole is set in the middle of the smallest
 * page that holds it. Every identity is a UUID made from the board's seed and what it identifies, so the same board is
 * printed the same way twice.
 */
public final class KiCadPcbBuilder {

    /** The two fields KiCad gives every footprint, written from the part itself: its designator and its value. */
    private static final String REFERENCE_FIELD = "Reference";
    private static final String VALUE_FIELD = "Value";

    /** KiCad 8's format: the first that names every identity {@code uuid} and a footprint's fields as properties. */
    public static final long VERSION = 20240108L;

    /** The pages KiCad offers, smallest first, landscape, in millimetres. */
    private static final List<Page> PAGES = List.of(new Page("A4", 297, 210), new Page("A3", 420, 297),
            new Page("A2", 594, 420), new Page("A1", 841, 594), new Page("A0", 1189, 841));

    /** Kept clear between the board and the page's edge. */
    private static final double MARGIN = 20;

    /** The order KiCad numbers its copper layers by: the front 0, inner ones from 1, the back 31. */
    private static final int BACK_COPPER_ORDINAL = 31;
    private static final String INNER_COPPER = "In%d.Cu";

    /** Every layer besides the copper a KiCad board declares, with the number and the name its own boards give it. */
    private static final List<TechnicalLayer> TECHNICAL_LAYERS = List.of(
            new TechnicalLayer(32, "B.Adhes", "B.Adhesive"), new TechnicalLayer(33, "F.Adhes", "F.Adhesive"),
            new TechnicalLayer(34, "B.Paste", null), new TechnicalLayer(35, "F.Paste", null),
            new TechnicalLayer(36, "B.SilkS", "B.Silkscreen"), new TechnicalLayer(37, "F.SilkS", "F.Silkscreen"),
            new TechnicalLayer(38, "B.Mask", null), new TechnicalLayer(39, "F.Mask", null),
            new TechnicalLayer(40, "Dwgs.User", "User.Drawings"), new TechnicalLayer(41, "Cmts.User", "User.Comments"),
            new TechnicalLayer(42, "Eco1.User", "User.Eco1"), new TechnicalLayer(43, "Eco2.User", "User.Eco2"),
            new TechnicalLayer(44, KiCadLayer.EDGE, null), new TechnicalLayer(45, "Margin", null),
            new TechnicalLayer(46, "B.CrtYd", "B.Courtyard"), new TechnicalLayer(47, "F.CrtYd", "F.Courtyard"),
            new TechnicalLayer(48, "B.Fab", null), new TechnicalLayer(49, "F.Fab", null),
            new TechnicalLayer(50, "User.1", null), new TechnicalLayer(51, "User.2", null), new TechnicalLayer(52, "User.3", null),
            new TechnicalLayer(53, "User.4", null), new TechnicalLayer(54, "User.5", null), new TechnicalLayer(55, "User.6", null),
            new TechnicalLayer(56, "User.7", null), new TechnicalLayer(57, "User.8", null), new TechnicalLayer(58, "User.9", null));

    /** How KiCad's own measures are drawn, as it makes a new one: its arrows, its extension lines, its figures. */
    private static final double DIMENSION_ARROW = 1.27;
    private static final double DIMENSION_EXTENSION_HEIGHT = 0.58642;
    private static final double DIMENSION_EXTENSION_OFFSET = 0.5;
    private static final double DIMENSION_TEXT = 1;
    private static final int DIMENSION_MILLIMETRES = 2;
    private static final int DIMENSION_PRECISION = 4;

    /** How a pour's outline is hatched on the screen, as KiCad draws a new one: no figure of the board. */
    private static final double ZONE_HATCH = 0.5;

    private record Page(String name, double width, double height) {
    }

    /** How far the board reaches in its own frame: its left, its top, its right, its bottom, in millimetres. */
    public record Extent(double left, double top, double right, double bottom) {

        double width() {
            return right - left;
        }

        double height() {
            return bottom - top;
        }
    }

    private record TechnicalLayer(int ordinal, String name, String userName) {
    }

    /** What an area of the board is: copper poured on a net, or a rule that keeps copper out. */
    public enum AreaKind {
        POUR, KEEPOUT
    }

    /**
     * An area: copper poured on a net across its layers, or a keepout no copper enters.
     *
     * @param net          the net a pour is on; empty for a keepout
     * @param clearance    how far it stays from copper of other nets
     * @param leastWidth   the narrowest copper it leaves when poured
     * @param thermalGap   how far a pad of its own net stands from it, joined across by spokes
     * @param thermalSpoke how wide those spokes are
     */
    public record Area(AreaKind kind, String net, List<String> layers, double clearance, int priority, double leastWidth,
                       double thermalGap, double thermalSpoke, List<KiCadPoint> outline) {

        public Area {
            layers = List.copyOf(layers);
            outline = List.copyOf(outline);
        }
    }

    /**
     * A footprint stood on the board.
     *
     * @param libraryId the {@code LIBRARY:NAME} KiCad knows it by
     * @param footprint the footprint as a library holds it: as it stands on the front, unturned
     * @param path      the symbol it stands for, {@code /symbol-uuid}; empty for one only the board has
     * @param angle     degrees it is turned, counter-clockwise as seen from the front
     * @param reference where its reference is printed, in the footprint's own frame and turned relative to it, and on
     *                  which of the footprint's own layers — {@code F.SilkS}, {@code F.Fab}
     * @param value     the same for its value
     * @param padNets   the net each of its pads is on, by pad number; a pad on none is not named
     * @param boardOnly whether it is only the board's — a mounting hole, a logo —: no part of the schematic
     */
    public record PlacedFootprint(String libraryId, KiCadFootprint footprint, Optional<String> path, String designator,
                                  String valueText, KiCadSide side, KiCadPoint at, double angle, Field reference, Field value,
                                  Map<String, String> padNets, boolean boardOnly, KiCadArrangement arrangement) {

        public PlacedFootprint {
            padNets = Map.copyOf(padNets);
            arrangement = arrangement == null ? KiCadArrangement.FREE : arrangement;
        }
    }

    /**
     * Where a footprint's text stands, in its own frame; turned relative to it; how tall; whether it is shown; which part of
     * its letters stands on its point along them.
     */
    public record Field(KiCadPoint at, double angle, String layer, double height, boolean hidden, KiCadPcb.Along along) {

        /** A text centred on its point. */
        public Field(KiCadPoint at, double angle, String layer, double height, boolean hidden) {
            this(at, angle, layer, height, hidden, KiCadPcb.Along.CENTRE);
        }
    }

    /** How words stand on their point: along their line, and across it. */
    public record Justification(KiCadPcb.Along along, KiCadPcb.Across across) {

        public static final Justification CENTRED = new Justification(KiCadPcb.Along.CENTRE, KiCadPcb.Across.MIDDLE);
    }

    private final String seed;
    private final Page page;
    private final Extent extent;
    private final double left;
    private final double top;
    private final double maskClearance;
    private final List<String> copper;
    private final Map<String, Integer> nets = new LinkedHashMap<>();
    private final List<SExpressionNode> drawn = new ArrayList<>();
    private final List<SExpressionNode> footprints = new ArrayList<>();
    private final List<SExpressionNode> copperItems = new ArrayList<>();
    private final List<SExpressionNode> areas = new ArrayList<>();
    /** The board's groups in the order they were told of: each its lock and the identities of its members so far. */
    private final Map<String, GroupBeingWritten> groups = new LinkedHashMap<>();

    /** A group as the builder gathers it: whether it is locked, and the identities of what was put in it. */
    private record GroupBeingWritten(KiCadLock lock, List<String> members) {
    }
    private final double thickness;
    private final double letterStroke;
    private final String title;
    private int identities;

    /**
     * @param seed          what makes this board's identities its own: the same seed, the same UUIDs
     * @param extent        how far the board reaches in its own frame: the page is chosen to hold it, and it stands in its middle
     * @param copper        its copper layers from the front, as KiCad names them ({@link #copperLayers})
     * @param thickness     how thick the board is, in millimetres
     * @param letterStroke  how thick a letter's stroke is, as a share of its height
     * @param maskClearance how far the solder mask opens past a pad all round
     */
    private KiCadPcbBuilder(String seed, String title, Extent extent, List<String> copper, double thickness, double letterStroke,
                            double maskClearance) {
        this.seed = seed;
        this.extent = extent;
        this.letterStroke = letterStroke;
        this.maskClearance = maskClearance;
        double width = extent.width();
        double height = extent.height();
        this.title = title;
        this.copper = List.copyOf(copper);
        this.thickness = thickness;
        this.page = PAGES.stream().filter(candidate -> width + 2 * MARGIN <= candidate.width()
                && height + 2 * MARGIN <= candidate.height()).findFirst().orElse(PAGES.getLast());
        this.left = Math.max(MARGIN, (page.width() - width) / 2);
        this.top = Math.max(MARGIN, (page.height() - height) / 2);
        nets.put("", 0);
    }

    public static KiCadPcbBuilder of(String seed, String title, Extent extent, int copperLayers, double thickness, double letterStroke,
                                     double maskClearance) {
        return new KiCadPcbBuilder(seed, title, extent, copperLayers(copperLayers), thickness, letterStroke, maskClearance);
    }

    /** A board's copper layers from the front, as KiCad names them: {@code F.Cu}, {@code In1.Cu} …, {@code B.Cu}. */
    public static List<String> copperLayers(int count) {
        if (count <= 1) {
            return List.of(KiCadSide.BACK.copper());
        }
        List<String> layers = new ArrayList<>();
        layers.add(KiCadSide.FRONT.copper());
        for (int inner = 1; inner <= count - 2; inner++) {
            layers.add(INNER_COPPER.formatted(inner));
        }
        layers.add(KiCadSide.BACK.copper());
        return List.copyOf(layers);
    }

    /** The number a net is written under, given in the order nets are first named; the empty net is 0. */
    public int net(String name) {
        return nets.computeIfAbsent(name, ignored -> nets.size());
    }

    // ─── footprints ─────────────────────────────────────────────────────────────────────────────────

    /**
     * A footprint where it stands, written as KiCad keeps it on a board: on the back its pads and lines mirrored about X and
     * on the back's layers, each pad's angle the angle it has on the board — what {@link KiCadPcbFootprint} undoes.
     */
    public KiCadPcbBuilder footprint(PlacedFootprint placed) {
        KiCadSide side = placed.side();
        KiCadFootprint footprint = placed.footprint();
        String identity = identity("footprint " + placed.designator());
        List<SExpressionNode> items = new ArrayList<>();
        items.add(SExpressionString.of(placed.libraryId()));
        items.add(SExpressionList.of(LAYER, SExpressionString.of(side.copper())));
        items.add(uuid(identity));
        arranged(items, placed.arrangement(), identity);
        items.add(at(onPage(placed.at()), placed.angle()));
        if (!footprint.description().isBlank()) {
            items.add(SExpressionList.of(DESCRIPTION, SExpressionString.of(footprint.description())));
        }
        if (!footprint.tags().isEmpty()) {
            items.add(SExpressionList.of(TAGS, SExpressionString.of(String.join(" ", footprint.tags()))));
        }
        items.add(field(REFERENCE_FIELD, placed.designator(), placed.reference(), side, placed.angle(), identity));
        items.add(field(VALUE_FIELD, placed.valueText(), placed.value(), side, placed.angle(), identity));
        // Every other field it keeps — whose it is, its licence, its source —, hidden where its value stands, as KiCad keeps a
        // footprint's Datasheet and Description.
        Field kept = new Field(placed.value().at(), placed.value().angle(), placed.value().layer(), placed.value().height(), true);
        footprint.properties().forEach((name, text) -> {
            if (!REFERENCE_FIELD.equals(name) && !VALUE_FIELD.equals(name)) {
                items.add(field(name, text, kept, side, placed.angle(), identity));
            }
        });
        placed.path().ifPresent(path -> items.add(SExpressionList.of(PATH, SExpressionString.of(path))));
        attributes(placed).ifPresent(items::add);
        UnaryOperator<KiCadPoint> written = side::unmirrored;
        for (KiCadFootprintGraphic graphic : footprint.graphics()) {
            items.add(onSide(graphic, side, written).written(false, identity(identity + " graphic")));
        }
        for (KiCadPad pad : footprint.pads()) {
            String net = placed.padNets().getOrDefault(pad.number(), KiCadPad.NO_NET);
            items.add(onSide(pad, side, placed.angle()).written(net.isEmpty() ? 0 : net(net), net,
                    identity(identity + " pad " + pad.number())));
        }
        footprint.models().stream().map(KiCadModel::written).forEach(items::add);
        footprints.add(SExpressionList.of(FOOTPRINT, items.toArray(SExpressionNode[]::new)));
        return this;
    }

    /** What a footprint is to KiCad's files of positions and its bill: how it is mounted, and whether it is the board's alone. */
    private static Optional<SExpressionList> attributes(PlacedFootprint placed) {
        List<SExpressionNode> words = new ArrayList<>();
        placed.footprint().mounting().ifPresent(mounting -> words.add(SExpressionAtom.of(mounting)));
        if (placed.boardOnly()) {
            words.add(SExpressionAtom.of("board_only"));
            words.add(SExpressionAtom.of("exclude_from_pos_files"));
            words.add(SExpressionAtom.of("exclude_from_bom"));
        }
        return words.isEmpty() ? Optional.empty() : Optional.of(SExpressionList.of("attr", words.toArray(SExpressionNode[]::new)));
    }

    /** A footprint's field as KiCad 8 writes it: a property, where it stands turned with the footprint, on its side's layer. */
    private SExpressionList field(String name, String text, Field field, KiCadSide side, double angle, String footprint) {
        KiCadPoint at = side.unmirrored(field.at());
        List<SExpressionNode> items = new ArrayList<>();
        items.add(SExpressionString.of(name));
        items.add(SExpressionString.of(text));
        items.add(SExpressionList.of(AT, SExpressionAtom.of(at.x()), SExpressionAtom.of(at.y()), SExpressionAtom.of(normalized(field.angle() + angle))));
        items.add(SExpressionList.of(LAYER, SExpressionString.of(side.libraryLayer(field.layer()))));
        if (field.hidden()) {
            items.add(SExpressionList.of(HIDE, SExpressionAtom.of("yes")));
        }
        items.add(uuid(identity(footprint + " " + name)));
        items.add(effects(field.height(), new Justification(field.along(), KiCadPcb.Across.MIDDLE), side == KiCadSide.BACK));
        return SExpressionList.of(PROPERTY, items.toArray(SExpressionNode[]::new));
    }

    /** A footprint's graphic as it stands on its side: on the back mirrored about X and on the back's layer. */
    private static KiCadFootprintGraphic onSide(KiCadFootprintGraphic graphic, KiCadSide side, UnaryOperator<KiCadPoint> written) {
        return graphic.moved(written, side.libraryLayer(graphic.layer()));
    }

    /** A pad as it stands on the board: its angle the footprint's added, on the back its own negated first and mirrored. */
    private static KiCadPad onSide(KiCadPad pad, KiCadSide side, double angle) {
        double turned = side == KiCadSide.BACK ? angle - pad.angle() : pad.angle() + angle;
        return new KiCadPad(pad.number(), pad.type(), pad.shape(), side.unmirrored(pad.at()), normalized(turned), pad.width(),
                pad.height(), pad.drill(), pad.roundness(), pad.layers().stream().map(side::libraryLayer).toList(), pad.maskMargin(),
                pad.pasteMargin(), pad.primitives().stream().map(primitive -> onSide(primitive, side, side::unmirrored)).toList());
    }

    // ─── copper ─────────────────────────────────────────────────────────────────────────────────────

    /**
     * A group of the board — {@code (group "front panel" (uuid …) (locked yes) (members …))} —, to put items in by its name; written
     * after them, naming every item that was put in it.
     */
    public KiCadPcbBuilder group(String name, KiCadLock lock) {
        groups.putIfAbsent(name, new GroupBeingWritten(lock, new ArrayList<>()));
        return this;
    }

    /** A straight run of copper; one of no length is copper all the same — a dot as wide as the track. */
    public KiCadPcbBuilder track(KiCadPoint start, KiCadPoint end, double width, String layer, String net) {
        return track(start, end, width, layer, net, KiCadArrangement.FREE);
    }

    /** The same, in a group or locked. */
    public KiCadPcbBuilder track(KiCadPoint start, KiCadPoint end, double width, String layer, String net, KiCadArrangement arrangement) {
        String identity = identity("track");
        List<SExpressionNode> items = new ArrayList<>(List.of(point(START, onPage(start)), point(END, onPage(end)),
                SExpressionList.of(WIDTH, SExpressionAtom.of(width)), SExpressionList.of(LAYER, SExpressionString.of(layer)),
                SExpressionList.of(NET, SExpressionAtom.of(net(net))), uuid(identity)));
        arranged(items, arrangement, identity);
        copperItems.add(SExpressionList.of(SEGMENT, items.toArray(SExpressionNode[]::new)));
        return this;
    }

    public KiCadPcbBuilder arc(KiCadPoint start, KiCadPoint middle, KiCadPoint end, double width, String layer, String net) {
        return arc(start, middle, end, width, layer, net, KiCadArrangement.FREE);
    }

    /** The same, in a group or locked. */
    public KiCadPcbBuilder arc(KiCadPoint start, KiCadPoint middle, KiCadPoint end, double width, String layer, String net, KiCadArrangement arrangement) {
        String identity = identity("arc");
        List<SExpressionNode> items = new ArrayList<>(List.of(point(START, onPage(start)), point(MIDDLE, onPage(middle)), point(END, onPage(end)),
                SExpressionList.of(WIDTH, SExpressionAtom.of(width)), SExpressionList.of(LAYER, SExpressionString.of(layer)),
                SExpressionList.of(NET, SExpressionAtom.of(net(net))), uuid(identity)));
        arranged(items, arrangement, identity);
        copperItems.add(SExpressionList.of(TRACK_ARC, items.toArray(SExpressionNode[]::new)));
        return this;
    }

    /** Says how an item stands: `(locked yes)` when it is locked, and puts it among its group's members. */
    private void arranged(List<SExpressionNode> items, KiCadArrangement arrangement, String identity) {
        if (arrangement.lock() == KiCadLock.LOCKED) {
            items.add(SExpressionList.of(LOCKED, SExpressionAtom.of("yes")));
        }
        if (!arrangement.group().isEmpty()) {
            GroupBeingWritten group = groups.get(arrangement.group());
            if (group == null) {
                throw new IllegalArgumentException("No group '" + arrangement.group() + "' was told of before what is put in it");
            }
            group.members().add(identity);
        }
    }

    /**
     * A via from one copper layer to another: through the board when they are its front and its back, blind when one of
     * them is, buried otherwise.
     */
    public KiCadPcbBuilder via(KiCadPoint at, double diameter, double drill, String from, String to, String net) {
        return via(at, diameter, drill, from, to, net, KiCadArrangement.FREE);
    }

    /** The same, in a group or locked. */
    public KiCadPcbBuilder via(KiCadPoint at, double diameter, double drill, String from, String to, String net, KiCadArrangement arrangement) {
        List<SExpressionNode> items = new ArrayList<>();
        boolean outerFrom = from.equals(copper.getFirst()) || from.equals(copper.getLast());
        boolean outerTo = to.equals(copper.getFirst()) || to.equals(copper.getLast());
        if (outerFrom != outerTo) {
            items.add(SExpressionAtom.of("blind"));
        } else if (!outerFrom) {
            items.add(SExpressionAtom.of("buried"));
        }
        KiCadPoint placed = onPage(at);
        items.add(SExpressionList.of(AT, SExpressionAtom.of(placed.x()), SExpressionAtom.of(placed.y())));
        items.add(SExpressionList.of(SIZE, SExpressionAtom.of(diameter)));
        items.add(SExpressionList.of(DRILL, SExpressionAtom.of(drill)));
        items.add(SExpressionList.of(LAYERS, SExpressionString.of(from), SExpressionString.of(to)));
        items.add(SExpressionList.of(NET, SExpressionAtom.of(net(net))));
        String identity = identity("via");
        items.add(uuid(identity));
        arranged(items, arrangement, identity);
        copperItems.add(SExpressionList.of(VIA, items.toArray(SExpressionNode[]::new)));
        return this;
    }

    /** An area, its outline a closed run of points. */
    public KiCadPcbBuilder area(Area area) {
        List<SExpressionNode> items = new ArrayList<>();
        String net = area.kind() == AreaKind.POUR ? area.net() : "";
        items.add(SExpressionList.of(NET, SExpressionAtom.of(net(net))));
        items.add(SExpressionList.of(NET_NAME, SExpressionString.of(net)));
        items.add(area.layers().size() == 1 ? SExpressionList.of(LAYER, SExpressionString.of(area.layers().getFirst()))
                : SExpressionList.of(LAYERS, area.layers().stream().map(SExpressionString::of).toArray(SExpressionNode[]::new)));
        items.add(uuid(identity("area")));
        items.add(SExpressionList.of("hatch", SExpressionAtom.of("edge"), SExpressionAtom.of(ZONE_HATCH)));
        if (area.priority() > 0) {
            items.add(SExpressionList.of(PRIORITY, SExpressionAtom.of(area.priority())));
        }
        items.add(SExpressionList.of(CONNECT_PADS, SExpressionList.of(CLEARANCE, SExpressionAtom.of(area.clearance()))));
        items.add(SExpressionList.of("min_thickness", SExpressionAtom.of(area.leastWidth())));
        items.add(SExpressionList.of("filled_areas_thickness", SExpressionAtom.of("no")));
        if (area.kind() == AreaKind.KEEPOUT) {
            items.add(SExpressionList.of(KEEPOUT, notAllowed("tracks"), notAllowed("vias"), notAllowed("pads"),
                    notAllowed("copperpour"), SExpressionList.of("footprints", SExpressionAtom.of("allowed"))));
        }
        items.add(SExpressionList.of(FILL, area.kind() == AreaKind.POUR ? SExpressionAtom.of("yes") : SExpressionAtom.of("no"),
                SExpressionList.of("thermal_gap", SExpressionAtom.of(area.thermalGap())),
                SExpressionList.of("thermal_bridge_width", SExpressionAtom.of(area.thermalSpoke()))));
        items.add(SExpressionList.of(ZONE_OUTLINE, SExpressionList.of(POINTS, area.outline().stream()
                .map(point -> point(POINT, onPage(point))).toArray(SExpressionNode[]::new))));
        areas.add(SExpressionList.of(ZONE, items.toArray(SExpressionNode[]::new)));
        return this;
    }

    private static SExpressionList notAllowed(String what) {
        return SExpressionList.of(what, SExpressionAtom.of("not_allowed"));
    }

    // ─── what is drawn ──────────────────────────────────────────────────────────────────────────────

    /** A line, an arc, a shape drawn on one of the board's layers — its edge on {@link KiCadLayer#EDGE}. */
    public KiCadPcbBuilder graphic(KiCadFootprintGraphic graphic) {
        drawn.add(onSide(graphic, KiCadSide.FRONT, this::onPage).written(true, identity("graphic")));
        return this;
    }

    /** Words on a layer, one line of them; on the back read from the back, so mirrored as KiCad mirrors them. */
    public KiCadPcbBuilder text(String text, KiCadPoint at, double angle, String layer, double height, Justification justification) {
        KiCadPoint placed = onPage(at);
        drawn.add(SExpressionList.of(BOARD_TEXT, SExpressionString.of(text),
                SExpressionList.of(AT, SExpressionAtom.of(placed.x()), SExpressionAtom.of(placed.y()), SExpressionAtom.of(normalized(angle))),
                SExpressionList.of(LAYER, SExpressionString.of(layer)), uuid(identity("text")),
                effects(height, justification, KiCadLayer.isBack(layer))));
        return this;
    }

    /** A measure between two points, its line {@code height} off them: KiCad works out and writes its figure itself. */
    public KiCadPcbBuilder dimension(KiCadPoint start, KiCadPoint end, double height, String layer) {
        String identity = identity("dimension");
        KiCadPoint from = onPage(start);
        KiCadPoint to = onPage(end);
        KiCadPoint middle = new KiCadPoint((from.x() + to.x()) / 2, (from.y() + to.y()) / 2);
        drawn.add(SExpressionList.of(DIMENSION, SExpressionList.of(TYPE, SExpressionAtom.of("aligned")),
                SExpressionList.of(LAYER, SExpressionString.of(layer)), uuid(identity),
                SExpressionList.of(POINTS, point(POINT, from), point(POINT, to)),
                SExpressionList.of(HEIGHT, SExpressionAtom.of(height)),
                SExpressionList.of(BOARD_TEXT, SExpressionString.of(""), SExpressionList.of(AT, SExpressionAtom.of(middle.x()), SExpressionAtom.of(middle.y())),
                        SExpressionList.of(LAYER, SExpressionString.of(layer)), uuid(identity(identity + " figure")),
                        effects(DIMENSION_TEXT, Justification.CENTRED, false)),
                SExpressionList.of("format", SExpressionList.of("prefix", SExpressionString.of("")), SExpressionList.of("suffix", SExpressionString.of("")),
                        SExpressionList.of("units", SExpressionAtom.of(DIMENSION_MILLIMETRES)), SExpressionList.of("units_format", SExpressionAtom.of(1)),
                        SExpressionList.of("precision", SExpressionAtom.of(DIMENSION_PRECISION))),
                SExpressionList.of("style", SExpressionList.of("thickness", SExpressionAtom.of(DIMENSION_TEXT * letterStroke)),
                        SExpressionList.of("arrow_length", SExpressionAtom.of(DIMENSION_ARROW)),
                        SExpressionList.of("text_position_mode", SExpressionAtom.of(0)),
                        SExpressionList.of("extension_height", SExpressionAtom.of(DIMENSION_EXTENSION_HEIGHT)),
                        SExpressionList.of("extension_offset", SExpressionAtom.of(DIMENSION_EXTENSION_OFFSET)),
                        SExpressionAtom.of("keep_text_aligned"))));
        return this;
    }

    // ─── the file ───────────────────────────────────────────────────────────────────────────────────

    /** The board's tree. */
    public SExpressionDocument document(String fileName) {
        List<SExpressionNode> items = new ArrayList<>();
        items.add(SExpressionList.of(KiCadKeyword.VERSION, SExpressionAtom.of(String.valueOf(VERSION))));
        items.add(SExpressionList.of(GENERATOR, SExpressionString.of("jmouse")));
        items.add(SExpressionList.of("general", SExpressionList.of("thickness", SExpressionAtom.of(thickness)),
                SExpressionList.of("legacy_teardrops", SExpressionAtom.of("no"))));
        items.add(SExpressionList.of("paper", SExpressionString.of(page.name())));
        items.add(SExpressionList.of("title_block", SExpressionList.of("title", SExpressionString.of(title))));
        items.add(layers());
        items.add(SExpressionList.of(SETUP, SExpressionList.of("pad_to_mask_clearance", SExpressionAtom.of(maskClearance)),
                SExpressionList.of("allow_soldermask_bridges_in_footprints", SExpressionAtom.of("no"))));
        nets.forEach((name, number) -> items.add(SExpressionList.of(NET, SExpressionAtom.of(number), SExpressionString.of(name))));
        items.addAll(footprints);
        items.addAll(drawn);
        items.addAll(copperItems);
        items.addAll(areas);
        groups.forEach((name, group) -> {
            List<SExpressionNode> written = new ArrayList<>(List.of(SExpressionString.of(name), uuid(identity("group " + name))));
            if (group.lock() == KiCadLock.LOCKED) {
                written.add(SExpressionList.of(LOCKED, SExpressionAtom.of("yes")));
            }
            written.add(SExpressionList.of(MEMBERS, group.members().stream().map(SExpressionString::of).toArray(SExpressionNode[]::new)));
            items.add(SExpressionList.of(GROUP, written.toArray(SExpressionNode[]::new)));
        });
        return SExpressionDocument.of(fileName, SExpressionList.of(PCB_FILE, items.toArray(SExpressionNode[]::new)));
    }

    /** The board's text, printed by {@link SExpressionPrinter}. */
    public String print(String fileName) {
        return new SExpressionPrinter().print(document(fileName));
    }

    /** {@code (layers (0 "F.Cu" signal) … (31 "B.Cu" signal) (32 "B.Adhes" user "B.Adhesive") …)}. */
    private SExpressionList layers() {
        List<SExpressionNode> items = new ArrayList<>();
        for (int index = 0; index < copper.size(); index++) {
            String layer = copper.get(index);
            int ordinal = layer.equals(KiCadSide.BACK.copper()) ? BACK_COPPER_ORDINAL : index;
            items.add(SExpressionList.of(String.valueOf(ordinal), SExpressionString.of(layer), SExpressionAtom.of("signal")));
        }
        for (TechnicalLayer layer : TECHNICAL_LAYERS) {
            items.add(layer.userName() == null
                    ? SExpressionList.of(String.valueOf(layer.ordinal()), SExpressionString.of(layer.name()), SExpressionAtom.of("user"))
                    : SExpressionList.of(String.valueOf(layer.ordinal()), SExpressionString.of(layer.name()), SExpressionAtom.of("user"),
                            SExpressionString.of(layer.userName())));
        }
        return SExpressionList.of(LAYERS, items.toArray(SExpressionNode[]::new));
    }

    private SExpressionList effects(double height, Justification justification, boolean mirrored) {
        List<SExpressionNode> justified = new ArrayList<>(KiCadPcb.justifyWords(justification.along(), justification.across()));
        if (mirrored) {
            justified.add(SExpressionAtom.of("mirror"));
        }
        SExpressionList font = SExpressionList.of(FONT, SExpressionList.of(SIZE, SExpressionAtom.of(height), SExpressionAtom.of(height)),
                SExpressionList.of("thickness", SExpressionAtom.of(height * letterStroke)));
        return justified.isEmpty() ? SExpressionList.of(EFFECTS, font)
                : SExpressionList.of(EFFECTS, font, SExpressionList.of(JUSTIFY, justified.toArray(SExpressionNode[]::new)));
    }

    private static SExpressionList at(KiCadPoint point, double angle) {
        return angle == 0 ? SExpressionList.of(AT, SExpressionAtom.of(point.x()), SExpressionAtom.of(point.y()))
                : SExpressionList.of(AT, SExpressionAtom.of(point.x()), SExpressionAtom.of(point.y()), SExpressionAtom.of(normalized(angle)));
    }

    private static SExpressionList point(KiCadKeyword name, KiCadPoint point) {
        return SExpressionList.of(name, SExpressionAtom.of(point.x()), SExpressionAtom.of(point.y()));
    }

    private static SExpressionList uuid(String identity) {
        return SExpressionList.of(KiCadKeyword.UUID, SExpressionString.of(identity));
    }

    /** A point of the board where it stands on the page. */
    private KiCadPoint onPage(KiCadPoint point) {
        return new KiCadPoint(point.x() - extent.left() + left, point.y() - extent.top() + top);
    }

    private static double normalized(double degrees) {
        double turned = ((degrees % 360) + 360) % 360;
        return turned > 180 ? turned - 360 : turned;
    }

    /** The next identity, made from the seed, what it names and how many came before it. */
    private String identity(String what) {
        identities++;
        return java.util.UUID.nameUUIDFromBytes((seed + "/" + what + "/" + identities).getBytes(StandardCharsets.UTF_8)).toString();
    }
}
