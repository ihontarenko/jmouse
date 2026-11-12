package org.jmouse.kicad;

import tools.jackson.core.JacksonException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.node.ArrayNode;
import tools.jackson.databind.node.ObjectNode;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Pattern;

/**
 * What a {@code .kicad_pro} says of how its board is made: its net classes, which nets are in which, the design rules, and
 * the track and via sizes it offers. A file too old or too bare to say any of it says nothing.
 *
 * @param usual    the class every net is in that no other names; empty where the file has none
 * @param classes  every other class, by name
 * @param nets     the class a net is written into, by the net as KiCad names it
 * @param patterns the classes a net falls in by its name: the first that matches answers
 * @param rules    the design rules as KiCad names them, {@code min_track_width}, in millimetres
 */
public record KiCadProjectSettings(Optional<NetClass> usual, List<NetClass> classes, Map<String, String> nets, List<NetPattern> patterns,
                                   Map<String, Double> rules, List<Double> trackWidths, List<ViaSize> viaSizes) {

    private static final String USUAL_CLASS = "Default";
    private static final ObjectMapper JSON = new ObjectMapper();
    private static final String PROJECT_EXTENSION = ".kicad_pro";
    /** The project file's and its net settings' versions as KiCad 8 writes them. */
    private static final int PROJECT_VERSION = 1;
    private static final int NET_SETTINGS_VERSION = 3;

    public static final KiCadProjectSettings NONE = new KiCadProjectSettings(Optional.empty(), List.of(), Map.of(), List.of(), Map.of(), List.of(), List.of());

    public KiCadProjectSettings {
        classes = List.copyOf(classes);
        nets = Map.copyOf(nets);
        patterns = List.copyOf(patterns);
        rules = Map.copyOf(rules);
        trackWidths = List.copyOf(trackWidths);
        viaSizes = List.copyOf(viaSizes);
    }

    /** A net class: the width its tracks are laid at, how far they keep, and the via they change layer by. */
    public record NetClass(String name, double trackWidth, double clearance, double viaDrill, double viaDiameter) {
    }

    /** Nets a class takes by their name, KiCad's wildcards: {@code *} any run, {@code ?} one character. */
    public record NetPattern(String netClass, String wildcard) {

        /** The names it takes, as a regular expression. */
        public Pattern pattern() {
            return KiCadProjectSettings.wildcard(wildcard);
        }
    }

    public record ViaSize(double drill, double diameter) {
    }

    /**
     * The settings as a project file writes them, the file {@link #of} reads back; what KiCad does not find in it, it takes
     * as its own. A net written into a class is said as a pattern of its whole name — the one way of saying it every KiCad
     * since 7 reads alike —, after the patterns of the settings themselves.
     *
     * @param project what the project is called: its files' name
     */
    public String json(String project) {
        ObjectNode root = JSON.createObjectNode();
        ObjectNode design = root.putObject("board").putObject("design_settings");
        ObjectNode writtenRules = design.putObject("rules");
        rules.forEach(writtenRules::put);
        ArrayNode widths = design.putArray("track_widths");
        // KiCad's first, a nought, stands for "the net class's".
        widths.add(0.0);
        trackWidths.forEach(widths::add);
        ArrayNode vias = design.putArray("via_dimensions");
        vias.addObject().put("diameter", 0.0).put("drill", 0.0);
        viaSizes.forEach(size -> vias.addObject().put("diameter", size.diameter()).put("drill", size.drill()));
        ObjectNode meta = root.putObject("meta");
        meta.put("filename", project + PROJECT_EXTENSION);
        meta.put("version", PROJECT_VERSION);
        ObjectNode net = root.putObject("net_settings");
        ArrayNode writtenClasses = net.putArray("classes");
        usual.ifPresent(found -> written(writtenClasses, new NetClass(USUAL_CLASS, found.trackWidth(), found.clearance(),
                found.viaDrill(), found.viaDiameter())));
        classes.forEach(netClass -> written(writtenClasses, netClass));
        net.putObject("meta").put("version", NET_SETTINGS_VERSION);
        ArrayNode writtenPatterns = net.putArray("netclass_patterns");
        patterns.forEach(pattern -> writtenPatterns.addObject().put("netclass", pattern.netClass()).put("pattern", pattern.wildcard()));
        nets.forEach((name, netClass) -> writtenPatterns.addObject().put("netclass", netClass).put("pattern", name));
        return JSON.writerWithDefaultPrettyPrinter().writeValueAsString(root);
    }

    private static void written(ArrayNode classes, NetClass netClass) {
        classes.addObject().put("name", netClass.name()).put("clearance", netClass.clearance()).put("track_width", netClass.trackWidth())
                .put("via_diameter", netClass.viaDiameter()).put("via_drill", netClass.viaDrill());
    }

    /** The settings a project file holds; {@link #NONE} when it is not one that can be read. */
    public static KiCadProjectSettings of(String json) {
        JsonNode project;
        try {
            project = JSON.readTree(json);
        } catch (JacksonException unreadable) {
            return NONE;
        }
        JsonNode net = project.path("net_settings");
        JsonNode design = project.path("board").path("design_settings");
        Optional<NetClass> usual = Optional.empty();
        List<NetClass> classes = new ArrayList<>();
        for (JsonNode written : net.path("classes")) {
            NetClass read = new NetClass(written.path("name").asString(""), written.path("track_width").asDouble(0),
                    written.path("clearance").asDouble(0), written.path("via_drill").asDouble(0), written.path("via_diameter").asDouble(0));
            if (read.name().equals(USUAL_CLASS)) {
                usual = Optional.of(read);
            } else if (!read.name().isBlank()) {
                classes.add(read);
            }
        }
        return new KiCadProjectSettings(usual, classes, assignments(net.path("netclass_assignments")), patterns(net.path("netclass_patterns")),
                rules(design.path("rules")), trackWidths(design.path("track_widths")), viaSizes(design.path("via_dimensions")));
    }

    /** The class a net is in: written into one, or matching one's pattern; empty for the usual class. */
    public Optional<String> classOf(String net) {
        String written = nets.get(net);
        if (written != null) {
            return Optional.of(written);
        }
        return patterns.stream().filter(pattern -> pattern.pattern().matcher(net).matches()).map(NetPattern::netClass).findFirst();
    }

    /** KiCad 7 writes a net's class as a word, KiCad 8 as a list of classes, the first being its own. */
    private static Map<String, String> assignments(JsonNode written) {
        Map<String, String> nets = new LinkedHashMap<>();
        for (Map.Entry<String, JsonNode> assignment : written.properties()) {
            JsonNode value = assignment.getValue();
            String netClass = value.isArray() ? (value.isEmpty() ? "" : value.get(0).asString("")) : value.asString("");
            if (!netClass.isBlank()) {
                nets.put(assignment.getKey(), netClass);
            }
        }
        return nets;
    }

    private static List<NetPattern> patterns(JsonNode written) {
        List<NetPattern> patterns = new ArrayList<>();
        for (JsonNode pattern : written) {
            patterns.add(new NetPattern(pattern.path("netclass").asString(""), pattern.path("pattern").asString("")));
        }
        return patterns;
    }

    static Pattern wildcard(String written) {
        StringBuilder regular = new StringBuilder();
        for (char letter : written.toCharArray()) {
            switch (letter) {
                case '*' -> regular.append(".*");
                case '?' -> regular.append('.');
                default -> regular.append(Pattern.quote(String.valueOf(letter)));
            }
        }
        return Pattern.compile(regular.toString());
    }

    private static Map<String, Double> rules(JsonNode written) {
        Map<String, Double> rules = new LinkedHashMap<>();
        for (Map.Entry<String, JsonNode> rule : written.properties()) {
            if (rule.getValue().isNumber()) {
                rules.put(rule.getKey(), rule.getValue().asDouble());
            }
        }
        return rules;
    }

    /** The widths it offers; KiCad's first, a nought, stands for "the net class's". */
    private static List<Double> trackWidths(JsonNode written) {
        List<Double> widths = new ArrayList<>();
        for (JsonNode width : written) {
            if (width.asDouble(0) > 0) {
                widths.add(width.asDouble());
            }
        }
        return widths;
    }

    private static List<ViaSize> viaSizes(JsonNode written) {
        List<ViaSize> sizes = new ArrayList<>();
        for (JsonNode size : written) {
            if (size.path("diameter").asDouble(0) > 0) {
                sizes.add(new ViaSize(size.path("drill").asDouble(0), size.path("diameter").asDouble()));
            }
        }
        return sizes;
    }
}
