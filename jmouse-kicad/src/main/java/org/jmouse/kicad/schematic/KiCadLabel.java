package org.jmouse.kicad.schematic;

import static org.jmouse.kicad.KiCadKeyword.*;

import org.jmouse.kicad.KiCadKeyword;
import org.jmouse.kicad.KiCadPoint;
import org.jmouse.kicad.parser.node.KiCadList;

/**
 * A net's name written on a sheet: a {@code label} joins what touches it to every label of its name on the same sheet,
 * a {@code global_label} to every one in the design, a {@code hierarchical_label} to the pin of its name on the sheet
 * symbol that places this sheet.
 *
 * @param kind  which of the three it is
 * @param text  the net's name
 * @param at    the point it connects at
 * @param angle which way it faces, degrees
 */
public record KiCadLabel(Kind kind, String text, KiCadPoint at, int angle) {

    public enum Kind {
        LOCAL(LABEL), GLOBAL(GLOBAL_LABEL), HIERARCHICAL(HIERARCHICAL_LABEL);

        private final KiCadKeyword keyword;

        Kind(KiCadKeyword keyword) {
            this.keyword = keyword;
        }

        public KiCadKeyword keyword() {
            return keyword;
        }
    }

    static KiCadLabel of(Kind kind, KiCadList label) {
        KiCadList at = label.requireList(AT);
        return new KiCadLabel(kind, label.word(1), KiCadPoint.of(at), at.size() > 3 ? (int) at.number(3) : 0);
    }
}
