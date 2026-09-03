package org.jmouse.script.el.examples;

import org.jmouse.el.evaluation.DefaultEvaluationContext;
import org.jmouse.el.evaluation.EvaluationContext;
import org.jmouse.el.evaluation.EvaluationException;
import org.jmouse.script.el.ScriptEvaluator;
import org.jmouse.script.el.ScriptParseException;
import org.jmouse.script.el.budget.ScriptBudget;
import org.jmouse.script.el.budget.ScriptBudgetExceededException;
import org.jmouse.script.el.host.*;
import org.jmouse.script.el.node.BehaviourNode;
import org.jmouse.script.el.node.HandlerNode;
import org.jmouse.script.el.node.ScriptDocumentNode;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

/**
 * Runs the whole of jMS end to end and prints what happened. 🔎
 *
 * <h2>⚠️ The names here are deliberately about nothing</h2>
 *
 * <p>A facade called {@code store} and an event called {@code opened} are this file's, invented so it
 * has something to demonstrate with. They are not the language's vocabulary and they are not any
 * product's: nothing in {@code main} may name a game's own nouns, and the way to
 * keep that true is for the module's own example to be about a shop counter.</p>
 *
 * <p>Run it with {@code main}. It parses a fixture covering every construction the grammar has, binds
 * it against a catalogue, dispatches an event against real facades, and then shows each way a document
 * is refused — an unknown event, an undeclared facade, a comparison written where an assignment was
 * meant, and a body nobody closed.</p>
 *
 * @author Ivan Hontarenko (Mr. Jerry Mouse)
 * @author ihontarenko@gmail.com
 */
public final class ScriptSmoke {

    private static final String FIXTURE = """
            # jMS — every construction the grammar has, and nothing that means anything.

            include 'common.jms'

            script "counter" {

                function busy()
                    return @store.pending() > 2
                end

                function loud(entry)
                    return entry.weight > 10
                end

                on opened do
                    @journal.record('open')
                    @alarm.arm('front', 30)
                end

                on changed when entry.kind == 'delivery' do
                    local weight = entry.weight

                    if weight == 1 then
                        @journal.record('first')
                    elseif weight > 10 then
                        @journal.record('heavy')
                    else
                        @journal.record('ordinary')
                    end
                end

                on changed when busy() do
                    if !@store.open() then
                        @alarm.arm('back', 5)
                    end
                end

                on ticked 180 do
                    for entry in @store.pending_entries() do
                        if loud(entry) then
                            @journal.record('loud')
                        end
                    end
                end
            }

            behaviour "counter-clerk" do

                function step(entry)
                    if entry.state == 'waiting' then
                        local next = @store.next_slot(entry)
                        if next == null then
                            return
                        end
                        entry.state = 'moving'
                    elseif entry.state == 'moving' then
                        @journal.record('moved')
                        entry.state = 'waiting'
                    end
                end

            end
            """;

    public static void main(String[] arguments) {
        ScriptEvaluator    evaluator = new ScriptEvaluator();
        ScriptDocumentNode document  = evaluator.parse(FIXTURE, "counter.jms");

        report(document);

        Journal        journal   = new Journal();
        Store          store     = new Store();
        Alarm          alarm     = new Alarm();
        ScriptCatalogue catalogue = ScriptCatalogue.builder()
                .facade("journal", journal)
                .facade("store", store)
                .facade("alarm", alarm)
                .event("opened")
                .event("changed")
                .event("ticked")
                .build();

        BoundScript bound = new ScriptBinder(catalogue).bind(document);

        System.out.println();
        System.out.println("BOUND      : " + bound);
        System.out.println("EVENTS     : " + bound.handledEvents());
        System.out.println("INCLUDES   : " + bound.getIncludes());
        System.out.println("BEHAVIOURS : " + bound.behaviour("counter-clerk"));
        System.out.println("IDEMPOTENT : " + bound.equals(new ScriptBinder(catalogue).bind(document)));

        dispatch(bound, catalogue, journal);
        budgets(evaluator, document, catalogue);

        System.out.println();
        System.out.println("--- refusals ------------------------------------------------------------");

        refuses("an event nobody fires", () -> new ScriptBinder(catalogue)
                .bind(evaluator.parse("script \"x\" { on unheard_of do @journal.record('a') end }", "x.jms")));

        refuses("a facade nobody declared", () -> new ScriptBinder(catalogue)
                .bind(evaluator.parse("script \"x\" { on opened do @ledger.record('a') end }", "x.jms")));

        refuses("a function nobody registered", () -> new ScriptBinder(catalogue)
                .bind(evaluator.parse("script \"x\" { on opened do @journal.record(secret()) end }", "x.jms")));

        refuses("'==' written where a value was meant to be set", () -> evaluator.parse(
                "behaviour \"b\" do function f(entry) entry.state == 'moving' end end", "b.jms"));

        refuses("a body nobody closed", () -> evaluator.parse(
                "behaviour \"b\" do function f(entry) @journal.record('a') end", "b.jms"));

        refuses("a statement that computes a value and drops it", () -> new ScriptBinder(catalogue)
                .bind(evaluator.parse("script \"x\" { on opened do entry.weight end }", "x.jms")));

        refuses("a loop over something that is not a collection", () -> {
            BoundScript       loop    = new ScriptBinder(catalogue).bind(evaluator.parse(
                    "script \"x\" { on opened do for n in @store.pending() do @journal.record('a') end end }",
                    "x.jms"));
            EvaluationContext context = context(loop, catalogue);

            loop.handlersFor("opened").forEach(handler -> handler.evaluate(context));
        });

        refuses("a facade reached without going through the binder",
                () -> new FacadeLookup(catalogue).getBean("ledger", Object.class));

        refuses("the application container, asked for by type",
                () -> new FacadeLookup(catalogue).getBean(Object.class));

        System.out.println();
        System.out.println("JOURNAL    : " + journal.entries);
    }

