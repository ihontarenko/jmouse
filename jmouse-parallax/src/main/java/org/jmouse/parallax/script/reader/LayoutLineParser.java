package org.jmouse.parallax.script.reader;

import org.jmouse.el.lexer.BasicToken;
import org.jmouse.el.lexer.BoundedTokenCursor.Boundary;
import org.jmouse.el.lexer.Token;
import org.jmouse.el.lexer.TokenCursor;
import org.jmouse.parallax.script.Block;
import org.jmouse.parallax.script.Block.Placement;
import org.jmouse.parallax.script.Block.Point;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * A line of {@code layout}: {@code R1 at 240, 200 rotate 90}, {@code GND1 ground at 600, 520},
 * {@code A1 ground agnd at 640, 520}, {@code L1 label vp at 80, 40}, {@code W1 wire R1.negative to C1.positive via 240 120}.
 */
final class LayoutLineParser extends JmxLineParser<LayoutLineParser.Laid> {

    private static final String SHAPE = "a layout line is 'ID at x, y', 'ID ground [net] at x, y', "
            + "'ID label|port|global NET at x, y', 'ID common at x, y', 'ID junction at x, y' or 'ID wire from to to'";
    private static final String WIRE = "a wire is 'ID wire PART.pin to PART.pin [via x y, x y …]'";

    /** The layout marks that name the net they are: {@code ground agnd}, {@code label vp}, {@code port FB}, {@code global vcc}. */
    private static final Set<String> NAMING_MARKS = Set.of("ground", "label", "port", "global");

    /** The marks that stand without a net: a ground of node 0, a common, a junction. */
    private static final Set<String> MARKS = Set.of("ground", "common", "junction");

    /** The texts of a part that can be put somewhere by hand, in the order a layout line writes them. */
    static final List<String> LABELS = List.of("name", "value", "readings");

    /** What an instrument's display may be set to read: its mode, and whether it shows a figure or a graph. */
    static final List<String> SHOWS = List.of("dc", "ac", "rms", "figure", "gauge", "graph");

    /** Where a coordinate ends: a comma, or a placement word. */
    private static final Boundary COORDINATE = (token, after, depth) -> token.type() == BasicToken.T_COMMA
            || token.value().equals("rotate") || token.value().equals("mirror") || token.value().equals("shows")
            || (LABELS.contains(token.value()) && after.value().equals("at"));

    /** What a layout line lays out: a part or a mark, or a wire. */
    sealed interface Laid {
    }

    record Item(Block.LayoutItem item) implements Laid {
    }

    record Wire(Block.LayoutWire wire) implements Laid {
    }

    private final Reading reading;
    private final JmxValues values;
    /** The ids the block being read has laid out: an id is laid out once. */
    private final Set<String> ids = new HashSet<>();

    LayoutLineParser(Reading reading, JmxValues values) {
        this.reading = reading;
        this.values = values;
    }

    /** A new layout block begins: no id is laid out yet. */
    void begin() {
        ids.clear();
    }

    @Override
    Laid read() {
        TokenCursor cursor = reading.cursor;
        Token first = cursor.current();
        // An id is one word, joined where nothing separates its tokens: the canvas names a wire 'wire-3'.
        String id = reading.joinedWord();
        TokenCursor line = reading.lineView();
        if (line.remaining() < 2) {
            throw reading.refusal(first, SHAPE);
        }
        if (!ids.add(id)) {
            throw reading.refusal(first, "'" + id + "' is laid out twice");
        }
        String word = line.valueAt(0);
        if (word.equals("wire")) {
            cursor.next();
            return new Wire(wire(id));
        }
        if (word.equals("at")) {
            cursor.next();
            return new Item(new Block.LayoutItem(id, Optional.empty(), Optional.empty(), placement()));
        }
        if (MARKS.contains(word) && line.checkValueAt(1, "at")) {
            cursor.shift(2);
            return new Item(new Block.LayoutItem(id, Optional.of(word), Optional.empty(), placement()));
        }
        if (NAMING_MARKS.contains(word) && line.remaining() > 2 && line.checkValueAt(2, "at")) {
            // Another ground: the mark is the net it names, kept apart from node 0 by the engine. A label is a net's
            // flag, as KiCad draws a power port: every label of one net is that net, with no wire drawn between them.
            // A port draws a net the sheet hands to its parent, a global one a net every sheet of the project shares.
            String net = line.valueAt(1);
            cursor.shift(3);
            return new Item(new Block.LayoutItem(id, Optional.of(word), Optional.of(net), placement()));
        }
        throw reading.refusalAhead(0, "after '" + id + "' comes 'at x, y', 'ground [net] at', 'label|port|global NET at', "
                + "'common at', 'junction at' or 'wire'");
    }

