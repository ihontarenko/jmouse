# 06 — Readings and the live run

What is read off a solution, by whom, and how it reaches a frame.

## What a part is — one rule

`PartIndex`: a device belongs to the part it names (`Device.part()`). A device that names none — a node's leak, a
failed part's stand-in, a logic pin's capacitance — belongs to no part and is never read. A part of several units
(`A1.a`, `A1.b`) is all of them, added up. Every reading below goes through this one index.

## The definitions the engine supplies

```
 power of a model      Σ over its current equations  i_dissipated · (v_a − v_b)
                       + Σ over its voltage equations  i_branch · (v_a − v_b)
                       where i_dissipated is the current with every charge's rate at nought:
                       a charging current stores energy and gives it back; it is not heat
 power of a source     v · i   (negative: it delivers)
 current into a pin    Σ of the currents of the equations that name the pin: what `current a -> b`
                       carries leaves pin a inside the part, so it ENTERS the part at a
```

A current is read off the line the equation was stamped as, at the solution — the current Kirchhoff's sums were
solved with. The value at the Newton guess alone is not: a diode that turned off within the step kept its conducting
current there.

## One piece running — LiveIntegrator

```
                       TransientStepper.step(…)
                               │
         ┌─────────────────────┼──────────────────────────────────────────────┐
         │ while the step is whole (AcceptedStep)                              │ after it
         ▼                                                                     ▼
   PartLedger.accepted                                     ┌──────────────────────────────────────┐
     each device's power ONCE                              │ NodeEnvelope.take   peaks, integrals  │
       → its heat energy                                   │ TraceRecorder.take  terminal columns  │
       → its part's energy (a step that landed on          │ MeterRecorder.take  metered parts     │
         an edge: the power before it)                     └──────────────────────────────────────┘
     every 0.1 ms on a realistic bench: advance heat
```

| Reader | Keeps | Read as |
|---|---|---|
| `PartLedger` | each part's energy since the last reading; each warming device's heat energy | a part's average power, its temperature (`readParts`) |
| `NodeEnvelope` | each node's lowest and highest voltage since the last read sample, over every step; each node's integral | a sample's `SampleEnvelope`: peaks and the exact mean over its interval |
| `TraceRecorder` | every terminal's current, every node's voltage, weighted by the step it held | a traced sample's `SampleWindow`: means and RMS over its stretch |
| `MeterRecorder` | the metered parts' currents, power and states at the last two step ends | a sample's `MeteredPart`, laid along the step to the sample's time |

Samples are read off the solution — on the parabola through the last three accepted points — never by forcing a step
to land on them: a calm stretch between two edges is a few long steps, not one per sample. A step brings its
**start** into the peaks; its end belongs to the next interval. The peaks of a difference a probe reads across a part
are kept as the difference's own: the lowest of one node less the highest of the other is a band twice as wide.

## The pieces together — LiveCircuitSolver

Each piece samples at the same instants, so a sample of the whole is the pieces' samples placed in the whole
circuit's node order. A part the cut split between two pieces (a supply both use) adds up. A part made of parts
(`ComponentHierarchy.Assembly`) reports by its own pins: the current that crosses its boundary.

```
 loads()  every part: average power since asked, current into each terminal now, temperature (°C),
          its limit (T_maximum) and rating (power_rating), how it failed
 levels() every digital node's LogicLevel
 continuing(object, previous)   the same circuit with new values, from now: state, histories, digital life, heat
                                carried; the integration restarts as at an edge. A topology change starts again.
```

## The session — what a person watches

`LiveCircuitSession` holds the solver, the probes and two collaborators:

```
 FrameReader    a frame's samples → each probe's channel:
                  voltage   positive − negative             (NodeReference: a node, ground, or absent → NaN)
                  current   into the probe's terminal of its part
                  power     the part's
                  reading   an instrument's own `measure` expression, over its pins and its part's readings
                → each voltage channel's peaks, every node's mean level, the trace columns (ColumnNames)
 PartFailures   on a realistic bench: a part past its limit fails as its component says
                (open | short | short <pins>) — written into the circuit as the bench's own failure,
                the run goes on from where it was with the part failed, cooling by its own heat law
 PartDisplays   each part's `display` figures (a capacitor's charge, a lamp's glow), each an expression of
                the same language an instrument reads; stresses at their worst over the frame
```

A frame (`LiveCircuitSession.Frame`) is then encoded by the live module (page 01).

## Columns — one naming

`ColumnNames`: a node's voltage by the node (`out`, V); a part's current by the part (`R1`, the current into its first
terminal, A) or by its pin (`U1.output`); a part's power as `R1.power` (W). The live trace, a computed transient, an
operating point and a sweep all name a reading this way, so a reader of one finds it in another.
