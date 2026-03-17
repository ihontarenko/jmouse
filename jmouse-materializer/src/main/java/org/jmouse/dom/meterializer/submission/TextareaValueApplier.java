package org.jmouse.dom.meterializer.submission;

import org.jmouse.dom.Node;
import org.jmouse.dom.TagName;
import org.jmouse.dom.node.TextNode;

import java.util.ArrayList;
import java.util.List;

/**
 * {@link ControlValueApplier} for {@code <textarea>} elements. 🧩
 *
 * <p>
 * Applies submitted values by replacing the textarea content
 * with a single {@link TextNode}.
 * </p>
 *
 * <p>
 * Any existing child nodes are removed before inserting the new text.
 * </p>
 */
public final class TextareaValueApplier implements ControlValueApplier {

    /**
     * Supports {@code <textarea>} nodes.
     */
    @Override
    public boolean supports(Node node) {
        return node.getTagName() == TagName.TEXTAREA;
    }

    /**
     * Replaces textarea content with the provided value.
     */
    @Override
    public void apply(Node node, Object value) {
        String text = value == null ? "" : String.valueOf(value);

        while (!node.getChildren().isEmpty()) {
            node.removeChild(node.getChildren().getFirst());
        }

        node.append(new TextNode(text));
    }
}