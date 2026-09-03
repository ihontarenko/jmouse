package org.jmouse.script.el.budget;

import org.jmouse.el.evaluation.DefaultEvaluationContext;
import org.jmouse.el.evaluation.EvaluationContext;
import org.jmouse.script.el.ScriptEvaluator;
import org.jmouse.script.el.host.BoundScript;
import org.jmouse.script.el.host.FacadeLookup;
import org.jmouse.script.el.host.ScriptBinder;
import org.jmouse.script.el.host.ScriptCatalogue;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.List;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * <strong>Can a script that will not stop be stopped?</strong>
 *
 * <p>The budget is what makes a tenant-authored script safe to run on a request thread, and the whole of
 * it rests on two things being true: that the four limits actually fire, and that a document's own
 * number can never beat the host's. Both are asserted here rather than demonstrated on a console, because
 * a security boundary shown in printed output is a boundary nobody notices breaking.</p>
 *
 * <p>⚠️ The facades and events below are about a shop counter on purpose. Nothing in this module may name
 * a unit, a tile or a mission — see the module's own smoke for the same rule.</p>
 *
 * @author Ivan Hontarenko (Mr. Jerry Mouse)
 * @author ihontarenko@gmail.com
 */
class ScriptBudgetTest {

    private final ScriptEvaluator evaluator = new ScriptEvaluator();
    private final ScriptCatalogue catalogue = ScriptCatalogue.builder()
            .facade("store", new Store())
            .event("opened")
            .build();

    /**
     * Binds a source under a ceiling and a request, and dispatches {@code opened} once.
     *
     * @param source    the script
     * @param ceiling   the most it may ask for
     * @param requested what it asked for
     */
    private void dispatch(String source, ScriptBudget ceiling, ScriptBudget requested) {
        BoundScript       bound   = new ScriptBinder(catalogue, ceiling)
                .bind(evaluator.parse(source, "test.jms"), requested);
        EvaluationContext context = context(bound);

        try {
            bound.begin(context);
            bound.handlersFor("opened").forEach(handler -> handler.evaluate(context));
        } finally {
            bound.finish(context);
        }
    }

    private EvaluationContext context(BoundScript bound) {
        DefaultEvaluationContext context = new DefaultEvaluationContext();

        context.setExtensions(evaluator.getExtensions());
        context.setBeanLookup(new FacadeLookup(catalogue));
        bound.installFunctions(context);

        return context;
    }

    private static final String WALKS_A_LONG_LIST = """
            script "walks" {
                on opened do
                    for step in @store.numbers() do
                        @store.pending()
                    end
                end
            }
            """;

    private static final String CALLS_ITSELF = """
            script "deep" {
                function down(step)
                    return down(step)
                end

                on opened do
                    @store.take(down(1))
                end
            }
            """;

    @Nested
    @DisplayName("the arithmetic — a request held to a ceiling")
    class Clamping {

        @Test
        @DisplayName("takes the lower of the two, limit by limit")
        void lowerOfTheTwo() {
            ScriptBudget requested = ScriptBudget.builder().steps(1_000_000).loopIterations(5).build();
            ScriptBudget ceiling   = ScriptBudget.builder().steps(10).loopIterations(500).build();
            ScriptBudget effective = requested.clampTo(ceiling);

            assertEquals(10, effective.steps(), "the ceiling is lower and must win");
            assertEquals(5, effective.loopIterations(), "the request is lower and must be honoured");
        }

        @Test
        @DisplayName("⚠️ remembers which limit a ceiling decided, so a refusal can say so")
        void remembersTheClamp() {
            ScriptBudget effective = ScriptBudget.builder().steps(1_000_000).loopIterations(5).build()
                    .clampTo(ScriptBudget.builder().steps(10).loopIterations(500).build());

            assertTrue(effective.wasClamped(ScriptLimit.STEPS));
            assertFalse(effective.wasClamped(ScriptLimit.LOOP_ITERATIONS),
                        "the script asked for this one itself, and raising it would help");
        }

        @Test
        @DisplayName("a host with no ceiling changes nothing")
        void noCeiling() {
            ScriptBudget requested = ScriptBudget.builder().steps(7).build();

            assertSame(requested, requested.clampTo(ScriptBudget.unlimited()));
            assertSame(requested, requested.clampTo(null));
        }

        @Test
        @DisplayName("a ceiling applies to a document that asked for nothing")
        void requestedNothing() {
            ScriptBudget effective = ScriptBudget.unlimited()
                    .clampTo(ScriptBudget.builder().steps(10).build());

            assertEquals(10, effective.steps());
            assertFalse(effective.isUnlimited());
        }

        @Test
        @DisplayName("the shorter deadline wins, and no deadline is not the shortest")
        void deadlines() {
            ScriptBudget asked = ScriptBudget.builder().deadline(Duration.ofSeconds(10)).build();

            assertEquals(Duration.ofSeconds(1),
                         asked.clampTo(ScriptBudget.builder().deadline(Duration.ofSeconds(1)).build())
                                 .deadline());
            assertEquals(Duration.ofSeconds(10),
                         asked.clampTo(ScriptBudget.builder().steps(5).build()).deadline(),
                         "a ceiling that says nothing about time must not remove a deadline");
        }
    }

