> **jMX** — the one object language of jMouse Parallax (`jmouse-parallax`, `org.jmouse.parallax.script`).
> A companion to `JMOUSE_PARALLAX.md` (the engine): this file is the language every object is written in.

# jMX — the object language

## What it is in one paragraph

Every object in Parallax — a surface, a fractal, a circuit, an electronic component, a symbol, a program,
a graph, a catalogue section — is **one jMX document**, and the stored jMX text is the truth the rest is
derived from. A document names its **kind** and a title, then holds **blocks**. The kind decides which
blocks are allowed; the blocks' **structure** decides what the object computes — never a keyword naming
an algorithm. jMX is line-oriented outside the few brace-bodies that hold another language (jMF
mathematics, jMS statements). Expressions are cut verbatim from the author's text, so `x²`, `kΩ` and
`π` survive to the compiler.

## The map

```
<kind> "Title" [as slug] [type role] {
    use <slug>, <slug>        # pull in shared documents — a logic family, a jMF library, a dependency
    extends <slug>            # (a component) specialize another component's physics
    units A, B                # (a component) the units of a multi-unit package

    meta { … }                # what it is: summary, category, tags, source … (every kind)
    <blocks>                  # the kind's own blocks
}
```

- **Head** — the kind's word, then the title in quotes. `as slug` fixes the address (else it is derived
  from the title); `type role` is the role within the kind (`resistor`, `timer`, `flip-flop`).
- A file holds one or more documents; one object is one file, and a collection lists objects by slug.
- A block the kind does not have is **refused with its line**, never ignored.

## Kinds and roles

A kind is the *structure* of a definition, not its subject. `ObjectKind.nature()` and `Kind.Role` group
them; `EngineSelection` derives the engine from the structure.

| Role | Kinds | For |
|---|---|---|
| **COMPUTABLE** | `curve` `surface` `hypersurface` `field` `dynamics` `iteration` `mapping` `region` `iterated_function_system` `lindenmayer_system` `matrix` `points` `construction` `polytope` `value` `program` `component` `circuit` | an object with inputs, parameters and outputs |
| **DRAWING** | `symbol` | a drawing other documents are shown with |
| **SHARED** | `family` `functions` | values or definitions others pull in: a logic family, a jMF library |
| **COMPOSITION** | `graph` | lines drawn together on panes, moved by one clock; it computes nothing itself |
| **CATALOGUE** | `category` `collection` `course` `instrument` | organises the catalogue |

The subject (what a fractal *is*) lives in `meta { category }`; the kind stays structural. A fractal is
an `iteration` whose output calls `escape_time`; a distribution is a `curve` whose output is `: density`;
a random walk is an `iteration` whose step calls a `random_…` function. `type <family>` on a computable
document is refused — the engine is read from the shape, in `EngineSelection`, nowhere else.

## Pulling documents in — `use`, `extends`, `units`

One idea, "take this from another document," is **one word**: `use`. What a `use` brings is decided by
the *kind* of the document it names — the same derive-from-structure rule the engine follows — so no new
keyword is invented per case.

