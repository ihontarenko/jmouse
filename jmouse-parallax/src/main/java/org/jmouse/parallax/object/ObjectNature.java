package org.jmouse.parallax.object;

/**
 * What kind of thing an object is at the coarsest grain — and so in what it is defined and where it is worked on.
 *
 * <p>A kind decides the shape of a definition and, through the computations that accept it, how the object may be
 * shown. Its nature decides the rest: which language or editor writes the definition, and which workbench a reader
 * opens it in — a plotter for mathematics, a runner for a program, an simulator for a circuit. It is
 * <strong>derived</strong> from the kind ({@link ObjectKind#nature()}) and never stored, so it cannot disagree with
 * it.</p>
 */
public enum ObjectNature {

    /** Defined declaratively in jMF — curves, surfaces, fields, fractals, maps, distributions. */
    MATHEMATICAL,

    /** jMF formulas processed by a jMS program, whose answer is what is shown. */
    PROGRAM,

    /** An electronic circuit: a netlist simulated over time and frequency. */
    CIRCUIT,

    /** A component model in jMC, used by circuits and checked against its datasheet on a bench. */
    COMPONENT,

    /** A reusable editable drawing assigned to a component model. */
    SYMBOL,

    /** Lines drawn together on panes, moved by one clock: the Graph workbench, saved. */
    GRAPH
}
