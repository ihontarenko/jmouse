/**
 * The circuit engine: an elaborated circuit ({@code circuit.elaboration}) numbered into a
 * {@link org.jmouse.parallax.circuit.Netlist}, stamped by its devices into a modified-nodal system, and solved — an
 * operating point, a small signal, a transient that keeps running —; and what every analysis and every reader reads off
 * the solution.
 *
 * <p>Within it, by responsibility, each reading only what is above it in this list:</p>
 * <ul>
 *   <li><strong>What is solved</strong> — {@code Netlist}, the {@code Device}s (a model unit as
 *       {@code ModelDevice} over {@code UnitLayout}, {@code UnitArguments}, {@code UnitLine} and a
 *       {@code UnitLinearization}), {@code ModelEquations}, {@code SourceSignal}, {@code PartHeat}, the digital side
 *       ({@code EventSimulation}, {@code DigitalUnit}, {@code DigitalEvent}, {@code SimulatorFunctions},
 *       {@code DigitalDriver}, {@code LogicLevel}, {@code LogicFamily}, {@code OutputBridge}).</li>
 *   <li><strong>How</strong> — {@code Assembly}, {@code CircuitNewton}, {@code CircuitSolver} (operating point, small
 *       signal), {@code TransientStepper} (the transient's step control).</li>
 *   <li><strong>What is read</strong> — {@code PartIndex} (the one rule of what a part is), {@code PartLedger},
 *       {@code NodeEnvelope}, {@code TraceRecorder}, {@code MeterRecorder}, {@code PartReadings}, {@code ColumnNames}.</li>
 *   <li><strong>Running</strong> — {@code LiveIntegrator} (one piece), {@code LiveCircuitSolver} (the pieces
 *       together), {@code LiveCircuitSession} (probes, frames, {@code PartFailures}, {@code FrameReader}).</li>
 *   <li><strong>Analyses</strong> — {@code CircuitComputations}, {@code TransientRun}, {@code ComponentCheck}.</li>
 * </ul>
 *
 * <p>The engine stays one package so that its core stays package-private: what a host sees is the analyses, the
 * running session and the readings they answer with.</p>
 */
package org.jmouse.parallax.circuit;
