package org.jmouse.cad.simulation.engine;

import org.jmouse.cad.simulation.elaboration.BenchConditions;
import org.jmouse.cad.simulation.elaboration.ElaboratedCircuit;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * The parts that failed during one run on a realistic bench, how — as each one's component says it fails — and why: a
 * part past its limit fails at once, the run goes on with it failed, and it stays failed through every change of value
 * until the run starts again. The failure is written into the circuit as the bench's own ({@link BenchConditions#failures}),
 * so the parts that replace it are the bench's work, not the session's.
 */
final class PartFailures {

    private final Map<String, BenchConditions.Failure> failures = new LinkedHashMap<>();
    /** Why each failed part failed: overheated, overvoltage, overcurrent. */
    private final Map<String, String> causes = new LinkedHashMap<>();

    /** Whether a circuit stands on a realistic bench, where a part can fail. */
    static boolean realistic(ElaboratedCircuit circuit) {
        return circuit.bench().realistic();
    }

    /**
     * Every part past its limit in these loads fails now, as its component says it fails.
     *
     * @return whether one failed that had not before
     */
    boolean burnOut(Map<String, PartLoad> loads, LiveCircuitSolver solver) {
        boolean failed = false;
        for (Map.Entry<String, PartLoad> load : loads.entrySet()) {
            String cause = load.getValue().failure() == null ? load.getValue().breaking() : null;
            if (cause != null && !failures.containsKey(load.getKey())) {
                failures.put(load.getKey(), solver.failureOf(load.getKey()));
                causes.put(load.getKey(), cause);
                failed = true;
            }
        }
        return failed;
    }

    /** Every failed part's load with why it failed. */
    Map<String, PartLoad> withCauses(Map<String, PartLoad> loads) {
        if (causes.isEmpty()) {
            return loads;
        }
        Map<String, PartLoad> caused = new LinkedHashMap<>(loads);
        causes.forEach((part, cause) -> caused.computeIfPresent(part, (ignored, load) -> load.causedBy(cause)));
        return caused;
    }

    /**
     * The bench given, with every part that failed so far failed on it: what the circuit is elaborated again on, so the
     * parts that replace a failed one are the bench's work, not the session's.
     */
    BenchConditions applyTo(BenchConditions bench) {
        BenchConditions conditions = bench;
        for (Map.Entry<String, BenchConditions.Failure> failure : failures.entrySet()) {
            conditions = conditions.withFailure(failure.getKey(), failure.getValue());
        }
        return conditions;
    }

    /** From the beginning, with new parts: nothing has failed yet. */
    void clear() {
        failures.clear();
        causes.clear();
    }
}
