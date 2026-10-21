package org.jmouse.parallax.script.reader;

import org.jmouse.el.lexer.BasicToken;
import org.jmouse.el.lexer.BoundedTokenCursor.Boundary;
import org.jmouse.el.lexer.Token;
import org.jmouse.el.lexer.TokenCursor;
import org.jmouse.parallax.script.Block;
import org.jmouse.parallax.script.Block.FootprintOutline;
import org.jmouse.parallax.script.Block.FootprintPad;
import org.jmouse.parallax.script.Block.PadShape;
import org.jmouse.parallax.script.Block.PadType;
import org.jmouse.parallax.script.Kind;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;

/**
 * How a part sits on a board, in millimetres — a board is drawn in millimetres, and a sheet's grid would mean nothing
 * on it:
 *
 * <pre>
 * pad "1" through-hole rectangle at 0, 0 size 1.905 x 2 drill 1.1
 * pad "2" surface rounded-rectangle at 2.54, 0 rotate 90 size 1 x 1.5 roundness 0.25
 * outline silkscreen width 0.12 "M-2.58 -3.27L7.66 -3.27"
 * outline courtyard width 0.05 filled "M…Z"
 * </pre>
 *
 * A {@code footprint} document is those lines at the top level of its body; a component names the one its package lands
 * on with {@code footprint to-220-3-vertical}.
 */
final class FootprintReader {

    /** The layers a footprint draws on, by what they are for. */
    static final Set<String> LAYERS = Set.of("silkscreen", "fabrication", "courtyard", "copper");

    private static final String PAD_LINE = "a pad is 'pad \"NUMBER\" TYPE SHAPE at x, y [rotate degrees] size width x height "
            + "[drill width [x height]] [roundness share]'";
    private static final String OUTLINE_LINE = "an outline is 'outline LAYER [width w] [filled] \"path\"'";

    /** A point's x ends at its comma. */
    private static final Boundary COMMA = (token, after, depth) -> depth == 0 && token.type() == BasicToken.T_COMMA;

    /** An outline's width ends where its path, or the word filled, begins. */
    private static final Boundary BEFORE_PATH = (token, after, depth) -> depth == 0
            && (token.type() == BasicToken.T_STRING || token.value().equals("filled"));

    private final Reading reading;
    private final JmxValues values;

    FootprintReader(Reading reading) {
        this.reading = reading;
        this.values = new JmxValues(reading);
    }

    /** {@code footprint to-220-3-vertical} in a component: the footprint its package lands on. */
    Block.FootprintUse use(Token keyword) {
        if (reading.atLineEnd()) {
            throw reading.refusal(keyword, "'footprint' names one footprint: 'footprint to-220-3-vertical'");
        }
        String footprint = reading.joinedWord();
        if (!reading.atLineEnd()) {
            throw reading.refusal(keyword, "'footprint' names one footprint: 'footprint to-220-3-vertical'");
        }
        return new Block.FootprintUse(footprint);
    }

    /** Whether the cursor stands on a footprint's line: {@code pad} or {@code outline}. */
    boolean atFootprintLine() {
        return !reading.atEnd() && reading.cursor.current().type() != BasicToken.T_STRING
                && Kind.FOOTPRINT_LINES.contains(reading.cursor.current().value());
    }

    Drawing drawing() {
        return new Drawing();
    }

    /** The lines of a footprint document, collected one at a time. */
    final class Drawing {

        private final List<FootprintPad> pads = new ArrayList<>();
        private final List<FootprintOutline> outlines = new ArrayList<>();

        /** The footprint line the cursor stands on. */
        void read() {
            Token first = reading.cursor.current();
            switch (first.value()) {
                case "pad" -> pads.add(pad(first));
                case "outline" -> outlines.add(outline(first));
                default -> throw reading.refusal(first, "'" + first.value() + "' is not part of a footprint: pad or outline");
            }
        }

        Block.Footprint footprint() {
            return new Block.Footprint(pads, outlines);
        }

        private FootprintPad pad(Token first) {
            TokenCursor cursor = reading.cursor;
            TokenCursor line = reading.lineView();
            if (line.remaining() < 8 || !reading.isString(line.lookAt(1))) {
                throw reading.refusal(first, PAD_LINE);
            }
            String number = reading.literal(line.lookAt(1));
            cursor.shift(2);
            // A type and a shape are words of several tokens: through-hole, rounded-rectangle.
            Token typeWritten = cursor.current();
            PadType type = PadType.of(reading.joinedWord()).orElseThrow(() -> reading.refusal(typeWritten,
                    "a pad's type is " + Arrays.stream(PadType.values()).map(PadType::word).toList()));
            Token shapeWritten = cursor.current();
            PadShape shape = PadShape.of(reading.joinedWord()).orElseThrow(() -> reading.refusal(shapeWritten,
                    "a pad's shape is " + Arrays.stream(PadShape.values()).map(PadShape::word).toList()));
            if (!at("at")) {
                throw reading.refusal(first, PAD_LINE);
            }
            cursor.next();
            double x = values.quantity(COMMA);
            cursor.next();
            double y = values.quantity(JmxValues.words(Set.of("rotate", "size")));
            double rotation = 0;
            if (at("rotate")) {
                cursor.next();
                rotation = values.quantity(JmxValues.words(Set.of("size")));
            }
            if (!at("size")) {
                throw reading.refusal(first, PAD_LINE);
            }
            cursor.next();
            double width = values.quantity(JmxValues.words(Set.of("x")));
            cursor.next();
            double height = values.quantity(JmxValues.words(Set.of("drill", "roundness")));
            double drillWidth = 0;
            double drillHeight = 0;
            if (at("drill")) {
                cursor.next();
                drillWidth = values.quantity(JmxValues.words(Set.of("x", "roundness")));
                drillHeight = drillWidth;
                if (at("x")) {
                    cursor.next();
                    drillHeight = values.quantity(JmxValues.words(Set.of("roundness")));
                }
            }
            double roundness = 0;
            if (at("roundness")) {
                cursor.next();
                roundness = values.quantity(JmxValues.LINE);
            }
            if (!reading.atLineEnd()) {
                throw reading.refusal(cursor.current(), PAD_LINE);
            }
            return new FootprintPad(number, type, shape, x, y, rotation, width, height, drillWidth, drillHeight, roundness);
        }

        private FootprintOutline outline(Token first) {
            TokenCursor cursor = reading.cursor;
            TokenCursor line = reading.lineView();
            if (line.remaining() < 3 || !LAYERS.contains(line.valueAt(1))) {
                throw reading.refusal(first, OUTLINE_LINE + "; a layer is " + LAYERS);
            }
            String layer = line.valueAt(1);
            cursor.shift(2);
            double width = 0;
            boolean filled = false;
            while (!reading.atLineEnd() && !reading.isString(cursor.current())) {
                if (at("width")) {
                    cursor.next();
                    width = values.quantity(BEFORE_PATH);
                } else if (at("filled")) {
                    filled = true;
                    cursor.next();
                } else {
                    throw reading.refusal(cursor.current(), OUTLINE_LINE);
                }
            }
            if (reading.atLineEnd()) {
                throw reading.refusal(first, OUTLINE_LINE);
            }
            String path = reading.literal(cursor.current());
            cursor.next();
            if (!reading.atLineEnd()) {
                throw reading.refusal(cursor.current(), OUTLINE_LINE);
            }
            return new FootprintOutline(layer, width, filled, path);
        }

        private boolean at(String word) {
            return !reading.atLineEnd() && reading.cursor.current().value().equals(word);
        }
    }
}
