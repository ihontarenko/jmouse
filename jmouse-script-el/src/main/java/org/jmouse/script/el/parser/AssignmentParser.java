package org.jmouse.script.el.parser;

import org.jmouse.core.Priority;
import org.jmouse.el.lexer.BasicToken;
import org.jmouse.el.lexer.TokenCursor;
import org.jmouse.el.node.Node;
import org.jmouse.el.parser.AbstractParser;
import org.jmouse.el.parser.ParserContext;
import org.jmouse.script.el.CursorMatcher;
import org.jmouse.script.el.ScriptParseException;
import org.jmouse.script.el.SourceReader;
import org.jmouse.script.el.node.AssignmentNode;

/**
 * Parses {@code entry.state = 'moving'}.
 *
 * <h2>⚠️ Why {@code ==} is refused here rather than parsed elsewhere</h2>
 *
 * <p>The lexer answers {@code =} and {@code ==} with one token type, so this parser is offered both and
 * has to tell them apart by what was written. It could decline the second and let it fall through to
 * the expression parser — and that is exactly the wrong thing to do: a comparison standing alone as a
 * statement parses, evaluates, throws its answer away and reports nothing, so the script keeps loading
 * and the behaviour silently never changes state.</p>
 *
 * <p>A typed {@code ==} where an assignment was meant is the most likely mistake in this language.
 * Turning it into a sentence at load, with a line and a column, costs one branch.</p>
 *
 * @author Ivan Hontarenko (Mr. Jerry Mouse)
 * @author ihontarenko@gmail.com
 */
@Priority(ParserPriority.ASSIGNMENT)
public class AssignmentParser extends AbstractParser {

    @Override
    public void parse(TokenCursor cursor, Node parent, ParserContext context) {
        AssignmentNode node = new AssignmentNode();

        node.setSpan(SourceReader.span(cursor));
        node.setPath(SourceReader.propertyPath(cursor));

        if (!CursorMatcher.assignsAt(cursor, 0)) {
            throw new ScriptParseException(
                    SourceReader.at(cursor),
                    ("'%s' compares two values and this statement assigns one; write a single '=' "
                            + "to set '%s', or move the comparison into an 'if'")
                            .formatted(cursor.current().value(), node.getPath())
            );
        }

        cursor.ensure(BasicToken.T_EQ);
        node.setValue(Expressions.read(cursor, context));

        parent.add(node);
    }

    @Override
    public boolean supports(TokenCursor cursor) {
        return CursorMatcher.assignment().matches(cursor);
    }

}
