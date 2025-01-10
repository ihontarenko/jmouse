package svit.dom.node;

import svit.dom.NodeType;
import svit.dom.TagName;

public class ElementNode extends AbstractNode {

    public ElementNode(TagName tagName) {
        super(NodeType.ELEMENT, tagName);
    }

}
