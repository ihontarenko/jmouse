package org.jmouse.kicad;

import java.util.List;
import java.util.Map;

/**
 * The properties KiCad itself gives meaning to — what a {@code (property "Reference" "R5")} names. A sheet's name and
 * file were written {@code Sheet name} and {@code Sheet file} before KiCad 8; both spellings are read.
 */
public enum KiCadProperty {

    REFERENCE("Reference"),
    VALUE("Value"),
    FOOTPRINT("Footprint"),
    DATASHEET("Datasheet"),
    DESCRIPTION("Description"),
    SHEET_NAME("Sheetname", "Sheet name"),
    SHEET_FILE("Sheetfile", "Sheet file");

    private final List<String> spellings;

    KiCadProperty(String... spellings) {
        this.spellings = List.of(spellings);
    }

    /** How KiCad writes it now. */
    public String word() {
        return spellings.getFirst();
    }

    /** Its value among written properties, under any of its spellings; empty when none is written. */
    public String in(Map<String, String> properties) {
        return spellings.stream().map(properties::get).filter(value -> value != null).findFirst().orElse("");
    }
}
