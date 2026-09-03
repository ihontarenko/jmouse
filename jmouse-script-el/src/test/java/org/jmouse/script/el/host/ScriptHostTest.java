package org.jmouse.script.el.host;

import org.jmouse.el.evaluation.EvaluationContext;
import org.jmouse.script.el.ScriptParseException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * <strong>End-to-end: does the dialect work, and does the library stay free of the thing it works for?</strong>
 *
 * <p>Written as if from a game, because a game exercises every construction at once — a document that
 * composes, handlers with and without guards, a loop, a state machine, and facades called in an order
 * somebody can check. ⚠️ Every game noun here is in <em>test</em> sources; {@code NoGameNounsTest} is
 * what keeps that true.</p>
 *
 * <p>The whole suite goes through {@link ScriptHost}, which is the seam a real host meets. Reaching past
 * it into the binder or the nodes would test the library rather than the contract.</p>
 *
 * @author Ivan Hontarenko (Mr. Jerry Mouse)
 * @author ihontarenko@gmail.com
 */
class ScriptHostTest {

    private FakeHost          game;
    private ScriptHost        host;
    private AtomicInteger     reads;
    private BoundScript       script;
    private EvaluationContext context;

    @BeforeEach
    void load() {
        game = new FakeHost();
        reads = new AtomicInteger();

        Map<String, String> files = Map.of("common.jms", GameShapedFixture.COMMON);

        host = ScriptHost.builder()
                .catalogue(game.catalogue())
                .resources(path -> {
                    reads.incrementAndGet();
                    return files.get(path);
                })
                .build();

        script = host.load("slice-01.jms", GameShapedFixture.SLICE);
        context = host.newContext(script);
    }

    @Nested
    @DisplayName("a document, loaded and dispatched")
    class RoundTrip {

        @Test
        @DisplayName("runs a handler and calls the facades in the order they were written")
        void inOrder() {
            assertEquals(1, host.dispatch(script, "start", context));

            assertEquals(List.of(
                    "world.reveal(player_base)",
                    "mission.objective(unload-once)",
                    "mission.objective(kill-scout)",
                    "world.spawn3(harvester)",
                    "world.spawn4(scout)"
            ), game.calls);
        }

        @Test
        @DisplayName("⚠️ a when clause filters a handler out rather than failing it")
        void guards() {
            // Three handlers are written for `destroyed`. This one is a scout, so the first matches
            // and the other two — one asking for a harvester, one for a building — must not run.
            //
            // ⚠️ ONLY `unit` is set. The third handler is guarded on `building.kind`, and the host did
            // not put a building in the context for an event about a unit — which is the ordinary shape
            // of an event with more than one subject. That guard has to answer false rather than throw:
            // it used to throw an NPE naming a local variable, see JMF-270.
            context.setValue("unit", new FakeHost.Unit("enemy", true, "scout", "scout"));

            assertEquals(1, host.dispatch(script, "destroyed", context));
            assertEquals(List.of("mission.complete(kill-scout)", "mission.complete"), game.calls);
        }

        @Test
        @DisplayName("an event nobody wrote a handler for is not an error")
        void nobodyListening() {
            // `paused` is declared by this host and handled by nothing. A host firing every event it
            // has must not have to ask first whether anybody was listening.
            assertEquals(0, host.dispatch(script, "paused", context));
            assertTrue(game.calls.isEmpty(), game.calls.toString());

            // And one that IS handled runs, so the zero above means "nobody listening" rather than
            // "dispatch does nothing".
            assertEquals(1, host.dispatch(script, "timer", context));
        }

        @Test
        @DisplayName("a guard that reads the host's own context decides on it")
        void guardReadsContext() {
            context.setValue("building", new FakeHost.Building("dropoff", "me"));
            context.setValue("count", 1);

            assertEquals(1, host.dispatch(script, "unload", context));
            assertTrue(game.calls.contains("world.enable(factory)"), game.calls.toString());
        }
    }

    @Nested
    @DisplayName("⚠️ include actually composes two files")
    class Composition {

        @Test
        @DisplayName("a function declared in the included file is callable from the includer")
        void acrossFiles() {
            // `hostile` lives only in common.jms. The `enter` handler loops the ridge and calls it, so a
            // failure to compose shows up as "no function called hostile" at load rather than here.
            context.setValue("unit", new FakeHost.Unit("enemy", true, "scout", "scout"));

            assertEquals(1, host.dispatch(script, "enter", context));
            assertEquals(List.of("orders.attack"), game.calls,
                         "one of the two units on the ridge is the player's own");
        }

        @Test
        @DisplayName("the included file is read once, at load, and never again")
        void readOnce() {
            int atLoad = reads.get();

            for (int fired = 0; fired < 50; fired++) {
                host.dispatch(script, "start", context);
            }

            assertEquals(atLoad, reads.get(), "a dispatch that re-read a file would be re-parsing it");
            assertEquals(1, atLoad);
        }

