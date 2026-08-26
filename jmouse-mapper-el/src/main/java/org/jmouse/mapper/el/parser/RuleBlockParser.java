package org.jmouse.mapper.el.parser;

import org.jmouse.el.lexer.BasicToken;
import org.jmouse.el.lexer.TokenCursor;
import org.jmouse.el.lexer.support.SourceReading;
import org.jmouse.el.node.expression.SpanNode;
import org.jmouse.mapper.el.lexer.JmmToken;
import org.jmouse.mapper.el.node.IncludeNode;
import org.jmouse.mapper.el.node.LetNode;
import org.jmouse.mapper.el.node.RuleBlockNode;
import org.jmouse.mapper.el.node.RuleNode;

/**
 * Reads the body of a rule block — the inside of {@code always}, {@code from} and {@code fragment}.
 *
 * <p>Three constructions share this because they differ in where they sit and what they apply to, never
 * in what may be written inside them.</p>
 *
 * <h2>⚠️ A rule's left-hand side may be any keyword</h2>
 *
 * <p>Every word this language reserves is a plausible property name — a target with a property called
 * {@code target}, {@code from}, {@code when} or {@code source} is ordinary. So a name is read with
 * {@link JmmToken#nameTokens()} rather than {@code T_IDENTIFIER}, or such a property becomes one that
 * cannot be written down at all and the only advice left is to rename the field.</p>
 *
 * <p>Nothing is ambiguous despite that: {@code let} and {@code include} are only keywords when a
 * {@code =} or a name follows them and no {@code :} does, and every other line is a rule.</p>
 *
 * <h2>⚠️ A {@code refuse} is caught here rather than read as a rule</h2>
 *
 * <p>A refusal opens the block it guards and may not stand between rules. That constraint is real and
 * worth keeping — a reader meets what stops the mapping before what performs it — but a parser that
 * simply falls through to reading a rule reports it as a missing colon, naming tokens nobody typed. So
 * the line is recognised and refused on its own terms.</p>
 *
 * @author Ivan Hontarenko (Mr. Jerry Mouse)
 * @author ihontarenko@gmail.com
 */
public final class RuleBlockParser {

    private RuleBlockParser() {
    }

    /**
     * Reads a braced block of rules.
     *
     * @param cursor the cursor, positioned on the opening brace
     * @return the block
     */
    public static RuleBlockNode parse(TokenCursor cursor) {
        RuleBlockNode block = new RuleBlockNode();

        cursor.ensure(BasicToken.T_OPEN_CURLY);
        Separators.skip(cursor);
        parseBody(cursor, block);
        cursor.ensure(BasicToken.T_CLOSE_CURLY);

        return block;
    }

    /**
     * Reads entries until the block's closing brace, without touching either brace.
     *
     * <p>⚠️ Exposed because a {@code from} block may open with a {@code refuse} before its rules begin.
     * The alternative was to let the caller consume the brace and then wind the cursor back to it, which
     * only works while the thing in between is exactly one token — and a refusal is a whole block.</p>
     *
     * @param cursor the cursor, positioned on the first entry
     * @param block  the block being filled
     */
    public static void parseBody(TokenCursor cursor, RuleBlockNode block) {
        while (cursor.hasNext() && !cursor.isCurrent(BasicToken.T_CLOSE_CURLY)) {
            readEntry(cursor, block);
            Separators.skip(cursor);
        }
    }

    /**
     * Reads one line of a block: a binding, an include, or a rule.
     *
     * @param cursor the cursor, positioned on the first token of the line
     * @param block  the block being filled
     */
    private static void readEntry(TokenCursor cursor, RuleBlockNode block) {
        if (isBinding(cursor)) {
            block.add(readBinding(cursor));
            return;
        }

        if (isInclude(cursor)) {
            IncludeNode include = new IncludeNode();

            include.setSpan(SpanNode.of(cursor.current().lineNumber(), SourceReading.column(cursor)));

            cursor.ensure(JmmToken.T_INCLUDE);
            include.setName(cursor.ensure(JmmToken.nameTokens()).value());
            block.include(include);

            return;
        }

        if (RefuseParser.opensBlock(cursor)) {
            throw new JmmSyntaxException(cursor, "a 'refuse' block is not a rule and cannot stand "
                    + "between rules — it opens the block it guards, so that whoever reads the file "
                    + "meets what stops the mapping before what performs it. Move it above the first "
                    + "rule of the 'from' whose source it names, or out to target level when it is "
                    + "about the target");
        }

        RuleNode rule     = readRule(cursor);
        RuleNode existing = block.add(rule);

        if (existing != null) {
            throw new JmmSyntaxException(cursor,
                    "'%s' already has a rule in this block".formatted(rule.getProperty()));
        }
    }

