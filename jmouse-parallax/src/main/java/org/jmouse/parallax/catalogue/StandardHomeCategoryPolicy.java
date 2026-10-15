package org.jmouse.parallax.catalogue;

import org.jmouse.parallax.object.ObjectKind;

import java.util.Optional;

/**
 * Every kind's home among the sections the shipped catalogue names ({@code catalogue.jmx}).
 *
 * <p>The switch is exhaustive on purpose: a new kind does not compile until it has a home.</p>
 */
public class StandardHomeCategoryPolicy implements HomeCategoryPolicy {

    @Override
    public Optional<String> homeOf(ObjectKind kind) {
        return Optional.of(switch (kind) {
            case EXPLICIT_SURFACE, IMPLICIT_SURFACE, PARAMETRIC_SURFACE -> "SURFACES";
            case EXPLICIT_CURVE, IMPLICIT_CURVE, PARAMETRIC_CURVE, REGION -> "CURVES";
            case IMPLICIT_HYPERSURFACE, POLYTOPE -> "HIGHER_DIMENSIONS";
            case VECTOR_FIELD -> "FIELDS";
            case DIFFERENTIAL_SYSTEM, DISCRETE_MAP -> "DYNAMICS";
            case COMPLEX_MAPPING -> "COMPLEX";
            case ESCAPE_TIME_FRACTAL, NEWTON_FRACTAL, ITERATED_FUNCTION_SYSTEM, LINDENMAYER_SYSTEM -> "FRACTALS";
            case MATRIX, AFFINE_TRANSFORMATION -> "LINEAR_ALGEBRA";
            case NONLINEAR_TRANSFORMATION, POINT_SET, GEOMETRIC_CONSTRUCTION -> "GEOMETRY";
            case PROBABILITY_DISTRIBUTION -> "PROBABILITY";
            case SEQUENCE -> "SEQUENCES";
            case VALUE -> "CALCULUS";
            case PROGRAM -> "PROGRAMS";
            case CIRCUIT, COMPONENT, SYMBOL -> "CIRCUITS";
            case GRAPH -> "GRAPHS";
            case FAMILY -> "CURVES";
        });
    }
}
