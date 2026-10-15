package org.jmouse.parallax.program;

import org.jmouse.parallax.Computation;
import org.jmouse.parallax.ComputationProgress;
import org.jmouse.parallax.ComputationSettings;
import org.jmouse.parallax.Dataset;
import org.jmouse.parallax.RepresentationKind;
import org.jmouse.parallax.SettingSpecification;
import org.jmouse.parallax.object.CompiledObject;
import org.jmouse.parallax.object.ObjectDefinition;
import org.jmouse.script.el.budget.ScriptBudget;

import java.time.Duration;
import java.util.List;

/** The representation of a program object: what its program returned, as a dataset. */
public final class ProgramComputations {

    private ProgramComputations() {
    }

    /**
     * Runs the program once under a budget: as many statement steps as the setting allows, as many loop iterations,
     * a recursion depth of 200 and half a minute. The cost estimate is the step budget, so a heavy program becomes
     * a queued job rather than a request held open.
     */
    public static class Output implements Computation {

        public RepresentationKind kind() {
            return RepresentationKind.PROGRAM_OUTPUT;
        }

        public boolean accepts(ObjectDefinition definition) {
            return definition instanceof ObjectDefinition.Program;
        }

        public List<SettingSpecification> settings() {
            return List.of(SettingSpecification.integer("steps", 1_000_000, 1_000, 50_000_000, "Statement steps the program may take"),
                    SettingSpecification.integer("seed", 42, 0, Integer.MAX_VALUE, "Random seed: the same seed gives the same run"));
        }

        public long estimatedCost(CompiledObject object, ComputationSettings settings) {
            return settings.integer("steps");
        }

        public Dataset compute(CompiledObject object, ComputationSettings settings, ComputationProgress progress) {
            long steps = settings.integer("steps");
            ScriptBudget budget = ScriptBudget.builder()
                    .steps(steps)
                    .loopIterations(steps)
                    .recursionDepth(200)
                    .deadline(Duration.ofSeconds(30))
                    .build();
            return ProgramRun.datasetOf(ProgramRun.run(object, budget, new java.util.SplittableRandom(settings.integer("seed"))));
        }
    }
}
