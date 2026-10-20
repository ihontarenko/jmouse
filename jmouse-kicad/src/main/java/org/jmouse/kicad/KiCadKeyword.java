package org.jmouse.kicad;

import org.jmouse.kicad.parser.node.KiCadName;

/**
 * Every list name the model reads or writes, as KiCad writes it: the one vocabulary of the files' structure. A word is
 * a keyword only where a list is looked up by it — KiCad reserves none —, so the lexer knows none of these.
 */
public enum KiCadKeyword implements KiCadName {

    // ─── files ────────────────────────────────────────────────────────────────────────────────────
    SCHEMATIC_FILE("kicad_sch"),
    SYMBOL_LIBRARY_FILE("kicad_symbol_lib"),
    VERSION("version"),
    GENERATOR("generator"),
    UUID("uuid"),

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
    JUNCTION("junction"),
    NO_CONNECT("no_connect"),
    LABEL("label"),
    GLOBAL_LABEL("global_label"),
    HIERARCHICAL_LABEL("hierarchical_label"),
    SHEET("sheet");

    private final String word;

    KiCadKeyword(String word) {
        this.word = word;
    }

    @Override
    public String word() {
        return word;
    }
}
