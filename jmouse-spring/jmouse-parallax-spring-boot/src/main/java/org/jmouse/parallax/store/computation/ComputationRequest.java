package org.jmouse.parallax.store.computation;

import org.jmouse.parallax.RepresentationKind;
import org.jmouse.parallax.owner.ParallaxOwner;
import org.jmouse.parallax.object.ObjectDefinition;
import org.jmouse.parallax.object.ParameterDefinition;

import java.util.List;
import java.util.Map;

/**
 * "Compute this representation of this object at these parameter values." The object is named by its `subject`
 * (`catalogue:<slug>` or `inline:<formula>`, see ComputationSubject), or spelled out: a stored one (`objectId`,
 * catalogue or owned) or defined inline in a scene (`definition` with its `parameters`, which then needs the
 * `owner` it is computed for — a host owner such as a workspace or a project in it). `sceneId` only attributes a
 * queued job.
 */
public record ComputationRequest(String subject, String objectId, ObjectDefinition definition, List<ParameterDefinition> parameters,
                                 Map<String, Double> values, Map<String, String> integerValues, RepresentationKind representation,
                                 Map<String, Object> settings, ParallaxOwner owner, String sceneId,
                                 Mode mode) {

    public enum Mode {
        /** Answer now when within the inline budget, otherwise queue a job. */
        AUTOMATIC,
        /** Always queue a job, even for a light computation (a parameter sweep run on purpose). */
        QUEUED
    }

    /** The same request with its object spelled out, and the subject that named it resolved away. */
    public ComputationRequest naming(String namedObjectId, ObjectDefinition namedDefinition, List<ParameterDefinition> namedParameters) {
        return new ComputationRequest(null, namedObjectId, namedDefinition, namedParameters, values, integerValues, representation, settings,
                owner, sceneId, mode);
    }

    /** The same request with one more value set: a frame of an animation over that value. */
    public ComputationRequest withValue(String name, double value) {
        Map<String, Double> changed = new java.util.HashMap<>(values);
        changed.put(name, value);
        return new ComputationRequest(subject, objectId, definition, parameters, changed, integerValues, representation, settings,
                owner, sceneId, Mode.AUTOMATIC);
    }

    public ComputationRequest {
        values = values == null ? Map.of() : Map.copyOf(values);
        integerValues = integerValues == null ? Map.of() : Map.copyOf(integerValues);
        settings = settings == null ? Map.of() : Map.copyOf(settings);
        parameters = parameters == null ? List.of() : List.copyOf(parameters);
        if (mode == null) {
            mode = Mode.AUTOMATIC;
        }
    }
}
