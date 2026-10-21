package org.jmouse.parallax.symbol;

import org.jmouse.parallax.script.Block;
import org.jmouse.parallax.script.Block.PinDirection;
import org.jmouse.parallax.script.Block.PinNature;

import java.util.ArrayList;
import java.util.Comparator;
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
 *     <li>on a side, buses in the order of their lowest package number, a bus's pins by their trailing number;</li>
 *     <li>a pin keeps its attributes ({@code edge} for a clock, {@code bubble} for an inverted one): the box draws them.</li>
 * </ul>
 */
public final class PinBox {

    public static final String LEFT = "left";
    public static final String RIGHT = "right";
    public static final String TOP = "top";
    public static final String BOTTOM = "bottom";

    /** A name's bus and its index: {@code PA15} is the bus {@code PA}, index 15; {@code VCC} is a bus of its own. */
    private static final Pattern BUS = Pattern.compile("^(.*?)(\\d+)$");

    private PinBox() {
    }

    /** The box's sides, each pin written as the box reads it: its name and its attributes, {@code clk edge}. */
    public static Map<String, List<String>> sides(List<Block.Pin> pins) {
        Map<String, List<Bus>> sides = new LinkedHashMap<>();
        for (String side : List.of(LEFT, RIGHT, TOP, BOTTOM)) {
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
            String lighter = count(sides.get(LEFT)) <= count(sides.get(RIGHT)) ? LEFT : RIGHT;
            sides.get(lighter).add(bus);
        }
        Map<String, List<String>> written = new LinkedHashMap<>();
        sides.forEach((side, buses) -> {
            List<String> entries = buses.stream().sorted(Comparator.comparingInt(Bus::firstNumber))
                    .flatMap(bus -> bus.pins().stream()).map(PinBox::written).toList();
            if (!entries.isEmpty()) {
                written.put(side, entries);
            }
        });
        return written;
    }

    /** Pins sharing a name before their trailing number, in the order of that number; no side when bidirectional. */
    private record Bus(Optional<String> side, List<Block.Pin> pins) {

        int firstNumber() {
            return pins.stream().flatMap(pin -> pin.numbers().stream()).min(Integer::compare).orElse(Integer.MAX_VALUE);
        }
    }

    private record BusKey(Optional<String> side, String bus) {
    }

    private static List<Bus> busesOf(List<Block.Pin> pins) {
        Map<BusKey, List<Block.Pin>> grouped = new LinkedHashMap<>();
        for (Block.Pin pin : pins) {
            grouped.computeIfAbsent(new BusKey(sideOf(pin), busOf(functionOf(pin))), ignored -> new ArrayList<>()).add(pin);
        }
        List<Bus> buses = new ArrayList<>();
        grouped.forEach((key, members) -> buses.add(new Bus(key.side(),
                members.stream().sorted(Comparator.comparingLong(PinBox::indexOf)).toList())));
        return buses;
    }

    /** Where a pin stands by what it is; none for a bidirectional one, which goes with its bus. */
    private static Optional<String> sideOf(Block.Pin pin) {
        if (pin.nature() == PinNature.POWER) {
            return Optional.of(pin.direction() == PinDirection.OUTPUT ? RIGHT : TOP);
        }
        if (pin.nature() == PinNature.GROUND) {
            return Optional.of(BOTTOM);
        }
        return switch (pin.direction()) {
            case INPUT -> Optional.of(LEFT);
            case OUTPUT -> Optional.of(RIGHT);
            case BIDIRECTIONAL -> Optional.empty();
        };
    }

    /**
     * What a pin does, as a bus is named: its description when that is one word — {@code PA0}, {@code ~{RESET}} without
     * KiCad's overbar —, else its name. The box prints the same (the client's {@code pinCaption}).
     */
    private static String functionOf(Block.Pin pin) {
        String description = pin.description() == null ? "" : pin.description().strip();
        if (description.isEmpty() || description.chars().anyMatch(Character::isWhitespace)) {
            return pin.name();
        }
        return description.replaceAll("~\\{([^}]*)}", "$1");
    }

    private static String busOf(String name) {
        Matcher matcher = BUS.matcher(name);
        return matcher.matches() ? matcher.group(1) : name;
    }

    private static long indexOf(Block.Pin pin) {
        Matcher matcher = BUS.matcher(functionOf(pin));
        return matcher.matches() && matcher.group(2).length() < 18 ? Long.parseLong(matcher.group(2)) : -1;
    }

    private static int count(List<Bus> buses) {
        return buses.stream().mapToInt(bus -> bus.pins().size()).sum();
    }

    private static String written(Block.Pin pin) {
        return pin.attributes().isEmpty() ? pin.name() : pin.name() + " " + String.join(" ", pin.attributes());
    }
}
