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
| `probes` | `CH1 out 2 V/div` · `CH2 out against reference` · `L1 level clock` | oscilloscope channels and logic probes (meters are parts, not probes) |
| `analysis` | `analysis transient for 5 ms` · `analysis ac from 10 Hz to 1 MHz points 200` | a batch run spec |
| `sheet` | `sheet 1200 x 760 grid 20` | the schematic canvas size |

A pin's **name is its voltage** (`(anode - j) / R_series`); a branch is `current a -> b = …` or
`voltage a -> b = …`; a charge's rate is `d(q)/dt`. SPICE's `V(…)`, `I(…)`, `ddt(…)` are refused with
what to write instead. A data column is the circuit's own name with a unit — `out` [V], `R1` [A].

## Component blocks (`component`)

| Block | Purpose |
|---|---|
| `pins` | `out analog output 1 7 "…"` — the package pins: name, nature (`analog`/`digital`/`power`/`ground`), direction, package numbers per unit, attributes (`edge`, `bubble`) |
| `nodes` | `name [digital] "…"` — internal nodes |
| `state` · `equations` | the device physics: `x = 1 continuous`, and `current a -> b = …`, `d(x)/dt = …`, `local n = …` |
| `behaviour` | `behaviour { … }` — jMS event logic (digital parts) |
| `measure` | `reading = plus - minus unit V signal` — makes the component an instrument; the engine evaluates the figure on every sample |
| `display` | `charge = 100 * (…) / voltage_rating unit % "of the rated voltage"` — live figures drawn on the symbol |
| `leads` | `plus red stripes black` — the colour of an instrument's leads |
| `symbol` | how a part is drawn — see below |
| `parts` · `nets` | a component built from other parts (a subcircuit) |

Behaviour, heat and drift are written in jMX, never coded per component: heat from `power_rating`,
`thermal_resistance`, `thermal_time_constant`, `T_maximum` and `meta failure open|short`; drift from a
parameter against `T_nominal` and `temperature` (the bench's, in kelvin). A new component gets all of it
by writing those lines — a datasheet part needs only `extends <base>` and its own numbers.

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