| Declaration | Names | Effect |
|---|---|---|
| `use cmos-hc` | a `family` document | the logic family a component's digital pins follow |
| `use smooth-transitions, junctions` | `functions` documents | their jMF definitions join this document's `formulas` |
| `use lm358, ne555` | any documents | a plain dependency |
| `extends diode` | a `component` document | this component specializes it: it inherits the base's physics (formulas, nodes, states, equations, behaviour, parts) and overrides only its own parameters, package, drawing and checks — never the physics, unless it writes its own |
| `units A, B` | — | the units of a multi-unit package (a dual op-amp's two halves); not an import |

`extends` is inheritance; `use` is import. They are deliberately different words: one says "I am a
specialization of that," the other "I draw on that." A shared jMF library is pulled in with `use`, never
by quoting slugs after `formulas` — `formulas` is only a brace body of local definitions.

## General blocks (the computable kinds share these)

| Block | A line looks like | Purpose |
|---|---|---|
| `meta` | `summary "…"; category c; tags a, b; source "…"; authors "x"; links { "label" "https://…" }` | what the object is — description, where it is filed, provenance. Every kind has it. Unknown keys are kept as free typed fields |
| `parameters` | `R = 10 kΩ in 100 Ω .. 1 MΩ step 10 Ω log "desc"` · `N = 7 in [1, 3, 7]` | the tunable scalars: a value (SI), an optional range/step, flags (`log` `integer` `animate` `live` `exact` `per unit`), a description. `[a, b, c]` is a slider that stops at listed values |
| `inputs` | `x in -3 .. 3` · `(z) in -2 - 2i .. 2 + 2i` · `n in 0 .. 500 integer` · `t in 0 .. 2π periodic` | the free variables and their domains; `(…)` marks a complex input or a shared range over several |
| `outputs` | `z : surface = A sin(k x)` · `trace : curve = (sin t, cos t)` · `boundary : surface where …` · `mean = mu` | what is drawn, and its type; `where` is an implicit form, a tuple a parametric one |
| `formulas` | `formulas { f(u) = u^2 - 1 }` | jMF definitions local to this document. A shared library is pulled in with `use` |
| `view` | `surface primary resolution 120` · `preset "Calm" { A = .3 }` · `camera azimuth 45` · `explain k "…"` · `without play` | how it is presented: the default representation, presets, camera, withheld capabilities |
| `expect` | `expect "title" from "ref" { bench for 3 ms at 85 °C realistic { parts } measure … near 6.86 kHz +- 5% }` | a datasheet/law check the harness runs; see below |

### The value and criterion mini-grammar (shared everywhere)

- A **quantity** is a number and a unit whose prefix is folded into SI: `4.7 nF`, `10 kΩ`, `2 ms`.
  Prefixes `f p n u m k M G T`; standalone units `% deg rad dB ppm °C` take no prefix; compound units
  `V/µs`, `A/V^2`, `1/s` are understood. Normalization turns `Ω→Ohm`, `µ→u`, `±→+-`, `²→^2`, `π→pi`
  before the lexer.
- A **range** is `a .. b` — in `parameters`, `inputs`, `state`, graph `clock` and panes, and meta spans,
  one grammar.
- A **criterion** is `near <typical> +- <tolerance>[%]`, `within <a> .. <b>`, or `is <value>`; shared by
  component `expect` and graph `expect`.

## Computable structure → engine (no kind switch)

`EngineSelection.of(document)` is the one place a document becomes an `ObjectDefinition`. Examples:

| Kind | Structure | Engine |
|---|---|---|
| `curve` | output `: density` · `where` over 2 inputs · a tuple over 1 · one expr over an `integer` input · one expr | density · implicit · parametric · sequence · explicit curve |
| `surface` | output `: density` over 2 · `where` over 3 · tuple over 2 · one expr over 2 | density · implicit · parametric · explicit surface |
| `region` | `where` over 2 inputs · integer inputs with `exact` parameters | the pixels where it holds · decided in exact fractions |
| `iteration` | output calls `escape_time` / `newton_basin` · a `state` with `x_next` | escape time / basins · discrete map (sample paths when a step calls `random_…`) |
| `dynamics` | a `state` with `d(x)/dt` | differential system (Euler–Maruyama when an equation calls `white_noise()`) |
| `matrix` | one matrix output | matrix; a Markov chain's evolution when its rows are distributions |

## Circuit blocks (`circuit`)

| Block | A line looks like | Purpose |
|---|---|---|
| `parts` | `R1 resistor (in, out) resistance = R symbol resistor-iec` · `U1 lm358@3 (A.output = out, Vcc = vcc)` | the netlist: a part by component, connected in pin order (primitive) or by pin name; `@rev` pins a revision |
| `nets` | `out route 300,200 380,200` · `gnd ground` | node routing and ground flags |
| `layout` | `R1 at 240,200 rotate 90 name at dx,dy shows rms graph` · `W1 wire R1.negative to C1.positive via 240 120` | the sheet drawing — placements and wires; a comment here survives a save from the canvas |
| `ports` | `input in` | makes a circuit reusable as a component |
| `probes` | `CH1 out 2 V/div` · `CH2 out against reference` · `L1 level clock` · `time 1 ms/div` | oscilloscope channels and logic probes (meters are parts, not probes); `time` is the time base the bench opens on — a fresh run keeps it rather than taking one from the trigger's period, and a save from the bench writes the time base it was saved with |
| `analysis` | `analysis transient for 5 ms` · `analysis ac from 10 Hz to 1 MHz points 200` | a batch run spec |
| `sheet` | `sheet 1200 x 760 grid 20` | the schematic canvas size |

A pin's **name is its voltage** (`(anode - j) / R_series`); a branch is `current a -> b = …` or
`voltage a -> b = …`; a charge's rate is `d(q)/dt`. SPICE's `V(…)`, `I(…)`, `ddt(…)` are refused with
what to write instead. A data column is the circuit's own name with a unit — `out` [V], `R1` [A].

A pin has two **members**: `anode.current` — the current into it — and `anode.voltage`, the same as the bare
`anode`. They are jMF host calls, written as something the pin has: `anode.current` and `current(anode)` are one call,
bound in the tree (`PinQuantities`), never read out of the text — so a pin spelled like a keyword works too
(`in.current`). `pin.current` is the form the catalogue writes; `current(pin)` stays an equal spelling.

## Component blocks (`component`)

| Block | Purpose |
|---|---|
| `pins` | `out analog output 1 7 "…"` — the package pins: name, nature (`analog`/`digital`/`power`/`ground`), direction, package numbers per unit, attributes (`edge`, `bubble`) |
| `nodes` | `name [digital] "…"` — internal nodes |
| `state` · `equations` | the device physics: `x = 1 continuous`, and `current a -> b = …`, `d(x)/dt = …`, `let n = …` / `let I_through : current = …` (`local` is the same word) |
| `behaviour` | `behaviour { … }` — jMS event logic (digital parts) |
| `measure` | `reading = plus - minus unit V signal` — makes the component an instrument; the engine evaluates the figure on every sample |
| `display` | `charge = 100 * (…) / voltage_rating unit % "of the rated voltage"` — live figures drawn on the symbol |
| `leads` | `plus red stripes black` — the colour of an instrument's leads |
| `symbol` | how a part is drawn — see below |
| `parts` · `nets` | a component built from other parts (a subcircuit) |

**A `let` may say what its value is**: `let V_diode : voltage = j - cathode`, `let Q_junction : charge = …`. The type
is jMX's electrical quantity (`ElectricalTypes`: `voltage` V, `current` A, `charge` C, `power` W, `energy` J,
`resistance` Ω, `conductance` S, `capacitance` F, `inductance` H, `flux` Wb, `frequency` Hz, `time` s, `temperature`
K, `ratio`) or one of the core's (`double`, `int`…); an unknown one is refused at its line, naming them all. Every
quantity is carried as a `double` and the type never reaches the solver. A `behaviour` knows the same types — its
`let delay : time = propagation_delay * delay_scale(Vcc - GND)` is checked on every write, as jMS checks any typed
name — but no dimensions: those are an equation's.

