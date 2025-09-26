package org.jmouse.el.renderable.node;

import org.jmouse.el.lexer.Token;
import org.jmouse.el.node.AbstractNode;
import org.jmouse.el.node.Expression;
import org.jmouse.el.node.Visitor;
import org.jmouse.el.node.expression.NameSetNode;
import org.jmouse.el.renderable.NodeVisitor;

public class UseNode extends AbstractNode {

    private Expression  path;
    private Token.Type  type;
    private NameSetNode names;
    private Expression  alias;

    public Expression getPath() {
        return path;
    }

    public void setPath(Expression path) {
        this.path = path;
    }

    public NameSetNode getNames() {
        return names;
    }

    public void setNames(NameSetNode names) {
        this.names = names;
    }

    public Expression getAlias() {
        return alias;
    }

    public void setAlias(Expression alias) {
        this.alias = alias;
    }

    public Token.Type getType() {
        return type;
    }

    public void setType(Token.Type type) {
        this.type = type;
    }

    @Override
    public void accept(Visitor visitor) {
        if (visitor instanceof NodeVisitor nv) {
            nv.visit(this);
        }
    }

    @Override
    public String toString() {
        return "USE: %s FOR %s".formatted(path, type);
    }

}
