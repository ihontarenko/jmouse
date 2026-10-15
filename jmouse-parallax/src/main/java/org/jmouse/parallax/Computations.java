package org.jmouse.parallax;

import org.jmouse.parallax.object.ObjectDefinition;
import org.jmouse.parallax.object.ParameterDefinition;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Every representation and the computation behind it. The boot fails when a representation kind has no
 * computation or two, so the enum and the implementations cannot drift apart.
 */
public class Computations {

    private final Map<RepresentationKind, Computation> byKind = new EnumMap<>(RepresentationKind.class);

    public Computations(List<Computation> computations) {
        for (Computation computation : computations) {
            Computation previous = byKind.put(computation.kind(), computation);
            if (previous != null) {
                throw new IllegalStateException("Two computations for " + computation.kind() + ": "
                        + previous.getClass().getName() + " and " + computation.getClass().getName());
            }
        }
        for (RepresentationKind kind : RepresentationKind.values()) {
            if (!byKind.containsKey(kind)) {
                throw new IllegalStateException("No computation implements the representation " + kind);
            }
        }
    }

    public Computation require(RepresentationKind kind) {
        return byKind.get(kind);
    }

    public Optional<Computation> forObject(RepresentationKind kind, ObjectDefinition definition) {
        Computation computation = byKind.get(kind);
        if (definition instanceof ObjectDefinition.Family family) {
            return computation.accepts(family.member()) ? Optional.of(new FamilyComputation(computation)) : Optional.empty();
        }
        return computation.accepts(definition) ? Optional.of(computation) : Optional.empty();
    }

    /** The representations an object admits, in declaration order: what its representation stack may hold. */
    public List<RepresentationKind> applicableTo(ObjectDefinition definition) {
        if (definition instanceof ObjectDefinition.Family family) {
            return applicableTo(family.member());
        }
        return byKind.values().stream().filter(computation -> computation.accepts(definition))
                .map(Computation::kind).sorted().toList();
    }

    /** The representations that mean something for this object with its parameters — what a reader is offered. */
    public List<RepresentationKind> applicableTo(ObjectDefinition definition, List<ParameterDefinition> parameters) {
        if (definition instanceof ObjectDefinition.Family family) {
            return applicableTo(family.member());
        }
        return byKind.values().stream().filter(computation -> computation.accepts(definition, parameters))
                .map(Computation::kind).sorted().toList();
    }

    public List<Computation> all() {
        return List.copyOf(byKind.values());
    }
}
