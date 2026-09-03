package org.jmouse.mapper.el.parser;

import org.jmouse.core.Priority;
import org.jmouse.el.lexer.BasicToken;
import org.jmouse.el.lexer.TokenCursor;
import org.jmouse.el.lexer.support.SourceReading;
import org.jmouse.el.node.Node;
import org.jmouse.el.parser.ParserContext;
import org.jmouse.mapper.el.lexer.JmmToken;
import org.jmouse.mapper.el.node.FragmentNode;
import org.jmouse.mapper.el.node.MappingDocumentNode;
import org.jmouse.mapper.el.node.TargetNode;
import org.jmouse.mapper.el.node.UseNode;

/**
 * Reads the {@code mapping "name" { … }} block — the whole of a file, once its trivia is past.
 *
 * <p>⚠️ The <em>root</em> is {@link MappingDocumentParser}, not this. Leading comments, blank lines and
 * whatever follows the closing brace are a question about a <em>file</em>, and
 * {@link org.jmouse.el.language.parser.AbstractBlockParser} rightly knows nothing about files — it
 * ensures its keyword under the cursor and would refuse a document whose first line is a comment.</p>
 *
 * @author Ivan Hontarenko (Mr. Jerry Mouse)
 * @author ihontarenko@gmail.com
 */
@Priority(JmmParserPriority.MAPPING)
public class MappingParser extends JmmBlockParser<MappingDocumentNode, JmmToken> {

    @Override
    protected MappingDocumentNode createNode(TokenCursor cursor, ParserContext context) {
        MappingDocumentNode document = new MappingDocumentNode();

        document.setName(SourceReading.literal(cursor.ensure(BasicToken.T_STRING)));

        return document;
    }

    @Override
    protected JmmToken token() {
        return JmmToken.T_MAPPING;
    }

    @Override
    protected boolean matches(TokenCursor cursor) {
        return cursor.isCurrent(JmmToken.T_MAPPING);
    }

    @Override
    protected void parseBody(TokenCursor cursor, MappingDocumentNode node, ParserContext context) {
        Node children = parseStatements(cursor, context);

        for (Node statement : children.getChildren()) {
            file(cursor, node, statement);
        }
    }

    /**
     * Files one statement onto the document.
     *
     * @param cursor    the cursor, for a failure's line
     * @param node      the document being filled
     * @param statement what dispatch produced
     */
    private void file(TokenCursor cursor, MappingDocumentNode node, Node statement) {
        switch (statement) {
            case UseNode use -> {
                UseNode existing = node.add(use);

                if (existing != null) {
                    throw new JmmSyntaxException(cursor, ("'%s' is imported twice — as '%s' and as "
                            + "'%s'. A file where a name means whichever line came last is a file "
                            + "nobody can read")
                            .formatted(use.getSimpleName(), existing.getQualifiedName(),
                                       use.getQualifiedName()));
                }
            }
            case FragmentNode fragment -> {
                if (node.add(fragment.getName(), fragment) != null) {
                    throw new JmmSyntaxException(cursor,
                            "'%s' is declared twice".formatted(fragment.getName()));
                }
            }
            case TargetNode target -> node.add(target);

            default -> throw new JmmSyntaxException(cursor,
                    "a mapping file holds 'use', 'fragment' and 'target' at the top level");
        }

        if (statement instanceof org.jmouse.el.node.Expression expression) {
            node.addExpression(expression);
        }
    }
}
