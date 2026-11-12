package org.jmouse.kicad.footprint;

import org.jmouse.kicad.KiCadPoint;
import org.jmouse.kicad.KiCadProperty;
import org.jmouse.kicad.pcb.KiCadPcb;
import org.jmouse.sexpression.node.SExpressionList;

import java.util.Optional;

import static org.jmouse.kicad.KiCadKeyword.*;

/**
 * Where a footprint prints what the part placed on it says — its reference ({@code R5}) or its value ({@code 10k}) —: the
 * middle of the text, as KiCad places one, its turn, its layer and its letters' height. KiCad 8 writes it as a property
 * with a place, {@code (property "Reference" "REF**" (at 0 -1.4 0) (layer "F.SilkS") (effects (font (size 1 1))))};
 * before it, as {@code (fp_text reference "REF**" (at 0 -1.4) …)}. Both are read.
 *
 * @param property {@link KiCadProperty#REFERENCE} or {@link KiCadProperty#VALUE}
 * @param at       the point the text stands on, by its {@code along} and {@code across}: its middle where it says nothing
 * @param rotation degrees, counter-clockwise as seen
 * @param layer    KiCad's layer, {@code F.SilkS}
 * @param height   the letters' height, in millimetres
 * @param hidden   whether KiCad keeps it from being printed
 */
public record KiCadFootprintField(KiCadProperty property, KiCadPoint at, double rotation, String layer, double height, boolean hidden,
                                  KiCadPcb.Along along, KiCadPcb.Across across) {

    /** A field standing on its middle, as KiCad places one that says nothing of its justification. */
    public KiCadFootprintField(KiCadProperty property, KiCadPoint at, double rotation, String layer, double height, boolean hidden) {
        this(property, at, rotation, layer, height, hidden, KiCadPcb.Along.CENTRE, KiCadPcb.Across.MIDDLE);
    }

    /** The field a footprint's list writes, either way; empty for any other list, or one with no place. */
    public static Optional<KiCadFootprintField> of(SExpressionList list) {
        Optional<KiCadProperty> property = propertyOf(list);
        if (property.isEmpty()) {
            return Optional.empty();
        }
        Optional<SExpressionList> at = list.list(AT);
        if (at.isEmpty()) {
            return Optional.empty();
        }
        Optional<SExpressionList> effects = list.list(EFFECTS);
        double height = effects.flatMap(effect -> effect.list(FONT)).flatMap(font -> font.list(SIZE)).map(size -> size.number(1))
                .orElse(DEFAULT_HEIGHT);
        // KiCad 8 writes (hide yes) on the field; before, a bare hide on the text or in its effects.
        boolean hidden = list.says(HIDE) || effects.map(effect -> effect.says(HIDE)).orElse(false);
        return Optional.of(new KiCadFootprintField(property.get(), KiCadPoint.of(at.get()), at.get().size() > 3 ? at.get().number(3) : 0,
                list.wordOf(LAYER).orElse(""), height, hidden, KiCadPcb.alongOf(list), KiCadPcb.acrossOf(list)));
    }

    /** KiCad's own text height when a text does not say, in millimetres. */
    private static final double DEFAULT_HEIGHT = 1.0;

    /** What a list is the field of: a reference or a value property with a place, or an fp_text of either kind. */
    private static Optional<KiCadProperty> propertyOf(SExpressionList list) {
        if (list.is(PROPERTY) && list.size() > 1) {
            String name = list.word(1);
            return name.equals(KiCadProperty.REFERENCE.word()) ? Optional.of(KiCadProperty.REFERENCE)
                    : name.equals(KiCadProperty.VALUE.word()) ? Optional.of(KiCadProperty.VALUE) : Optional.empty();
        }
        if (list.is(FOOTPRINT_TEXT) && list.size() > 1) {
            String kind = list.word(1);
            return kind.equals(REFERENCE_TEXT) ? Optional.of(KiCadProperty.REFERENCE)
                    : kind.equals(VALUE_TEXT) ? Optional.of(KiCadProperty.VALUE) : Optional.empty();
        }
        return Optional.empty();
    }

    /** What an {@code fp_text} of the reference and of the value is written as before KiCad 8. */
    private static final String REFERENCE_TEXT = "reference";
    private static final String VALUE_TEXT = "value";
}
