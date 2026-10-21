package org.jmouse.parallax.script.engine;

import org.jmouse.parallax.script.Block;
import org.jmouse.parallax.script.ExperienceDocument;
import org.jmouse.parallax.script.Meta;
import org.jmouse.parallax.script.Meta.MetaValue;
import org.jmouse.parallax.RepresentationKind;
import org.jmouse.parallax.script.engine.DefinedObject.Preset;
import org.jmouse.parallax.script.engine.DefinedObject.Property;
import org.jmouse.parallax.script.engine.DefinedObject.Relation;
import org.jmouse.parallax.script.engine.DefinedObject.RelationKind;
import org.jmouse.parallax.script.engine.DefinedObject.Representation;
import org.jmouse.parallax.object.ObjectDefinition;
import org.jmouse.parallax.object.ParameterDefinition;
import org.jmouse.units.Quantity;
import org.jmouse.units.Units;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * Reads a {@link DefinedObject} from one jMX document: identity from the head, description and relations from
 * {@code meta}, sliders from the parameters that have a range, the definition through {@link EngineSelection}, and
 * representations and presets from {@code view}.
 */
final class ObjectDocumentReader {

    private ObjectDocumentReader() {
    }

    /** The object a document describes, with the definition it computes. */
    static DefinedObject read(ExperienceDocument document) {
        List<Block.Parameter> rangeless = document.parameters().stream()
                .filter(parameter -> parameter.range().isEmpty()).toList();
        ObjectDefinition definition = EngineSelection.read(document, new DefinitionConstants(rangeless));
        return read(document, definition, sliders(document));
    }

    /**
     * The object around a definition read elsewhere — a component, whose definition is its own source text. Its
     * parameters belong to that source, so the object has no sliders of its own.
     */
    static DefinedObject readAround(ExperienceDocument document, ObjectDefinition definition) {
        return read(document, definition, List.of());
    }

    private static DefinedObject read(ExperienceDocument document, ObjectDefinition definition,
                                      List<ParameterDefinition> parameters) {
        Meta meta = document.meta();
        return new DefinedObject(document.slug(), document.title(), meta.summary(), meta.description(),
                meta.category() == null ? null : meta.category().toUpperCase(Locale.ROOT), meta.tags(),
                meta.field(ObjectDocumentBuilder.FEATURED).map(ObjectDocumentReader::isYes).orElse(false),
                meta.field(ObjectDocumentBuilder.LATEX).map(ObjectDocumentReader::text).orElse(null), definition,
                parameters, properties(document), representations(document), presets(document), relations(document));
    }

    // ─── parameters ─────────────────────────────────────────────────────────────────────────────────

    /**
     * Every parameter with a range is a slider; its description is its label.
     *
     * <p>A slider written with a unit — {@code U = 5 V in 0.1 .. 24 step 0.1 "Input voltage"} — is in SI, and a bare
     * number in its range or step is in the unit its value is written in. The slider is shown in that unit, prefix
     * included, and labelled with it — {@code "Capacitance, µF"} — while its numbers stay in SI: a slider written
     * {@code C = 1 µF in 1 nF .. 10 µF} moves in microfarads and the circuit receives farads.</p>
     */
    private static List<ParameterDefinition> sliders(ExperienceDocument document) {
        List<ParameterDefinition> sliders = new ArrayList<>();
        for (Block.Parameter parameter : document.parameters()) {
            if (parameter.range().isPresent()) {
                sliders.add(slider(parameter));
            }
        }
        return sliders;
    }

    /** One parameter with a range as its slider: value, range, step and values in SI, shown in the unit it is written in. */
    static ParameterDefinition slider(Block.Parameter parameter) {
        String what = "parameter '" + parameter.name() + "'";
        String unit = sliderUnit(parameter);
        double value = parameter.quantity().map(Quantity::value)
                .orElseGet(() -> Notation.parseNumber(parameter.value().text(), what));
        Block.Range range = parameter.range().get();
        double step = parameter.step()
                .map(written -> Notation.parseInUnit(written.text(), "the step of " + what, unit)).orElse(0.0);
        String shown = Units.shown(unit);
        return new ParameterDefinition(parameter.name(), sliderLabel(parameter.description(), shown), value,
                Notation.parseInUnit(range.minimum().text(), "the minimum of " + what, unit),
                Notation.parseInUnit(range.maximum().text(), "the maximum of " + what, unit), step,
                parameter.has(Block.ParameterFlag.ANIMATE), shown,
                unit.isEmpty() ? 1 : Quantity.of(1, unit).map(Quantity::value).orElse(1.0),
                parameter.values().stream().map(written -> Notation.parseInUnit(written.text(), "a value of " + what, unit)).toList());
    }

    /** The unit a slider's value is written in, prefix included; empty for a plain number. */
    private static String sliderUnit(Block.Parameter parameter) {
        return parameter.quantity().map(Quantity::writtenUnit).orElse("");
    }

    /**
     * {@code "Input voltage"} in volts is labelled «Input voltage, V»; a slider without a description keeps its name.
     *
     * @param unit the unit the slider shows its numbers in, as {@link Units#shown} prints it
     */
    private static String sliderLabel(String description, String unit) {
        if (unit.isEmpty() || description.isBlank()) {
            return description;
        }
        return description + ", " + unit;
    }

