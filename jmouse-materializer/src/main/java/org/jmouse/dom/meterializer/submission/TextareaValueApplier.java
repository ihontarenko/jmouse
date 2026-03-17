package org.jmouse.dom.meterializer.submission;

import org.jmouse.dom.Node;
import org.jmouse.dom.TagName;
import org.jmouse.dom.node.TextNode;

import java.util.ArrayList;
import java.util.List;

public final class TextareaValueApplier implements ControlValueApplier {

    @Override
    public boolean supports(Node node) {
        return node.getTagName() == TagName.TEXTAREA;
    }

    @Override
    public void apply(Node node, Object value) {
        String     text     = value == null ? "" : String.valueOf(value);
        List<Node> children = new ArrayList<>(node.getChildren());

        for (Node child : children) {
            node.removeChild(child);
        }

        node.append(new TextNode(text));
    }
}