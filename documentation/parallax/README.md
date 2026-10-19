# Parallax circuit engine — how a circuit is solved

How a circuit travels from a browser to a number on a screen, layer by layer: what reads it, what it becomes on the
way, which equations are built and how they are solved. Written for whoever changes the engine next — every claim
names the class that makes it true, so it can be checked against the code rather than believed.

The language a circuit and a component are written in is documented on its own: `../jmx/JMOUSE_PARALLAX_JMX.md`.
The module's whole API is `../JMOUSE_PARALLAX.md`. These pages are about what happens *inside*.

| Page | What it covers |
|---|---|
| [01 — From the browser to the engine](01-request-flow.md) | the two ways in: the live socket and a computed representation |
| [02 — Documents become models](02-documents-and-models.md) | a `component` jMX document → `ComponentModel`; the expression language; the model cache |
| [03 — Elaboration](03-elaboration.md) | a written circuit → the circuit as it is simulated: bench, subcircuits, primitives, parameters, pieces |
| [04 — Netlist and stamping](04-netlist-and-stamping.md) | unknowns numbered; devices; a model's equations compiled; the system assembled and solved |
| [05 — Newton and the transient](05-newton-and-transient.md) | the operating point, Newton, step control, edges, events, heat |
| [06 — Readings and the live run](06-readings-and-live.md) | what is read off a solution: parts, power, energy, peaks, trace, meters, failures, frames |
| [07 — Nuances](07-nuances.md) | the things that look wrong and are not, and the ones that cost real time to learn |

## The whole road on one page

```
  browser (Simulator page)                       browser (a graph, a datasheet check)
        │  WebSocket  /jmouse/parallax/live              │  REST  computation request
        ▼                                                 ▼
  ┌──────────────┐                                ┌──────────────┐
  │   LiveRun    │  one socket, one run           │  Computing   │  cache · cost · inline or job
  └──────┬───────┘                                └──────┬───────┘
         │ jMX text + values                             │ jMX text + values
         ▼                                               ▼
  ┌──────────────────────────────────────────────────────────────────┐
  │ jMX reader → ObjectDefinition.Circuit → DefinitionCompiler        │  org.jmouse.parallax.script / object
  │                                     → CompiledObject              │
  └──────────────────────────────┬───────────────────────────────────┘
                                 │
  ┌──────────────────────────────▼───────────────────────────────────┐
  │ COMPONENT LAYER     component jMX → ComponentModel (cached)       │  org.jmouse.parallax.component
  └──────────────────────────────┬───────────────────────────────────┘
                                 │
  ┌──────────────────────────────▼───────────────────────────────────┐
  │ ELABORATION         bench · subcircuits · primitives → models     │  org.jmouse.parallax.circuit.elaboration
  │                     ElaboratedCircuit, built once → pieces         │
  └──────────────────────────────┬───────────────────────────────────┘
                                 │
  ┌──────────────────────────────▼───────────────────────────────────┐
  │ ENGINE              Netlist → Devices → Assembly → SparseSystem   │  org.jmouse.parallax.circuit
  │                     CircuitNewton · CircuitSolver · TransientStepper
  │                     readers: PartIndex · PartLedger · NodeEnvelope · TraceRecorder · MeterRecorder
  └──────────────────────────────┬───────────────────────────────────┘
                                 │
         ┌───────────────────────┴───────────────────────┐
         ▼                                               ▼
  LiveCircuitSession → Frame → FrameEncoding        CircuitComputations → Dataset
  (binary frame on the socket, 25 a second)         (a time series, a Bode plot, a phasor dial …)
```

## The three rules the engine keeps

1. **Physics lives in documents.** A resistor's Ohm's law, a capacitor's charge, a diode's junction, a part's heat
   law, a logic family's thresholds — all are jMX. The engine supplies only definitions (power is Σ i·v) and
   numerics (how to solve). A new component in the catalogue needs no Java.
2. **Each rule is written once.** One resolver of a part's parameters, one Newton loop, one rule of what a part is,
   one place the names the engine gives every document live, one place a column is named. Two copies of a rule are
   two answers waiting to disagree.
3. **Each layer reads only the ones below it.** A document knows no circuit; elaboration knows no solver; the engine
   reads an elaborated circuit and never a document's text again.
