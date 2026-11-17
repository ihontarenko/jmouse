---
subject: analysis
title: Analysis
summary: What a circuit does at rest, over time and over frequency — computed rather than run
---
## What it computes

The analysis works the circuit out rather than running it on the bench:

- **Operating point** — every node's voltage and every branch's current at rest.
- **Transient response** — the readings over a stretch of time, from the circuit's start.
- **Frequency response** — the gain and phase between two nodes over a band of frequencies.
- **Phasors** — the sizes and phases of the voltages and currents at one frequency.
- **Feature and critical points** — the points of interest the computation finds on a curve.

Choose one, set what it needs — the nodes, the time, the band — and it is computed. When a computation fails, the reason is said
in the corner of the window and on the bench.

## Reading the results

The charts read like an instrument: move along a curve to read its value at a point; the legend switches a curve off and on.