A document may name its own: **`type power_density = power / area`** — known types joined by `*`, `/` and whole
powers (`current ^ 2`, `1 / time`), then `let p : power_density = …`.

**Dimensions are checked when a component is read** (`DimensionCheck`): a current equation's right side must be a
current, a voltage equation's a voltage, `d(x)/dt` what `x` is over a second, a typed `let` its type, a `display` or a
`measure` figure what its `unit` says; a sum may not join a voltage and a current, and `exp`, `tanh`, `sin`… take pure
numbers. It is **conservative**: a pin is a voltage, `pin.current` a current, `time` and `temperature` what they say, a
parameter what its written unit says — but a parameter written without a unit, and a library function's result, are
*unknown*, and a bare number is *free* (a pure number in a product, anything in a sum). A model is refused only where
both sides are known and disagree, naming them: `'current a -> b' is a current [A], but its expression is a voltage
[V]`. A hidden scale is written as what it is — `let R_carrier : resistance = 1` for a current carried on a node at
one volt per ampere, `let V_supply_knee : voltage = 1` inside `tanh((Vcc - GND) / V_supply_knee)`.

A rate `d(q)/dt` is **read in the tree**, never in the text: jMF's `withRate("t", …)` takes the division whose left
ends in the call `d(…)` and whose right is the name `dt` as one node — `polarity * d(q)/dt` reaches it as
`(polarity * d(q)) / dt` and means `polarity · dq/dt`. A `d(…)` not over `dt`, and `dt` not under a `d(…)`, are
refused saying how a rate is written.

