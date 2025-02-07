package org.jmouse.expression;

import org.jmouse.common.ast.lexer.AbstractLexer;
import org.jmouse.common.ast.token.Token;

import java.util.List;

public class DefaultLexer extends AbstractLexer {

    public DefaultLexer(List<Token.Entry> entries) {
        super(entries);
    }

}