    /**
     * Prints what the parser made of the file — names only, because names are all it knows.
     */
    private static void report(ScriptDocumentNode document) {
        System.out.println("DOCUMENT   : " + document);
        System.out.println("INCLUDES   : " + document.getIncludes());

        document.getScripts().forEach(script -> {
            System.out.println("SCRIPT     : " + script);
            script.getFunctions().forEach(function -> System.out.println("  function : " + function));
            script.getHandlers().forEach(handler -> System.out.println(
                    "  handler  : %s when=%s argument=%s statements=%d".formatted(
                            handler.getEvent(),
                            handler.getCondition() == null ? "-" : handler.getCondition().toSource(),
                            handler.getArgument() == null ? "-" : handler.getArgument().toSource(),
                            handler.getExpressions().size())));
        });

        document.getBehaviours().forEach(behaviour -> {
            System.out.println("BEHAVIOUR  : " + behaviour);
            behaviour.getFunctions().forEach(function -> System.out.println("  function : " + function));
        });
    }

    /**
     * Fires one event through the bound script, against the real facades.
     */
    private static void dispatch(BoundScript bound, ScriptCatalogue catalogue, Journal journal) {
        EvaluationContext context = context(bound, catalogue);

        context.setValue("entry", new Entry("delivery", 12, "waiting"));

        for (HandlerNode handler : bound.handlersFor("changed")) {
            handler.evaluate(context);
        }

        System.out.println("DISPATCHED : changed -> " + journal.entries);

        // ⚠️ The handler above wrote `local weight`. If it is still here, `local` is a lie and the next
        // handler somebody adds reads a value the file never gave it.
        System.out.println("LOCAL LEAK : weight after the handler = " + context.getValue("weight"));

        // ⚠️ The `ticked` handler loops over `entry` — the same name the host put in the context for the
        // event. If the loop variable survives, the host's own subject has been overwritten by the last
        // element of somebody's collection.
        Entry subject = new Entry("delivery", 12, "waiting");

        context.setValue("entry", subject);

        for (HandlerNode handler : bound.handlersFor("ticked")) {
            handler.evaluate(context);
        }

        System.out.println("LOOP LEAK  : entry after the loop is the host's = "
                + (context.getValue("entry") == subject));

        BehaviourNode clerk = bound.behaviour("counter-clerk");
        Entry         entry = new Entry("delivery", 12, "waiting");

        context.setValue("entry", entry);
        clerk.getFunctions().getFirst().evaluate(context);

        System.out.println("BEHAVIOUR  : step -> entry.state = " + entry.state);
    }

