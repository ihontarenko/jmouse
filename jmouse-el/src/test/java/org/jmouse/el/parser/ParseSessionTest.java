package org.jmouse.el.parser;

import org.jmouse.el.node.BasicNode;
import org.jmouse.el.lexer.BasicToken;
import org.jmouse.el.lexer.Token;
import org.junit.jupiter.api.Test;
import org.jmouse.core.context.ContextKey;
import java.util.Locale;

import static org.junit.jupiter.api.Assertions.*;

class ParseSessionTest {
    @Test
    void typedContextIsRestoredWhenAScopedGrammarFails() {
        ContextKey<String> context = ContextKey.of("test.context", String.class);
        ParseSession session = new ParseSession(new ParseRuleRegistry(), new ParsingBudget(10, 100));
        session.with(context, "outer", () -> {
            assertEquals("outer", session.context(context));
            assertThrows(ParseException.class, () -> session.with(context, "inner", () -> {
                assertEquals("inner", session.context(context));
                throw new ParseException("intentional");
            }));
            assertEquals("outer", session.context(context));
            return null;
        });
        assertNull(session.context(context));
    }
    private static final ParseRole<String, BasicNode> WORD =
            new ParseRole<>("test.word", String.class, BasicNode.class);

    @Test
    void explicitRoleReplacementDoesNotChangeAnExistingSession() {
        ParseRuleRegistry registry = new ParseRuleRegistry();
        registry.bind(WORD, "original", (input, session) -> node(input));
        ParseSession first = new ParseSession(registry, new ParsingBudget(10, 100));
        registry.replace(WORD, "original", "replacement", (input, session) -> node(input.toUpperCase(Locale.ROOT)));

        assertEquals("word", first.parse(WORD, "word").token().value());
        assertEquals("WORD", new ParseSession(registry, new ParsingBudget(10, 100))
                .parse(WORD, "word").token().value());
        assertThrows(IllegalArgumentException.class,
                () -> registry.bind(WORD, "duplicate", (input, session) -> node(input)));
        assertThrows(IllegalStateException.class,
                () -> first.rules().bind(WORD, "late", (input, session) -> node(input)));
        assertThrows(IllegalArgumentException.class,
                () -> registry.replace(WORD, "original", "lost-update", (input, session) -> node(input)));
    }

    @Test
    void failedNestedRuleRestoresDepthWithoutResettingTheWorkBudget() {
        ParseRole<Integer, BasicNode> nesting = new ParseRole<>("test.nesting", Integer.class, BasicNode.class);
        ParseRuleRegistry registry = new ParseRuleRegistry();
        registry.bind(nesting, "nested", (count, session) -> count == 0
                ? node("done") : session.parse(nesting, count - 1));
        ParseSession session = new ParseSession(registry, new ParsingBudget(4, 100));
        assertThrows(ParsingLimitException.class, () -> session.parse(nesting, 10));
        assertEquals("done", session.parse(nesting, 0).token().value());
        assertEquals(6, session.steps());
    }

    private static BasicNode node(String word) {
        return BasicNode.forToken(new Token(word, BasicToken.T_IDENTIFIER, 0, 0, 1));
    }
}