    /**
     * Whether this line binds a name — {@code let x = …} rather than a property called {@code let}.
     *
     * @param cursor the cursor to inspect
     * @return {@code true} when it is a binding
     */
    private static boolean isBinding(TokenCursor cursor) {
        return cursor.isCurrent(JmmToken.T_LET)
                && cursor.isNext(JmmToken.nameTokens())
                && !cursor.checkAt(2, BasicToken.T_COLON);
    }

    /**
     * Whether this line pulls in a fragment — {@code include x} rather than a property called
     * {@code include}.
     *
     * @param cursor the cursor to inspect
     * @return {@code true} when it is an include
     */
    private static boolean isInclude(TokenCursor cursor) {
        return cursor.isCurrent(JmmToken.T_INCLUDE)
                && cursor.isNext(JmmToken.nameTokens())
                && !cursor.checkAt(2, BasicToken.T_COLON);
    }

    /**
     * Reads {@code let name = expression}.
     *
     * @param cursor the cursor, positioned on {@code let}
     * @return the binding
     */
    private static LetNode readBinding(TokenCursor cursor) {
        LetNode binding = new LetNode();

        binding.setSpan(SpanNode.of(cursor.current().lineNumber(), SourceReading.column(cursor)));

        cursor.ensure(JmmToken.T_LET);
        binding.setName(cursor.ensure(JmmToken.nameTokens()).value());
        cursor.ensure(BasicToken.T_EQ);
        binding.setExpression(RuleValueReader.read(cursor));

        return binding;
    }

    /**
     * Reads {@code property : value [when condition]} or {@code property : ignore}.
     *
     * @param cursor the cursor, positioned on the property name
     * @return the rule
     */
    private static RuleNode readRule(TokenCursor cursor) {
        RuleNode rule = new RuleNode();

        // ⚠️ Stamped here, where the cursor still knows where it is. A validation failure raised later
        // has only the node, and a message that cannot name a line is a message nobody can act on.
        rule.setSpan(SpanNode.of(cursor.current().lineNumber(), SourceReading.column(cursor)));

        rule.setProperty(SourceReading.dottedName(cursor, JmmToken.nameTokens()));

        if (rule.getProperty().indexOf('.') >= 0) {
            throw new JmmSyntaxException(cursor, ("'%s' is a path, and a rule names one property of the "
                    + "target. A nested target needs the runtime to materialise what is between, which it "
                    + "does not do yet — write the rule on the target's own property")
                    .formatted(rule.getProperty()));
        }

        cursor.ensure(BasicToken.T_COLON);

        if (cursor.isCurrent(JmmToken.T_IGNORE)) {
            cursor.ensure(JmmToken.T_IGNORE);
            rule.setIgnored(true);

            // ⚠️ Refused here, on its own terms. An ignore takes no condition, and the reader used to
            // simply return — leaving the 'when' to be read as the NEXT line's property name, which then
            // failed on a colon that never came. The constraint was enforced by accident, and reported
            // as two tokens nobody typed.
            if (cursor.isCurrent(JmmToken.T_WHEN)) {
                throw new JmmSyntaxException(cursor, ("an 'ignore' is unconditional — a property is "
                        + "either carried or it is not. Write the condition on the rule that would "
                        + "otherwise have written: '%s : <value> when <condition>'")
                        .formatted(rule.getProperty()));
            }

            return rule;
        }

        rule.setValue(RuleValueReader.read(cursor));
        rule.setCondition(RuleValueReader.readCondition(cursor));

        return rule;
    }
}
