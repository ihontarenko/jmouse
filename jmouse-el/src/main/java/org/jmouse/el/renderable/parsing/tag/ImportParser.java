package org.jmouse.el.renderable.parsing.tag;

import org.jmouse.el.lexer.BasicToken;
import org.jmouse.el.lexer.Token;
import org.jmouse.el.lexer.TokenCursor;
import org.jmouse.el.node.Node;
import org.jmouse.el.node.expression.literal.StringLiteralNode;
import org.jmouse.el.parser.LiteralParser;
import org.jmouse.el.parser.Parser;
import org.jmouse.el.parser.ParserContext;
import org.jmouse.el.parser.TagParser;
import org.jmouse.el.renderable.lexer.TemplateToken;
import org.jmouse.el.renderable.node.ImportNode;

public class ImportParser implements TagParser {

    @Override
    public Node parse(TokenCursor cursor, ParserContext context) {
        cursor.ensure(TemplateToken.T_IMPORT);

        Parser     parser = context.getParser(LiteralParser.class);
        ImportNode node   = new ImportNode();

        if (parser.parse(cursor, context) instanceof StringLiteralNode string) {
            node.setSource(string);
        }

        if (cursor.isCurrent(TemplateToken.T_AS)) {
            cursor.ensure(TemplateToken.T_AS);
            Token alias = cursor.ensure(BasicToken.T_IDENTIFIER);
            node.setAlias(new StringLiteralNode(alias.value()));
        }

        return node;
    }

    @Override
    public String getName() {
        return "import";
    }

}
