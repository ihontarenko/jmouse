package org.jmouse.dom.node;

import org.jmouse.dom.NodeType;
import org.jmouse.dom.TagName;

public class ElementNode extends AbstractNode {

    public ElementNode(TagName tagName) {
        super(NodeType.ELEMENT, tagName);
    }

}
