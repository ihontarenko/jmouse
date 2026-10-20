package org.jmouse.parallax.script.form;

import org.jmouse.parallax.script.Block;
import org.jmouse.parallax.script.ExperienceDocument;
import org.jmouse.parallax.script.ExperienceReader;
import org.jmouse.parallax.script.Kind;
import org.jmouse.parallax.script.Meta;
import org.jmouse.parallax.script.SourceText;
import org.jmouse.units.Quantity;
import org.jmouse.units.Units;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * The codec of every block whose lines have fields, and the one for a line kept as written.
 *
 * <p>The words a field allows come from the reader's own enumerations — {@link Block.ParameterFlag},
 * {@link Block.OutputType} — so a flag the reader learns is a flag the form offers, with nothing restated here.</p>
 */
final class LineCodecs {

    private static final Map<String, LineCodec> BY_BLOCK = Map.of(
            "meta", new MetaLine(),
            "parameters", new ParameterLine(),
            "inputs", new InputLine(),
            "outputs", new OutputLine(),
            "state", new StateLine(),
            "equations", new EquationLine(),
            "symbol", new PortLine());

    static final LineCodec AS_WRITTEN = new WrittenLine();

    private LineCodecs() {
    }

    /** The codec a block's lines are edited with; a block without one keeps each line as written. */
    static LineCodec of(String block) {
        return BY_BLOCK.getOrDefault(block, AS_WRITTEN);
    }

    static boolean hasFields(String block) {
        return BY_BLOCK.containsKey(block);
    }

    // ─── reading one line ───────────────────────────────────────────────────────────────────────────

    /** The one-line document a line is read in: the line, in its block, in a document of the line's kind. */
    static ExperienceDocument fragment(Kind kind, String block, String line) {
        return ExperienceReader.readOne(fragmentHead(kind, block) + line + "\n}\n}\n", "line");
    }

    /** What stands in front of the line in its one-line document: offsets the reader reports are past it. */
    private static String fragmentHead(Kind kind, String block) {
        return kind.keyword() + " \"Form\" {\n" + block + " {\n";
    }

    /**
     * A value exactly as the line writes it, unit included — {@code 1 kOhm} stays {@code 1 kOhm} and {@code 4.7 µF}
     * stays {@code 4.7 µF}, so a line nobody changed is written back unchanged. A unit is one word: the reader joins
     * its tokens only where nothing separates them.
     */
    static String asWrittenIn(String line, Kind kind, String block, SourceText value, Optional<Quantity> quantity) {
        int start = value.offset() - fragmentHead(kind, block).length();
        if (start < 0 || !line.startsWith(value.text(), start)) {
            return authored(value, quantity);
        }
        int end = start + value.text().length();
        if (quantity.map(Quantity::unit).filter(unit -> !unit.isEmpty()).isPresent()) {
            int unitStart = end;
            while (unitStart < line.length() && line.charAt(unitStart) == ' ') {
                unitStart++;
            }
            int unitEnd = unitStart;
            while (unitEnd < line.length() && !Character.isWhitespace(line.charAt(unitEnd))) {
                unitEnd++;
            }
            end = unitEnd;
        }
        return line.substring(start, end);
    }

    /** A top-level statement read on its own: {@code uses lm358}, {@code symbol uses resistor-symbol}. */
    static ExperienceDocument statement(Kind kind, String statement) {
        return ExperienceReader.readOne(kind.keyword() + " \"Form\" {\n" + statement + "\n}\n", "line");
    }

    // ─── how values are written ─────────────────────────────────────────────────────────────────────

    /** A value with its unit, as an author types it: {@code 4.7 nF} keeps its number, {@code uF} becomes {@code µF}. */
    static String authored(SourceText value, Optional<Quantity> quantity) {
        String unit = quantity.map(LineCodecs::writtenUnit).orElse("");
        if (unit.isEmpty()) {
            return value.text();
        }
        return value.text() + (unit.equals("%") ? "" : " ") + unit;
    }

    private static String writtenUnit(Quantity quantity) {
        if (quantity.unit().isEmpty()) {
            return "";
        }
        return Units.shown(quantity.written().replaceFirst("^[-+0-9.eE]+\\s*", ""));
    }

