# 07 — Nuances

Things that look wrong and are not, and things that cost real time to learn. Each one names where it is decided.

## Numerics

* **Error is measured in what is stored, never in a node's voltage** (`Truncation`). A node behind a blocking diode
  with a few picofarads may wander by millivolts without anything noticing; judged by its voltage, it kept a whole
  circuit at nanosecond steps.
* **Backward Euler after every edge, trapezoidal otherwise.** The trapezoidal rule rings after a jump — its rate flips
  sign every step and never dies while the voltage stands still. That is also why a charging current is left out of a
  part's power: counted in, a switched-off MOSFET at 12 V "dissipated" 9 kW.
* **A step that does not settle is cut once, not twice** (`TransientStepper.newton`). A shorter step starts closer to
  its answer; where the cut step does not settle either, length is not what is wrong — a converter's first edge
  settled at no length down to 10⁻¹⁶ s, and cutting towards it ran out of steps.
* **Singular at the first guess is the circuit's fault only sometimes.** At t = 0, or where an unknown no row
  mentions (a node a switch just left floating), it is; anywhere else a prediction landed where the circuit cannot be
  — a Zener's cathode predicted at 15 V on a 12 V bench — and the step is cut instead of the run stopped.
* **Bypass is per attempt, and across steps only for a unit with no charge and no time.** The integration's
  coefficient enters a state row when it is written, not when it is linearized, so a new step size does not undo it.
* **The two Newton settlings differ on purpose** (page 05): an operating point starts from a guess nothing was solved
  at and needs a confirming iteration; a transient step starts from a prediction it is linearized exactly at.

## Elaboration

* **The bench's figures come first among a part's parameters** (`PartParameters`), so the bench, writing on a part,
  overrides them like any other override — and a part's equations read the bench without knowing there is one.
* **A primitive written in pin order reports "+" and "−"**, though its document names the pins
  `terminal_one` / `terminal_two`: a sheet draws it that way, and a meter names it that way.
* **A component that is one primitive keeps its parent's id** when flattened — R1 written as the resistor component
  is the resistor R1, its current reported as R1's.
* **A multi-unit package contributes only the units wired into the circuit.** An idle amplifier with floating inputs
  has no operating point; inventing nodes for it would make the whole circuit unsolvable.
* **A failed part is still a part.** Its stand-ins are the bench's; the part itself is reported as failed, with no
  power, cooling by its own heat law.

## Readings

* **Read a sweep's currents at each point, not after the sweep.** A nonlinear part's current is read off the line it
  was stamped as; after the sweep that line is the last point's. (It was read after, and a diode's cathode carried
  0.19 A at 0 V.)
* **A sample is not a step.** Samples are read off the solution between steps; a step that crosses a sample does not
  stop there. Peaks and means are kept step by step so a fast signal between samples is not lost.
* **A traced moment that keeps one sample in many is the stretch's mean, with its RMS beside it.** A current that
  alternates faster than the moments has a mean near zero and an RMS that is not: it moves no charge, and still heats.

## The cache and the build

* **Parsing is cached by text** (`ExperienceReader.readOne`); **building a model is cached by its text and the texts
  it used** (`ComponentDocuments.read`). Editing a base component rebuilds its members the next time they are read.
* **`mvn test` without `clean` keeps resources that `src` no longer has.** The SPICE test once passed on stale copies
  in `target/` that the engine silently ran as Java primitives. The engine now refuses an element it has no device
  for, so a stale copy fails loudly.

## Speed — what it is spent on

Measured with JFR on the benchmark circuits (`PairedEngineBenchmark`, `LiveEngineBenchmark`):

* the tapes of nonlinear models (a diode's junction) — the physics itself;
* the sparse elimination — replayed, not replanned;
* a model's power, once per step per device — the ledger shares it between heat and energy;
* nothing per step allocates a map: terminals are columns laid out once (`TraceRecorder`).

A change to the engine is not done until `PairedEngineBenchmark` says the answer did not move and the speed did not
fall: the machine drifts 15–25 % within a session, so only paired, interleaved runs say anything.
