# 04 — Netlist and stamping

`org.jmouse.parallax.circuit`. From here on a circuit is a system of equations: modified nodal analysis (MNA). Every
analysis — operating point, transient, small signal, sweep — goes through the same stamps, so a part is described
once and every analysis agrees about it.

## The unknowns

`Netlist.of(piece)` numbers everything the solver solves for, in this order:

```
 x = [ v₀ … vₙ₋₁ │ i₀ … iₘ₋₁ │ s₀ … sₖ₋₁ ]
       node        branch      continuous
       voltages    currents    states
```

* **Nodes** — every analog node of the circuit, then each model's internal nodes (`U1.j`) and the private node of an
  unconnected pin. Ground (`0`) is the reference and has no number: a stamp on it is dropped.
* **Branches** — an unknown current where a voltage is imposed: a voltage generator's own (reported under the
  part's name), and each `voltage a -> b = …` equation inside a model (named for a refusal, reported by the part's
  pins).
* **States** — each continuous `state` a model declares, with `d(x)/dt = …`.
* A node only digital pins touch is not an unknown at all — the event simulation resolves it (page 05).

The engine adds elements of its own (`Netlist.Element`, kind `RESISTOR` · `CAPACITOR` · `VOLTAGE_SOURCE` ·
`CURRENT_SOURCE`):

```
 gmin_N        1 TΩ from every analog node a model touches to ground — SPICE's gmin: a pin that only a
               switched-off part drives must not leave the system singular.         part: none
 R1_failed_N   a failed part's stand-in, from the bench (page 03).                   part: none
 U1.a.Cin      a logic pin's capacitance, from its family (EventSimulation).         part: none
 G1            a generator: an ideal source whose value is its document's formula
               of time (SourceSignal).                                               part: G1
```

Everything a person placed is a **model unit** (`Netlist.ModelUnit`): one block of one part (a dual op-amp is
two), its parameters resolved, each pin bound to a node number or a digital node, its rows.

## A model's equations, compiled

`ModelEquations.of(model, parameters, pins, internalNodes)` compiles once per *form* (the equations, the names; not
the values) and copies for every unit after the first — twenty-four diodes in a multiplier compile one diode once.

Each formula becomes a function of one argument array:

```
 arguments = [ node voltages │ branch currents │ charge rates │ coefficient │ charge histories │
               states │ time │ parameters … ]
                                                    ▲ filled by the device before every evaluation
```

and is compiled by `GradientCompiler` into a **tape**: its value and every partial derivative in two passes,
turned into bytecode where it can be — the exact Jacobian Newton needs, never a finite difference.

* A pin's name becomes its node's slot; `anode.current` becomes the branch currents of the voltage equations on
  that pin, or the other current equations' right sides, written out.
* `d(q)/dt` becomes the slot of a **charge rate**. The charge `q` is compiled on its own. The rate is what the
  integrator says it is — so a capacitance written as a charge is integrated by the same rule as every other.
* Where an equation reads a rate, a second, **written-out** form replaces the rate by `coefficient · q − history`
  inside one tape, so a diode's diffusion exponential — read by its current and by its charge — is computed once.
* `ModelEquations.linear` is a **proof** from the formulas, not a guess: no partial by an unknown reads an unknown.

## A device stamps

```
 current a -> b = f(x)     adds f leaving a, entering b — its tangent at the guess:
                             row a:  +∂f/∂x · x      right side a: −(f − ∂f/∂x · x_guess)
                             row b:  −∂f/∂x · x      right side b: +(…)
 voltage a -> b = f(x)     a branch k:  row k says  v_a − v_b − f(x) = 0;  i_k enters the KCL rows of a and b
 d(x)/dt = g(x)            a state row:  at DC g = 0;  in a transient  coefficient · (x − x_prev) − g = previous rate
```

`ModelDevice` holds this as three pieces and a strategy:

```
 UnitLayout        where everything stands: each argument's unknown, each equation's rows and locals,
                   each pin's terminal — found once when the unit is bound
 UnitArguments     the argument arrays, filled from a state; one unit's alone
 UnitLine          the line stamped: each equation's value, its tangent over the unit's locals, the point
 UnitLinearization how the line is found:
   ├─ AffineLinearization  equations affine in the unknowns, no time, no discrete state (a resistance, a
   │                       capacitance, a winding): f₀ + D·x + R·rate, q₀ + Q·x, the coefficients found once
   │                       and again only when its own temperature moves. No tape on any attempt.
   └─ TapedLinearization   anything else: every tape run at the guess — unless the last linearization still
                           holds (SPICE's bypass: no local unknown moved more than 1e-4·|x| + 1e-6)
```

A tangent is accumulated over the unit's few **locals**, not over the circuit: a dense row per equation made every
unit cost as much as the circuit is large.

## The system

`Assembly` is one iteration's system over a `SparseSystem` owned by the solver, layered so nothing is stamped twice:

```
 keepAsConstant   the invariant devices (a resistance, a leak) — stamped once for the whole run
 keepAsBase       + the linear devices of this attempt (a capacitance's companion at this step)
 every iteration  + the nonlinear devices at the guess
```

`SparseSystem` solves it by sparse LU:

```
 slots     every entry ever held (assembled or fill-in) owns a slot in one flat array; nothing allocated per solve
 order     minimum degree on the symmetric pattern, again only when a new entry appears
 pivots    threshold: the diagonal while ≥ a tenth of the largest candidate, else the largest
 replay    the first elimination writes its plan down; the next solves replay it (SPICE refactors with
           its pivots kept) — until a pivot grows small, and the elimination is planned again
```

The small-signal system (`PhasorAssembly`) is complex and dense: every device stamps its admittance at the operating
point. It is factored once per frequency and solved for as many right sides as asked — a noise analysis drives one
noise source after another through the same factors.
