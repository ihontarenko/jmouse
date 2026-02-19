package org.jmouse.dom.template.model;

import org.jmouse.dom.TagName;

import java.util.List;
import java.util.Map;

/**
 * Base node contract for form composition.
 *
 * <p>Blueprints should render this structure into a {@code Node} tree.</p>
 */
public sealed interface FormNodeModel
        permits FormNodeModel.ElementModel,
                FormNodeModel.TextModel,
                ControlModel,
                FormNodeModel.FragmentModel {

    /**
     * Arbitrary tag-based node with attributes and children.
     *
     * <p>Use this for layout, groups, wrappers, fieldsets, and any structural nodes.</p>
     */
    record ElementModel(
            TagName tagName,
            Map<String, String> attributes,
            List<FormNodeModel> children
    ) implements FormNodeModel {

        public ElementModel {
            attributes = attributes == null ? Map.of() : Map.copyOf(attributes);
            children = children == null ? List.of() : List.copyOf(children);
        }
    }

    /**
     * Text node model.
     */
    record TextModel(String text) implements FormNodeModel {
        public TextModel {
            text = text == null ? "" : text;
        }
    }

    /**
     * Placeholder node that indicates "include another blueprint block".
     *
     * <p>This is NOT the same as {@code Blueprint.IncludeBlueprint}; this is a data-level include request.
     * A blueprint can interpret it and decide how to map it into include at IR level.</p>
     *
     * <p>Useful for client-defined composition:
     * header/footer blocks, action bars, reusable field templates, and so on.</p>
     */
    record FragmentModel(
            String fragmentKey,
            Object model
    ) implements FormNodeModel {

        public FragmentModel {
            fragmentKey = fragmentKey == null ? "" : fragmentKey;
        }
    }
}
