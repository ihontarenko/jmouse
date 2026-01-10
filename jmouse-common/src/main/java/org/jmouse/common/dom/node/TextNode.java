package org.jmouse.common.dom.node;

import org.jmouse.common.dom.NodeType;

public class TextNode extends NonElementNode {

    private String text;

    public TextNode() {
        this(null);
    }

    public TextNode(String text) {
        super(NodeType.TEXT);
        this.text = text;
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

}