        @Test
        @DisplayName("a missing file is refused at load, naming the path")
        void missingFile() {
            ScriptHost bare = ScriptHost.builder().catalogue(game.catalogue()).build();

            ScriptParseException refused = assertThrows(
                    ScriptParseException.class,
                    () -> bare.load("x.jms", "include 'nowhere.jms'\nscript \"x\" { }"));

            assertTrue(refused.getMessage().contains("nowhere.jms"), refused.getMessage());
        }

        @Test
        @DisplayName("⚠️ a file that includes itself is refused rather than followed")
        void cycle() {
            ScriptHost circular = ScriptHost.builder()
                    .catalogue(game.catalogue())
                    .resources(ScriptResources.of(Map.of("a.jms", "include 'a.jms'\nscript \"a\" { }")))
                    .build();

            ScriptParseException refused = assertThrows(
                    ScriptParseException.class,
                    () -> circular.load("root.jms", "include 'a.jms'\nscript \"root\" { }"));

            assertTrue(refused.getMessage().contains("includes itself"), refused.getMessage());
        }
    }

    @Nested
    @DisplayName("a behaviour driven repeatedly")
    class StateMachine {

        @Test
        @DisplayName("⚠️ walks the states in the order the file says, tick after tick")
        void sequence() {
            FakeHost.Unit unit = new FakeHost.Unit("me", true, "harvester", "harvester");

            assertEquals("seek", unit.getState());

            host.call(script, "tick", context, unit);
            assertEquals("harvest", unit.getState(), "at the spice, so it starts harvesting");

            host.call(script, "tick", context, unit);
            assertEquals("return", unit.getState(), "the field is empty, so it heads back");

            host.call(script, "tick", context, unit);
            assertEquals("unload", unit.getState(), "at the refinery");

            unit.setEmpty(true);
            host.call(script, "tick", context, unit);
            assertEquals("seek", unit.getState(), "empty again, so back out");

            assertEquals(List.of("orders.unload"), game.calls);
        }

        @Test
        @DisplayName("the same node tree runs every time — nothing is rebuilt between calls")
        void neverRebuilt() {
            Object before = script.behaviour("gatherer");

            for (int tick = 0; tick < 100; tick++) {
                host.call(script, "tick", context, new FakeHost.Unit("me", true, "harvester", "h"));
            }

            assertSame(before, script.behaviour("gatherer"));
            assertSame(script.handlersFor("start").getFirst(), script.handlersFor("start").getFirst());
        }
    }

    @Nested
    @DisplayName("⚠️ refused at load, with a position")
    class Refusals {

        @Test
        @DisplayName("an event this host does not fire")
        void unknownEvent() {
            ScriptBindException refused = assertThrows(
                    ScriptBindException.class,
                    () -> host.load("x.jms", "script \"x\" { on landed do @world.reveal('a') end }"));

            assertTrue(refused.getMessage().contains("landed"), refused.getMessage());
            assertTrue(refused.at().isKnown(), "a refusal without a line is a refusal nobody can act on");
            assertEquals("x.jms", refused.at().document());
        }

        @Test
        @DisplayName("an @ name this host did not declare")
        void unknownFacade() {
            ScriptBindException refused = assertThrows(
                    ScriptBindException.class,
                    () -> host.load("x.jms", "script \"x\" { on start do @economy.tax(1) end }"));

            assertTrue(refused.getMessage().contains("@economy"), refused.getMessage());
            assertTrue(refused.at().isKnown());
        }

        @Test
        @DisplayName("⚠️ and a refusal inside an included file names THAT file, not the one that included it")
        void namesTheRightFile() {
            ScriptHost composing = ScriptHost.builder()
                    .catalogue(game.catalogue())
                    .resources(ScriptResources.of(Map.of(
                            "broken.jms", "script \"broken\" { on start do @economy.tax(1) end }")))
                    .build();

            ScriptBindException refused = assertThrows(
                    ScriptBindException.class,
                    () -> composing.load("root.jms", "include 'broken.jms'\nscript \"root\" { }"));

            assertEquals("broken.jms", refused.at().document(),
                         "after a merge, a line number belongs to nothing unless the file is named");
        }
    }

    @Nested
    @DisplayName("the closed catalogue holds at run time too")
    class Closed {

        @Test
        @DisplayName("the context a host builds resolves facades and nothing else")
        void facadesOnly() {
            assertNotNull(host.newContext(script));

            assertThrows(ScriptAccessException.class,
                         () -> new FacadeLookup(game.catalogue()).getBean("economy", Object.class));
            assertThrows(ScriptAccessException.class,
                         () -> new FacadeLookup(game.catalogue()).getBean(Object.class));
        }
    }
}
