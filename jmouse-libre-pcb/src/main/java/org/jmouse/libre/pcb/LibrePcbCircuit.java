package org.jmouse.libre.pcb;

import org.jmouse.sexpression.node.SExpressionList;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** What a project's circuit says: its nets and their classes, and its parts with the net each signal is on. */
public record LibrePcbCircuit(Map<String, Net> nets, Map<String, Bus> buses, Map<String, NetClass> netClasses, Map<String, Part> parts) {

    public LibrePcbCircuit {
        nets = Map.copyOf(nets);
        buses = Map.copyOf(buses);
        netClasses = Map.copyOf(netClasses);
        parts = Collections.unmodifiableMap(new LinkedHashMap<>(parts));
    }

    /**
     * @param named    whether its author named it, rather than LibrePCB's {@code N5}
     * @param netClass the class it is in, by id
     */
    public record Net(String id, String name, boolean named, String netClass) {
    }

    /** A bus of the circuit: its name — {@code D[0..7]} — says the nets it carries; a sheet draws it as bus segments. */
    public record Bus(String id, String name) {
    }

    /** A class of nets; what it leaves to the board's rules is empty. */
    public record NetClass(String id, String name, Optional<Double> traceWidth, Optional<Double> viaDrill, double clearance) {
    }

    /**
     * @param devices which of the library's devices it may be, its first the one it is
     * @param nets    the net each signal is on, by signal; a signal on none is left out
     */
    public record Part(String id, String libraryComponent, String variant, String name, String value, Map<String, String> attributes,
                       List<String> devices, Map<String, String> nets) {

        /** A place in a value where words are filled in: {@code {{RESISTANCE}}}, {@code {{MPN or DEVICE}}}. */
        private static final Pattern FILLED_IN = Pattern.compile("\\{\\{\\s*(.*?)\\s*}}");

        /** Between the names of a place in a value, the first that has words being taken. */
        private static final String OR = "\\s+or\\s+";

        public Part {
            attributes = Map.copyOf(attributes);
            devices = List.copyOf(devices);
            nets = Map.copyOf(nets);
        }

        /**
         * Its value as a reader is told it, its places filled in from its own attributes and from what the caller knows of
         * it; one that nothing fills is left out.
         *
         * @param known what else may be named: {@code DEVICE}, {@code COMPONENT}
         */
        public String valueInWords(Map<String, String> known) {
            Matcher places = FILLED_IN.matcher(value);
            String filled = places.replaceAll(place -> {
                for (String name : place.group(1).split(OR)) {
                    String words = attributes.getOrDefault(name, known.getOrDefault(name, ""));
                    if (!words.isBlank()) {
                        return Matcher.quoteReplacement(words);
                    }
                }
                return "";
            });
            return filled.replaceAll("\\s+", " ").strip();
        }

        public Optional<String> device() {
            return devices.stream().findFirst();
        }
    }

    static LibrePcbCircuit of(SExpressionList circuit) {
        Map<String, Net> nets = new LinkedHashMap<>();
        for (SExpressionList net : circuit.lists(LibrePcbKeyword.NET)) {
            nets.put(net.word(1), new Net(net.word(1), LibrePcbValues.text(net, LibrePcbKeyword.NAME),
                    !LibrePcbValues.flag(net, LibrePcbKeyword.AUTOMATIC_NAME), LibrePcbValues.text(net, LibrePcbKeyword.NET_CLASS)));
        }
        Map<String, Bus> buses = new LinkedHashMap<>();
        for (SExpressionList bus : circuit.lists(LibrePcbKeyword.BUS)) {
            buses.put(bus.word(1), new Bus(bus.word(1), LibrePcbValues.text(bus, LibrePcbKeyword.NAME)));
        }
        Map<String, NetClass> classes = new LinkedHashMap<>();
        for (SExpressionList netClass : circuit.lists(LibrePcbKeyword.NET_CLASS)) {
            classes.put(netClass.word(1), new NetClass(netClass.word(1), LibrePcbValues.text(netClass, LibrePcbKeyword.NAME),
                    LibrePcbValues.numberIfOne(netClass, LibrePcbKeyword.DEFAULT_TRACE_WIDTH),
                    LibrePcbValues.numberIfOne(netClass, LibrePcbKeyword.DEFAULT_VIA_DRILL),
                    LibrePcbValues.numberOr(netClass, LibrePcbKeyword.COPPER_CLEARANCE, 0)));
        }
        Map<String, Part> parts = new LinkedHashMap<>();
        for (SExpressionList part : circuit.lists(LibrePcbKeyword.COMPONENT)) {
            Map<String, String> signals = new LinkedHashMap<>();
            for (SExpressionList signal : part.lists(LibrePcbKeyword.SIGNAL)) {
                LibrePcbValues.reference(signal, LibrePcbKeyword.NET).ifPresent(net -> signals.put(signal.word(1), net));
            }
            parts.put(part.word(1), new Part(part.word(1), LibrePcbValues.text(part, LibrePcbKeyword.LIBRARY_COMPONENT),
                    LibrePcbValues.text(part, LibrePcbKeyword.LIBRARY_VARIANT), LibrePcbValues.text(part, LibrePcbKeyword.NAME),
                    LibrePcbValues.text(part, LibrePcbKeyword.VALUE), attributesOf(part),
                    part.lists(LibrePcbKeyword.DEVICE).stream().map(device -> device.word(1)).toList(), signals));
        }
        return new LibrePcbCircuit(nets, buses, classes, parts);
    }

    /** A component's attributes by name, each as a reader says it: {@code 100 nF}. A unit this format's reader does not know is left off. */
    private static Map<String, String> attributesOf(SExpressionList part) {
        Map<String, String> attributes = new LinkedHashMap<>();
        for (SExpressionList attribute : part.lists(LibrePcbKeyword.ATTRIBUTE)) {
            String words = LibrePcbValues.text(attribute, LibrePcbKeyword.VALUE).strip();
            String symbol = LibrePcbUnit.ofWord(LibrePcbValues.text(attribute, LibrePcbKeyword.UNIT)).map(LibrePcbUnit::symbol).orElse("");
            attributes.put(attribute.word(1), symbol.isEmpty() || words.isEmpty() ? words : words + " " + symbol);
        }
        return attributes;
    }
}
