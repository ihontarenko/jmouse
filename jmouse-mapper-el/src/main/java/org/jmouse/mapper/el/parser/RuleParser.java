package org.jmouse.mapper.el.parser;

import org.jmouse.core.Priority;
import org.jmouse.el.lexer.BasicToken;
import org.jmouse.el.lexer.TokenCursor;
import org.jmouse.el.lexer.support.SourceReading;
import org.jmouse.el.node.Node;
import org.jmouse.el.parser.AbstractParser;
import org.jmouse.el.parser.ParserContext;
import org.jmouse.mapper.el.lexer.JmmToken;
import org.jmouse.mapper.el.node.RuleNode;

/**
 * Reads {@code property : value [when condition]} or {@code property : ignore}.
 *
 * <p>The default statement of a rule block: a line is a rule when it is nothing else. That is why it
 * sorts last in {@link JmmParserPriority}.</p>
 *
 * @author Ivan Hontarenko (Mr. Jerry Mouse)
 * @author ihontarenko@gmail.com
 */
@Priority(JmmParserPriority.RULE)
public class RuleParser extends AbstractParser {

    @Override
    public void parse(TokenCursor cursor, Node parent, ParserContext context) {
        RuleNode rule = new RuleNode();

        // ⚠️ Stamped here, where the cursor still knows where it is. A validation failure raised later
        // has only the node, and a message that cannot name a line is a message nobody can act on.
        rule.setSpan(JmmSpans.at(cursor));
        rule.setProperty(SourceReading.dottedName(cursor, JmmToken.nameTokens()));

        if (rule.getProperty().indexOf('.') >= 0) {
            throw new JmmSyntaxException(cursor, ("'%s' is a path, and a rule names one property of the "
                    + "target. A nested target needs the runtime to materialise what is between, which "
                    + "it does not do yet — write the rule on the target's own property")
                    .formatted(rule.getProperty()));
        }

        cursor.ensure(BasicToken.T_COLON);

        if (cursor.consumeIf(JmmToken.T_IGNORE)) {
            rule.setIgnored(true);

            // ⚠️ Refused here, on its own terms. An ignore takes no condition, and simply returning left
            // the 'when' to be read as the NEXT line's property name, which then failed on a colon that
            // never came — the constraint enforced by accident and reported as tokens nobody typed.
            if (cursor.isCurrent(JmmToken.T_WHEN)) {
                throw new JmmSyntaxException(cursor, ("an 'ignore' is unconditional — a property is "
                        + "either carried or it is not. Write the condition on the rule that would "
                        + "otherwise have written: '%s : <value> when <condition>'")
                        .formatted(rule.getProperty()));
            }

            parent.add(rule);

            return;
        }

        rule.setValue(RuleValueReader.read(cursor));
        rule.setCondition(RuleValueReader.readCondition(cursor));

        parent.add(rule);
    }

    /**
     * Whether this line is a rule.
     *
     * <p>A name — any name, keywords included — followed by a colon. ⚠️ That is what makes a target
     * property called {@code from}, {@code when} or {@code refuse} writable: every keyword-led parser
     * asks the opposite question first, so by the time this one is offered the line, the word in front
     * of the colon can only be a property name.</p>
     */
    @Override
    public boolean supports(TokenCursor cursor) {
        return cursor.isCurrent(JmmToken.nameTokens());
    }
}
