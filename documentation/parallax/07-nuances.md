# 07 — Nuances

Things that look wrong and are not, and things that cost real time to learn. Each one names where it is decided.

## Numerics

* **Error is measured in what is stored, never in a node's voltage** (`Truncation`). A node behind a blocking diode
  with a few picofarads may wander by millivolts without anything noticing; judged by its voltage, it kept a whole
  circuit at nanosecond steps.
* **Backward Euler after every edge, trapezoidal otherwise.** The trapezoidal rule rings after a jump — its rate flips
  sign every step and never dies while the voltage stands still. That is also why a charging current is left out of a
  part's power: counted in, a switched-off MOSFET at 12 V "dissipated" 9 kW.
* **A generator's edge never ends a step at the new level** (`TransientStepper.step`). The step before it ends at the
  last instant of the old level; the jump is the next, short step's. Ended at the new level, the step was a ramp as long
  as itself, and how a transistor switched depended on the sample interval — the overshoot of a MOSFET's drain vanished
  at 25 ns samples. A sample at the edge's own instant reads the old level, as ngspice's does.
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
* **Everything a circuit compiles is compiled once per FORM, never per part**: a model's equations (`ModelEquations`),
  a heat law (`PartHeat`), a generator's signal (`SourceSignal`) — keyed by what they are written as and the names of
  their parameters, the values being arguments. A heat law was compiled for every part on every run: a ladder's two
  hundred resistors compiled the one shared law two hundred times.
* **`mvn test` without `clean` keeps resources that `src` no longer has.** The SPICE test once passed on stale copies
  in `target/` that the engine silently ran as Java primitives. The engine now refuses an element it has no device
  for, so a stale copy fails loudly.

## Speed — what it is spent on

Measured on the benchmark circuits (`PairedEngineBenchmark`, `LiveEngineBenchmark`, phase timers):

* the tapes of nonlinear models (a diode's junction) — the physics itself;
* the sparse elimination — replayed, not replanned;
* a model's power, once per step per device — the ledger shares it between heat and energy;
* nothing per step allocates a map: terminals are columns laid out once (`TraceRecorder`).
* only the devices that store something are asked for their error and their acceptance (`Device.stores`) — in a ladder
  that halves both; and an affine unit reads its charges from flat columns (`AffineLinearization`), never a map.
* a model's charge is evaluated once per settled attempt (`Device.measure`), and its error is judged against the
  polynomial through its own accepted charges, as SPICE judges it. It used to be evaluated three times a step — at the
  solution for the error, at the predicted voltages for the error again, and at the solution for the acceptance —, and
  a junction's charge formula is its whole exponential: a 12-stage diode multiplier spent a third of its time there
  (truncation 34 ms and acceptance 19 ms → 3 and 2 ms).

Measured with phase timers, not with JFR: a sampling profiler attributed 30 % of a ladder to `chargeAt`, which cost 4 %
— inlined code is charged to whatever frame the sample lands in.

A large linear circuit now costs what ngspice's own analysis does: `rc-ladder-200` (400 parts from documents, one
coupled piece) 101 ms against 92 ms, `rc-ladder-40` 24 against 23, warm (2026-10-19).

A change to the engine is not done until `PairedEngineBenchmark` says the answer did not move and the speed did not
fall: the machine drifts 15–25 % within a session, so only paired, interleaved runs say anything.
