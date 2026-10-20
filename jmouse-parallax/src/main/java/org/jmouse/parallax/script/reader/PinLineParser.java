package org.jmouse.parallax.script.reader;

import org.jmouse.el.lexer.Token;
import org.jmouse.el.lexer.TokenCursor;
import org.jmouse.parallax.script.Block;
import org.jmouse.parallax.script.Block.Pin;
import org.jmouse.parallax.script.Block.PinDirection;
import org.jmouse.parallax.script.Block.PinNature;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

/**
 * A line of {@code pins}: {@code out analog output 1 7 "output"}, {@code clk digital input 3 11 edge} — or a bus,
 * {@code q[0..7] digital output 15, 1, 2, 3, 4, 5, 6, 7 "parallel outputs"}, a pin for each bit.
 */
final class PinLineParser extends JmxLineParser<PinLineParser.Declared> {

    private static final int NARROWEST_BUS = 2;
    private static final int WIDEST_BUS = 32;

    /** What a pins line declares: its pins, and the bus they make when it is one. */
    record Declared(List<Pin> pins, Optional<Block.Bus> bus) {
    }

    private final Reading reading;

    PinLineParser(Reading reading) {
        this.reading = reading;
    }

    @Override
    Declared read() {
        TokenCursor line = reading.lineView();
        if (line.remaining() > 1 && line.checkValueAt(1, "[")) {
            return bus(line);
        }
        Token first = reading.cursor.current();
        String name = reading.cursor.next().value();
        return new Declared(List.of(pin(first, name)), Optional.empty());
    }

    /**
     * Every bit a pin — {@code q_0} … {@code q_7} —, each with its package number in the order the range is written, all
     * of one nature. Bit k is always {@code q_k}, the least significant first, whichever way the range runs. A bus
     * takes as many numbers as it has bits, or none; the bits of one bus are never split across units.
     */
    private Declared bus(TokenCursor line) {
        Token first = reading.cursor.current();
        String name = line.valueAt(0);
        int range = line.offsetOf(2, "..");
        if (range != 3 || line.remaining() < 6 || !line.checkValueAt(5, "]") || !line.valueAt(2).matches("\\d+")
                || !line.valueAt(4).matches("\\d+")) {
            throw reading.refusal(first, "a bus is written 'name[first..last] nature …', as 'q[0..7] digital output 15, 1, 2, …'");
        }
        int low = Integer.parseInt(line.valueAt(2));
        int high = Integer.parseInt(line.valueAt(4));
        int width = Math.abs(high - low) + 1;
        if (width < NARROWEST_BUS || width > WIDEST_BUS) {
            throw reading.refusal(first, "a bus has " + NARROWEST_BUS + " to " + WIDEST_BUS + " bits, not " + width);
        }
        reading.cursor.shift(6);
        Pin template = pin(first, name);
        if (template.nature() != PinNature.DIGITAL) {
            // A bus is read and driven as one number of levels; a voltage has no bits to make one of.
            throw reading.refusal(first, "bus '" + name + "' is " + template.nature().name().toLowerCase(Locale.ROOT)
                    + ": a bus is digital — write analog pins one by one, as 'channel_0 analog 1'");
        }
        if (!template.numbers().isEmpty() && template.numbers().size() != width) {
            throw reading.refusal(first, "bus '" + name + "' has " + width + " bits and " + template.numbers().size()
                    + " package numbers: write one number for each bit, in the order of its bits, or none");
        }
        int step = high >= low ? 1 : -1;
        List<Pin> pins = new ArrayList<>();
        List<String> bits = new ArrayList<>();
        for (int position = 0; position < width; position++) {
            String bit = name + "_" + (low + position * step);
            List<Integer> number = template.numbers().isEmpty() ? List.of() : List.of(template.numbers().get(position));
            pins.add(new Pin(bit, number, template.nature(), template.direction(), template.attributes(), template.description()));
            bits.add(bit);
        }
        if (step < 0) {
            // q_k is bit k however the range is written: q[7..0] lists its package numbers from q_7 down, and q_0 is
            // still the least significant bit, as a data sheet and every HDL read it.
            Collections.reverse(bits);
        }
        return new Declared(pins, Optional.of(new Block.Bus(name, bits)));
    }

    /** A pin's words from where the cursor stands to the line's end: its nature, direction, numbers, attributes. */
    private Pin pin(Token first, String name) {
        TokenCursor cursor = reading.cursor;
        List<Integer> numbers = new ArrayList<>();
        List<String> attributes = new ArrayList<>();
        PinNature nature = null;
        PinDirection direction = PinDirection.INPUT;
        String description = "";
        while (!reading.atLineEnd()) {
            Token token = cursor.current();
            if (reading.isString(token)) {
                description = reading.literal(cursor.next());
            } else if (token.value().matches("\\d+")) {
                numbers.add(Integer.parseInt(cursor.next().value()));
            } else if (token.value().equals(",")) {
                // A comma only parts numbers — '15, 1, 2' is three of them.
                cursor.next();
            } else {
                // Every other word says something, and may be hyphenated: 'open-drain' is one word.
                String word = reading.joinedWord();
                switch (word) {
                    case "analog" -> nature = PinNature.ANALOG;
                    case "digital" -> nature = PinNature.DIGITAL;
                    case "power" -> nature = PinNature.POWER;
                    case "ground" -> nature = PinNature.GROUND;
                    case "output" -> direction = PinDirection.OUTPUT;
                    case "input" -> direction = PinDirection.INPUT;
                    case "bidirectional" -> direction = PinDirection.BIDIRECTIONAL;
                    default -> attributes.add(word);
                }
            }
        }
        if (nature == null) {
            throw reading.refusal(first, "pin '" + name + "' needs a nature: analog, digital, power or ground");
        }
        return new Pin(name, numbers, nature, direction, attributes, description);
    }
}