Behaviour, heat and drift are written in jMX, never coded per component: heat from `power_rating`,
`thermal_resistance`, `thermal_time_constant`, `T_maximum` and `meta failure open|short`; drift from a
parameter against `T_nominal` and `temperature`. A new component gets all of it by writing those lines — a
datasheet part needs only `extends <base>` and its own numbers.

**A component writes its physics; the engine only defines and integrates.** `temperature` in a part's
equations is the part's *own*, in kelvin. How it gets there is a law, and a law is physics, so it is jMX — the
`heat` block:

```
heat {
    d(heating)/dt = (power * thermal_resistance - heating) / thermal_time_constant
    temperature = ambient + heating
}
```

`d(x)/dt` lines are thermal states (starting at nought), `let` names a value, `temperature = …` is what the
part's equations read. The law reads `power`, `ambient` (the bench, K) and the part's parameters. It is written
**once**, in the shared document `heat` (`catalogue/electronics/functions/heat.jmx`): a part that gives a
`thermal_resistance` warms by it unless it writes its own `heat { … }` (a junction → case → heatsink as two
states), inherits one through `extends`, or names another `functions` document holding one with `use`.

`power` is not a law but a definition, so it is the engine's and never written by hand: the current of every
equation times the voltage it flows across, a capacitance's charging current left out. The engine advances the law
by backward Euler, outside the circuit's Newton, once a tenth of a millisecond of circuit time has gathered — heat
moves in seconds. On a realistic bench the part's equations and displays read the temperature it makes; on an
ideal bench it is only shown. A second, hand-written power would be a second truth that drifts from the first.

**The engine proves a model linear when it is**: no partial of an equation or a charge by a voltage, a
current, a charge rate or a state reads one of those. Such a part — an electrolytic, a transformer, a meter, a
generator — is solved in one step like a resistor, however its values move with `temperature`, `time` or its
discrete states; Newton runs only for the parts that need it. A linear part that reads no time and no discrete
state is **affine**: its conductances, capacitances and constant are found once (again only when its own
temperature moves) and every step is stamped from them, as SPICE stamps an R or a C — no tape is run.

**Every part is its document — the primitives too.** `resistor`, `capacitor`, `inductor`, `voltage-source`,
`current-source` and `diode` write their physics as any component does:

```
current terminal_one -> terminal_two = (terminal_one - terminal_two) / R_now + noise(4 * boltzmann * temperature / R_now)
current terminal_one -> terminal_two = d(C_now * (terminal_one - terminal_two))/dt
voltage terminal_one -> terminal_two = d(inductance * terminal_one.current)/dt
voltage positive -> negative = U_output
```

with `R_now = resistance * (1 + tolerance_corner * tolerance) * (1 + temperature_coefficient * (temperature - T_nominal))`
— the tolerance a bench's corner picks, the drift its temperature moves. A part written in pin order,
`R1 resistor (a, b) resistance = 10 kΩ`, is its document's model with its pins in order and its value as the
document's first parameter. The engine keeps devices of its own only for what it adds itself: a node's leak to
ground, a failed part's stand-ins, a logic pin's capacitance, and the ideal source a generator's equation drives.