    // ─── meta ───────────────────────────────────────────────────────────────────────────────────────

    /** The free meta fields that are neither read by jMX itself, nor relations, nor owned by the definition. */
    private static List<Property> properties(ExperienceDocument document) {
        Set<String> owned = EngineSelection.ownedMetaFields(document);
        List<Property> properties = new ArrayList<>();
        document.meta().fields().forEach((field, value) -> {
            if (field.equals(ObjectDocumentBuilder.relationField(RelationKind.USES_SYMBOL))) {
                throw new IllegalArgumentException("'" + document.title() + "' names its symbol in meta; a part is drawn "
                        + "with 'symbol uses <slug> { port pin … }', the one place that choice is written.");
            }
            if (field.startsWith("schematic_layout")) {
                throw new IllegalArgumentException("'" + document.title() + "' keeps its drawing in meta; a circuit is "
                        + "drawn by its 'layout' block: 'layout { R1 at 240, 200 rotate 90 }'.");
            }
            if (ObjectDocumentBuilder.RESERVED_FIELDS.contains(field) || ObjectDocumentBuilder.isRelationField(field)
                    || owned.contains(field)) {
                return;
            }
            String key = field.replace('_', '-');
            String label = document.meta().label(field).orElse(ObjectDocumentBuilder.propertyLabel(key));
            properties.add(new Property(key, label, text(value)));
        });
        return properties;
    }

    /**
     * {@code see_also saddle, wave}: one meta field per kind of relation; and the symbols a component is drawn with,
     * which {@code symbol uses resistor-symbol} names — the default first — and no meta field does.
     */
    private static List<Relation> relations(ExperienceDocument document) {
        List<Relation> relations = new ArrayList<>();
        document.meta().fields().forEach((field, value) -> {
            for (RelationKind kind : RelationKind.values()) {
                if (kind != RelationKind.USES_SYMBOL && ObjectDocumentBuilder.relationField(kind).equals(field)) {
                    names(value).forEach(slug -> relations.add(new Relation(slug, kind)));
                }
            }
        });
        document.block(Block.Symbol.class).ifPresent(symbol -> symbol.uses()
                .forEach(slug -> relations.add(new Relation(slug, RelationKind.USES_SYMBOL))));
        return relations;
    }

    private static boolean isYes(MetaValue value) {
        String text = text(value);
        return text.equals("yes") || text.equals("true");
    }

    /** A free field's value as the text a property shows. */
    static String text(MetaValue value) {
        return switch (value) {
            case MetaValue.Text text -> text.text();
            case MetaValue.Amount amount -> amount.quantity().written();
            case MetaValue.Span span -> span.minimum().written() + " .. " + span.maximum().written();
            case MetaValue.Names names -> String.join(", ", names.names());
            case MetaValue.Items items -> "[" + String.join(", ", items.items().stream().map(ObjectDocumentReader::text).toList()) + "]";
        };
    }

    private static List<String> names(MetaValue value) {
        return switch (value) {
            case MetaValue.Names names -> names.names();
            case MetaValue.Text text -> List.of(text.text());
            default -> throw new IllegalArgumentException("A relation names slugs: 'see_also saddle, wave', not " + text(value) + ".");
        };
    }

    // ─── view ───────────────────────────────────────────────────────────────────────────────────────

    private static List<Block.ViewItem> viewItems(ExperienceDocument document) {
        return document.block(Block.View.class).map(Block.View::items).orElse(List.of());
    }

    private static List<Representation> representations(ExperienceDocument document) {
        List<Representation> representations = new ArrayList<>();
        for (Block.ViewItem item : viewItems(document)) {
            if (item instanceof Block.Representation representation) {
                RepresentationKind kind = DefinitionWords.constant(RepresentationKind.class, representation.representation(),
                        "a representation");
                representations.add(new Representation(kind, representation.primary(),
                        SettingNotation.read(representation.settings().text())));
            }
        }
        return representations;
    }

    /** A preset's bare numbers are in the units their sliders are written in, as the sliders' own bounds are. */
    private static List<Preset> presets(ExperienceDocument document) {
        Map<String, String> units = new LinkedHashMap<>();
        document.parameters().forEach(parameter -> units.put(parameter.name(), sliderUnit(parameter)));
        List<Preset> presets = new ArrayList<>();
        for (Block.ViewItem item : viewItems(document)) {
            if (item instanceof Block.Preset preset) {
                Map<String, Double> values = new LinkedHashMap<>();
                preset.values().forEach((name, value) -> values.put(name, Notation.parseInUnit(value.text(),
                        "preset '" + preset.title() + "' value of '" + name + "'", units.getOrDefault(name, ""))));
                Map<String, Object> camera = preset.camera().map(ObjectDocumentReader::camera).orElse(null);
                presets.add(new Preset(preset.title(), preset.description(), values, camera));
            }
        }
        return presets;
    }

    private static Map<String, Object> camera(Block.Camera camera) {
        Map<String, Object> settings = new LinkedHashMap<>();
        camera.settings().forEach((name, value) -> settings.put(name, SettingNotation.parse(value.text())));
        return settings;
    }
}
