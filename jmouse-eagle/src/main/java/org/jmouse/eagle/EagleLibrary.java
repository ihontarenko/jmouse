package org.jmouse.eagle;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * A library as a design carries it: its packages — the footprints —, its symbols, and its device sets, each a part
 * one may place, its gates drawn with symbols and its devices landing on packages.
 */
public record EagleLibrary(String name, Map<String, Package> packages, Map<String, Symbol> symbols,
                           Map<String, DeviceSet> deviceSets) {

    public EagleLibrary {
        packages = ordered(packages);
        symbols = ordered(symbols);
        deviceSets = ordered(deviceSets);
    }

    /**
     * A package: its through-hole pads, its surface pads, its holes, and what is drawn with it.
     *
     * @param description what Eagle says of it, as it says it — often HTML
     */
    public record Package(String name, String description, List<Pad> pads, List<Smd> smds, List<Hole> holes,
                          List<EagleGraphic> graphics) {

        public Package {
            pads = List.copyOf(pads);
            smds = List.copyOf(smds);
            holes = List.copyOf(holes);
            graphics = List.copyOf(graphics);
        }
    }

    /** A through-hole pad: its drill, its ring's diameter — zero for Eagle's own, set by the design rules —, its shape. */
    public record Pad(String name, double x, double y, double drill, double diameter, Shape shape, EagleRotation rotation) {

        /** The shape of its ring. */
        public enum Shape {
            ROUND, SQUARE, OCTAGON,
            /** Twice as long as wide, round at both ends. */
            LONG,
            /** As long, its drill at one end. */
            OFFSET;

            /** Whether it is longer than wide. */
            public boolean elongated() {
                return this == LONG || this == OFFSET;
            }
        }
    }

    /**
     * A surface pad on layer 1 (16 when its package is placed on the bottom), {@code dx} by {@code dy}.
     *
     * @param roundness how round its corners are, in percent of the shorter side's half
     */
    public record Smd(String name, double x, double y, double dx, double dy, int layer, double roundness, EagleRotation rotation) {
    }

    /** A hole with no copper round it. */
    public record Hole(double x, double y, double drill) {
    }

    /** A symbol: what it is drawn with, and its pins. */
    public record Symbol(String name, List<EagleGraphic> graphics, List<Pin> pins) {

        public Symbol {
            graphics = List.copyOf(graphics);
            pins = List.copyOf(pins);
        }
    }

    /**
     * A symbol's pin: where it connects, and its line reaching from there by its {@code length} in the direction its
     * rotation turns it — {@code R0} points right.
     */
    public record Pin(String name, double x, double y, Length length, Direction direction, Function function,
                      EagleRotation rotation, Visibility visible) {

        /** How long its line is. */
        public enum Length {
            POINT(0), SHORT(2.54), MIDDLE(5.08), LONG(7.62);

            private final double millimetres;

            Length(double millimetres) {
                this.millimetres = millimetres;
            }

            public double millimetres() {
                return millimetres;
            }
        }

        /** What it carries. */
        public enum Direction {
            /** Not connected. */
            NC,
            IN,
            OUT,
            /** Both ways. */
            IO,
            /** An open collector. */
            OC,
            /** A power input of a part. */
            PWR,
            PAS,
            /** A three-state output. */
            HIZ,
            /** A supply: the one pin of a supply symbol, which names its net. */
            SUP
        }

        /** How its end is drawn. */
        public enum Function {
            NONE, DOT, CLK, DOTCLK;

            /** Whether its end carries an inverting dot. */
            public boolean inverted() {
                return this == DOT || this == DOTCLK;
            }

            /** Whether it acts on an edge: its end carries a clock's wedge. */
            public boolean clocked() {
                return this == CLK || this == DOTCLK;
            }
        }

        /** What is written beside it. */
        public enum Visibility {
            OFF, PAD, PIN, BOTH;

            /** Whether its name is. */
            public boolean nameShown() {
                return this == PIN || this == BOTH;
            }
        }

        /**
         * Its name as Eagle shows it: without the {@code @2} that tells apart several pins of one name — a chip's
         * {@code GND@1} and {@code GND@2} both read {@code GND}.
         */
        public String shownName() {
            int at = name.indexOf('@');
            return at < 0 ? name : name.substring(0, at);
        }
    }

    /** A device set: the part one places, the letters it is named by, its gates and its devices. */
    public record DeviceSet(String name, String prefix, boolean userValue, String description, List<Gate> gates,
                            List<Device> devices) {

        public DeviceSet {
            gates = List.copyOf(gates);
            devices = List.copyOf(devices);
        }

        public Optional<Device> device(String name) {
            return devices.stream().filter(device -> device.name().equals(name)).findFirst();
        }

        public Optional<Gate> gate(String name) {
            return gates.stream().filter(gate -> gate.name().equals(name)).findFirst();
        }
    }

    /** A gate of a device set: one symbol it is drawn with, where it stands from the others. */
    public record Gate(String name, String symbol, double x, double y) {
    }

    /**
     * A device: the package it lands on — none for a part with no footprint, a frame, a supply —, which pads each gate's
     * pin is, and its technologies' attributes.
     *
     * @param technologies each technology's attributes, by its name — {@code ""} for the device's own
     */
    public record Device(String name, String packageName, List<Connect> connects, Map<String, Map<String, String>> technologies) {

        public Device {
            connects = List.copyOf(connects);
            technologies = ordered(technologies);
        }
    }

    /** A pin of a gate and the pads it is: one, or several joined ({@code pad="1 2"}). */
    public record Connect(String gate, String pin, List<String> pads) {

        public Connect {
            pads = List.copyOf(pads);
        }
    }

    static <V> Map<String, V> ordered(Map<String, V> map) {
        return Collections.unmodifiableMap(new LinkedHashMap<>(map));
    }
}
