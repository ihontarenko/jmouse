package org.jmouse.parallax;

/**
 * One way of showing a mathematical object — an entry of its representation stack. Every kind is computed by
 * exactly one {@link Computation}; which object kinds it admits is that computation's to say. The order is the order
 * a reader is offered them in, and an object whose text names none opens on the first that applies — so the picture
 * of an object (a surface before its level sets, an escape-time picture before one point's orbit) comes first.
 */
public enum RepresentationKind {

    SURFACE(Family.GEOMETRY),
    CURVE(Family.GEOMETRY),
    IMPLICIT_SURFACE(Family.GEOMETRY),
    LEVEL_SET(Family.GEOMETRY),
    POINTS(Family.GEOMETRY),
    CONSTRUCTION(Family.GEOMETRY),
    REGION(Family.GEOMETRY),
    TURTLE_PATH(Family.GEOMETRY),

    CROSS_SECTION(Family.ANALYSIS),
    PROJECTION(Family.ANALYSIS),
    GRADIENT(Family.ANALYSIS),
    NORMALS(Family.ANALYSIS),
    CURVATURE(Family.ANALYSIS),
    TANGENT_PLANE(Family.ANALYSIS),
    CRITICAL_POINTS(Family.ANALYSIS),
    DERIVATIVE(Family.ANALYSIS),
    INTEGRAL(Family.ANALYSIS),

    VECTOR_ARROWS(Family.FIELDS),
    STREAMLINES(Family.FIELDS),
    PARTICLES(Family.FIELDS),
    SCALAR_FIELD(Family.FIELDS),

    TRAJECTORY(Family.DYNAMICS),
    PHASE_PORTRAIT(Family.DYNAMICS),
    FLOW_IMAGE(Family.DYNAMICS),
    EQUILIBRIA(Family.DYNAMICS),
    ESCAPE_TIME(Family.FRACTALS),
    NEWTON_BASINS(Family.FRACTALS),
    ORBIT(Family.DYNAMICS),
    BIFURCATION(Family.DYNAMICS),

    DOMAIN_COLORING(Family.COMPLEX),
    RIEMANN_SPHERE(Family.COMPLEX),

    CHAOS_GAME(Family.FRACTALS),

    TRANSFORMED_GRID(Family.LINEAR_ALGEBRA),
    MATRIX_ANALYSIS(Family.LINEAR_ALGEBRA),
    SVD_DECOMPOSITION(Family.LINEAR_ALGEBRA),

    POLYTOPE_PROJECTION(Family.HIGHER_DIMENSIONS),
    POLYTOPE_SLICE(Family.HIGHER_DIMENSIONS),

    PARAMETER_SWEEP(Family.PARAMETERS),
    GRADIENT_DESCENT(Family.OPTIMIZATION),

    DENSITY(Family.PROBABILITY),
    SAMPLES(Family.PROBABILITY),
    RANDOM_WALK(Family.PROBABILITY),

    SEQUENCE_TERMS(Family.SEQUENCES),

    VALUE(Family.VALUES),

    TRANSIENT_RESPONSE(Family.CIRCUITS),
    FREQUENCY_RESPONSE(Family.CIRCUITS),
    NOISE_RESPONSE(Family.CIRCUITS),
    OPERATING_POINT(Family.CIRCUITS),
    PHASOR_DIAGRAM(Family.CIRCUITS),
    DC_SWEEP(Family.CIRCUITS),

    PROGRAM_OUTPUT(Family.PROGRAMS);

    public enum Family {
        GEOMETRY, ANALYSIS, FIELDS, DYNAMICS, COMPLEX, FRACTALS, LINEAR_ALGEBRA, HIGHER_DIMENSIONS, PARAMETERS,
        OPTIMIZATION, PROBABILITY, SEQUENCES, VALUES, CIRCUITS, PROGRAMS
    }

    private final Family family;

    RepresentationKind(Family family) {
        this.family = family;
    }

    public Family family() {
        return family;
    }
}
