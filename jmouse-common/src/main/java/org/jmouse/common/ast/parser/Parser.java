package org.jmouse.common.ast.parser;

import org.jmouse.common.ast.lexer.Lexer;
import org.jmouse.common.ast.node.EntryNode;
import org.jmouse.common.ast.node.Node;
import org.jmouse.common.ast.token.Token;

public interface Parser {

    void parse(Lexer lexer, Node parent, ParserContext context);

    default Node parse(Lexer lexer, ParserContext context) {
        Node node = new EntryNode();

        parse(lexer, node, context);

        return node.first();
    }

    default void ensureNext(Lexer lexer, Token... tokens) {
        if (!lexer.isNext(tokens)) {
            throwSyntaxErrorException(lexer, 1, tokens);
        }
    }

    default void ensureCurrent(Lexer lexer, Token... tokens) {
        if (!lexer.isCurrent(tokens)) {
            throwSyntaxErrorException(lexer, 0, tokens);
        }
    }

    default boolean is(Lexer lexer, Lexer.Checker tester) {
        return tester.test(lexer.lexer());
    }

    default void shift(Lexer lexer, Token... tokens) {
        if (lexer.isNext(tokens)) {
            lexer.next();
        } else {
            throwSyntaxErrorException(lexer, 1, tokens);
        }
    }

    default void throwSyntaxErrorException(Lexer lexer, int offset, Token... token) {
        throw new SyntaxErrorException(this, lexer.lookAhead(offset), token);
    }

    default void throwSyntaxErrorException(Lexer lexer, int offset, String expected) {
        throw new SyntaxErrorException(this, lexer.lookAhead(offset), expected);
    }

}
