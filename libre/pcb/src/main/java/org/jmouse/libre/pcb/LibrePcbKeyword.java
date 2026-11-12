package org.jmouse.libre.pcb;

import org.jmouse.sexpression.node.SExpressionName;

/** Every word the model reads a project's lists by, as LibrePCB writes it: the one vocabulary of the files' structure. */
public enum LibrePcbKeyword implements SExpressionName {

    // ─── what everything says ────────────────────────────────────────────────────────────────────────────────────────
    NAME("name"),
    DESCRIPTION("description"),
    AUTHOR("author"),
    VERSION("version"),
    POSITION("position"),
    ROTATION("rotation"),
    ANGLE("angle"),
    SIZE("size"),
    LAYER("layer"),
    WIDTH("width"),
    VALUE("value"),
    ATTRIBUTE("attribute"),
    UNIT("unit"),

    // ─── what is drawn ───────────────────────────────────────────────────────────────────────────────────────────────
    POLYGON("polygon"),
    CIRCLE("circle"),
    TEXT("text"),
    STROKE_TEXT("stroke_text"),
    VERTEX("vertex"),
    FILL("fill"),
    DIAMETER("diameter"),
    HEIGHT("height"),
    ALIGN("align"),
    MIRROR("mirror"),
    HOLE("hole"),

    // ─── the library ─────────────────────────────────────────────────────────────────────────────────────────────────
    PIN("pin"),
    LENGTH("length"),
    SIGNAL("signal"),
    ROLE("role"),
    NEGATED("negated"),
    CLOCK("clock"),
    FORCED_NET("forced_net"),
    VARIANT("variant"),
    GATE("gate"),
    SYMBOL("symbol"),
    PREFIX("prefix"),
    SCHEMATIC_ONLY("schematic_only"),
    COMPONENT("component"),
    PACKAGE("package"),
    PACKAGE_PAD("package_pad"),
    MODEL("3d_model"),
    MODEL_POSITION("3d_position"),
    MODEL_ROTATION("3d_rotation"),
    LIBRARY_MODEL("lib_3d_model"),
    PAD("pad"),
    FOOTPRINT("footprint"),
    SIDE("side"),
    SHAPE("shape"),
    RADIUS("radius"),
    STOP_MASK("stop_mask"),
    SOLDER_PASTE("solder_paste"),

    // ─── the circuit ─────────────────────────────────────────────────────────────────────────────────────────────────
    NET("net"),
    NET_CLASS("netclass"),
    AUTOMATIC_NAME("auto"),
    LIBRARY_COMPONENT("lib_component"),
    LIBRARY_VARIANT("lib_variant"),
    DEVICE("device"),
    DEFAULT_TRACE_WIDTH("default_trace_width"),
    DEFAULT_VIA_DRILL("default_via_drill_diameter"),
    COPPER_CLEARANCE("min_copper_copper_clearance"),

    // ─── sheets ──────────────────────────────────────────────────────────────────────────────────────────────────────
    LIBRARY_GATE("lib_gate"),
    SEGMENT("netsegment"),
    BUS("bus"),
    BUS_SEGMENT("bussegment"),
    JUNCTION("junction"),
    LINE("line"),
    LABEL("label"),
    FROM("from"),
    TO("to"),

    // ─── boards ──────────────────────────────────────────────────────────────────────────────────────────────────────
    SILKSCREEN_TOP("silkscreen_layers_top"),
    SILKSCREEN_BOTTOM("silkscreen_layers_bot"),
    INNER("inner"),
    LAYERS("layers"),
    THICKNESS("thickness"),
    DESIGN_RULES("design_rules"),
    DESIGN_RULE_CHECK("design_rule_check"),
    MASK_CLEARANCE("stopmask_clearance"),
    PASTE_CLEARANCE("solderpaste_clearance"),
    LARGEST_TENTED_DRILL("stopmask_max_via_drill_diameter"),
    RATIO("ratio"),
    MINIMUM("min"),
    MAXIMUM("max"),
    LIBRARY_DEVICE("lib_device"),
    LIBRARY_FOOTPRINT("lib_footprint"),
    FLIP("flip"),
    VIA("via"),
    DRILL("drill"),
    EXPOSURE("exposure"),
    TRACE("trace"),
    PLANE("plane"),
    PRIORITY("priority"),
    MINIMUM_COPPER_CLEARANCE("min_copper_clearance"),

    // ─── index files ─────────────────────────────────────────────────────────────────────────────────────────────────
    BOARD("board"),
    SCHEMATIC("schematic");

    private final String word;

    LibrePcbKeyword(String word) {
        this.word = word;
    }

    @Override
    public String word() {
        return word;
    }
}
