package org.jmouse.parallax.circuit;

import org.jmouse.parallax.object.CompiledObject;
import org.jmouse.parallax.object.DefinitionCompiler;
import org.jmouse.parallax.object.ObjectDefinition.BenchConditions;
import org.jmouse.parallax.object.ObjectDefinition.Circuit;

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
    static boolean realistic(CompiledObject circuit) {
        return circuit.definition() instanceof Circuit written && written.conditions() != null
                && written.conditions().realistic();
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

    /** The circuit given, with every part that failed so far failed in it. */
    CompiledObject applyTo(CompiledObject object) {
        if (failures.isEmpty() || !(object.definition() instanceof Circuit circuit)) {
            return object;
        }
        BenchConditions conditions = circuit.conditions() != null ? circuit.conditions()
                : new BenchConditions(BenchConditions.NOMINAL_TEMPERATURE, 0, Map.of(), Map.of());
        for (Map.Entry<String, BenchConditions.Failure> failure : failures.entrySet()) {
            conditions = conditions.withFailure(failure.getKey(), failure.getValue());
        }
        Circuit failedCircuit = new Circuit(circuit.elements(), circuit.time(), circuit.layout(), conditions, circuit.probes());
        return DefinitionCompiler.compile(failedCircuit, object.parameters(), object.parameterValues(), object.library());
    }

    /** From the beginning, with new parts: nothing has failed yet. */
    void clear() {
        failures.clear();
        causes.clear();
    }
}
