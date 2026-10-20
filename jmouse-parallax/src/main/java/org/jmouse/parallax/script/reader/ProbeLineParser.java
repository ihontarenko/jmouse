package org.jmouse.parallax.script.reader;

import org.jmouse.el.lexer.BoundedTokenCursor.Boundary;
import org.jmouse.el.lexer.Token;
import org.jmouse.el.lexer.TokenCursor;
import org.jmouse.parallax.script.Block.Probe;
import org.jmouse.units.Quantity;

import java.util.Optional;
import java.util.Set;

/**
 * A line of {@code probes}: an oscilloscope's channel — {@code CH1 out 2 V/div}, {@code CH2 out against reference},
 * {@code CH3 current R1}, {@code L1 level clock}, {@code CH1 out 1 V/div shows X * pi / 2} — or its time base,
 * {@code time 5 ms/div}. A meter is no probe: it is a part, and a line written as one is refused saying so.
 */
final class ProbeLineParser extends JmxLineParser<ProbeLineParser.Probed> {

    /** The word that opens the oscilloscope's time base; no channel is named so. */
    static final String TIME_BASE = "time";

    /** The word after a channel's scale that opens what its screen shows instead of the reading. */
    private static final String SHOWS = "shows";

    /** What a meter's display showed when meters were probes: refused on a channel, which has none. */
    private static final Set<String> DISPLAY_MODES = Set.of("dc", "ac", "rms");

    private static final String CHANNEL = "CH[0-9]+";

    /** A scale ends where its {@code /div} begins, or where the screen's expression does. */
    private static final Boundary SCALE = (token, after, depth) -> (token.value().equals("/") && after.value().equals("div"))
            || token.value().equals(SHOWS);

    /** What a probes line declares. */
    sealed interface Probed {
    }

    record Channel(Probe probe) implements Probed {
    }

    /** @param first where the line begins, for a refusal of a second time base */
    record TimeBase(Token first, Quantity perDivision) implements Probed {
    }

    private final Reading reading;
    private final JmxValues values;

    ProbeLineParser(Reading reading, JmxValues values) {
        this.reading = reading;
        this.values = values;
    }

    @Override
    Probed read() {
        return reading.lineView().checkValueAt(0, TIME_BASE) ? timeBase() : new Channel(channel());
    }

    /** {@code time 5 ms/div}: seconds a division, positive. */
    private TimeBase timeBase() {
        TokenCursor cursor = reading.cursor;
        TokenCursor line = reading.lineView();
        Token first = cursor.current();
        int perDivision = line.offsetOf(1, "/");
        if (line.remaining() < 2 || perDivision < 2 || !line.checkValueAt(perDivision + 1, "div")) {
            throw reading.refusal(first, "the time base is written as time a division: 'time 5 ms/div'");
        }
        cursor.next();
        Token time = cursor.current();
        Quantity quantity = values.written(SCALE).quantity()
                .orElseThrow(() -> reading.refusal(time, "the time base is a time: 'time 5 ms/div'"));
        if (!(quantity.value() > 0)) {
            throw reading.refusal(time, "the time base is a positive time a division");
        }
        reading.skipLine();
        return new TimeBase(first, quantity);
    }

    private Probe channel() {
        TokenCursor cursor = reading.cursor;
        TokenCursor line = reading.lineView();
        Token first = cursor.current();
        int length = line.remaining();
        if (length < 2) {
            throw reading.refusal(first, "a probe is 'ID node [against node] [scale]', 'ID current part', "
                    + "'ID power part' or 'ID level node'");
        }
        if (line.checkValueAt(2, "(")) {
            throw reading.refusalAhead(1, "a probe names what it measures: 'CH1 out', 'CH2 out against reference', "
                    + "'A1 current R1', 'M1 power RL', 'L1 level clock'");
        }
        String id = line.valueAt(0);
        String what = line.valueAt(1);
        char measure = switch (what) {
            case "current" -> 'I';
            case "power" -> 'P';
            case "level" -> 'L';
            default -> 'V';
        };
        // A meter is a part of the circuit now, wired as on a bench; this block holds the oscilloscope's channels alone.
        // A channel may still take a CURRENT probe — a clamp around a part's lead, `CH2 current R1 10 mA/div` —: that is
        // the scope's own probe, not a meter, and it is what shows a current lagging a voltage.
        if (measure == 'I' && !id.matches(CHANNEL)) {
            throw reading.refusalAhead(1, "a meter is a part: an ammeter goes in the break of a wire in 'parts' — "
                    + "'" + id + " ammeter (in = a, out = b)' —; 'probes' holds the oscilloscope's channels");
        }
        if (measure == 'P') {
            throw reading.refusalAhead(1, "a meter is a part: '" + id + " wattmeter (in = a, out = b, plus = b, minus = 0)' "
                    + "in 'parts'; 'probes' holds the oscilloscope's channels");
        }
        if (measure == 'V' && (what.equals("voltage") || !id.matches(CHANNEL))) {
            throw reading.refusalAhead(0, "a voltmeter is a part: '" + id + " voltmeter (plus = a, minus = b)' in 'parts'; "
                    + "'probes' holds the oscilloscope's channels, CH1 to CH4");
        }
        int at = measure == 'V' ? 1 : 2;
        if (at >= length) {
            throw reading.refusal(first, "'" + what + "' names what it reads: '" + what + " R1'");
        }
        cursor.shift(at);
        String target = reading.joinedWord();
        Optional<String> reference = Optional.empty();
        if (cursor.checkValueAt(0, "against") && !reading.atLineEnd()) {
            Token against = cursor.next();
            if (measure != 'V' || reading.atLineEnd()) {
                throw reading.refusal(against, "a voltage is read 'against' another node: 'CH2 out against reference'");
            }
            reference = Optional.of(cursor.next().value());
        }
        TokenCursor rest = reading.lineView();
        int shows = rest.offsetOf(0, SHOWS);
        int last = shows >= 0 ? shows : rest.remaining();
        if (shows >= 0 && shows + 1 >= rest.remaining()) {
            throw reading.refusalAhead(shows, "'shows' is followed by what the screen draws, X being the reading: "
                    + "'shows X * pi / 2'");
        }
        if (last > 0 && DISPLAY_MODES.contains(rest.valueAt(last - 1))) {
            throw reading.refusalAhead(last - 1, "a channel has no display mode; a meter's display is chosen on the bench");
        }
        Optional<Quantity> scale = Optional.empty();
        if (last > 0) {
            scale = values.written(SCALE).quantity();
            while (!reading.atLineEnd() && !cursor.checkValueAt(0, SHOWS)) {
                cursor.next();
            }
        }
        Optional<String> expression = Optional.empty();
        if (!reading.atLineEnd()) {
            cursor.next();
            expression = Optional.of(values.expression(JmxValues.LINE).text());
        }
        reading.skipLine();
        return new Probe(id, measure, target, reference, scale, Optional.empty(), expression);
    }
}
