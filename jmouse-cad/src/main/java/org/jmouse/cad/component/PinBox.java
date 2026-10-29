package org.jmouse.cad.component;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Where a chip's pins stand on its box, worked out from what each pin is — never from how another library drew it: a
 * box is our drawing of a part's facts.
 *
 * <ul>
 *     <li>supplies on top, grounds at the bottom;</li>
 *     <li>what comes in on the left, what goes out on the right;</li>
 *     <li>a bidirectional pin goes with its bus — the pins sharing its name before the trailing number, {@code PA0}…
 *     {@code PA15} —, and a whole bus to the side holding fewer pins, so a port is never split and the box stays
 *     square;</li>
 *     <li>on a side, buses in the order of their lowest package number, a bus's pins by their trailing number.</li>
 * </ul>
 */
public final class PinBox {

    /** The sides of a box, in the order they are written. */
    public enum Side {
        LEFT, RIGHT, TOP, BOTTOM
    }

    /** A name's bus and its index: {@code PA15} is the bus {@code PA}, index 15; {@code VCC} is a bus of its own. */
    private static final Pattern BUS = Pattern.compile("^(.*?)(\\d+)$");

    /** The longest trailing number read as an index rather than as part of a name. */
    private static final int LONGEST_INDEX = 18;

    private PinBox() {
    }

    /** The box's sides, each its pins in order; a side no pin stands on is left out. */
    public static Map<Side, List<Pin>> sides(List<Pin> pins) {
        Map<Side, List<Bus>> sides = new EnumMap<>(Side.class);
        for (Side side : Side.values()) {
            sides.put(side, new ArrayList<>());
        }
        List<Bus> bidirectional = new ArrayList<>();
        for (Bus bus : busesOf(pins)) {
            if (bus.side().isEmpty()) {
                bidirectional.add(bus);
            } else {
                sides.get(bus.side().get()).add(bus);
            }
        }
        for (Bus bus : bidirectional) {
            Side lighter = count(sides.get(Side.LEFT)) <= count(sides.get(Side.RIGHT)) ? Side.LEFT : Side.RIGHT;
            sides.get(lighter).add(bus);
        }
        Map<Side, List<Pin>> placed = new LinkedHashMap<>();
        sides.forEach((side, buses) -> {
            List<Pin> entries = buses.stream().sorted(Comparator.comparingInt(Bus::firstNumber))
                    .flatMap(bus -> bus.pins().stream()).toList();
            if (!entries.isEmpty()) {
                placed.put(side, entries);
            }
        });
        return placed;
    }

    /** Pins sharing a name before their trailing number, in the order of that number; no side when bidirectional. */
    private record Bus(Optional<Side> side, List<Pin> pins) {

        int firstNumber() {
            return pins.stream().flatMap(pin -> pin.numbers().stream()).flatMap(List::stream)
                    .filter(number -> number.matches("\\d{1,9}")).map(Integer::parseInt).min(Integer::compare)
                    .orElse(Integer.MAX_VALUE);
        }
    }

    private record BusKey(Optional<Side> side, String bus) {
    }

    private static List<Bus> busesOf(List<Pin> pins) {
        Map<BusKey, List<Pin>> grouped = new LinkedHashMap<>();
        for (Pin pin : pins) {
            grouped.computeIfAbsent(new BusKey(sideOf(pin), busOf(functionOf(pin))), ignored -> new ArrayList<>()).add(pin);
        }
        List<Bus> buses = new ArrayList<>();
        grouped.forEach((key, members) -> buses.add(new Bus(key.side(),
                members.stream().sorted(Comparator.comparingLong(PinBox::indexOf)).toList())));
        return buses;
    }

    /** Where a pin stands by what it is; none for a bidirectional one, which goes with its bus. */
    private static Optional<Side> sideOf(Pin pin) {
        if (pin.power()) {
            return Optional.of(pin.reads() ? Side.TOP : Side.RIGHT);
        }
        if (pin.ground()) {
            return Optional.of(Side.BOTTOM);
        }
        if (pin.bidirectional()) {
            return Optional.empty();
        }
        return Optional.of(pin.reads() ? Side.LEFT : Side.RIGHT);
    }

    /**
     * What a pin does, as a bus is named: its label when that is one word — {@code PA0}, {@code ~{RESET}} without KiCad's
     * overbar —, else its name.
     */
    private static String functionOf(Pin pin) {
        String label = pin.label().strip();
        if (label.isEmpty() || label.chars().anyMatch(Character::isWhitespace)) {
            return pin.name();
        }
        return label.replaceAll("~\\{([^}]*)}", "$1");
    }

    private static String busOf(String name) {
        Matcher matcher = BUS.matcher(name);
        return matcher.matches() ? matcher.group(1) : name;
    }

    private static long indexOf(Pin pin) {
        Matcher matcher = BUS.matcher(functionOf(pin));
        return matcher.matches() && matcher.group(2).length() < LONGEST_INDEX ? Long.parseLong(matcher.group(2)) : -1;
    }

    private static int count(List<Bus> buses) {
        return buses.stream().mapToInt(bus -> bus.pins().size()).sum();
    }
}
