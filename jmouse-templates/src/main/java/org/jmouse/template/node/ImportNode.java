package org.jmouse.template.node;

import org.jmouse.el.node.AbstractNode;

public class ImportNode extends AbstractNode {

    private String alias;
    private String template;

    public String getAlias() {
        return alias;
    }

    public void setAlias(String alias) {
        this.alias = alias;
    }

    public String getTemplate() {
        return template;
    }

    public void setTemplate(String template) {
        this.template = template;
    }
}
