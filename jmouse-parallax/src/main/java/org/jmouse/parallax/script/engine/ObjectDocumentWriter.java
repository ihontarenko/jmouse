package org.jmouse.parallax.script.engine;

import org.jmouse.parallax.script.engine.DefinedObject.Preset;
import org.jmouse.parallax.script.engine.DefinedObject.Property;
import org.jmouse.parallax.script.engine.DefinedObject.Relation;
import org.jmouse.parallax.script.engine.DefinedObject.RelationKind;
import org.jmouse.parallax.script.engine.DefinedObject.Representation;
import org.jmouse.parallax.object.ObjectDefinition;
import org.jmouse.parallax.object.ParameterDefinition;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * Writes a {@link DefinedObject} as one jMX document: the head, {@code meta} first, {@code parameters}, the
 * definition's own blocks, then {@code view} — four-space indents, one block per section, as the grammar's examples
 * are written.
 */
final class ObjectDocumentWriter {

    /** Meta fields the catalogue reads itself; a property cannot be called by one of these names. */
    static final Set<String> RESERVED_FIELDS = Set.of("summary", "description", "category", "tags", "source", "authors",
            "license", "links", "featured", "latex");

    static final String FEATURED = "featured";
    static final String LATEX    = "latex";

    private ObjectDocumentWriter() {
    }

    static String write(DefinedObject object) {
        WrittenDefinition definition = DefinitionWriter.write(object.definition());
        StringBuilder text = new StringBuilder(head(object, definition)).append(" {\n");
        List<String> meta = meta(object, definition);
        if (!meta.isEmpty()) {
            text.append(TextBlock.braced("meta", meta).render("    "));
        }
        List<String> parameters = parameters(object.parameters(), definition.constants());
        if (!parameters.isEmpty()) {
            text.append(TextBlock.braced("parameters", parameters).render("    "));
        }
        definition.blocks().forEach(block -> text.append(block.render("    ")));
        List<String> view = view(object);
        if (!view.isEmpty()) {
            requireViewAllowed(object);
            text.append(TextBlock.braced("view", view).render("    "));
        }
        return text.append("}\n").toString();
    }

    private static String head(DefinedObject object, WrittenDefinition definition) {
        return definition.kind() + " " + Notation.text(object.title()) + " as " + object.slug()
                + definition.type().map(type -> " type " + type).orElse("");
    }

    /** A symbol is a drawing: it has meta and nothing it would show or vary. */
    private static void requireViewAllowed(DefinedObject object) {
        if (object.definition() instanceof ObjectDefinition.Symbol) {
            throw new IllegalArgumentException("Symbol '" + object.slug() + "' is a drawing; it has no representations or presets.");
        }
    }

    // ─── meta ───────────────────────────────────────────────────────────────────────────────────────

    private static List<String> meta(DefinedObject object, WrittenDefinition definition) {
        List<String> lines = new ArrayList<>();
        if (object.summary() != null) {
            lines.add("summary " + Notation.longText(object.summary(), ""));
        }
        if (object.description() != null) {
            lines.add("description " + Notation.longText(object.description(), ""));
        }
        if (object.category() != null) {
            lines.add("category " + object.category().toLowerCase(Locale.ROOT));
        }
        if (!object.tags().isEmpty()) {
            lines.add("tags " + String.join(", ", object.tags()));
        }
        if (object.featured()) {
            lines.add(FEATURED + " yes");
        }
        if (object.formulaLatex() != null) {
            lines.add(LATEX + " " + Notation.text(object.formulaLatex()));
        }
        lines.addAll(definition.metaFields());
        object.properties().forEach(property -> lines.add(property(property, definition)));
        relationsByKind(object.relations()).forEach((kind, slugs) -> lines.add(relationField(kind) + " " + String.join(", ", slugs)));
        return lines;
    }

    /**
     * {@code critical_point "(0, 0) — a saddle"}: the key with {@code _} for {@code -}, the label after the value only
     * when the key in words does not already say it.
     */
    private static String property(Property property, WrittenDefinition definition) {
        String field = propertyField(property.key());
        boolean ownedByDefinition = definition.metaFields().stream().anyMatch(line -> line.startsWith(field + " "));
        if (RESERVED_FIELDS.contains(field) || isRelationField(field) || ownedByDefinition) {
            throw new IllegalArgumentException("Property '" + property.key() + "' has the name of a meta field jMX reads itself.");
        }
        String label = property.label().equals(propertyLabel(property.key())) ? "" : " " + Notation.text(property.label());
        return field + " " + Notation.text(property.value()) + label;
    }

    /** {@code euler-characteristic} → {@code euler_characteristic}. */
    static String propertyField(String key) {
        if (!key.matches("[a-z][a-z0-9-]*")) {
            throw new IllegalArgumentException("A property key is lowercase words joined by '-', not '" + key + "'.");
        }
        return key.replace('-', '_');
    }

