package org.jmouse.parallax.store.catalogue;

import org.jmouse.parallax.RepresentationKind;
import org.jmouse.parallax.object.ObjectDefinition;
import org.jmouse.parallax.object.ParameterDefinition;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * An object as submitted: by an editor of the catalogue, or by somebody defining an object in their workspace. The
 * whole object is replaced at once, so what is saved is always something that was checked together.
 *
 * <p>The object is its jMX text. A draft either carries that text in {@code source} — and every other field is then
 * ignored — or the fields a form builds, which the server writes as text through the adapter before keeping it.
 * The second form exists for the editors that still build a definition object; they move to the text itself.</p>
 */
public record ObjectDraft(String source, String slug, String title, String summary, String description, String category,
                          String formulaLatex, Boolean featured, ObjectDefinition definition,
                          List<ParameterDefinition> parameters, List<PropertyDraft> properties,
                          List<RepresentationDraft> representations, List<RelationDraft> relations, Set<String> tags,
                          List<PresetDraft> presets) {

    public ObjectDraft {
        // Absent means not featured: a draft that does not mention it is not refused for it.
        featured = Boolean.TRUE.equals(featured);
        parameters = parameters == null ? List.of() : List.copyOf(parameters);
        properties = properties == null ? List.of() : List.copyOf(properties);
        representations = representations == null ? List.of() : List.copyOf(representations);
        relations = relations == null ? List.of() : List.copyOf(relations);
        tags = tags == null ? Set.of() : Set.copyOf(tags);
        presets = presets == null ? List.of() : List.copyOf(presets);
    }

    public record PropertyDraft(String key, String label, String value) {
    }

    /** One entry of the default representation stack; `primary` marks the view that opens first. */
    public record RepresentationDraft(RepresentationKind kind, boolean primary, Map<String, Object> settings) {
    }

    /** A relation to another object, named by its slug so a seed file can refer forwards. */
    public record RelationDraft(String slug, String kind) {
    }

    public record PresetDraft(String name, String description, Map<String, Double> values, Map<String, Object> camera) {
    }
}
