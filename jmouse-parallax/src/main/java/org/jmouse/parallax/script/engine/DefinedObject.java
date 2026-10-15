package org.jmouse.parallax.script.engine;

import org.jmouse.parallax.RepresentationKind;
import org.jmouse.parallax.object.ObjectDefinition;
import org.jmouse.parallax.object.ParameterDefinition;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Everything a catalogue object is apart from where it is stored: what it computes, the sliders that vary it, how it
 * is described, shown and related to other objects. A jMX document holds exactly this, so
 * {@link ExperienceDefinitions} turns one into the other in both directions.
 *
 * @param slug          the stable address
 * @param summary       one line under the title; {@code null} for none
 * @param description   longer markdown; {@code null} for none
 * @param category      the catalogue section's code ({@code SURFACES}); {@code null} for none
 * @param formulaLatex  the curated formula, when one is written; {@code null} lets the interface derive it
 * @param parameters    the sliders, in the order shown
 * @param properties    the facts shown beside the object, in the order written
 * @param relations     the objects this one is related to, and how
 */
public record DefinedObject(String slug, String title, String summary, String description, String category,
                            List<String> tags, boolean featured, String formulaLatex, ObjectDefinition definition,
                            List<ParameterDefinition> parameters, List<Property> properties,
                            List<Representation> representations, List<Preset> presets, List<Relation> relations) {

    public DefinedObject {
        tags = List.copyOf(tags);
        parameters = List.copyOf(parameters);
        properties = List.copyOf(properties);
        representations = List.copyOf(representations);
        presets = List.copyOf(presets);
        relations = List.copyOf(relations);
    }

    /** A fact shown beside the object: {@code euler-characteristic} labelled «Euler characteristic», value {@code 2}. */
    public record Property(String key, String label, String value) {
    }

    /**
     * One way of showing the object.
     *
     * @param primary  whether it is what the object opens on
     * @param settings what the representation is told, as JSON would carry it: numbers, words, lists
     */
    public record Representation(RepresentationKind kind, boolean primary, Map<String, Object> settings) {
        public Representation {
            settings = Collections.unmodifiableMap(new LinkedHashMap<>(settings));
        }
    }

    /**
     * A named point of the parameter space worth returning to.
     *
     * @param description what makes it worth returning to; {@code null} for nothing said
     * @param camera      where it is looked at from, as JSON would carry it; {@code null} for the default view
     */
    public record Preset(String name, String description, Map<String, Double> values, Map<String, Object> camera) {
        public Preset {
            description = description == null || description.isBlank() ? null : description;
            values = Collections.unmodifiableMap(new LinkedHashMap<>(values));
            camera = camera == null ? null : Collections.unmodifiableMap(new LinkedHashMap<>(camera));
        }
    }

    /** Another object this one is related to: {@code mobius} is {@link RelationKind#CONSTRUCTED_FROM} a strip. */
    public record Relation(String slug, RelationKind kind) {
    }

    /** How one object is related to another. */
    public enum RelationKind {
        GENERALIZES, SPECIAL_CASE, DEFORMATION_OF, CONSTRUCTED_FROM, DUAL, SIMILAR, SEE_ALSO, USES_SYMBOL
    }
}
