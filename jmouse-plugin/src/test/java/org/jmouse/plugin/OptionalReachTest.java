package org.jmouse.plugin;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;
import java.util.function.Consumer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** A plugin adding to another's point when that one runs, and doing without it when it does not. */
class OptionalReachTest {

    private static final Contributions<String> SOURCES = new Contributions<>("models.sources", String.class);

    private static Plugin plugin(String id, Consumer<PluginRegistration> registering) {
        return new Plugin() {
            @Override
            public PluginDescriptor descriptor() {
                return PluginDescriptor.of(id, id, "1");
            }

            @Override
            public void register(PluginRegistration registration) {
                registering.accept(registration);
            }
        };
    }

    private static final Plugin MODELS = plugin("models", registration -> registration.declare(SOURCES));

    private static final Plugin KICAD = plugin("kicad", registration -> registration.contributeWhenDeclared(SOURCES, "kicad-libraries"));

    private static PluginState stateOf(Plugins plugins, String id) {
        return plugins.plugins().stream().filter(state -> state.plugin().id().equals(id)).findFirst().orElseThrow();
    }

    @Test
    void addsToThePointWhenItsDeclarerRuns() {
        try (Plugins plugins = Plugins.start(List.of(KICAD, MODELS))) {
            assertEquals(List.of("kicad-libraries"), plugins.contributions(SOURCES));
            assertEquals(PluginState.Status.STARTED, stateOf(plugins, "kicad").status());
            assertEquals(Set.of("models"), stateOf(plugins, "kicad").extending());
        }
    }

    @Test
    void startsWithoutThePointWhenNoneDeclaresIt() {
        try (Plugins plugins = Plugins.start(List.of(KICAD))) {
            assertEquals(PluginState.Status.STARTED, stateOf(plugins, "kicad").status());
            assertTrue(stateOf(plugins, "kicad").extending().isEmpty());
        }
    }

    @Test
    void aRequiredContributionStillRefusesWithoutItsPoint() {
        Plugin hard = plugin("hard", registration -> registration.contribute(SOURCES, "hard"));
        try (Plugins plugins = Plugins.start(List.of(hard))) {
            assertEquals(PluginState.Status.REFUSED, stateOf(plugins, "hard").status());
        }
    }
}