    /** A text in quotes; one holding a double quote is written in single quotes. */
    static String quoted(String text) {
        return text.contains("\"") ? "'" + text + "'" : "\"" + text + "\"";
    }

    static String value(Map<String, String> values, String name) {
        String value = values.get(name);
        return value == null ? "" : value.strip();
    }

    /** Words of a field that holds several, separated by commas or spaces. */
    static List<String> words(Map<String, String> values, String name) {
        return Arrays.stream(value(values, name).split("[,\\s]+")).filter(word -> !word.isBlank()).toList();
    }

    /** {@code  in a .. b}, when either bound is given. */
    static String range(Map<String, String> values) {
        String minimum = value(values, "minimum");
        String maximum = value(values, "maximum");
        return minimum.isEmpty() && maximum.isEmpty() ? "" : " in " + minimum + " .. " + maximum;
    }

    static String description(Map<String, String> values) {
        String description = value(values, "description");
        return description.isEmpty() ? "" : " " + quoted(description);
    }

    static String word(Enum<?> constant) {
        return constant.name().toLowerCase(Locale.ROOT).replace('_', ' ');
    }

    static List<String> wordsOf(Enum<?>[] constants) {
        return Arrays.stream(constants).map(LineCodecs::word).toList();
    }

    private static final FieldSchema MINIMUM = FieldSchema.of("minimum", "From", FieldType.EXPRESSION);
    private static final FieldSchema MAXIMUM = FieldSchema.of("maximum", "To", FieldType.EXPRESSION);
    private static final FieldSchema STEP = FieldSchema.of("step", "Step", FieldType.EXPRESSION);
    private static final FieldSchema DESCRIPTION = FieldSchema.of("description", "Description", FieldType.TEXT);

    // ─── parameters ─────────────────────────────────────────────────────────────────────────────────

    /** {@code R = 10 kΩ in 100 Ω .. 1 MΩ step 10 Ω log "Resistance"}. */
    private static final class ParameterLine implements LineCodec {

        private static final List<String> FLAGS = wordsOf(Block.ParameterFlag.values());

        @Override
        public List<FieldSchema> fields() {
            return List.of(FieldSchema.required("name", "Name", FieldType.NAME),
                    FieldSchema.required("value", "Value", FieldType.EXPRESSION), MINIMUM, MAXIMUM, STEP,
                    FieldSchema.flags("flags", "Flags", FLAGS), DESCRIPTION);
        }

        @Override
        public List<FormField> read(Kind kind, String line) {
            Block.Parameter parameter = fragment(kind, "parameters", line).parameters().getFirst();
            List<FieldSchema> fields = fields();
            return List.of(new FormField(fields.get(0), parameter.name()),
                    new FormField(fields.get(1), asWrittenIn(line, kind, "parameters", parameter.value(), parameter.quantity())),
                    new FormField(fields.get(2), parameter.range().map(range -> range.minimum().text()).orElse("")),
                    new FormField(fields.get(3), parameter.range().map(range -> range.maximum().text()).orElse("")),
                    new FormField(fields.get(4), parameter.step().map(SourceText::text).orElse("")),
                    new FormField(fields.get(5), parameter.flags().stream().sorted().map(LineCodecs::word)
                            .collect(Collectors.joining(" "))),
                    new FormField(fields.get(6), parameter.description()));
        }

        @Override
        public String write(Map<String, String> values, String indent) {
            StringBuilder line = new StringBuilder(value(values, "name")).append(" = ").append(value(values, "value"));
            line.append(range(values));
            if (!value(values, "step").isEmpty()) {
                line.append(" step ").append(value(values, "step"));
            }
            List<String> flags = words(values, "flags");
            for (String flag : FLAGS) {
                if (flag.equals("per unit") ? value(values, "flags").contains("per unit") : flags.contains(flag)) {
                    line.append(' ').append(flag);
                }
            }
            return line.append(description(values)).toString();
        }
    }

    // ─── inputs ─────────────────────────────────────────────────────────────────────────────────────

