package org.jmouse.el;

import org.jmouse.el.evaluation.EvaluationContext;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * <strong>Does this language still mean what everybody thinks it means?</strong>
 *
 * <p>The first tests this module has ever had, and they exist because of how the last two defects
 * were found: not here, but three levels up, by a subject area in a product that had stopped working.
 * {@code !=} threw where {@code ==} answered, and {@code and} evaluated an operand it had already been
 * told it did not need — each harmless-looking, and together enough to refuse every request in a
 * workspace.
 *
 * <p>⚠️ <strong>This is a smoke test and it is meant to stay one.</strong> It is broad and shallow on
 * purpose: one assertion per thing anybody writing an expression assumes without checking. What it is
 * for is the failure that would otherwise reach a product — an operator that changed its mind, a
 * precedence that shifted, a null that started throwing.
 *
 * <p>Everything here goes through {@link ExpressionLanguage#evaluate} with a plain context, so it
 * tests the engine as a caller meets it rather than through internals a refactor may move.
 */
class ExpressionSmokeTest {

    private final ExpressionLanguage el = new ExpressionLanguage();

    private Object evaluate(String expression) {
        return el.evaluate(expression);
    }

    private Object evaluate(String expression, Map<String, Object> values) {
        EvaluationContext context = el.newContext();
        values.forEach(context::setValue);
        return el.evaluate(expression, context);
    }

    // ── Literals and arithmetic ───────────────────────────────────────────────

    @Nested
    @DisplayName("literals and arithmetic")
    class Arithmetic {

        @Test
        @DisplayName("numbers, strings and booleans come back as themselves")
        void literals() {
            assertEquals(42, evaluate("42"));
            assertEquals("hello", evaluate("'hello'"));
            assertEquals(true, evaluate("true"));
            assertEquals(false, evaluate("false"));
        }

        @Test
        @DisplayName("the four operations, and the one that is not division")
        void operations() {
            assertEquals(7, evaluate("3 + 4"));
            assertEquals(-1, evaluate("3 - 4"));
            assertEquals(12, evaluate("3 * 4"));
            assertEquals(2, evaluate("8 / 4"));
        }

        /**
         * ⚠️ Multiplication binds tighter than addition. Stated as an assertion rather than assumed,
         * because a precedence table is exactly the kind of thing an unrelated change reshuffles.
         */
        @Test
        @DisplayName("⚠️ precedence: `*` binds tighter than `+`")
        void precedence() {
            assertEquals(14, evaluate("2 + 3 * 4"));
            assertEquals(20, evaluate("(2 + 3) * 4"));
        }

        @Test
        @DisplayName("`+` on strings joins them")
        void concatenation() {
            assertEquals("abc", evaluate("'ab' + 'c'"));
        }
    }

    // ── Comparison ────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("comparison")
    class Comparison {

        @Test
        @DisplayName("the ordering operators")
        void ordering() {
            assertEquals(true,  evaluate("3 < 4"));
            assertEquals(false, evaluate("4 < 3"));
            assertEquals(true,  evaluate("4 >= 4"));
            assertEquals(true,  evaluate("3 <= 4"));
        }

        @Test
        @DisplayName("equality on numbers and on strings")
        void equality() {
            assertEquals(true,  evaluate("3 == 3"));
            assertEquals(false, evaluate("3 == 4"));
            assertEquals(true,  evaluate("'a' == 'a'"));
            assertEquals(false, evaluate("'a' == 'b'"));
        }

        /**
         * ⚠️ <strong>The defect this module was fixed for, from both sides.</strong>
         *
         * <p>{@code !=} used to be {@code !left.equals(right)} with no null check while {@code ==}
         * handled null carefully — so the two <em>disagreed</em> about the same pair, and one of them
         * threw. It is now the exact negation, and both directions are asserted because "null throws"
         * was never the real problem; the disagreement was.
         */
        @Test
        @DisplayName("⚠️ `!=` is the exact negation of `==`, null included")
        void notEqualNegatesEqual() {
            Map<String, Object> nothing = Map.of();

            assertDoesNotThrow(() -> evaluate("missing != 'ASSET'", nothing),
                    "`!=` throws on a null left operand again. Where an authorization rule reads it, "
                    + "that makes a condition unanswerable — and an unanswerable deny is applied.");

            assertEquals(true,  evaluate("missing != 'ASSET'", nothing));
            assertEquals(false, evaluate("missing == 'ASSET'", nothing));

            // The two must never agree about the same pair, whatever the pair is.
            for (String pair : List.of("3 %s 3", "3 %s 4", "'a' %s 'a'", "'a' %s 'b'")) {
                assertEquals(
                        evaluate(pair.formatted("==")),
                        !(Boolean) evaluate(pair.formatted("!=")),
                        "`==` and `!=` disagree about " + pair.formatted("…"));
            }
        }

        @Test
        @DisplayName("null equals null, and equals nothing else")
        void nullAgainstNull() {
            assertEquals(true,  evaluate("missing == absent", Map.of()));
            assertEquals(false, evaluate("missing != absent", Map.of()));
        }
    }

    // ── Logic ─────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("logic")
    class Logic {

        @Test
        @DisplayName("the truth table")
        void truthTable() {
            assertEquals(true,  evaluate("true and true"));
            assertEquals(false, evaluate("true and false"));
            assertEquals(true,  evaluate("false or true"));
            assertEquals(false, evaluate("false or false"));
        }

        /**
         * ⚠️ <strong>The second defect, and the one with teeth.</strong>
         *
         * <p>{@code and} used to evaluate its right operand even when the left had already decided, so
         * the commonest defensive shape in the language — a cheap test in front of a fragile operand —
         * guarded nothing. The right-hand side here cannot survive being evaluated; if the assertion
         * passes, it was never reached.
         */
        @Test
        @DisplayName("⚠️ `and` stops at the first false, so a fragile right operand is never reached")
        void andShortCircuits() {
            assertEquals(false, evaluate("false and missing.deeper.still == 1", Map.of()),
                    "`and` reached its right operand after the left was already false.");
        }

        /** The mirror of it: {@code or} stops at the first true. */
        @Test
        @DisplayName("⚠️ `or` stops at the first true")
        void orShortCircuits() {
            assertEquals(true, evaluate("true or missing.deeper.still == 1", Map.of()),
                    "`or` reached its right operand after the left was already true.");
        }

        /**
         * ⚠️ And it must still <em>reach</em> the right operand when the left has not decided —
         * a short-circuit that fires too eagerly is the same bug wearing the opposite hat.
         */
        @Test
        @DisplayName("but each still reads the right operand when the left has not decided")
        void shortCircuitDoesNotFireTooEarly() {
            assertEquals(true,  evaluate("true and 1 == 1"));
            assertEquals(false, evaluate("false or 1 == 2"));
        }

        /**
         * ⚠️ <strong>The one answer short-circuiting changed, pinned so it stays deliberate.</strong>
         *
         * <p>{@code LogicalCalculator} answers {@code false} whenever <em>either</em> operand is not a
         * {@link Boolean}, so {@code true or 'text'} used to be {@code false} — a decided {@code true}
         * dragged down by a right-hand side that was never boolean. Short-circuiting cannot preserve
         * that, because the point is not to look at the right operand at all.
         *
         * <p>The new answer is the right one. But it is a change, and it belongs in a test rather than
         * in somebody's surprise.
         */
        @Test
        @DisplayName("⚠️ `true or <non-boolean>` is true — it used to be false, and that was the bug")
        void theOneAnswerThatChanged() {
            assertEquals(true, evaluate("true or 'text'"));
            assertEquals(true, evaluate("true or 5"));

            // ⚠️ And the AND side is unaffected: false either way, by both routes.
            assertEquals(false, evaluate("false and 'text'"));
            assertEquals(false, evaluate("true and 'text'"),
                    "A non-boolean right operand still drags an undecided AND down, exactly as before "
                    + "— short-circuiting never reaches this case.");
        }

        @Test
        @DisplayName("⚠️ precedence: `and` binds tighter than `or`")
        void logicalPrecedence() {
            // false or (true and false) — not (false or true) and false, which would also be false,
            // so the pair below is what actually distinguishes them.
            assertEquals(true, evaluate("true or true and false"));
            assertEquals(false, evaluate("(true or true) and false"));
        }
    }

    // ── Values from the context ───────────────────────────────────────────────

    @Nested
    @DisplayName("values")
    class Values {

        @Test
        @DisplayName("a bound name reads its value")
        void boundNames() {
            assertEquals("equipment", evaluate("area", Map.of("area", "equipment")));
            assertEquals(true, evaluate("area == 'equipment'", Map.of("area", "equipment")));
        }

        /**
         * ⚠️ An unbound name is null rather than an error, which is what makes a rule about one route
         * safe to evaluate on another. It is also why {@code !=} being null-safe matters so much.
         */
        @Test
        @DisplayName("⚠️ an unbound name is null, not an error")
        void unboundNames() {
            assertNull(evaluate("nobodyPublishedThis", Map.of()));
        }

        @Test
        @DisplayName("a property of a bound object")
        void propertyPaths() {
            assertEquals("DRAFT", evaluate("entry.status", Map.of("entry", Map.of("status", "DRAFT"))));
        }
    }

    // ── The shapes rules are actually written in ──────────────────────────────

    @Nested
    @DisplayName("the shapes real rules use")
    class RealShapes {

        /**
         * ⚠️ <strong>The exact rule that took a subject area off the air</strong>, and the reason this
         * class exists at all. It must answer — not throw — on a call that published none of what it
         * reads.
         */
        @Test
        @DisplayName("⚠️ a guarded rule answers on a call that publishes none of what it reads")
        void aGuardedRuleAnswersEverywhere() {
            String rule = "action == 'entry.listByPurpose' and area == 'equipment' "
                          + "and purpose != 'ASSET' and purpose != 'HOLDER'";

            assertFalse((Boolean) evaluate(rule, Map.of("area", "equipment")),
                    "A rule about one action held on a call with no action at all.");

            assertTrue((Boolean) evaluate(rule, Map.of(
                            "action", "entry.listByPurpose",
                            "area", "equipment",
                            "purpose", "FEEDBACK")),
                    "…and it must still refuse what it was written to refuse.");

            assertFalse((Boolean) evaluate(rule, Map.of(
                            "action", "entry.listByPurpose",
                            "area", "equipment",
                            "purpose", "ASSET")),
                    "…and must not refuse what it was written to permit.");
        }

        @Test
        @DisplayName("a range check, the way a threshold gets written")
        void aRangeCheck() {
            assertEquals(true,  evaluate("value > 2 and value < 8", Map.of("value", 5)));
            assertEquals(false, evaluate("value > 2 and value < 8", Map.of("value", 9)));
        }
    }
}
