package org.jmouse.parallax.circuit;

import org.jmouse.parallax.component.ComponentModel;

import org.jmouse.parallax.object.CompiledObject;
import org.jmouse.parallax.object.ObjectDefinition;
import org.jmouse.parallax.object.ObjectDefinition.CircuitExpectation;
import org.jmouse.parallax.verification.ConsistencyCheck;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * A circuit held against what it states of itself: every {@code expect} of the circuit run on the circuit as written —
 * simulated, measured as it says, and compared with its window. Without this check a circuit's expectations were read
 * and never run, so a circuit that no longer did what it claimed said nothing.
 */
public final class CircuitExpectations implements ConsistencyCheck {

    public String code() {
        return "CIRCUIT_EXPECTATIONS";
    }

    public String description() {
        return "A circuit does what its own expectations say";
    }

    public boolean appliesTo(ObjectDefinition definition) {
        return definition instanceof ObjectDefinition.Circuit circuit && !circuit.expectations().isEmpty();
    }

    public CheckResult check(CompiledObject object) {
        List<CircuitExpectation> expectations = ((ObjectDefinition.Circuit) object.definition()).expectations();
        List<String> verdicts = new ArrayList<>();
        int failed = 0;
        for (CircuitExpectation expectation : expectations) {
            String verdict;
            try {
                ComponentModel.Criterion criterion = criterionOf(expectation);
                ComponentCheck.Measurement measurement = ComponentCheck.measureCompiled(object, expectation.measure(), ComponentCheck.SAMPLES);
                boolean admitted = measurement.admittedBy(criterion);
                if (!admitted) {
                    failed++;
                }
                verdict = measurement.verdict(admitted, criterion);
            } catch (RuntimeException failure) {
                failed++;
                verdict = "ERROR " + failure.getMessage();
            }
            verdicts.add(expectation.title() + ": " + verdict + (expectation.reference() == null ? "" : " — " + expectation.reference()));
        }
        String detail = (expectations.size() - failed) + " of " + expectations.size() + " expectations met. " + String.join("; ", verdicts);
        return new CheckResult(failed == 0 ? Outcome.PASSED : Outcome.FAILED, (double) failed, 0.0, detail);
    }

    private static ComponentModel.Criterion criterionOf(CircuitExpectation expectation) {
        ComponentModel.Criterion.Kind kind = ComponentModel.Criterion.Kind.valueOf(expectation.criterion().toUpperCase(Locale.ROOT));
        return new ComponentModel.Criterion(kind, expectation.minimum(), expectation.maximum(), expectation.exact());
    }
}
