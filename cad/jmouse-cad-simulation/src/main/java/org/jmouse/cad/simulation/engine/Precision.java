package org.jmouse.cad.simulation.engine;

import org.jmouse.cad.simulation.elaboration.BenchConditions;
import org.jmouse.cad.simulation.elaboration.BenchConditions.Detail;

/**
 * How finely the engine solves a run, at the bench's level of detail: the share of {@link Truncation}'s allowed error a
 * step may use, when Newton has settled, and the longest step while samples are read one in several (SPICE's TMAX).
 * {@link Detail#STANDARD} is SPICE's own tolerances, which every check is held to; a draft is ten times looser, a
 * precise run ten times tighter.
 *
 * @param errorBudget  how much of the allowed local error a step may use: 1 is SPICE's trtol · (reltol · |q| + chgtol)
 * @param settling     when a transient step's Newton has settled
 * @param accurateStep the longest step once a run reads one sample in several, seconds — as SPICE's TMAX is its print
 *                     step; at standard, the interval the engine was matched against ngspice at. The error control
 *                     alone lets a slow exponential take long steps whose error gathers where a comparator turns — a 555
 *                     ran 0.4 % fast with steps up to 2.5 ms
 */
record Precision(double errorBudget, CircuitNewton.Settling settling, double accurateStep) {

    static final Precision STANDARD = new Precision(1, CircuitNewton.Settling.TRANSIENT, 1e-5);

    /** The precision a bench asks for; standard without one. */
    static Precision of(BenchConditions bench) {
        return of(bench == null ? Detail.STANDARD : bench.detail());
    }

    static Precision of(Detail detail) {
        return switch (detail) {
            case DRAFT -> new Precision(10, new CircuitNewton.Settling(1e-3, 1e-5, false), 1e-4);
            case STANDARD -> STANDARD;
            case PRECISE -> new Precision(0.1, new CircuitNewton.Settling(1e-5, 1e-7, false), 1e-6);
        };
    }
}
