package org.jmouse.el.renderable.node;

import org.jmouse.core.MimeParser;
import org.jmouse.el.node.AbstractNode;
import org.jmouse.el.node.Visitor;
import org.jmouse.el.renderable.NodeVisitor;

public class URLNode extends AbstractNode {

    private String rawURL;

    public String getRawURL() {
        return rawURL;
    }

    public void setRawURL(String rawURL) {
        this.rawURL = MimeParser.unquote(rawURL);
    }

    @Override
    public void accept(Visitor visitor) {
        if (visitor instanceof NodeVisitor nv) {
            nv.visit(this);
        } else {
            visitor.visit(this);
        }
    }

    @Override
    public String toString() {
        return "URL: %s".formatted(rawURL);
    }

}
