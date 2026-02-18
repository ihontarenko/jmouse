package org.jmouse.dom.blueprint.model;

import org.jmouse.dom.TagName;

import java.util.List;
import java.util.Map;

/**
 * Control contract for inputs, selects, textareas, buttons, and other interactive elements.
 *
 * <p>Controls can be rendered directly, or expanded into more complex structures
 * (for example checkbox group, radio group, and so on).</p>
 *
 * <p>This model is stable and is the recommended boundary for client mapping.</p>
 */
public record ControlModel(
        TagName tagName,
        ControlSemantics semantics,
        String name,
        String label,
        String description,
        Map<String, String> attributes,
        List<OptionModel> options,
        ControlState state
) implements FormNodeModel {

    public ControlModel {
        attributes = attributes == null ? Map.of() : Map.copyOf(attributes);
        options = options == null ? List.of() : List.copyOf(options);
        semantics = semantics == null ? ControlSemantics.none() : semantics;
        state = state == null ? ControlState.empty() : state;
    }

    /**
     * True when this control logically supports options.
     */
    public boolean isOptionable() {
        return !options.isEmpty()
                || tagName == TagName.SELECT
                || semantics.kind() == ControlKind.RADIO_GROUP
                || semantics.kind() == ControlKind.CHECKBOX_GROUP;
    }

    /**
     * True when this control is an input and has an input type.
     */
    public boolean isInput() {
        return tagName == TagName.INPUT;
    }
}
