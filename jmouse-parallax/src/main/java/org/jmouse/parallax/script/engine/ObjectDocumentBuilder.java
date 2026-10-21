package org.jmouse.parallax.script.engine;

import org.jmouse.parallax.object.ObjectDefinition;
import org.jmouse.parallax.object.ParameterDefinition;
import org.jmouse.parallax.script.Block;
import org.jmouse.parallax.script.Block.Parameter;
import org.jmouse.parallax.script.Block.ParameterFlag;
import org.jmouse.parallax.script.ExperienceDocument;
import org.jmouse.parallax.script.Meta;
import org.jmouse.parallax.script.Meta.MetaValue;
import org.jmouse.parallax.script.SourceText;
import org.jmouse.parallax.script.engine.DefinedObject.Preset;
import org.jmouse.parallax.script.engine.DefinedObject.Property;
import org.jmouse.parallax.script.engine.DefinedObject.Relation;
import org.jmouse.parallax.script.engine.DefinedObject.RelationKind;
import org.jmouse.parallax.script.engine.DefinedObject.Representation;
import org.jmouse.units.Quantity;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Pattern;

import static org.jmouse.parallax.script.engine.DefinitionBlocks.written;

/**
 * A {@link DefinedObject} as the jMX document that says it: the head, {@code meta}, {@code parameters}, the
 * definition's own blocks ({@link DefinitionBlocks}), then {@code view} — the model
 * {@link org.jmouse.parallax.script.writer.DocumentWriter} writes, never its text.
 */
final class ObjectDocumentBuilder {

    /** Meta fields the catalogue reads itself; a property cannot be called by one of these names. */
    static final Set<String> RESERVED_FIELDS = Set.of("summary", "description", "category", "tags", "source", "authors",
            "license", "links", "featured", "latex");

    static final String FEATURED = "featured";
    static final String LATEX    = "latex";

    /** What {@code featured} says when an object is: the word the reader takes as a yes. */
    private static final String YES = "yes";

    private ObjectDocumentBuilder() {
    }

    static ExperienceDocument documentOf(DefinedObject object) {
        DefinitionBlocks.Mapped mapped = DefinitionBlocks.of(object.definition());
        List<Block> blocks = new ArrayList<>();
        List<Parameter> parameters = new ArrayList<>(object.parameters().stream().map(ObjectDocumentBuilder::slider).toList());
        parameters.addAll(mapped.constants());
        if (!parameters.isEmpty()) {
            blocks.add(new Block.Parameters(parameters));
        }
        blocks.addAll(mapped.blocks());
        List<Block.ViewItem> view = view(object);
        if (!view.isEmpty()) {
            requireViewAllowed(object);
            blocks.add(new Block.View(view));
        }
        return new ExperienceDocument(mapped.kind(), object.title(), object.slug(), Optional.empty(), meta(object, mapped),
                Optional.empty(), List.of(), List.of(), blocks, 0);
    }

    /** A symbol or a footprint is a drawing: it has meta and nothing it would show or vary. */
    private static void requireViewAllowed(DefinedObject object) {
        if (object.definition() instanceof ObjectDefinition.Symbol || object.definition() instanceof ObjectDefinition.Footprint) {
            throw new IllegalArgumentException("'" + object.slug() + "' is a drawing; it has no representations or presets.");
        }
    }

    // ─── meta ───────────────────────────────────────────────────────────────────────────────────────