    /** {@code x in -3 .. 3}, {@code (z) in -2 - 2 i .. 2 + 2 i}, {@code n in 1 .. 50 integer}. */
    private static final class InputLine implements LineCodec {

        private static final List<String> FLAGS = List.of("integer", "periodic", "complex");

        @Override
        public List<FieldSchema> fields() {
            return List.of(FieldSchema.required("names", "Names", FieldType.NAMES), MINIMUM, MAXIMUM, STEP,
                    FieldSchema.flags("flags", "Flags", FLAGS), DESCRIPTION);
        }

        @Override
        public List<FormField> read(Kind kind, String line) {
            Block.Input input = fragment(kind, "inputs", line).block(Block.Inputs.class).orElseThrow().inputs().getFirst();
            List<String> flags = new ArrayList<>();
            if (input.integer()) {
                flags.add("integer");
            }
            if (input.periodic()) {
                flags.add("periodic");
            }
            if (input.complex()) {
                flags.add("complex");
            }
            List<FieldSchema> fields = fields();
            return List.of(new FormField(fields.get(0), String.join(", ", input.names())),
                    new FormField(fields.get(1), input.range().map(range -> range.minimum().text()).orElse("")),
                    new FormField(fields.get(2), input.range().map(range -> range.maximum().text()).orElse("")),
                    new FormField(fields.get(3), input.step().map(SourceText::text).orElse("")),
                    new FormField(fields.get(4), String.join(" ", flags)),
                    new FormField(fields.get(5), input.description()));
        }

        @Override
        public String write(Map<String, String> values, String indent) {
            List<String> names = words(values, "names");
            List<String> flags = words(values, "flags");
            StringBuilder line = new StringBuilder(names.size() > 1 || flags.contains("complex")
                    ? "(" + String.join(", ", names) + ")" : String.join("", names));
            line.append(range(values));
            if (!value(values, "step").isEmpty()) {
                line.append(" step ").append(value(values, "step"));
            }
            for (String flag : List.of("integer", "periodic")) {
                if (flags.contains(flag)) {
                    line.append(' ').append(flag);
                }
            }
            return line.append(description(values)).toString();
        }
    }

    // ─── outputs ────────────────────────────────────────────────────────────────────────────────────

    /** {@code z : surface = A sin(k x)}, {@code boundary : surface where …}, {@code estimate : series}. */
    private static final class OutputLine implements LineCodec {

        @Override
        public List<FieldSchema> fields() {
            return List.of(FieldSchema.required("name", "Name", FieldType.NAME),
                    FieldSchema.word("type", "Type", false, wordsOf(Block.OutputType.values())),
                    FieldSchema.of("gluing", "Glued as", FieldType.NAME),
                    FieldSchema.of("expression", "Equals", FieldType.EXPRESSION),
                    FieldSchema.of("condition", "Where", FieldType.EXPRESSION), DESCRIPTION);
        }

        @Override
        public List<FormField> read(Kind kind, String line) {
            Block.Output output = fragment(kind, "outputs", line).outputs().getFirst();
            List<String> components = output.components().stream().map(SourceText::text).toList();
            String expression = components.size() > 1 ? "(" + String.join(", ", components) + ")" : String.join("", components);
            List<FieldSchema> fields = fields();
            return List.of(new FormField(fields.get(0), output.name()),
                    new FormField(fields.get(1), output.type().map(LineCodecs::word).orElse("")),
                    new FormField(fields.get(2), output.gluing().orElse("")),
                    new FormField(fields.get(3), expression),
                    new FormField(fields.get(4), output.equation().map(SourceText::text).orElse("")),
                    new FormField(fields.get(5), output.description()));
        }

        @Override
        public String write(Map<String, String> values, String indent) {
            StringBuilder line = new StringBuilder(value(values, "name"));
            if (!value(values, "type").isEmpty()) {
                line.append(" : ").append(value(values, "type"));
                if (!value(values, "gluing").isEmpty()) {
                    line.append(" glued ").append(value(values, "gluing"));
                }
            }
            if (!value(values, "expression").isEmpty()) {
                line.append(" = ").append(value(values, "expression"));
            } else if (!value(values, "condition").isEmpty()) {
                line.append(" where ").append(value(values, "condition"));
            }
            return line.append(description(values)).toString();
        }
    }

