package org.jmouse.cad.store.activity;

/**
 * How many circuits run live now — what the live module tells the engine's activity. An installation without the live
 * module has none, and its activity says nought.
 */
public interface LiveActivity {

    /** The circuits running live now: one a socket that holds one. */
    int running();
}
