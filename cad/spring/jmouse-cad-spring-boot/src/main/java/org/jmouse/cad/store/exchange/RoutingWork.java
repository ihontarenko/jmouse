package org.jmouse.cad.store.exchange;

/**
 * The kinds of routing work over one board, each answered by an engine of its own: what the hand waits on — a leg, a segment, a part
 * carried —, and what a pcb is looked over for once it rests — its nodes, its jogs, its corners. An engine answers one ask at a time,
 * so the looking over never holds a gesture up.
 *
 * @author Ivan Hontarenko (Mr. Jerry Mouse)
 * @author ihontarenko@gmail.com
 */
public enum RoutingWork {
    GESTURES, RESTING
}
