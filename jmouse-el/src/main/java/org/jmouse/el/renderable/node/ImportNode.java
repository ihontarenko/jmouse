package org.jmouse.el.renderable.node;

import org.jmouse.el.node.AbstractNode;

public class ImportNode extends AbstractNode {

    private String alias;
    private String source;

    public String getAlias() {
        return alias;
    }

    public void setAlias(String alias) {
        this.alias = alias;
    }

    public String getSource() {
        return source;
    }

    public void setSource(String source) {
        this.source = source;
    }
}
