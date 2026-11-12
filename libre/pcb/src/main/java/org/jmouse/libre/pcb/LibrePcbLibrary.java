package org.jmouse.libre.pcb;

import org.jmouse.sexpression.node.SExpressionList;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * The library a LibrePCB project carries inside it: every symbol, component, device and package it uses, each by its UUID.
 * A component says what a part is and how it is drawn, a device which package it comes in.
 */
public record LibrePcbLibrary(Map<String, Symbol> symbols, Map<String, Component> components, Map<String, Device> devices,
                              Map<String, Package> packages) {

    private static final String NO_PIN_NAME = "none";
    private static final String BOTTOM_SIDE = "bottom";

    public LibrePcbLibrary {
        symbols = Map.copyOf(symbols);
        components = Map.copyOf(components);
        devices = Map.copyOf(devices);
        packages = Map.copyOf(packages);
    }

    /** A pin of a symbol: {@code at} is where a wire meets it, and it runs {@code length} along {@code rotation}. */
    public record Pin(String id, String name, LibrePcbPoint at, double rotation, double length) {

        static Pin of(SExpressionList pin) {
            return new Pin(pin.word(1), LibrePcbValues.text(pin, LibrePcbKeyword.NAME), LibrePcbValues.position(pin),
                    LibrePcbValues.number(pin, LibrePcbKeyword.ROTATION), LibrePcbValues.number(pin, LibrePcbKeyword.LENGTH));
        }
    }

    /** @param author who drew it, as the element says; empty where it says nobody */
    public record Symbol(String id, String name, String author, List<Pin> pins, List<LibrePcbGraphic> graphics) {

        public Symbol {
            pins = List.copyOf(pins);
            graphics = List.copyOf(graphics);
        }

        static Symbol of(SExpressionList symbol) {
            return new Symbol(symbol.word(1), LibrePcbValues.text(symbol, LibrePcbKeyword.NAME), LibrePcbValues.text(symbol, LibrePcbKeyword.AUTHOR),
                    symbol.lists(LibrePcbKeyword.PIN).stream().map(Pin::of).toList(), LibrePcbGraphic.of(symbol));
        }
    }

    /** What a signal does electrically. */
    public enum SignalRole {
        PASSIVE("passive"), POWER("power"), INPUT("input"), OUTPUT("output"), BIDIRECTIONAL("inout"), OPEN_DRAIN("opendrain");

        private final String word;

        SignalRole(String word) {
            this.word = word;
        }

        /** A role by its word; a file of a later LibrePCB may know more, and such a signal is passive until this one does. */
        static SignalRole of(String word) {
            for (SignalRole role : values()) {
                if (role.word.equals(word)) {
                    return role;
                }
            }
            return PASSIVE;
        }
    }

    /** @param forcedNet the net it forces, as written: {@code {{VALUE}}} for a supply named by its value; empty for none */
    public record Signal(String id, String name, SignalRole role, boolean negated, boolean clock, String forcedNet) {

        static Signal of(SExpressionList signal) {
            return new Signal(signal.word(1), LibrePcbValues.text(signal, LibrePcbKeyword.NAME),
                    SignalRole.of(LibrePcbValues.text(signal, LibrePcbKeyword.ROLE)), LibrePcbValues.flag(signal, LibrePcbKeyword.NEGATED),
                    LibrePcbValues.flag(signal, LibrePcbKeyword.CLOCK), LibrePcbValues.text(signal, LibrePcbKeyword.FORCED_NET));
        }
    }

    /**
     * One symbol of a component's variant.
     *
     * @param signals     the signal each of the symbol's pins carries, by pin; a pin with none is left out
     * @param namedPins   the pins whose names are shown on the sheet, by pin
     */
    public record Gate(String id, String symbol, Map<String, String> signals, List<String> namedPins) {

        public Gate {
            signals = Map.copyOf(signals);
            namedPins = List.copyOf(namedPins);
        }

        static Gate of(SExpressionList gate) {
            Map<String, String> signals = new LinkedHashMap<>();
            List<String> named = new ArrayList<>();
            for (SExpressionList pin : gate.lists(LibrePcbKeyword.PIN)) {
                LibrePcbValues.reference(pin, LibrePcbKeyword.SIGNAL).ifPresent(signal -> signals.put(pin.word(1), signal));
                if (!NO_PIN_NAME.equals(LibrePcbValues.text(pin, LibrePcbKeyword.TEXT))) {
                    named.add(pin.word(1));
                }
            }
            return new Gate(gate.word(1), LibrePcbValues.text(gate, LibrePcbKeyword.SYMBOL), signals, named);
        }
    }

    public record Variant(String id, List<Gate> gates) {

        public Variant {
            gates = List.copyOf(gates);
        }

        static Variant of(SExpressionList variant) {
            return new Variant(variant.word(1), variant.lists(LibrePcbKeyword.GATE).stream().map(Gate::of).toList());
        }
    }

    /** @param schematicOnly whether it stands on the sheet alone: a supply, a frame */
    /** @param author who made it, as the element says; empty where it says nobody */
    public record Component(String id, String name, String author, String description, String prefix, boolean schematicOnly, List<Signal> signals,
                            List<Variant> variants) {

        public Component {
            signals = List.copyOf(signals);
            variants = List.copyOf(variants);
        }

        static Component of(SExpressionList component) {
            return new Component(component.word(1), LibrePcbValues.text(component, LibrePcbKeyword.NAME), LibrePcbValues.text(component, LibrePcbKeyword.AUTHOR),
                    LibrePcbValues.text(component, LibrePcbKeyword.DESCRIPTION), LibrePcbValues.text(component, LibrePcbKeyword.PREFIX),
                    LibrePcbValues.flag(component, LibrePcbKeyword.SCHEMATIC_ONLY),
                    component.lists(LibrePcbKeyword.SIGNAL).stream().map(Signal::of).toList(),
                    component.lists(LibrePcbKeyword.VARIANT).stream().map(Variant::of).toList());
        }

        public Optional<Variant> variant(String id) {
            return variants.stream().filter(variant -> variant.id().equals(id)).findFirst();
        }

        public Optional<Signal> signal(String id) {
            return signals.stream().filter(signal -> signal.id().equals(id)).findFirst();
        }
    }

    /** @param padSignals the signal each of its package's pads carries, by pad; a pad with none is left out */
    public record Device(String id, String name, String component, String packageId, Map<String, String> padSignals) {

        public Device {
            padSignals = Map.copyOf(padSignals);
        }

        static Device of(SExpressionList device) {
            Map<String, String> signals = new LinkedHashMap<>();
            for (SExpressionList pad : device.lists(LibrePcbKeyword.PAD)) {
                LibrePcbValues.reference(pad, LibrePcbKeyword.SIGNAL).ifPresent(signal -> signals.put(pad.word(1), signal));
            }
            return new Device(device.word(1), LibrePcbValues.text(device, LibrePcbKeyword.NAME),
                    LibrePcbValues.text(device, LibrePcbKeyword.COMPONENT), LibrePcbValues.text(device, LibrePcbKeyword.PACKAGE), signals);
        }
    }

    /** @param vertices where it runs: one for a round hole, more for a slot, its diameter round them */
    public record Hole(double diameter, List<LibrePcbGraphic.Vertex> vertices) {

        public Hole {
            vertices = List.copyOf(vertices);
        }

        static Hole of(SExpressionList hole) {
            return new Hole(LibrePcbValues.number(hole, LibrePcbKeyword.DIAMETER), LibrePcbGraphic.verticesOf(hole));
        }
    }

    /** How a pad is cut. */
    public enum PadShape {
        ROUNDED_RECTANGLE("roundrect"), OCTAGON("octagon"), CUSTOM("custom");

        private final String word;

        PadShape(String word) {
            this.word = word;
        }

        static PadShape of(String word) {
            for (PadShape shape : values()) {
                if (shape.word.equals(word)) {
                    return shape;
                }
            }
            throw new LibrePcbReadException("'" + word + "' is no shape of a pad.");
        }
    }

    /**
     * A pad of a footprint.
     *
     * @param packagePad the package's pad it is, by id; empty for one that is none, a thermal one
     * @param onBottom   whether it stands on its footprint's bottom face
     * @param radius     how round its corners are, 1 for all the way
     * @param vertices   a custom pad's outline
     */
    public record FootprintPad(String id, Optional<String> packagePad, boolean onBottom, PadShape shape, LibrePcbPoint at, double rotation,
                               double width, double height, double radius, LibrePcbOpening stopMask, LibrePcbOpening solderPaste,
                               List<LibrePcbGraphic.Vertex> vertices, List<Hole> holes) {

        public FootprintPad {
            vertices = List.copyOf(vertices);
            holes = List.copyOf(holes);
        }

        static FootprintPad of(SExpressionList pad) {
            SExpressionList size = pad.requireList(LibrePcbKeyword.SIZE);
            return new FootprintPad(pad.word(1), LibrePcbValues.reference(pad, LibrePcbKeyword.PACKAGE_PAD),
                    BOTTOM_SIDE.equals(LibrePcbValues.text(pad, LibrePcbKeyword.SIDE)), PadShape.of(LibrePcbValues.text(pad, LibrePcbKeyword.SHAPE)), LibrePcbValues.position(pad),
                    LibrePcbValues.number(pad, LibrePcbKeyword.ROTATION), size.number(1), size.number(2),
                    LibrePcbValues.numberOr(pad, LibrePcbKeyword.RADIUS, 0), LibrePcbOpening.of(pad.wordOf(LibrePcbKeyword.STOP_MASK)),
                    LibrePcbOpening.of(pad.wordOf(LibrePcbKeyword.SOLDER_PASTE)), LibrePcbGraphic.verticesOf(pad),
                    pad.lists(LibrePcbKeyword.HOLE).stream().map(Hole::of).toList());
        }

        public boolean goesThrough() {
            return !holes.isEmpty();
        }
    }

    /** Three numbers a footprint places its models by: {@code (3d_position 0 0 0)}. */
    public record Axes(double x, double y, double z) {

        static Axes of(SExpressionList footprint, LibrePcbKeyword name) {
            return footprint.list(name).map(list -> new Axes(list.number(1), list.number(2), list.number(3))).orElse(new Axes(0, 0, 0));
        }
    }

    /**
     * One way a package lands on a board.
     *
     * @param models        the package's models it stands with, by id
     * @param modelPosition where they stand from its origin, millimetres
     * @param modelRotation how they are turned, degrees about each axis
     */
    public record Footprint(String id, String name, List<FootprintPad> pads, List<LibrePcbGraphic> graphics, List<Hole> holes, List<String> models,
                            Axes modelPosition, Axes modelRotation) {

        public Footprint {
            pads = List.copyOf(pads);
            graphics = List.copyOf(graphics);
            holes = List.copyOf(holes);
            models = List.copyOf(models);
        }

        static Footprint of(SExpressionList footprint) {
            return new Footprint(footprint.word(1), LibrePcbValues.text(footprint, LibrePcbKeyword.NAME),
                    footprint.lists(LibrePcbKeyword.PAD).stream().map(FootprintPad::of).toList(), LibrePcbGraphic.of(footprint),
                    footprint.lists(LibrePcbKeyword.HOLE).stream().map(Hole::of).toList(),
                    footprint.lists(LibrePcbKeyword.MODEL).stream().map(model -> model.word(1)).toList(),
                    Axes.of(footprint, LibrePcbKeyword.MODEL_POSITION), Axes.of(footprint, LibrePcbKeyword.MODEL_ROTATION));
        }

        public Optional<FootprintPad> pad(String id) {
            return pads.stream().filter(pad -> pad.id().equals(id)).findFirst();
        }
    }

    /**
     * @param padNames   each pad's name, by its id: what a footprint's pad is numbered with
     * @param author     who drew it, its footprints and its 3D models, as the element says; empty where it says nobody
     * @param modelNames each 3D model's name, by its id
     */
    public record Package(String id, String name, String author, String description, Map<String, String> padNames, Map<String, String> modelNames, List<Footprint> footprints) {

        public Package {
            padNames = Map.copyOf(padNames);
            modelNames = Map.copyOf(modelNames);
            footprints = List.copyOf(footprints);
        }

        static Package of(SExpressionList found) {
            Map<String, String> names = new LinkedHashMap<>();
            found.lists(LibrePcbKeyword.PAD).forEach(pad -> names.put(pad.word(1), LibrePcbValues.text(pad, LibrePcbKeyword.NAME)));
            Map<String, String> models = new LinkedHashMap<>();
            found.lists(LibrePcbKeyword.MODEL).forEach(model -> models.put(model.word(1), LibrePcbValues.text(model, LibrePcbKeyword.NAME)));
            return new Package(found.word(1), LibrePcbValues.text(found, LibrePcbKeyword.NAME), LibrePcbValues.text(found, LibrePcbKeyword.AUTHOR),
                    LibrePcbValues.text(found, LibrePcbKeyword.DESCRIPTION),
                    names, models, found.lists(LibrePcbKeyword.FOOTPRINT).stream().map(Footprint::of).toList());
        }

        public Optional<Footprint> footprint(String id) {
            return footprints.stream().filter(footprint -> footprint.id().equals(id)).findFirst();
        }
    }
}