    // ─── state ──────────────────────────────────────────────────────────────────────────────────────

    /** {@code x = 1 continuous in -25 .. 25 "…"}. */
    private static final class StateLine implements LineCodec {

        @Override
        public List<FieldSchema> fields() {
            return List.of(FieldSchema.required("name", "Name", FieldType.NAME),
                    FieldSchema.required("start", "Starts at", FieldType.EXPRESSION),
                    FieldSchema.word("nature", "Changes", true, List.of("continuous", "discrete")), MINIMUM, MAXIMUM,
                    DESCRIPTION);
        }

        @Override
        public List<FormField> read(Kind kind, String line) {
            Block.StateVariable variable = fragment(kind, "state", line).block(Block.State.class).orElseThrow()
                    .variables().getFirst();
            List<FieldSchema> fields = fields();
            return List.of(new FormField(fields.get(0), variable.name()),
                    new FormField(fields.get(1), variable.initial().text()),
                    new FormField(fields.get(2), variable.discrete() ? "discrete" : "continuous"),
                    new FormField(fields.get(3), variable.range().map(range -> range.minimum().text()).orElse("")),
                    new FormField(fields.get(4), variable.range().map(range -> range.maximum().text()).orElse("")),
                    new FormField(fields.get(5), variable.description()));
        }

        @Override
        public String write(Map<String, String> values, String indent) {
            String nature = value(values, "nature").isEmpty() ? "continuous" : value(values, "nature");
            return value(values, "name") + " = " + value(values, "start") + " " + nature + range(values)
                    + description(values);
        }
    }

    // ─── equations ──────────────────────────────────────────────────────────────────────────────────

    /** {@code d(x)/dt = sigma * (y - x)}: one equation, checked by the reader, edited whole. */
    private static final class EquationLine implements LineCodec {

        private static final FieldSchema EQUATION = FieldSchema.required("equation", "Equation", FieldType.EXPRESSION);

        @Override
        public List<FieldSchema> fields() {
            return List.of(EQUATION);
        }

        @Override
        public List<FormField> read(Kind kind, String line) {
            fragment(kind, "equations", line);
            return List.of(new FormField(EQUATION, line.strip()));
        }

        @Override
        public String write(Map<String, String> values, String indent) {
            return value(values, "equation");
        }
    }

    // ─── a component's symbol: port and pin ─────────────────────────────────────────────────────────

    /** {@code a terminal_one}: the symbol's port, then the component's pin it is. */
    private static final class PortLine implements LineCodec {

        @Override
        public List<FieldSchema> fields() {
            return List.of(FieldSchema.required("port", "Symbol port", FieldType.NAME),
                    FieldSchema.required("pin", "Component pin", FieldType.NAME));
        }

        @Override
        public List<FormField> read(Kind kind, String line) {
            Map<String, String> ports = statement(kind, "symbol uses form {\n" + line + "\n}")
                    .block(Block.Symbol.class).orElseThrow().ports();
            Map.Entry<String, String> port = ports.entrySet().iterator().next();
            List<FieldSchema> fields = fields();
            return List.of(new FormField(fields.get(0), port.getKey()), new FormField(fields.get(1), port.getValue()));
        }

        @Override
        public String write(Map<String, String> values, String indent) {
            return value(values, "port") + " " + value(values, "pin");
        }
    }

    // ─── meta ───────────────────────────────────────────────────────────────────────────────────────

    /**
     * One line of {@code meta}: a known field with its own type — the category a reference to a {@code category}
     * document, the description Markdown — or a free field, {@code name value ["label"]}.
     */
    private static final class MetaLine implements LineCodec {

        private static final FieldSchema KEY = FieldSchema.required("key", "Field", FieldType.NAME);
        private static final Map<String, FieldSchema> KNOWN = new java.util.LinkedHashMap<>();

