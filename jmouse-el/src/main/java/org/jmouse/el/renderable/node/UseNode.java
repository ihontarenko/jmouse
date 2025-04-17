package org.jmouse.el.renderable.node;

import org.jmouse.el.lexer.Token;
import org.jmouse.el.node.AbstractNode;
import org.jmouse.el.node.ExpressionNode;
import org.jmouse.el.node.Visitor;
import org.jmouse.el.node.expression.NameSetNode;
import org.jmouse.el.renderable.NodeVisitor;

public class UseNode extends AbstractNode {

    private ExpressionNode path;
    private Token.Type     type;
    private NameSetNode    names;
    private ExpressionNode alias;

    public ExpressionNode getPath() {
        return path;
    }

    public void setPath(ExpressionNode path) {
        this.path = path;
    }

    public NameSetNode getNames() {
        return names;
    }

    public void setNames(NameSetNode names) {
        this.names = names;
    }

    public ExpressionNode getAlias() {
        return alias;
    }

    public void setAlias(ExpressionNode alias) {
        this.alias = alias;
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

    public Token.Type getType() {
        return type;
    }

    public void setType(Token.Type type) {
        this.type = type;
    }
}
