package org.jmouse.common.dom.node;

import org.jmouse.common.dom.NodeType;
import org.jmouse.common.dom.TagName;

public class ElementNode extends AbstractNode {

    public ElementNode(TagName tagName) {
        super(NodeType.ELEMENT, tagName);
    }

}