        static {
            KNOWN.put("summary", FieldSchema.required("value", "Summary", FieldType.TEXT));
            KNOWN.put("description", FieldSchema.of("value", "Description", FieldType.LONG_TEXT));
            KNOWN.put("category", FieldSchema.reference("value", "Section", FieldType.REFERENCE, true,
                    List.of(Kind.CATEGORY.keyword())));
            KNOWN.put("tags", FieldSchema.of("value", "Tags", FieldType.NAMES));
            KNOWN.put("source", FieldSchema.of("value", "Source", FieldType.TEXT));
            KNOWN.put("license", FieldSchema.of("value", "License", FieldType.TEXT));
        }
        private static final FieldSchema FREE_VALUE = FieldSchema.required("value", "Value", FieldType.EXPRESSION);
        private static final FieldSchema FREE_LABEL = FieldSchema.of("label", "Shown as", FieldType.TEXT);

        /**
         * The key, then each known field under its own name, then a free field's value and label. A line itself carries
         * {@code key} and {@code value} (and a free field's {@code label}): the known field decides the value's type.
         */
        @Override
        public List<FieldSchema> fields() {
            List<FieldSchema> fields = new ArrayList<>(List.of(KEY));
            KNOWN.forEach((key, schema) -> fields.add(new FieldSchema(key, schema.label(), schema.type(),
                    schema.required(), schema.words(), schema.kinds())));
            fields.addAll(List.of(FREE_VALUE, FREE_LABEL));
            return fields;
        }

        @Override
        public List<FormField> read(Kind kind, String line) {
            String key = line.strip().split("\\s+", 2)[0];
            if (key.equals("links") || key.equals("authors")) {
                fragment(kind, "meta", line);
                return AS_WRITTEN.read(kind, line);
            }
            Meta meta = fragment(kind, "meta", line).meta();
            FormField keyField = new FormField(KEY, key);
            FieldSchema known = KNOWN.get(key);
            if (known != null) {
                String value = switch (key) {
                    case "summary" -> meta.summary();
                    case "description" -> meta.description();
                    case "category" -> meta.category();
                    case "tags" -> String.join(", ", meta.tags());
                    case "source" -> meta.source();
                    default -> meta.license();
                };
                return List.of(keyField, new FormField(known, value));
            }
            String rest = line.strip().substring(key.length()).strip();
            Optional<String> label = meta.label(key);
            if (label.isPresent()) {
                rest = rest.replaceFirst("\\s*(\"[^\"]*\"|'[^']*')\\s*$", "");
            }
            return List.of(keyField, new FormField(FREE_VALUE, rest), new FormField(FREE_LABEL, label.orElse("")));
        }

        @Override
        public String write(Map<String, String> values, String indent) {
            String key = value(values, "key");
            String value = value(values, "value");
            return switch (key) {
                case "summary", "source", "license" -> key + " " + quoted(value);
                case "description" -> key + " " + longText(values.getOrDefault("value", ""), indent);
                case "category" -> key + " " + value;
                case "tags" -> key + " " + String.join(", ", words(values, "value"));
                default -> key + " " + value + (value(values, "label").isEmpty() ? "" : " " + quoted(value(values, "label")));
            };
        }

        /** A text on one line in quotes; one that spans lines in {@code """ … """}, indented under its field. */
        private static String longText(String text, String indent) {
            String stripped = text.strip();
            if (!stripped.contains("\n") && !stripped.contains("\"")) {
                return "\"" + stripped + "\"";
            }
            String inner = indent + "    ";
            String body = Arrays.stream(stripped.split("\n", -1))
                    .map(line -> line.isBlank() ? "" : inner + line.stripTrailing())
                    .collect(Collectors.joining("\n"));
            return "\"\"\"\n" + body + "\n" + inner + "\"\"\"";
        }
    }

    // ─── a line kept as written ─────────────────────────────────────────────────────────────────────

    /** A line no narrower codec describes — a view item, a part, a pin — edited as the text it is. */
    private static final class WrittenLine implements LineCodec {

        private static final FieldSchema LINE = FieldSchema.required("line", "Line", FieldType.CODE);

        @Override
        public List<FieldSchema> fields() {
            return List.of(LINE);
        }

        @Override
        public List<FormField> read(Kind kind, String line) {
            return List.of(new FormField(LINE, line.strip()));
        }

        @Override
        public String write(Map<String, String> values, String indent) {
            return value(values, "line");
        }
    }
}
