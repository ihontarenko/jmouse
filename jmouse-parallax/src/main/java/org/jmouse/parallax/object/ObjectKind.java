package org.jmouse.parallax.object;

/** What a mathematical object is. The definition's shape follows from it, and so do the representations it admits. */
public enum ObjectKind {

    EXPLICIT_CURVE,
    EXPLICIT_SURFACE,
    IMPLICIT_CURVE,
    IMPLICIT_SURFACE,
    IMPLICIT_HYPERSURFACE,
    PARAMETRIC_CURVE,
    PARAMETRIC_SURFACE,
    VECTOR_FIELD,
    DIFFERENTIAL_SYSTEM,
    DISCRETE_MAP,
    COMPLEX_MAPPING,
    ESCAPE_TIME_FRACTAL,
    NEWTON_FRACTAL,
    ITERATED_FUNCTION_SYSTEM,
    LINDENMAYER_SYSTEM,
    MATRIX,
    AFFINE_TRANSFORMATION,
    NONLINEAR_TRANSFORMATION,
    POINT_SET,
    GEOMETRIC_CONSTRUCTION,
    POLYTOPE,
    PROBABILITY_DISTRIBUTION,
    REGION,
    SEQUENCE,
    VALUE,
    CIRCUIT,
    PROGRAM,
    COMPONENT,
    SYMBOL,
    FOOTPRINT,
    GRAPH,
    FAMILY;

    /** The coarse nature this kind belongs to; every kind not named here is mathematics. */
    public ObjectNature nature() {
        return switch (this) {
            case CIRCUIT -> ObjectNature.CIRCUIT;
            case PROGRAM -> ObjectNature.PROGRAM;
            case COMPONENT -> ObjectNature.COMPONENT;
            case SYMBOL -> ObjectNature.SYMBOL;
            case FOOTPRINT -> ObjectNature.FOOTPRINT;
            case GRAPH -> ObjectNature.GRAPH;
            default -> ObjectNature.MATHEMATICAL;
        };
    }
}
