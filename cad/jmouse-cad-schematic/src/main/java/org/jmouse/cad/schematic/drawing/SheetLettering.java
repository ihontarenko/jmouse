package org.jmouse.cad.schematic.drawing;

import org.jmouse.settings.Label;
import org.jmouse.cad.settings.SettingsSection;

/**
 * How big a sheet's texts are, in sheet units — 20 is 1.27 mm, KiCad's default text, so a symbol drawn at KiCad's size has its
 * texts at KiCad's size too —, and how much room a text takes: the technical face being monospaced, a text's
 * width is read from its letters. The one place the painters and the placing of labels both read.
 *
 * @param field          a part's designator and value
 * @param pinName        the name beside a pin
 * @param partName       a box's or a block's own name, and the sheet a block places
 * @param reading        a line of a running part's readings
 * @param characterWidth a letter's width, as a share of the text's size
 * @param lineSpacing    a line's height, as a share of the text's size
 */
public record SheetLettering(@Label("Field") double field, @Label("Pin name") double pinName, @Label("Part name") double partName, @Label("Reading") double reading, @Label("Character width") double characterWidth, @Label("Line spacing") double lineSpacing) {

    /** Its section of the settings tree (ADR 0004): {@code org/jmouse/cad/settings/schematic/lettering}. */
    public static final SettingsSection<SheetLettering> SECTION = new SettingsSection<>("schematic/lettering", "Sheet lettering", SheetLettering.class);
}
