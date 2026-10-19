# 05 — Newton and the transient

## Newton — one loop

`CircuitNewton.solve(assembly, start, iterations, searching, linearization)` is the only Newton–Raphson loop in the
engine; the operating point and every transient step run it, each with its own settling.

```
 guess = start
 repeat ≤ iterations:
   ┌─ assemble the system at the guess                       (the caller's Linearization)
   │  searching?  residual not lower than the last kept point → halve the step back towards it, again
   ├─ solve                                                  singular at the first guess → the caller decides:
   │                                                         the circuit's own (refuse) or the guess's (cut the step)
   ├─ linear circuit?  → done: one solve is the answer
   ├─ settled?         → done
   ├─ any value not finite → no answer (the step is cut)
   └─ guess = NewtonSteps.limited(guess, next)
              node voltages: the whole step scaled so none moves more than 10 V
              states: each clipped to ±10 on its own
              branch currents: free — they follow
```

| | operating point | transient step |
|---|---|---|
| settled when | every \|Δx\| ≤ 1e-6·\|x\| + 1e-6 | every \|Δx\| ≤ 1e-4·\|x\| + 1e-6 |
| believed on iteration 0 | no — a second iteration confirms | yes — iteration 0 is linearized exactly at the prediction |
| iterations | 300, then again with a line search | 10; a cut step 60; then 300 with a line search |

Why limit steps: a model in saturation has an almost flat equation, and a full Newton step off a flat equation lands
anywhere — an op-amp's gain stage asked to jump by its open-loop gain, 10⁶ V. Why the line search is a fallback, not
the default: where a diode is off its rows are a few picosiemens, and the residual is too rough a measure for a
circuit plain Newton solves at once.

## The operating point

`CircuitSolver.operatingPoint` — capacitances open, windings shorted, generators at their value at t = 0:

```
 Newton from rest ──── settles ──► done
        │ no
        ▼
 source stepping: every source raised 1/20, 2/20, … 20/20, each solved from the one before ──► done
        │ a stage did not settle
        ▼
 gmin stepping: every node held to ground by 10 mS, withdrawn a decade at a time to 1 pS, then 0 ──► done
        │
        ▼
 refused, naming what nothing determines ("Nothing determines 'the voltage of n3'")
```

A small-signal analysis (Bode, noise, phasors) linearizes every device at that point; a linear circuit's
linearization does not depend on its bias, so it is taken at rest.

## The transient — TransientStepper

```
                    ┌───────────────────────── one accepted step ─────────────────────────┐
 t ──────────────────●───────────────────────────────────────────────────────────────────●──── t + h
                     │                                                                    │
 1  h = min(step, maximumStep, the next digital event)
 2  a generator's edge inside?  far: stop one sample short of it · near: land exactly on it
 3  integration = backward Euler after an edge (restart), else trapezoidal
 4  predict x(t+h): a parabola through the last three accepted points (or a line through two)
 5  Newton from the prediction                                ── no answer → h / 8, once; then longer Newton
 6  local error = what the devices STORE moved off the prediction  (Truncation)
       charge, flux:   |Δq| / (7 · (1e-3·|q| + 1e-14 C))
       a model state:  |Δs| / (7 · (1e-3·|s| + 1e-4))
       three points:   ÷ 10 (Milne: corrector − predictor ≈ 13 trapezoidal errors)
 7  error > 1, or a digital input crossed its threshold inside → retry shorter
       h · max(0.2, min(2, 0.9 · error^(−1/order)))     never blind halving
 8  accept: every device accepts (charges, rates, companions), the ledger takes the step (power → energy, heat),
    the stepper moves on, the event simulation advances (a crossing may restart the integration)
```

What decides `maximumStep`: one sample interval while every sample is traced; up to 10 µs (SPICE's TMAX, the interval
the engine was matched against ngspice at) when a trace keeps one sample in several; and a generator's own
`maximumStep` — under half the shortest stretch between two of its edges, so no step holds two.

A step that **lands on an edge** is solved with the level after the jump; what lies inside it is the level before —
held, not a ramp. Readers know (`landedOnEdge`).

### How a generator's edges are found

`SourceSignal` derives an edge function from the formula itself — no square wave is known to the engine:

```
 l < r            →  l − r
 step(x), sign(x) →  x
 floor(x)         →  sin(πx)
 mod(x, p)        →  sin(πx/p)
```

A sign change of an edge function between the step's ends is an edge; it is bisected to the step's resolution and the
step lands just after it. `mod(frequency·time, 1) < duty` gets both of a square's edges from one comparison.

## Digital parts — the event simulation

`EventSimulation` runs beside the analog solve: it schedules, settles an instant (every handler that fires at one
instant reads the state as it was before it, and their writes land together after — the way a register samples on an
edge) and resolves each digital node from its drivers. Each model's discrete side is a `DigitalUnit` — its script,
states, drivers, inputs, watched expressions, periods and timers —; what happens to it is a `DigitalEvent` of a kind
(`START`, `RISING`, `FALLING`, `CHANGE`, `ABOVE`, `BELOW`, `EVERY`, `TIMER`); what its script may call (`drive`,
`schedule`, `since`, …) is `SimulatorFunctions`. A model with a `behaviour` script has digital pins:

```
 analog node ──► input: family thresholds input_low/high(supply) → LOW / HIGH, held between them
 digital node ─► input: the node's resolved level (released → unknown)
 script        on rising / falling / change / above / below / every / timer / start  → drive(pin, level, delay)
 output        a DigitalDriver: transactions on a timeline (transport delay: a new one removes only those at or
               after it) → an OutputBridge on an analog node: pulls the pin through output_resistance(supply)
               towards a fraction of the rails, moving over the family's edge time
```

Levels are `LogicLevel` (LOW, HIGH, UNKNOWN, RELEASED); a script reads and writes them as 0, 1, `X`, `Z`. A
watched expression (`above` / `below`) crossing inside a step makes the step retry just past the crossing, found by
regula falsi between its ends.

## Heat — between steps, never inside Newton

A part that warms has a `heat {}` law (`PartHeat`): its temperature's rate as a function of the power it takes.

```
 every accepted step     PartLedger: power(solution) × h  → the part's heat energy
 every 0.1 ms of circuit time, on a realistic bench:
                         heat law advanced at the average power, backward Euler (NewtonSolver)
 the next step           the model reads its own temperature as `temperature`
```

Because the temperature moves only between steps, a part that drifts with its temperature is as linear — and as
cheap — inside Newton as one at a fixed temperature. On an ideal bench heat is only shown, never read.
