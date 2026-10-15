package org.jmouse.parallax.value;

import org.jmouse.parallax.Computation;
import org.jmouse.parallax.ComputationProgress;
import org.jmouse.parallax.ComputationSettings;
import org.jmouse.parallax.Dataset;
import org.jmouse.parallax.RepresentationKind;
import org.jmouse.parallax.SettingSpecification;
import org.jmouse.parallax.object.CompiledObject;
import org.jmouse.parallax.object.ObjectDefinition;
import org.jmouse.parallax.object.ObjectDefinition.Value;

import java.util.List;
import java.util.Map;

/** The representation of a {@code value}: the one number its expression evaluates to. */
public final class ValueComputations {

    private ValueComputations() {
    }

    /**
     * Evaluates the expression with the parameters' current values and gives it back as a measurement named after
     * the output, the expression as its note. A value has no inputs, so there is nothing to sample.
     */
    public static class Evaluation implements Computation {

        public RepresentationKind kind() {
            return RepresentationKind.VALUE;
        }

        public boolean accepts(ObjectDefinition definition) {
            return definition instanceof Value;
        }

        public List<SettingSpecification> settings() {
            return List.of();
        }

        public long estimatedCost(CompiledObject object, ComputationSettings settings) {
            return 1;
        }

        public Dataset compute(CompiledObject object, ComputationSettings settings, ComputationProgress progress) {
            Value value = (Value) object.definition();
            return new Dataset.Measurements(Map.of(value.name(), object.constant(value.expression())),
                    Map.of(value.name(), value.expression()));
        }
    }
}
