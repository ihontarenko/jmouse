package org.jmouse.expression.ast;

import org.jmouse.common.ast.token.Token;

public class ClassNameNode extends LiteralNode {

    private String className;

    public ClassNameNode(Token.Entry entry) {
        super(entry);
    }

    public String getClassName() {
        return className;
    }

    public void setClassName(String className) {
        this.className = className;
    }

}
