package org.jmouse.kicad;

import org.jmouse.sexpression.node.SExpressionName;

/**
 * Every list name the model reads or writes, as KiCad writes it: the one vocabulary of the files' structure. A word is
 * a keyword only where a list is looked up by it — KiCad reserves none —, so the lexer knows none of these.
 */
public enum KiCadKeyword implements SExpressionName {

    // ─── files ────────────────────────────────────────────────────────────────────────────────────
    SCHEMATIC_FILE("kicad_sch"),
    SYMBOL_LIBRARY_FILE("kicad_symbol_lib"),
    PCB_FILE("kicad_pcb"),
    VERSION("version"),
    GENERATOR("generator"),
    UUID("uuid"),
    /** KiCad 6's name for an item's identity, before {@code uuid}. */
    TSTAMP("tstamp"),
    /** KiCad 6's name for a group's identity. */
    ID("id"),
    LOCKED("locked"),
    GROUP("group"),
    MEMBERS("members"),

    // ─── placing ──────────────────────────────────────────────────────────────────────────────────
    AT("at"),
    SIZE("size"),
    MIRROR("mirror"),
    POINTS("pts"),
    POINT("xy"),

    // ─── symbols ──────────────────────────────────────────────────────────────────────────────────
    SYMBOL("symbol"),
    LIBRARY_SYMBOLS("lib_symbols"),
    LIBRARY_ID("lib_id"),
    EXTENDS("extends"),
    POWER("power"),
    PROPERTY("property"),
    UNIT("unit"),
    PIN("pin"),
    NAME("name"),
    NUMBER("number"),
    LENGTH("length"),
    HIDE("hide"),
    EXCLUDE_FROM_SIMULATION("exclude_from_sim"),
    DO_NOT_POPULATE("dnp"),
    INSTANCES("instances"),
    PROJECT("project"),
    PATH("path"),
    REFERENCE("reference"),

    // ─── connections ──────────────────────────────────────────────────────────────────────────────
    WIRE("wire"),
    BUS("bus"),
    JUNCTION("junction"),
    NO_CONNECT("no_connect"),
    LABEL("label"),
    GLOBAL_LABEL("global_label"),
    HIERARCHICAL_LABEL("hierarchical_label"),
    SHEET("sheet"),

    // ─── footprints ───────────────────────────────────────────────────────────────────────────────
    FOOTPRINT("footprint"),
    DESCRIPTION("descr"),
    TAGS("tags"),
    PAD("pad"),
    DRILL("drill"),
    LAYER("layer"),
    LAYERS("layers"),
    ROUNDNESS("roundrect_rratio"),
    MASK_MARGIN("solder_mask_margin"),
    PASTE_MARGIN("solder_paste_margin"),
    PASTE_RATIO("solder_paste_margin_ratio"),
    PRIMITIVES("primitives"),
    PAD_OPTIONS("options"),
    ANCHOR("anchor"),
    SETUP("setup"),
    TENTING("tenting"),
    FRONT("front"),
    BACK("back"),
    LINE("fp_line"),
    RECTANGLE("fp_rect"),
    CIRCLE("fp_circle"),
    ARC("fp_arc"),
    POLYGON("fp_poly"),
    FOOTPRINT_CURVE("fp_curve"),
    START("start"),
    END("end"),
    CENTER("center"),
    MIDDLE("mid"),
    STROKE("stroke"),
    WIDTH("width"),
    FILL("fill"),
    TYPE("type"),

    // ─── boards ──────────────────────────────
    SEGMENT("segment"),
    TRACK_ARC("arc"),
    VIA("via"),
    NET("net"),
    NET_NAME("net_name"),
    ZONE("zone"),
    ZONE_OUTLINE("polygon"),
    KEEPOUT("keepout"),
    CONNECT_PADS("connect_pads"),
    CLEARANCE("clearance"),
    PRIORITY("priority"),
    BOARD_LINE("gr_line"),
    BOARD_RECTANGLE("gr_rect"),
    BOARD_CIRCLE("gr_circle"),
    BOARD_ARC("gr_arc"),
    BOARD_POLYGON("gr_poly"),
    BOARD_CURVE("gr_curve"),
    BOARD_TEXT("gr_text"),
    FOOTPRINT_TEXT("fp_text"),
    EFFECTS("effects"),
    FONT("font"),
    JUSTIFY("justify"),
    DIMENSION("dimension"),
    HEIGHT("height"),
    IMAGE("image"),

    // ─── 3D models ─────────────────────────────────────────────────────────────────────────────────
    MODEL("model"),
    OFFSET("offset"),
    SCALE("scale"),
    ROTATE("rotate"),
    XYZ("xyz");

    private final String word;

    KiCadKeyword(String word) {
        this.word = word;
    }

    @Override
    public String word() {
        return word;
    }
}