    /**
     * The same document under four different allowances, and what each one refuses.
     *
     * <p>⚠️ The point of the last one is not that it refuses — the first three do too — but that its
     * message says the number came from a <strong>ceiling</strong>. Without that sentence an author who
     * asked for a million steps and is running at ten raises their own figure, sees nothing change, and
     * concludes the feature is broken.</p>
     */
    private static void budgets(ScriptEvaluator evaluator, ScriptDocumentNode document, ScriptCatalogue catalogue) {
        System.out.println();
        System.out.println("--- budgets -------------------------------------------------------------");

        ScriptBudget generous = ScriptBudget.builder().steps(1_000).loopIterations(1_000).build();
        BoundScript  roomy    = new ScriptBinder(catalogue, generous).bind(document);

        System.out.println("EFFECTIVE  : " + roomy.getBudget());
        System.out.println("UNBUDGETED : " + new ScriptBinder(catalogue).bind(document).getBudget());

        spent("a loop allowed one turn over a collection of two",
              new ScriptBinder(catalogue, ScriptBudget.builder().loopIterations(1).build()).bind(document),
              catalogue, "ticked");

        spent("a handler allowed two steps",
              new ScriptBinder(catalogue, ScriptBudget.builder().steps(2).build()).bind(document),
              catalogue, "changed");

        String recursive = """
                script "deep" {
                    function down(step)
                        return down(step)
                    end

                    on opened do
                        @journal.record(down(1))
                    end
                }
                """;

        spent("a function that calls itself, eight deep",
              new ScriptBinder(catalogue, ScriptBudget.builder().recursionDepth(8).build())
                      .bind(evaluator.parse(recursive, "deep.jms")),
              catalogue, "opened");

        // ⚠️ The document asks for a million and the host allows two. It loads — clamping is not a
        // refusal — and the message it eventually gives says whose number stopped it.
        spent("a document asking for 1 000 000 steps under a ceiling of 2",
              new ScriptBinder(catalogue, ScriptBudget.builder().steps(2).build())
                      .bind(document, ScriptBudget.builder().steps(1_000_000).build()),
              catalogue, "changed");

        // ⚠️ And the other half of the same promise: a document asking for LESS than the ceiling is held
        // to its own number, not quietly raised to what it was allowed. A handler on a hot path budgeted
        // deliberately small has to fail in testing rather than be slow in production.
        spent("a document asking for 2 steps under a ceiling of 1 000 000",
              new ScriptBinder(catalogue, ScriptBudget.builder().steps(1_000_000).build())
                      .bind(document, ScriptBudget.builder().steps(2).build()),
              catalogue, "changed");

        // ⚠️ The deadline, exercised rather than claimed. It is the one limit that cannot be counted, so
        // the clock is read every few hundred steps — which means a script doing less than that never
        // meets it. A demonstration on the fixture's two-element loop would have proved nothing, so this
        // one walks a collection big enough for the check to actually happen.
        String patient = """
                script "patient" {
                    on opened do
                        for step in @store.numbers() do
                            @store.pending()
                        end
                    end
                }
                """;

        spent("a script given one millisecond and a long collection to walk",
              new ScriptBinder(catalogue, ScriptBudget.builder().deadline(Duration.ofMillis(1)).build())
                      .bind(evaluator.parse(patient, "patient.jms")),
              catalogue, "opened");
    }

    /**
     * Dispatches one event under a bound script's own budget and reports how it ran out.
     */
    private static void spent(String what, BoundScript bound, ScriptCatalogue catalogue, String event) {
        EvaluationContext context = context(bound, catalogue);

        context.setValue("entry", new Entry("delivery", 12, "waiting"));

        try {
            bound.begin(context);

            for (HandlerNode handler : bound.handlersFor(event)) {
                handler.evaluate(context);
            }

            System.out.println("!! NOT STOPPED : " + what);
        } catch (ScriptBudgetExceededException expected) {
            System.out.println("OK stopped     : %s%n                 %s".formatted(what, expected.getMessage()));
        } finally {
            bound.finish(context);
        }
    }

    /**
     * An evaluation context wired to the catalogue — and to nothing else.
     *
     * <p>⚠️ {@link FacadeLookup} is what makes {@code @name} mean the host's facade rather than
     * "whatever bean the application has under that name". It is the one line that has to be right.</p>
     */
    private static EvaluationContext context(BoundScript bound, ScriptCatalogue catalogue) {
        ScriptEvaluator          evaluator = new ScriptEvaluator();
        DefaultEvaluationContext context   = new DefaultEvaluationContext();

        context.setExtensions(evaluator.getExtensions());
        context.setBeanLookup(new FacadeLookup(catalogue));
        bound.installFunctions(context);

        return context;
    }

    private static void refuses(String what, Runnable attempt) {
        try {
            attempt.run();
            System.out.println("!! NOT REFUSED : " + what);
        } catch (ScriptBindException | ScriptParseException | ScriptAccessException | EvaluationException expected) {
            System.out.println("OK refused     : %s%n                 %s".formatted(what, expected.getMessage()));
        }
    }

    /** A thing the host hands to a handler. Nothing about it is the language's. */
    public static final class Entry {

        private final String kind;
        private final int    weight;

        private String state;

        public Entry(String kind, int weight, String state) {
            this.kind = kind;
            this.weight = weight;
            this.state = state;
        }

        public String getKind() {
            return kind;
        }

        public int getWeight() {
            return weight;
        }

        public String getState() {
            return state;
        }

        public void setState(String state) {
            this.state = state;
        }
    }

    /** A facade. */
    public static final class Journal {

        private final List<String> entries = new ArrayList<>();

        public void record(String what) {
            entries.add(what);
        }

        public int count() {
            return entries.size();
        }
    }

    /** A facade. */
    public static final class Store {

        public boolean open() {
            return true;
        }

        public int pending() {
            return 4;
        }

        public List<Entry> pending_entries() {
            return List.of(new Entry("delivery", 12, "waiting"), new Entry("pickup", 2, "waiting"));
        }

        /**
         * A long collection, so the deadline demonstration has something to walk.
         *
         * <p>⚠️ Here because a deadline cannot be shown on a two-element loop: the clock is read every
         * few hundred steps, so a short script never meets one.</p>
         */
        public List<Integer> numbers() {
            return java.util.stream.IntStream.range(0, 400_000).boxed().toList();
        }

        public String next_slot(Entry entry) {
            return entry == null ? null : "slot-1";
        }
    }

    /** A facade. */
    public static final class Alarm {

        public void arm(String where, int seconds) {
            System.out.println("ALARM      : %s for %ds".formatted(where, seconds));
        }
    }

    private ScriptSmoke() {
    }
}