**A charge may read a current where a voltage equation carries it**: a flux is `d(L * terminal_one.current)/dt`,
the current that very branch's unknown — a winding needs no node carrying its current as a voltage. A `let` reads
`pin.current` as an equation does.

**How a part fails is its document's**: `meta { failure open }` — nothing conducts —, `failure short` — every pin
together —, or `failure short drain source` — the pins named, the rest open. A MOSFET shorts drain to source, an op-amp
its supply to ground.

**A logic family writes its laws** as formulas of the supply its part runs from — `input_low(supply)`,
`input_high(supply)`, `output_resistance(supply)`, `delay_scale(supply)` — with the figures `minimum_supply`, `edge`,
`input_capacitance` and `output_capacitance`. CMOS writes shares of the supply, TTL fixed volts; a family that leaves
one out is refused, and nothing is assumed in its place.

**One reader of a written value** (`script/Quantity`): `90 deg` is π/2, `100 ppm/K` is 10⁻⁴ per kelvin, `1 %` is 0.01 —
on a part line as in `parameters`. The physical constants are the shared `constants` document's (`boltzmann`,
`elementary_charge`, `zero_celsius`), pulled in by `use` — `use` is transitive, so a library that uses them brings them.

## Symbols (`symbol`) and how parts are drawn

A `symbol` document is drawn three ways:

- `symbol uses resistor-symbol, fuse-symbol { a terminal_one; b terminal_two }` — be shown with other
  symbols, mapping port → pin (first is the default; a port left out is the pin of the same name).
- `symbol box { left d, clk edge; right q }` — a parametric box.
- a drawing body: `size 60 x 40; path "M…"; pin a at -30, 0; label name at 0, -26; indicator level from -4,-14 to 4,14`.

A component chooses its drawing with `symbol uses <symbol-slug> { … }`; that line is the only place the
choice lives. Primitives draw from their component (`resistor` → `resistor-symbol`), the sheet's ground
from `ground-symbol`.

## Graphs (`graph`)

| Block | A line looks like | Purpose |
|---|---|---|
| `clock` | `clock t in 0 .. 4π speed 1 loop` | the one time parameter that moves the whole graph |
| `panes` | `panes rows { wave plane x 0..4π y -1..1 ; fourier space ; sheet circuit uses bridge-rectifier over 0 .. 20 ms }` | the layout of views (plane · space · circuit · values) |
| `lines` | `lines { y = abs(vin(x)) until t at (t, 1) from sheet in wave color 2 ; "V = {…}" at (t, 1) in wave }` | the series drawn and live labels; a quoted expression with `{…}` is a label |
| `parameters` `formulas` `expect` | as the computable kinds | sliders, shared math, per-frame law checks |

## Catalogue kinds

| Kind | Blocks | Purpose |
|---|---|---|
| `category` | `meta` | a section of the tree |
| `collection` | `meta` `items` `section` | an ordered set of objects by slug (`items lorenz, rossler`), optionally in titled sections; a formula set is a collection |
| `course` | `meta` `lesson` `step` | a lesson sequence (`lesson "…" { object wave; step "…" { view … } }`) |
| `instrument` | `meta` `objects` `representations` `opens` | the workbench an object kind opens in |

## Gotchas

- A document's kind restricts its blocks; a `pins` block on a `surface` is refused with its line.
- `type <family>` on a computable document is refused — the engine is derived, not named.
- `formulas "slug"` is refused — write `use <slug>` at the top. `family <slug>` is gone — a component
  uses `extends <base>` (inheritance) or `use <logic-family>` (digital pins).
- A pin's name is its voltage; SPICE `V(…)`/`I(…)`/`ddt(…)` are refused.
- A quantity is read in SI: `resistance = 100 kΩ` stores 100000; never write `* 1e3`.

## What it is not

jMX is not SPICE and not a general template language. It has no `type`-names for engines, no per-kind
algorithm keywords, and no second spelling for "use another document." Reusing a value expression or an
existing construct is always preferred to inventing a keyword — the language stays small so that the
structure of a document, not a vocabulary, is what a reader has to understand.
