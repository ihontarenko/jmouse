package org.jmouse.cad.component;

import org.jmouse.cad.Index;

import java.util.List;
import java.util.Optional;

/**
 * A pin of a component: its name, what it carries, which way its signal goes, how it drives, the numbers it has on the
 * package, and a label for a reader.
 *
 * @param index     a bus's bits — {@code q[0..7]} —, each a pin of its own; null for one pin
 * @param nature    {@code analog}, {@code digital}, {@code power} or {@code ground}
 * @param direction {@code input}, {@code output}, {@code bidirectional}, or null for an analog pin that is neither
 * @param drive     how it drives — {@code open-drain} —, or null for a push-pull output and any other pin
 * @param numbers   its numbers on the package, in groups as written: {@code 1 7} one group, {@code 15, 1, 2} three —
 *                  a bus's pins are its groups, one a bit
 * @param edge      whether it acts on an edge rather than a level: a clock
 * @param label     what it is, for a reader; empty when not written
 */
public record Pin(String name, Index index, String nature, String direction, String drive, List<List<String>> numbers, boolean edge,
                  String label) {

    /** The words its nature, direction and drive are written with, as the documents write them. */
    private static final String DIGITAL       = "digital";
    private static final String POWER         = "power";
    private static final String GROUND        = "ground";
    private static final String OUTPUT        = "output";
    private static final String BIDIRECTIONAL = "bidirectional";
    private static final String OPEN_DRAIN    = "open-drain";

    public Pin {
        numbers = numbers.stream().map(List::copyOf).toList();
        label = label == null ? "" : label;
    }

    public Optional<String> flows() {
        return Optional.ofNullable(direction);
    }

    /** The pins it stands for: its bits, or itself. */
    public List<String> names() {
        return index == null ? List.of(name) : index.names(name);
    }

    public boolean digital() {
        return nature.equals(DIGITAL);
    }

    /** Whether it carries a part's supply, or its ground: what a digital part's levels are read against. */
    public boolean power() {
        return nature.equals(POWER);
    }

    public boolean ground() {
        return nature.equals(GROUND);
    }

    /** Whether it drives its line: an output, or a bus line a part both talks and listens on. */
    public boolean output() {
        return OUTPUT.equals(direction) || BIDIRECTIONAL.equals(direction);
    }

    /** Whether it both talks and listens on its line: a bus line. */
    public boolean bidirectional() {
        return BIDIRECTIONAL.equals(direction);
    }

    /** Whether it reads its line: every pin but an output; a pin written with no direction is an input. */
    public boolean reads() {
        return !OUTPUT.equals(direction);
    }

    /** Whether it only ever pulls its line low, the line released otherwise. */
    public boolean openDrain() {
        return OPEN_DRAIN.equals(drive);
    }
}
