package org.jmouse.dom.node;

import org.jmouse.dom.Node;
import org.jmouse.dom.NodeType;
import org.jmouse.dom.TagName;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

abstract public class NonElementNode extends AbstractNode {

    public NonElementNode(NodeType nodeType) {
        super(nodeType, null);
    }

    @Override
    public int getDepth() {
        return depth;
    }

    @Override
    public void setDepth(int depth) {
        this.depth = depth;
    }

    @Override
    public void prepend(Node child) {
        throw new UnsupportedOperationException("Node [%s] can to have child nodes".formatted(this));
    }

    @Override
    public void append(Node child) {
        throw new UnsupportedOperationException("Node [%s] can to have child nodes".formatted(this));
    }

    @Override
    public void removeChild(Node child) {
        children.remove(child);
    }

    @Override
    public List<Node> getChildren() {
        return new ArrayList<>();
    }

    @Override
    public Node getParent() {
        return parent;
    }

    @Override
    public void setParent(Node parent) {
        this.parent = parent;
    }

    @Override
    public NodeType getNodeType() {
        return nodeType;
    }

    @Override
    public TagName getTagName() {
        return tagName;
    }

    @Override
    public void addAttribute(String key, String value) {
        attributes.put(key, value);
    }

    @Override
    public Map<String, String> getAttributes() {
        return attributes;
    }

}
