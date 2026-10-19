# 02 — Documents become models

`org.jmouse.parallax.component`. A `component` (or `family`) jMX document is read into a `ComponentModel`: what the
part is, in data the engine can compile. Nothing in this layer knows a circuit.

```
 jMX text ──► ExperienceReader.readOne(text, name)         parse → ExperienceDocument   (cached by text)
                      │
                      ▼
            ComponentDocuments.read(text, name, library)    build → ComponentModel       (cached by text
                      │                                                                   and every text it used)
                      ├─ written(document)        meta, parameters in SI, pins, nodes, states, lets,
                      │                           equations, behaviour, expect, display, measure, leads
                      ├─ base component           `extends diode` → this model is a member of the diode:
                      │                           its numbers, the diode's physics (ComponentModel.memberOf)
                      ├─ heat law                 its own `heat {}`, or a used functions document's, or —
                      │                           when it gives a thermal_resistance and writes none —
                      │                           the library's shared `heat` (functions/heat.jmx)
                      ├─ formulas                 every function library it uses, transitively, depth first
                      ├─ family                   the `use` reference that names a `family` document
                      └─ DimensionCheck.require   every equation's units checked: a current is amperes,
                                                  d(x)/dt is x per second, …
```

## The expression language

Every expression a component writes — an equation's right side, a let, a heat law, a `measure` figure, a `display`
figure — is read in one language, `ModelLanguage.LANGUAGE`: jMF, plus

| written | means | how the engine reads it |
|---|---|---|
| `anode - j` | a pin's or internal node's name is its voltage | bound to the node's slot |
| `d(q)/dt` | the rate of a charge (or a flux) | a host call `d/dt`, replaced by a rate the integrator computes |
| `noise(2*q*I)`, `noise(d, fc)` | a noise current along a current equation, A²/Hz (and a 1/f corner) | nothing to the solution; a source to the noise analysis |
| `anode.current`, `current(anode)` | the current into a pin | `PinQuantities`: one host call, bound to the pin by position |
| `V(a,b)`, `I(x)`, `ddt(q)` | SPICE's spellings | refused, saying what to write instead |

An expression a component reads itself by — a `measure` figure, a `display` figure — is read in the same language
(`ModelLanguage.readings`), and refused where it writes what only an equation may: `d(q)/dt` adds a current, and a
noise current runs along one.

And it reads, beside the document's own names, the names the engine gives every document
(`ComponentVocabulary`):

```
 temperature        the part's temperature, kelvin — the bench's, raised by its own heat on a realistic bench
 realistic          1 on a realistic bench, 0 on an ideal one
 tolerance_corner   −1, 0 or +1: which corner of its spread the part stands at
 detail             the bench's level of detail: 0 draft, 1 standard, 2 precise — what it changes is the document's
                    to say: `let stored : ratio = min(detail, 1)` times a junction's charge leaves it out of a draft
 time               the circuit's time, seconds
 power              what a heat law and an instrument read: the power the part takes, watts
```

A document may not declare any of these as a parameter — `ComponentDocuments.parameters` refuses it.

## What a model knows about itself

`ComponentModel` answers questions about its shape that every layer above asks, so the answer is the model's, once:

```
 isGenerator()       one voltage or current equation between its only two pins, no state, no behaviour,
                     no node of its own, not an instrument — an ideal source
 generatesVoltage()  a voltage generator holds a node (and so cuts a circuit into pieces, page 03)
 failure()           how it fails when it overheats: open | short | short <pins>   (meta failure …)
 familyName()        the logic family its digital pins follow
 isInstrument()      it has a measure block
```

## The model cache

Parsing is cached by `ExperienceReader.readOne` (key: name + text). Building a model is cached by
`ComponentDocuments.read`, keyed by the document's text, **and** valid only while every document it read through its
library on the way still reads the same:

```
 read(text, name, library)
   key = (name, text)
   known? ── yes ─► for every (reference → text) it read when built:
             │        library.source(reference) still that text?  ── all yes ─► the cached model
             │                                                    └─ any no  ─► build again
             no ─► build, with a RecordingLibrary that remembers what it was asked and answered
                   keep (model, what it read)            LRU, 512 models
```

So editing `lm358` rebuilds every part that `extends lm358` the next time it is read, and nothing else. One slider
move used to build every part's model five times over; it now builds none.

## Why this layer exists on its own

A component is read by the engine, by the dimension check, by the editor (`ComponentModels.describe`), by the SPICE
converter. None of them should need a circuit to read one, and the circuit engine should never parse a document
twice. The layer below a circuit is exactly the layer of documents.
