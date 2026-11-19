package org.jmouse.java.bytecode.signature;

import org.jmouse.el.StringSource;
import org.jmouse.el.lexer.Lexer;
import org.jmouse.el.parser.ParseRole;
import org.jmouse.el.parser.ParseRuleRegistry;
import org.jmouse.el.parser.ParseSession;
import org.jmouse.el.parser.ParsingBudget;
import org.jmouse.java.bytecode.vocabulary.JvmFormatLimits;

import java.util.Objects;

/** Shared lifecycle and resource boundary for all decoded JVM type-text grammars. */
public final class JvmTextGrammar {
    private final Lexer lexer;
    private final ParseRuleRegistry rules;
    private final ParsingBudget budget;

    public JvmTextGrammar(Lexer lexer, ParseRuleRegistry rules, ParsingBudget budget) {
        this.lexer = Objects.requireNonNull(lexer, "lexer");
        this.rules = Objects.requireNonNull(rules, "rules").snapshot();
        this.budget = Objects.requireNonNull(budget, "budget");
    }

    public <N extends JvmTextNode> N parse(
            String name, String text, ParseRole<JvmTextInput, N> role) {
        N node = parseEmbedded(lexer, name, text, role, new ParseSession(rules, budget));
        node.freeze();
        return node;
    }

    /** Embedded productions share the classfile session's grammar and total resource budget. */
    public static <N extends JvmTextNode> N parseEmbedded(
            Lexer lexer,
            String name,
            String text,
            ParseRole<JvmTextInput, N> role,
            ParseSession session) {
        Objects.requireNonNull(text, "text");
        if (text.length() > JvmFormatLimits.MAXIMUM_UTF8_BYTES) {
            throw new IllegalArgumentException(
                    "JVM type text exceeds the constant-pool UTF8 limit");
        }
        JvmTextInput input = new JvmTextInput(lexer.tokenize(new StringSource(name, text)));
        N node = session.parse(role, input);
        if (!input.atEnd()) {
            throw input.failure("Unexpected trailing JVM type text");
        }
        return node;
    }
}
