package org.jmouse.dom.node;

import org.jmouse.dom.NodeType;

public final class CDataNode extends TextNode {

    public CDataNode(String text) {
        super(text);
    }

    @Override
    public NodeType getNodeType() {
        return NodeType.CDATA;
    }
}