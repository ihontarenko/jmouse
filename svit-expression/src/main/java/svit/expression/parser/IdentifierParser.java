package svit.expression.parser;

import svit.ast.parser.Parser;
import svit.ast.parser.ParserContext;
import svit.expression.ast.IdentifierNode;
import svit.ast.token.DefaultToken;
import svit.ast.lexer.Lexer;
import svit.ast.node.Node;

public class IdentifierParser implements Parser {

    @Override
    public void parse(Lexer lexer, Node parent, ParserContext context) {
        shift(lexer, DefaultToken.T_IDENTIFIER);
        IdentifierNode node = new IdentifierNode(lexer.current());
        node.setIdentifier(node.entry().value());
        parent.add(node);
    }

}
