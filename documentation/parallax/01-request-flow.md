# 01 — From the browser to the engine

A circuit reaches the engine by one of two roads. Both end in the same engine; they differ in who waits for whom.

```
                      ┌──────────────────────────── browser ────────────────────────────┐
                      │                                                                  │
   Simulator page ────┤  WebSocket: start / set / probes / pace / sample / restart …    │  live: the server steps,
   (sheet, scope,     │  ← started · frame (binary, every 40 ms) · changed · problem    │  the browser watches
    meters, popover)  │                                                                  │
                      │                                                                  │
   Object page, ──────┤  REST: computation request (object, values, representation)    │  computed: one answer,
   datasheet check    │  ← dataset (time series, Bode, phasor dial, measurements)       │  cached
                      └──────────────────────────────────────────────────────────────────┘
```

## Road 1 — the live socket

`jmouse-parallax-live`, `org.jmouse.parallax.live.socket`. The socket answers at `jmouse.parallax.live.path`,
by default `/jmouse/parallax/live` (`ParallaxLiveRoutes`). Each open socket owns one `LiveRun`.

```
 browser                          LiveRun (one per socket)                     engine
 ───────                          ────────────────────────                     ──────
 → start {objectId, values,  ──►  read the stored circuit, or the definition
          interval, probes,       sent with it
          speed}                  LiveCircuits.prepare(object, circuit, values)
                                    └─ Catalogue.compileChecked(…, components.forCircuit(object))
                                       the ComponentLibrary a circuit may see:
                                       its own owner's parts, the published catalogue,
                                       the shared families and function libraries
                                  new LiveCircuitSession(compiled, interval)  ──►  ElaboratedCircuit.of(…)
                                  session.probe(probes)                            CircuitPartitions → pieces
 ← started {nodes, controls} ◄──                                                   one LiveIntegrator each
                                  every 40 ms (scheduleAtFixedRate):
                                    due += speed · 40 ms / interval samples
                                    the first ≤ N samples: session.advance(64…) ──► step, sample, read
                                    the rest:               session.pass(…)    ──► step, trace only
                                    stop at 35 ms of work; what is not reached
                                    is not owed to the next frame
 ← frame (binary)            ◄──  FrameEncoding: header JSON + float32 columns
 → set {part, parameter,     ──►  session.retune(compiled with the new value) ──► LiveCircuitSolver.continuing
        value}                                                                      same time, same state, restart
 ← changed {continued}       ◄──                                                   the integration as at an edge
 → restart {source}          ──►  a new session from t = 0 (the text changed)
```

What a frame carries is the session's `Frame` (`LiveCircuitSession`), encoded by `FrameEncoding` as binary — a frame
is thousands of numbers, and writing them as decimal text took two thirds of a live circuit's time:

```
 int32   header length
 bytes   header JSON: channels, units, levels (node → mean volts), parts (power, °C, terminal currents,
         limits, failure, display figures), trace column names, peaks
 float64 first sample's time · float64 interval · int32 count
 float32 each channel's readings
 float32 each peaked channel's lows, highs, means
 float64 each traced moment's time
 float32 each trace column's value, then its RMS
```

The **speed governor** (`LiveRun.followComputed`, `fastestGapless`) measures what a frame costs — reading samples,
passing samples, encoding — and plays at the speed asked for or at the fastest the server sustains, whichever is
slower. A circuit the server cannot keep up with plays slower and smoothly, never in silent jerks.

## Road 2 — a computed representation

`jmouse-parallax-spring-boot`, `org.jmouse.parallax.store.computation.Computing`.

```
 request (object, values, representation, settings)
   │
   ▼
 Computing.compute ─► prepare: compile the object, resolve settings, fingerprint
   │                   (object text, values, settings, the component sources it uses — ComponentHierarchy.sources)
   ├─ cached?        ─► the dataset from DatasetCache
   ├─ cheap enough?  ─► computation.compute(object, settings, progress) inline, within a deadline
   └─ otherwise      ─► a job on the queue; the browser polls it
                            │
                            ▼
                     CircuitComputations.TransientResponse  ─► TransientRun.of(elaborated, samples, …)
                                       .FrequencyResponse   ─► CircuitSolver: operating point, phasors
                                       .NoiseResponse       ─► CircuitSolver: noise densities
                                       .PhasorDiagram       ─► CircuitSolver: one frequency
                                       .OperatingPoint      ─► CircuitSolver.operatingPoint
                                       .DirectCurrentSweep  ─► CircuitSolver.withSourceValue, point by point
```

A transient computed this way runs **the same engine the live bench runs** (`TransientRun` drives a
`LiveCircuitSolver` with `solutionOnly()` — no energy, no peaks), so a graph and a bench can never disagree about a
circuit. `CatalogueWarmup` computes the whole published catalogue at start-up, which also fills the model cache
(page 02).

## Where the two roads meet

Both build a `CompiledObject`: the circuit's `ObjectDefinition.Circuit`, its parameters' values, and the
`ComponentLibrary` its parts are looked up in. Everything after that is the engine's, and is the same for both.