    /** {@code R1.negative to C1.positive via 240 120, 300 120}. */
    private Block.LayoutWire wire(String id) {
        TokenCursor cursor = reading.cursor;
        Token from = cursor.current();
        TokenCursor line = reading.lineView();
        int toAt = line.offsetOf(0, "to");
        if (toAt <= 0) {
            throw reading.refusal(from, WIRE);
        }
        String start = reading.joinedWord();
        if (!cursor.checkValueAt(0, "to")) {
            throw reading.refusal(from, WIRE);
        }
        Token to = cursor.next();
        if (reading.atLineEnd() || cursor.checkValueAt(0, "via")) {
            throw reading.refusal(to, WIRE);
        }
        String end = reading.joinedWord();
        if (!reading.atLineEnd() && !cursor.checkValueAt(0, "via")) {
            throw reading.refusal(to, WIRE);
        }
        List<Point> bends = new ArrayList<>();
        if (!reading.atLineEnd()) {
            cursor.next();
            while (true) {
                Token corner = cursor.current();
                double x = coordinate(corner);
                double y = coordinate(corner);
                if (!reading.atLineEnd() && !cursor.isCurrent(BasicToken.T_COMMA)) {
                    throw reading.refusal(corner, "a corner is 'x y': 'via 240 120, 300 120'");
                }
                bends.add(new Point(x, y));
                if (reading.atLineEnd()) {
                    break;
                }
                cursor.next();
            }
        }
        return new Block.LayoutWire(id, start, end, bends);
    }

    private double coordinate(Token corner) {
        if (reading.atLineEnd() || reading.cursor.isCurrent(BasicToken.T_COMMA)) {
            throw reading.refusal(corner, "a corner is 'x y': 'via 240 120, 300 120'");
        }
        Token written = reading.cursor.current();
        return values.number().orElseThrow(() -> reading.refusal(corner, "'" + written.value() + "' is not a coordinate"));
    }

    /**
     * {@code x, y [rotate degrees] [mirror] [name at dx, dy] [value at dx, dy] [readings at dx, dy] [shows rms graph]},
     * after {@code at}: a text's place is measured from the part's centre.
     */
    private Placement placement() {
        TokenCursor cursor = reading.cursor;
        Token from = cursor.current();
        TokenCursor line = reading.lineView();
        int coordinatesEnd = line.remaining();
        for (int offset = 0; offset < line.remaining(); offset++) {
            if (COORDINATE.endsAt(line.lookAt(offset), line.lookAt(offset + 1), 0) && !line.checkValueAt(offset, ",")) {
                coordinatesEnd = offset;
                break;
            }
        }
        int comma = line.offsetOf(0, ",");
        if (comma < 0 || comma >= coordinatesEnd) {
            throw reading.refusal(from, "a position is 'at x, y [rotate degrees] [mirror] [name at dx, dy]'");
        }
        double x = values.quantity(COORDINATE);
        cursor.next();
        double y = values.quantity(COORDINATE);
        double rotation = 0;
        boolean mirrored = false;
        Map<String, Point> labels = new LinkedHashMap<>();
        List<String> shows = new ArrayList<>();
        while (!reading.atLineEnd()) {
            Token word = cursor.next();
            switch (word.value()) {
                case "rotate" -> rotation = values.number().filter(ignored -> atPlacementWord())
                        .orElseThrow(() -> reading.refusal(word, "a rotation is a number of degrees"));
                case "mirror" -> mirrored = true;
                case "shows" -> {
                    while (!reading.atLineEnd() && !atPlacementWord()) {
                        Token shown = cursor.next();
                        if (!SHOWS.contains(shown.value()) || shows.contains(shown.value())) {
                            throw reading.refusal(shown, "a display shows one of " + String.join(", ", SHOWS)
                                    + ", each once: 'shows rms graph'");
                        }
                        shows.add(shown.value());
                    }
                }
                default -> {
                    cursor.next();
                    TokenCursor rest = reading.lineView();
                    int labelComma = rest.offsetOf(0, ",");
                    int labelEnd = placementWordAhead(rest);
                    if (labelComma < 0 || labelComma >= labelEnd || labels.containsKey(word.value())) {
                        throw reading.refusal(word, "a text stands '" + word.value() + " at dx, dy' from the part's centre, once");
                    }
                    double dx = values.quantity(COORDINATE);
                    cursor.next();
                    labels.put(word.value(), new Point(dx, values.quantity(COORDINATE)));
                }
            }
        }
        return new Placement(x, y, rotation, mirrored, labels, shows);
    }

    /** Whether the cursor stands on a placement word, or the line's end: what ends the one before it. */
    private boolean atPlacementWord() {
        TokenCursor cursor = reading.cursor;
        return reading.atLineEnd() || (!cursor.isCurrent(BasicToken.T_COMMA)
                && COORDINATE.endsAt(cursor.lookAt(0), cursor.lookAt(1), 0));
    }

    /** The offset of the next placement word on the line, or the line's length. */
    private static int placementWordAhead(TokenCursor line) {
        for (int offset = 0; offset < line.remaining(); offset++) {
            if (!line.checkValueAt(offset, ",") && COORDINATE.endsAt(line.lookAt(offset), line.lookAt(offset + 1), 0)) {
                return offset;
            }
        }
        return line.remaining();
    }
}
