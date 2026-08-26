package org.jmouse.mapper.el.parser;

import org.jmouse.el.lexer.BasicToken;
import org.jmouse.el.lexer.TokenCursor;
import org.jmouse.el.lexer.support.SourceReading;
import org.jmouse.el.node.expression.SpanNode;
import org.jmouse.mapper.el.lexer.JmmToken;
import org.jmouse.mapper.el.node.FromNode;
import org.jmouse.mapper.el.node.RefuseNode;
import org.jmouse.mapper.el.node.RuleBlockNode;
import org.jmouse.mapper.el.node.TargetNode;

/**
 * Reads {@code target Order { … }} — the block holding everything about building one type.
 *
 * <p>Four things may appear inside, and each is keyword-led, so nothing has to be guessed:
 * {@code unmapped}, {@code refuse}, {@code always} and {@code from}.</p>
 *
 * <p>⚠️ A target-level {@code refuse} may only be about the target. A source refusal names a source
 * type, and out here there is no source to name — putting one at target level would be asserting about
 * whichever source happened to be used, which is not something a reader could predict.</p>
 *
 * @author Ivan Hontarenko (Mr. Jerry Mouse)
 * @author ihontarenko@gmail.com
 */
public final class TargetParser {

    private TargetParser() {
    }

    /**
     * Reads a target block.
     *
     * @param cursor the cursor, positioned on {@code target}
     * @return the target
     */
    public static TargetNode parse(TokenCursor cursor) {
        TargetNode target = new TargetNode();

        // ⚠️ Stamped here, where the cursor still knows where it is. Everything that refuses a target
        // later — a type that does not resolve, an 'unmapped fail' nothing can satisfy, a refusal that
        // cannot run — has only the node, and a message with no line is a message nobody can act on.
        target.setSpan(SpanNode.of(cursor.current().lineNumber(), SourceReading.column(cursor)));

        cursor.ensure(JmmToken.T_TARGET);
        target.setTargetType(TypeNames.read(cursor));

        cursor.ensure(BasicToken.T_OPEN_CURLY);
        Separators.skip(cursor);

        while (cursor.hasNext() && !cursor.isCurrent(BasicToken.T_CLOSE_CURLY)) {
            readEntry(cursor, target);
            Separators.skip(cursor);
        }

        cursor.ensure(BasicToken.T_CLOSE_CURLY);

        return target;
    }

    /**
     * Reads one construction inside a target block.
     *
     * @param cursor the cursor, positioned on its first token
     * @param target the target being filled
     */
    private static void readEntry(TokenCursor cursor, TargetNode target) {
        if (cursor.isCurrent(JmmToken.T_UNMAPPED)) {
            cursor.ensure(JmmToken.T_UNMAPPED);
            target.setUnmapped(readUnmapped(cursor));
            return;
        }

        if (cursor.isCurrent(JmmToken.T_REFUSE)) {
            RefuseNode refusal = RefuseParser.parse(cursor);

            if (refusal.getSubject() != RefuseNode.Subject.TARGET) {
                throw new JmmSyntaxException(cursor, "a 'refuse source' block names the source it is "
                        + "about, so it belongs inside the 'from' block for that source");
            }

            target.add(refusal);
            return;
        }

        if (cursor.isCurrent(JmmToken.T_ALWAYS)) {
            cursor.ensure(JmmToken.T_ALWAYS);

            if (target.getAlways() != null) {
                throw new JmmSyntaxException(cursor, "'%s' already has an 'always' block"
                        .formatted(target.getTargetType()));
            }

            target.setAlways(RuleBlockParser.parse(cursor));
            return;
        }

        if (cursor.isCurrent(JmmToken.T_FROM)) {
            target.add(readSource(cursor));
            return;
        }

        throw new JmmSyntaxException(cursor, "a target holds 'unmapped', 'refuse', 'always' and 'from' "
                + "— rules go inside one of the last two");
    }

    /**
     * Reads the value of {@code unmapped}.
     *
     * @param cursor the cursor, positioned after the keyword
     * @return what an unfed target property does
     */
    private static TargetNode.Unmapped readUnmapped(TokenCursor cursor) {
        if (cursor.consumeIf(JmmToken.T_FAIL)) {
            return TargetNode.Unmapped.FAIL;
        }

        if (cursor.consumeIf(JmmToken.T_IGNORE)) {
            return TargetNode.Unmapped.IGNORE;
        }

        throw new JmmSyntaxException(cursor, "'unmapped' takes 'fail' or 'ignore'");
    }

    /**
     * Reads {@code from Source { … }} or {@code from Source : expression}.
     *
     * @param cursor the cursor, positioned on {@code from}
     * @return the source block
     */
    private static FromNode readSource(TokenCursor cursor) {
        FromNode source = new FromNode();

        source.setSpan(SpanNode.of(cursor.current().lineNumber(), SourceReading.column(cursor)));

        cursor.ensure(JmmToken.T_FROM);
        source.setSourceType(TypeNames.read(cursor));

        // ⚠️ A whole-pair conversion and a block of rules are alternatives, never both: a block beside
        // a conversion leaves no answer to what the rules were supposed to apply to.
        if (cursor.consumeIf(BasicToken.T_COLON)) {
            source.setConversion(RuleValueReader.read(cursor));
            return source;
        }

        cursor.ensure(BasicToken.T_OPEN_CURLY);
        Separators.skip(cursor);

        // ⚠️ A loop rather than an if, so that a second refusal is refused for being a second one. Read
        // as a single leading block, the one after it falls through to the rule reader and is reported
        // as a rule missing its colon — a message about the wrong thing entirely.
        while (RefuseParser.opensBlock(cursor)) {
            RefuseNode refusal = RefuseParser.parse(cursor);

            if (refusal.getSubject() != RefuseNode.Subject.SOURCE) {
                throw new JmmSyntaxException(cursor, "a 'refuse target' block holds whatever the source "
                        + "is, so it belongs at target level rather than inside one 'from'");
            }

            if (source.getRefusal() != null) {
                throw new JmmSyntaxException(cursor, ("'from %s' already refuses its source — write "
                        + "every condition in the one block, since all of them are evaluated anyway")
                        .formatted(source.getSourceType()));
            }

            source.setRefusal(refusal);
            Separators.skip(cursor);
        }

        RuleBlockNode rules = new RuleBlockNode();

        RuleBlockParser.parseBody(cursor, rules);
        cursor.ensure(BasicToken.T_CLOSE_CURLY);
        source.setRules(rules);

        return source;
    }
}
