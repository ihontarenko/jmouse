/**
 * A circuit as it is written turned, once, into the circuit as it is simulated
 * ({@link org.jmouse.parallax.circuit.elaboration.ElaboratedCircuit}): its bench applied to every part, every part made
 * of a subcircuit replaced by the parts it is made of, every part written in pin order bound as its document's model,
 * every component it uses read, and every part's parameters resolved by one rule
 * ({@link org.jmouse.parallax.circuit.elaboration.PartParameters}).
 *
 * <p>The middle layer: it reads components ({@code org.jmouse.parallax.component}) and is read by the engine
 * ({@code org.jmouse.parallax.circuit}). A netlist, the pieces a circuit is cut into, a part's limits, its displays and
 * its instruments are all built from one elaborated circuit — none of them reads a document or applies a bench
 * again.</p>
 */
package org.jmouse.parallax.circuit.elaboration;