    /** {@code euler-characteristic} → «Euler characteristic»: the label a key gives when none is written. */
    static String propertyLabel(String key) {
        String words = key.replace('-', ' ').replace('_', ' ');
        return words.isEmpty() ? words : Character.toUpperCase(words.charAt(0)) + words.substring(1);
    }

    /**
     * The relations meta fields carry. {@code USES_SYMBOL} is not one: a component's {@code symbol uses} line says it,
     * and that line belongs to the component's own source.
     */
    private static Map<RelationKind, List<String>> relationsByKind(List<Relation> relations) {
        Map<RelationKind, List<String>> byKind = new LinkedHashMap<>();
        relations.stream().filter(relation -> relation.kind() != RelationKind.USES_SYMBOL)
                .forEach(relation -> byKind.computeIfAbsent(relation.kind(), kind -> new ArrayList<>()).add(relation.slug()));
        return byKind;
    }

    /** {@code SEE_ALSO} → {@code see_also}. */
    static String relationField(RelationKind kind) {
        return DefinitionWords.word(kind);
    }

    /** Whether a meta field is a relation; {@code uses_symbol} is not, since {@code symbol uses} says it. */
    static boolean isRelationField(String field) {
        for (RelationKind kind : RelationKind.values()) {
            if (kind != RelationKind.USES_SYMBOL && relationField(kind).equals(field)) {
                return true;
            }
        }
        return false;
    }

    // ─── parameters ─────────────────────────────────────────────────────────────────────────────────

    /** The sliders, then the definition's constants. */
    private static List<String> parameters(List<ParameterDefinition> sliders, List<String> constants) {
        List<String> lines = new ArrayList<>();
        sliders.forEach(slider -> lines.add(slider(slider)));
        lines.addAll(constants);
        return lines;
    }

    /**
     * {@code A = 1 in 0.2 .. 2 step 0.05 animate "Amplitude"}; the label is left out when it is the name. A slider
     * shown in a unit is written in it — {@code C = 1 µF in 0.001 .. 10 step 0.001 "Capacitance"} — its bare bounds and
     * step in the value's unit, and the unit the reader appends to the label taken off it.
     */
    static String slider(ParameterDefinition parameter) {
        double scale = parameter.scale();
        String unit = parameter.unit().isEmpty() ? "" : " " + parameter.unit();
        StringBuilder line = new StringBuilder(parameter.name()).append(" = ")
                .append(Notation.number(parameter.defaultValue() / scale)).append(unit)
                .append(" in ").append(Notation.number(parameter.minimum() / scale)).append(" .. ")
                .append(Notation.number(parameter.maximum() / scale))
                .append(" step ").append(Notation.number(parameter.step() / scale));
        if (parameter.animatable()) {
            line.append(" animate");
        }
        String label = unit.isEmpty() ? parameter.label()
                : parameter.label().replaceFirst(",\\s*" + Pattern.quote(parameter.unit()) + "$", "");
        if (!label.equals(parameter.name())) {
            line.append(' ').append(Notation.text(label));
        }
        return line.toString();
    }

    // ─── view ───────────────────────────────────────────────────────────────────────────────────────

    private static List<String> view(DefinedObject object) {
        List<String> lines = new ArrayList<>();
        object.representations().forEach(representation -> lines.add(representation(representation)));
        Map<String, Double> scales = new LinkedHashMap<>();
        object.parameters().forEach(parameter -> scales.put(parameter.name(), parameter.scale()));
        object.presets().forEach(preset -> lines.add(preset(preset, scales)));
        return lines;
    }

    /** {@code cross_section axis Y offset 0}, {@code surface primary}. */
    private static String representation(Representation representation) {
        String settings = SettingNotation.write(representation.settings());
        return DefinitionWords.word(representation.kind()) + (representation.primary() ? " primary" : "")
                + (settings.isEmpty() ? "" : " " + settings);
    }

    /**
     * {@code preset "Classic values" "σ = 10, ρ = 28" { sigma = 10, rho = 28 }} on one line; a preset with a camera
     * takes a line for each. A value is written in its slider's unit, as the reader takes a preset's bare number.
     *
     * @param scales how many SI units one shown unit of each slider is
     */
    private static String preset(Preset preset, Map<String, Double> scales) {
        String opening = "preset " + Notation.text(preset.name())
                + (preset.description() == null ? "" : " " + Notation.text(preset.description()));
        List<String> values = new ArrayList<>();
        preset.values().forEach((name, value) -> values.add(name + " = "
                + Notation.number(value / scales.getOrDefault(name, 1.0))));
        if (preset.camera() == null) {
            return opening + " { " + String.join(", ", values) + " }";
        }
        List<String> lines = new ArrayList<>();
        if (!values.isEmpty()) {
            lines.add(String.join(", ", values));
        }
        lines.add("camera " + SettingNotation.write(preset.camera()));
        return opening + " {\n    " + String.join("\n    ", lines) + "\n}";
    }
}
