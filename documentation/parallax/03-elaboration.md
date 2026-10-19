# 03 — Elaboration: the circuit as it is simulated

`org.jmouse.parallax.circuit.elaboration`. What a person writes and what a solver solves are not the same circuit.
`ElaboratedCircuit.of(compiled)` turns one into the other — **once** — and everything later reads the result.

```
 CompiledObject (the circuit as written, its bench on it)
   │
   ▼
 ComponentHierarchy.expand
   │  requireSolvable()                      a circuit with nothing to solve is refused here
   │
   ├─ BenchConditioning.apply                the bench written onto the parts
   │     every part      ← overrides temperature (K), realistic, tolerance_corner, detail   (when not nominal)
   │     a part given    ← its own tolerance and temperature_coefficient
   │       its own spread
   │     an offset       ← a DC source of its own: in series with a voltage source (through node offset_N),
   │                       beside a current source
   │     a failed part   ← replaced by stand-ins:  open  → each node 1 GΩ to ground
   │                                               short → the named pins tied through 1 mΩ, the rest open
   │
   ├─ flatten                                every part made of a subcircuit → the parts it is made of
   │     child ids       BR1_D1 (parent _ line); a component that is ONE primitive keeps its parent's id
   │     child values    read in the parent's parameters (PartParameters with the subcircuit's own reading)
   │     the bench       passed down: a part made of parts on a bench at 85 °C is told so, as its parts are
   │     Assembly        what each such part became — so it can report by its own pins (page 06)
   │
   ▼
 ElaboratedCircuit
   ├─ asModels           every part written in pin order (R1 resistor (a, b) 1k) → a MODEL part of its
   │                     document: pins in order on the two nodes, the written value as the document's
   │                     first parameter, its currents reported under "+" and "−" as it is drawn
   ├─ models             every component it uses, read once (page 02), shared by every piece
   └─ conditioned        the parts as written, each on the bench: what a part's own readings — instruments,
                         displays, limits — are read over
```

After elaboration, every part is a model of its document. The only elements left that are not are a failed part's
stand-ins — a conductance the bench wrote — and the engine adds its own later (page 04).

## One rule for a part's parameters

`PartParameters.resolve(model, element, unit, reading)` — the equations, the heat law, an instrument, a display and a
limit all read a part through it, so none can read another part than the solver solves:

```
 1. the bench's figures as a nominal bench sets them   temperature = 298.15 K, realistic = 0, tolerance_corner = 0, detail = 1
 2. the component's own parameters                     resistance = 1000, tolerance = 0.01, …
 3. a primitive's written value                        R1 resistor (a, b) 4k7  →  resistance = 4700
 4. what the part overrides                            Vos = 2 mV       every unit's
                                                       A.Vos = 2 mV     unit A's only
                                                       temperature, realistic, …   the bench's, written by step 1 above
    an override of a parameter the component does not have is refused, naming the part
```

The order is the point: the bench comes first so that a part, and the bench writing on it, can override it.

## Pieces: where a circuit can be cut

`CircuitPartitions.of(elaborated)` cuts the circuit into pieces that cannot affect one another, so each is integrated
with its own step:

```
        V1 (5 V, holds node vcc against ground)
         │
   vcc ──┼───────────────┬────────────────┐
         │               │                │
      [timer 1]       [timer 2]        [load]          each timer meets the others only at vcc and ground;
         │               │                │            vcc's voltage is V1's whatever they draw, so
   gnd ──┴───────────────┴────────────────┘            they are three pieces, each with its own copy of V1
```

A node an **ideal voltage source** holds against ground has that voltage whatever is connected — so parts that meet
only there, and at ground, are independent. The cut is exact, not an approximation. The holders are found from the
models' own shape (`ComponentModel.isGenerator`, `generatesVoltage`); the rest is a union-find over the other nodes.
Ten timers on one supply cost ten timers' steps, not a hundred: a global step would shorten every timer's steps at
every other timer's edges.

Each piece is `elaborated.piece(elements)` — a subset of the parts, the same models, the same bench.
