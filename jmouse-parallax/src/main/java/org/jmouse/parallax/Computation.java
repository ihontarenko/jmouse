package org.jmouse.parallax;

import org.jmouse.parallax.object.CompiledObject;
import org.jmouse.parallax.object.ObjectDefinition;
import org.jmouse.parallax.object.ParameterDefinition;

import java.util.List;

/**
 * Turns a compiled object into the dataset of one representation. Implementations are stateless beans, one per
 * {@link RepresentationKind}; adding a representation is adding one of these, nothing else changes.
 */
public interface Computation {

    RepresentationKind kind();

    /** Whether this representation means anything for the object: contours of a surface yes, of a matrix no. */
    boolean accepts(ObjectDefinition definition);

    /**
     * Whether it means anything for this object with these parameters: what most representations decide from the
     * definition alone, and a sweep — which varies a parameter — cannot without one.
     */
    default boolean accepts(ObjectDefinition definition, List<ParameterDefinition> parameters) {
        return accepts(definition);
    }

    List<SettingSpecification> settings();

    /**
     * Roughly how many function evaluations the computation will make. Above the inline budget the work becomes a
     * queued job rather than a synchronous answer.
     */
    long estimatedCost(CompiledObject object, ComputationSettings settings);

    Dataset compute(CompiledObject object, ComputationSettings settings, ComputationProgress progress);
}