    /** The known fields, then {@code featured}, {@code latex}, the definition's own fields, the properties, the relations. */
    private static Meta meta(DefinedObject object, DefinitionBlocks.Mapped mapped) {
        Map<String, MetaValue> fields = new LinkedHashMap<>();
        Map<String, String> labels = new LinkedHashMap<>();
        if (object.featured()) {
            fields.put(FEATURED, new MetaValue.Names(List.of(YES)));
        }
        if (object.formulaLatex() != null) {
            fields.put(LATEX, new MetaValue.Text(object.formulaLatex()));
        }
        fields.putAll(mapped.metaFields());
        for (Property property : object.properties()) {
            String field = propertyField(property.key());
            if (RESERVED_FIELDS.contains(field) || isRelationField(field) || mapped.metaFields().containsKey(field)) {
                throw new IllegalArgumentException("Property '" + property.key() + "' has the name of a meta field jMX reads itself.");
            }
            fields.put(field, new MetaValue.Text(property.value()));
            // The label after the value only when the key in words does not already say it.
            if (!property.label().equals(propertyLabel(property.key()))) {
                labels.put(field, property.label());
            }
        }
        relationsByKind(object.relations()).forEach((kind, slugs) -> fields.put(relationField(kind), new MetaValue.Names(slugs)));
        String category = object.category() == null ? null : object.category().toLowerCase(Locale.ROOT);
        return new Meta(object.summary(), object.description(), category, object.tags(), null, List.of(), null, List.of(),
                fields, labels);
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
     * The relations meta fields carry. {@code USES_SYMBOL} and {@code USES_FOOTPRINT} are not: a component's
     * {@code symbol uses} and {@code footprint} lines say them, and those lines belong to the component's own source.
     */
    private static Map<RelationKind, List<String>> relationsByKind(List<Relation> relations) {
        Map<RelationKind, List<String>> byKind = new LinkedHashMap<>();
        relations.stream().filter(relation -> !relation.kind().writtenByLine())
                .forEach(relation -> byKind.computeIfAbsent(relation.kind(), kind -> new ArrayList<>()).add(relation.slug()));
        return byKind;
    }

    /** {@code SEE_ALSO} → {@code see_also}. */
    static String relationField(RelationKind kind) {
        return DefinitionWords.word(kind);
    }

    /** Whether a meta field is a relation; {@code uses_symbol} and {@code uses_footprint} are not, since lines say them. */
    static boolean isRelationField(String field) {
        for (RelationKind kind : RelationKind.values()) {
            if (!kind.writtenByLine() && relationField(kind).equals(field)) {
                return true;
            }
        }
        return false;
    }

    // ─── parameters ─────────────────────────────────────────────────────────────────────────────────

    /**
     * {@code A = 1 in 0.2 .. 2 step 0.05 animate "Amplitude"}; the label is left out when it is the name. A slider
     * shown in a unit is written in it — {@code C = 1 µF in 0.001 .. 10 step 0.001 "Capacitance"} — its bare bounds and
     * step in the value's unit, and the unit the reader appends to the label taken off it.
     */
    static Parameter slider(ParameterDefinition parameter) {
        double scale = parameter.scale();
        double shown = parameter.defaultValue() / scale;
        Optional<Quantity> quantity = parameter.unit().isEmpty() ? Quantity.parse(Notation.number(shown))
                : Quantity.of(Double.parseDouble(Notation.number(shown)), parameter.unit());
        Block.Range range = new Block.Range(written(Notation.number(parameter.minimum() / scale)),
                written(Notation.number(parameter.maximum() / scale)));
        Set<ParameterFlag> flags = parameter.animatable() ? Set.of(ParameterFlag.ANIMATE) : Set.of();
        String label = parameter.unit().isEmpty() ? parameter.label()
                : parameter.label().replaceFirst(",\\s*" + Pattern.quote(parameter.unit()) + "$", "");
        return new Parameter(parameter.name(), written(Notation.number(shown)), quantity, Optional.of(range),
                Optional.of(written(Notation.number(parameter.step() / scale))), flags,
                label.equals(parameter.name()) ? "" : label);
    }

    // ─── view ───────────────────────────────────────────────────────────────────────────────────────

    private static List<Block.ViewItem> view(DefinedObject object) {
        List<Block.ViewItem> items = new ArrayList<>();
        object.representations().forEach(representation -> items.add(representation(representation)));
        Map<String, Double> scales = new LinkedHashMap<>();
        object.parameters().forEach(parameter -> scales.put(parameter.name(), parameter.scale()));
        object.presets().forEach(preset -> items.add(preset(preset, scales)));
        return items;
    }

    /** {@code cross_section axis Y offset 0}, {@code surface primary}. */
    private static Block.Representation representation(Representation representation) {
        return new Block.Representation(DefinitionWords.word(representation.kind()), representation.primary(),
                written(SettingNotation.write(representation.settings())));
    }

    /**
     * {@code preset "Classic values" "σ = 10, ρ = 28" { sigma = 10, rho = 28 }}, and where it looks from when it has a
     * camera. A value is written in its slider's unit, as the reader takes a preset's bare number.
     *
     * @param scales how many SI units one shown unit of each slider is
     */
    private static Block.Preset preset(Preset preset, Map<String, Double> scales) {
        Map<String, SourceText> values = new LinkedHashMap<>();
        preset.values().forEach((name, value) -> values.put(name, written(Notation.number(value / scales.getOrDefault(name, 1.0)))));
        Optional<Block.Camera> camera = Optional.ofNullable(preset.camera()).map(settings -> {
            Map<String, SourceText> written = new LinkedHashMap<>();
            settings.forEach((name, value) -> written.put(name, written(SettingNotation.value(value))));
            return new Block.Camera(written);
        });
        return new Block.Preset(preset.name(), preset.description() == null ? "" : preset.description(), values, camera);
    }
}