    @Nested
    @DisplayName("the four limits actually fire")
    class Enforcement {

        @Test
        @DisplayName("a loop is stopped by its own bound")
        void loopBound() {
            ScriptBudgetExceededException stopped = assertThrows(
                    ScriptBudgetExceededException.class,
                    () -> dispatch(WALKS_A_LONG_LIST,
                                   ScriptBudget.builder().loopIterations(3).build(),
                                   ScriptBudget.unlimited()));

            assertEquals(ScriptLimit.LOOP_ITERATIONS, stopped.limit());
            assertEquals("test.jms", stopped.document());
        }

        @Test
        @DisplayName("a body is stopped by the step ceiling")
        void steps() {
            ScriptBudgetExceededException stopped = assertThrows(
                    ScriptBudgetExceededException.class,
                    () -> dispatch(WALKS_A_LONG_LIST,
                                   ScriptBudget.builder().steps(4).build(),
                                   ScriptBudget.unlimited()));

            assertEquals(ScriptLimit.STEPS, stopped.limit());
        }

        @Test
        @DisplayName("⚠️ a function that calls itself is stopped before the stack is")
        void recursion() {
            ScriptBudgetExceededException stopped = assertThrows(
                    ScriptBudgetExceededException.class,
                    () -> dispatch(CALLS_ITSELF,
                                   ScriptBudget.builder().recursionDepth(16).build(),
                                   ScriptBudget.unlimited()));

            assertEquals(ScriptLimit.RECURSION_DEPTH, stopped.limit());
        }

        @Test
        @DisplayName("a deadline stops a script that is merely slow")
        void deadline() {
            ScriptBudgetExceededException stopped = assertThrows(
                    ScriptBudgetExceededException.class,
                    () -> dispatch(WALKS_A_LONG_LIST,
                                   ScriptBudget.builder().deadline(Duration.ofMillis(1)).build(),
                                   ScriptBudget.unlimited()));

            assertEquals(ScriptLimit.DEADLINE, stopped.limit());
            assertTrue(stopped.getMessage().contains("after 1ms"), stopped.getMessage());
        }
    }

    @Nested
    @DisplayName("⚠️ a document cannot raise its own limit")
    class TheWholePoint {

        @Test
        @DisplayName("asking for a million under a ceiling of four still stops at four")
        void cannotOutAsk() {
            ScriptBudgetExceededException stopped = assertThrows(
                    ScriptBudgetExceededException.class,
                    () -> dispatch(WALKS_A_LONG_LIST,
                                   ScriptBudget.builder().steps(4).build(),
                                   ScriptBudget.builder().steps(1_000_000).build()));

            assertTrue(stopped.hitCeiling(), "the number that stopped it was the host's");
            assertTrue(stopped.getMessage().contains("ceiling"), stopped.getMessage());
        }

        @Test
        @DisplayName("and asking for less than the ceiling is honoured, not quietly raised")
        void mayAskForLess() {
            ScriptBudgetExceededException stopped = assertThrows(
                    ScriptBudgetExceededException.class,
                    () -> dispatch(WALKS_A_LONG_LIST,
                                   ScriptBudget.builder().steps(1_000_000).build(),
                                   ScriptBudget.builder().steps(4).build()));

            assertFalse(stopped.hitCeiling(), "the script chose this limit and can raise it");
            assertTrue(stopped.getMessage().contains("asked for this limit itself"), stopped.getMessage());
        }
    }

    @Nested
    @DisplayName("a budget with nothing set")
    class Unlimited {

        @Test
        @DisplayName("installs nothing at all, so nothing downstream counts")
        void installsNothing() {
            BoundScript       bound   = new ScriptBinder(catalogue).bind(evaluator.parse("""
                    script "free" {
                        on opened do
                            @store.take(1)
                        end
                    }
                    """, "free.jms"));
            EvaluationContext context = context(bound);

            bound.begin(context);

            assertTrue(bound.getBudget().isUnlimited());
            assertEquals(null, ScriptExecution.from(context),
                         "an unlimited budget must leave the context exactly as it found it");
        }

        @Test
        @DisplayName("⚠️ and a spent allowance does not survive into the next dispatch")
        void doesNotLeak() {
            BoundScript       bound   = new ScriptBinder(catalogue, ScriptBudget.builder().steps(4).build())
                    .bind(evaluator.parse(WALKS_A_LONG_LIST, "test.jms"));
            EvaluationContext context = context(bound);

            // A context is reused across events on purpose — building one per event is what
            // installFunctions exists to avoid. So the allowance has to be taken back out, or the second
            // dispatch is refused for what the first one did.
            assertThrows(ScriptBudgetExceededException.class, () -> {
                try {
                    bound.begin(context);
                    bound.handlersFor("opened").forEach(handler -> handler.evaluate(context));
                } finally {
                    bound.finish(context);
                }
            });

            assertEquals(null, ScriptExecution.from(context), "finish() must clear the allowance");
        }
    }

    /** A facade. Nothing about it is the language's. */
    public static final class Store {

        public List<Integer> numbers() {
            return IntStream.range(0, 400_000).boxed().toList();
        }

        public int pending() {
            return 4;
        }

        public int take(Object anything) {
            return 1;
        }
    }
}
