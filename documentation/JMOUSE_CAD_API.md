# jmCAD API — `/jmouse/cad/api`

> The HTTP and socket contract of the jmCAD engine: what every route takes, what it answers, and an example of each.
> Spring integration modules `jmouse-cad-spring-boot` (services, storage, migrations) · `jmouse-cad-management` (these
> routes) · `jmouse-cad-live` (the live socket). Storage: [jmouse-cad-jpa](../jmouse-cad-jpa/pom.xml).

⚠️ **This is the contract the modules are built against, written before them.** The **State** column of every table
says which routes answer today: **✅ built** or **📐 contract** (agreed, not built yet). A route not listed here does not
exist and will not be added without a screen that calls it.

⚠️ **It replaces `/jmouse/parallax/api`, it does not extend it.** The old routes answered the mathematics engine's shapes
— `definition`, `representations`, `ambientDimension`, `formulaLatex`, `explanation`, `nature` — and none of those exist in
jmCAD. Nothing here answers them, and nothing here is shaped to be read by code written for them: the interface's
transport (`@jmouse/cad` · `transport/transport.ts`, `wire.ts`) moves to this contract when the product switches over.

---

## 1 · Conventions

### 1.1 Addresses

| What | Where |
|---|---|
| REST prefix | `/jmouse/cad/api` — `jmouse.cad.management.prefix` |
| Live socket | `/jmouse/cad/live` — `jmouse.cad.live.path` |
| An owner's things | `…/owners/{ownerType}/{ownerId}/…` — `/owners/PROJECT/42/sheets` |

Every example below writes paths after the prefix: `GET /documents/lm358` is `GET /jmouse/cad/api/documents/lm358`.

### 1.2 A document is its text

Everything jmCAD keeps is a **document of the jM language** — `component`, `family`, `functions`, `circuit`, `symbol`,
`portrait`, `frame`, `footprint`, `pcb`, `category`. Its text is the truth; every other field a route answers is read from
the head revision of that text. So:

- **A write sends text.** `POST /documents` takes `{ "source": "…" }` and nothing else — no field-by-field draft. Its
  slug, title, summary, category and tags are the ones its text writes (`component:lm358("LM358", …) { meta { … } }`).
- **A kind is a word**, the one the document's head opens with: `"kind": "component"`. A kind a new language module
  brings needs no change to this contract.
- **A relation is a word** too — how the text names the other document:

  | `relation` | Written in the text as | Meaning |
  |---|---|---|
  | `extends` | `component:2n2222(…) extends npn-transistor` | the base a component extends |
  | `use` | `use:cmos-hc` | a family or a function library it takes |
  | `symbol` | `symbol(uses: and-gate-symbol)` | a symbol it is drawn with |
  | `portrait` | `portrait(uses: …)` | how it looks in the hand |
  | `footprint` | `footprint(uses: …)` / a seat | a footprint its package lands on |
  | `parts` | `R1 resistor …` in `parts { }` | a component (or a sheet) a circuit places |
  | `circuit` | `circuit rc-discharging` in a `pcb` | the circuit a board is laid out for |
  | `frame` | `frame …` | the drawing frame a sheet is printed in |
  | `see_also` | `meta { see_also: … }` | a curator's link |

- **A revision is pinned with `@`**: `lm358@3` (number), `lm358@20261027_001` (label: the day and its ordinal),
  `lm358@20261027` (the last of that day), `lm358@latest`.

### 1.3 Model values

The model a route answers is the engine's own model (`org.jmouse.cad.*` records), as JSON. A **given value** — anything a
document writes that may be a formula or a text — is one of two shapes:

```json
{ "written": "4.7 k" }          // a formula, exactly as written: a quantity, an expression, a reference
{ "text": "NE555" }             // a text
```

A range is `{ "low": {…}, "high": {…} }` (between) or `{ "values": {…} }` (listed). Numbers a route works out — a
transient's samples, a footprint's millimetres — are plain JSON numbers in SI units.

### 1.4 Pages

A listing answers a page: `?page=0&size=50` (size 1–200, default 50).

```json
{ "rows": [ … ], "page": 0, "size": 50, "total": 1361 }
```

### 1.5 Refusals

Every refusal is an RFC 9457 problem document with `Content-Type: application/problem+json`:

```json
{
  "type": "about:blank",
  "title": "Bad Request",
  "status": 400,
  "detail": "what a part is connected to closes with ')'",
  "instance": "/jmouse/cad/api/documents/52736f18-…",
  "code": "invalid-document",
  "line": 3,
  "column": 21
}
```

| Status | `code` | When |
|---|---|---|
| 400 | `invalid-document` | the text does not read, or reads into something the engine refuses, or names a document the catalogue does not hold — `line` and `column` say where (0 when the refusal names no place) |
| 400 | `layout-disagrees` | a circuit's drawing joins nets its netlist keeps apart (`LayoutAgreement`) — `contradictions[]` lists them |
| 400 | `invalid-request` | a parameter out of range, a page size over 200, an unknown analysis |
| 401 | `unauthenticated` | a write, and nobody is signed in |
| 403 | `forbidden` | the reader may not do it |
| 404 | `not-found` | nothing answers that id, slug or revision — or the reader may not see it (the two are not told apart) |
| 409 | `conflict` | `version` is stale; a slug is taken; a category still files documents; another document still names the one deleted; nothing newer ships for `POST /documents/{id}/shipped` |
| 422 | `simulation-failed` | the circuit reads but does not run — `detail` says why |
| 422 | `component-refused` | a component reads but the engine cannot compile it (§2.3) |

### 1.6 Authorization

The routes decide nothing about *who*: each is gated by a `CadCapability`, and the product says which of its permissions
grant it (`CadAccessRules`, the same arrangement as the other management modules). A route marked **public** decides
inside its service, per document: a published catalogue document is open to anybody, a catalogue draft to whoever has
`CATALOGUE_WRITE`, an owner's document to whoever has `OWNER_READ` / `OWNER_WRITE` where its owner is. The services
ask the host the same questions through `CadAuthority` and `CadPlaces`, so a route and the live socket decide alike.

| Capability | Covers |
|---|---|
| `CATALOGUE_WRITE` | writing catalogue documents, their status and categories; reading drafts |
| `OWNER_READ` / `OWNER_WRITE` | an owner's documents and sheets, checked against the owner in the path or on the document |
| `SIMULATION_RUN` | simulations, expectations, the live socket |

⚠️ `CadAccessRules.build()` refuses to finish while a capability a route needs is unmapped, or while any handler has
neither a rule nor `@PublicEndpoint`: an ungated route is a failure to boot.

---

## 2 · The routes

### 2.1 Catalogue — documents and categories

| Method | Path | Takes | Answers | Capability | State |
|---|---|---|---|---|---|
| GET | `/categories` | `?kind=` count only that kind | `Category[]` | public | ✅ |
| PUT | `/categories/{code}` | `CategoryDraft` | `Category` | CATALOGUE_WRITE | ✅ |
| DELETE | `/categories/{code}` | — | 204 | CATALOGUE_WRITE | ✅ |
| GET | `/documents` | `?text= &category= &kind= &tag= &featured= &drafts= &page= &size=` | `Page<DocumentSummary>` | public | ✅ |
| POST | `/documents/summaries` | `{ "slugs": [] }` | `DocumentSummary[]`, in the order asked; one the reader cannot see is left out | public | ✅ |
| GET | `/documents/{address}` | `address`: the id or the slug, and may carry `@revision` | `DocumentDetail` | public | ✅ |
| POST | `/documents` | `{ "source" }` | 201 `DocumentDetail` | CATALOGUE_WRITE | ✅ |
| PUT | `/documents/{id}?version=` | `{ "source" }` | `DocumentDetail`; an unchanged text writes nothing | public | ✅ |
| PUT | `/documents/{id}/status` | `{ "status": "PUBLISHED" }` | `DocumentDetail` | CATALOGUE_WRITE | ✅ |
| POST | `/documents/{id}/shipped` | — | `DocumentDetail`, the shipped file's text as a new revision | CATALOGUE_WRITE | ✅ |
| DELETE | `/documents/{id}` | — | 204; refused (409) while another document names it | public | ✅ |
| GET | `/documents/{address}/revisions` | — | `Revision[]`, newest first | public | ✅ |
| GET | `/documents/{address}/revisions/{revision}` | `revision`: number, label, day or `latest` | `{ "revision", "label", "source" }` | public | ✅ |
| GET | `/documents/{address}/used-by` | `?relation=` | `DocumentSummary[]` — the documents whose text names this one | public | ✅ |

### 2.2 An owner's documents — a project's sheets

| Method | Path | Takes | Answers | Capability | State |
|---|---|---|---|---|---|
| GET | `/owners/{type}/{id}/documents` | `?kind= &page= &size=` | `Page<DocumentSummary>`, most recently changed first | OWNER_READ | ✅ |
| POST | `/owners/{type}/{id}/documents` | `{ "source" }` | 201 `DocumentDetail` | OWNER_WRITE | ✅ |
| GET | `/owners/{type}/{id}/sheets` | — | `SheetView[]` — every circuit of the owner as a sheet of one design | OWNER_READ | ✅ |
| GET | `/documents/{id}/sheet` | `?path=S1,S2` the blocks, outermost first | `SheetInstance` — that block as it runs inside the circuit | public, per document | ✅ |

### 2.3 The library — what a sheet and a board are built from

| Method | Path | Takes | Answers | Capability | State |
|---|---|---|---|---|---|
| GET | `/components` | — | `ComponentReference[]` — every component the reader may place | public | ✅ |
| GET | `/components/{reference}` | `reference`: `slug` or `slug@revision` | `ComponentView` — the component with its lineage applied | public | ✅ |
| GET | `/symbols` | — | `DrawnDocument<SymbolDrawing>[]` | public | ✅ |
| GET | `/portraits` | — | `DrawnDocument<SymbolDrawing>[]` | public | ✅ |
| GET | `/frames` | — | `DrawnDocument<SymbolDrawing>[]` | public | ✅ |
| GET | `/footprints` | — | `DrawnDocument<FootprintDrawing>[]` | public | ✅ |
| GET | `/footprints/{slug}` | — | `{ "footprint": DrawnDocument, "usedBy": DocumentSummary[] }` | public | ✅ |
| GET | `/footprint-rules` | — | `LandPatternRules` — the land-pattern table the footprints are generated from | public | ✅ |
| GET | `/board-rules` | — | `BoardRules` — the board standards table | public | ✅ |

### 2.4 Simulation

| Method | Path | Takes | Answers | Capability | State |
|---|---|---|---|---|---|
| POST | `/simulations` | `SimulationRequest` | 200 `SimulationResult` when it fits an answer, else 202 `Job` | SIMULATION_RUN | ✅ |
| GET | `/simulations/jobs/{jobId}` | — | `Job` | SIMULATION_RUN | ✅ |
| GET | `/simulations/jobs/{jobId}/result` | — | `SimulationResult`; 409 while the job runs | SIMULATION_RUN | ✅ |
| POST | `/simulations/jobs/{jobId}/cancel` | — | `Job` | SIMULATION_RUN | ✅ |
| POST | `/expectations` | `{ "reference" }` or `{ "source" }`, and `owner` | `Verdict[]` — every expectation the document writes, measured | SIMULATION_RUN | ✅ |

### 2.5 Editing — the jM language for editors

An editor never writes text itself: it sends the text it holds and the change, and the server answers the next text,
written by the language's own writer. The interface keeps no grammar of its own.

| Method | Path | Takes | Answers | Capability | State |
|---|---|---|---|---|---|
| GET | `/language/schema` | — | `LanguageSchema` — every kind, its blocks and the lines each holds, with their clauses and value words | public | ✅ |
| POST | `/language/form` | `{ "source" }` | `EditedDocument` — the text read into statements an editor shows as a form | public | ✅ |
| POST | `/language/edit` | `{ "source", "edit": { "operation", … } }` | `EditedDocument` of the next text | public | ✅ |
| POST | `/language/circuit` | `{ "source", "circuit": CircuitSheet, "title", "summary", "category" }` | `EditedDocument` — a schematic editor's sheet as the next text | public | ✅ |
| POST | `/language/drawing` | `{ "source", "drawing": SymbolDrawing, "title", "category" }` | `EditedDocument` — a symbol, portrait or frame | public | ✅ |
| POST | `/language/footprint` | `{ "source", "drawing": FootprintDrawing, "title", "category" }` | `EditedDocument` | public | ✅ |
| POST | `/language/board` | `{ "source", "drawing": BoardDrawing, "title", "category" }` | `EditedDocument` — a `pcb` | public | ✅ |
| POST | `/language/drawn` | `{ "source" }` | `DrawnText` — a drawn kind's text drawn without saving it | public | ✅ |

### 2.6 Exchange — formats the plugins bring

⚠️ **A format is a plugin's, never the engine's.** It brings a `DocumentImport` (`cad.exchange.imports`) and/or a
`DocumentExport` (`cad.exchange.exports`), and the routes below are the same for every format: an interface knows
none by name. Converting is the format's; whose components a conversion sees, who may read and store, how a refusal
is said and how what an import made is stored beside what the owner holds are the store's (`DocumentExchange`).

| Method | Path | Takes | Answers | Capability | State |
|---|---|---|---|---|---|
| GET | `/exchange` | — | `[{ "id", "title", "reads": ["*.cir"], "writes": ["circuit"] }]` | public | ✅ |
| POST | `/exchange/{format}/read` | `{ "files": { "path": "text" }, "main", "title", "owner" }` | `{ "documents": [{ "slug", "kind", "source" }], "notes": [] }` — converted, nothing stored; the one the files stand for first | public | ✅ |
| POST | `/owners/{type}/{id}/exchange/{format}/import` | `{ "files", "main", "title" }` | 201 `{ "root", "created": [], "updated": [], "reused": [], "notes": [] }` | OWNER_WRITE, in the service | ✅ |
| POST | `/exchange/{format}/export` | `{ "source", "owner" }` | `{ "name", "files": [{ "fileName", "text" }], "notes": [] }` — one file or a set (a board's fabrication files); ⚠️ since 2026-10-29, was `{ fileName, text, notes }` | public | ✅ |

An import stores what a design is built from first — one already called so is kept, an earlier import's untouched
first revision excepted —, then the circuits, each placed sheet before the sheet placing it, written over where an
earlier import of this owner made them and refused (409) where another owner holds the slug.

| Plugin | Module | Brings | Settings |
|---|---|---|---|
| `cad-kicad` | `jmouse-cad-plugin-kicad` | format `kicad` (reads a design — `main` its root `*.kicad_sch`, the sheets it places and the project's `LIBRARY.pretty/NAME.kicad_mod` among `files` —; writes a circuit as a `.kicad_sch`), part provider `kicad` | `jmouse.cad.kicad.footprint-folders`, `.symbol-folders` — KiCad's installed libraries: searched after a design's own footprints, and for parts |
| `cad-spice` | `jmouse-cad-plugin-spice` | format `spice` (reads `*.cir`, `*.sp`, …; writes circuit, component), engine `ngspice` | `jmouse.cad.spice.command` — the program that runs a deck from its standard input (`ngspice_con,-b`, `docker,run,--rm,-i,jmcad-ngspice`); empty: the engine is listed as off, reading and writing still work · `.timeout` (PT60S) |
| `cad-xyce` | `jmouse-cad-plugin-xyce` | engine `xyce` — the same deck as ngspice, closed in Xyce's dialect (`.options device`, `.options output initial_interval`, `.print tran format=csv`) | `jmouse.cad.xyce.command` — the program that runs a deck from its standard input and prints the run's CSV (`docker,run,--rm,-i,jmcad-xyce`); empty: the engine is listed as off · `.timeout` (PT120S) |

Another engine's transient — `GET /simulations/engines` (`[{ "id", "title", "available" }]`) and
`POST /simulations/engines/{engine}/transient` `{ "source", "parameters", "samples", "owner" }` →
`{ "engine", "result": Transient, "given", "notes" }` (SIMULATION_RUN, in the service); `samples` defaults to
`jmouse.cad.simulation.engine-samples` (1000).

Every route decides inside its service through the host's `CadAuthority`, so it needs no rule of the product's.
`owner` (`TYPE:id`, optional) lets a conversion read that owner's components too, once the reader may read there.

### 2.7 Parts from suppliers

⚠️ **The search is the engine's; the places beyond the catalogue are the plugins'.** It asks the catalogue first, then
every provider brought to `cad.parts.providers` — Nexar (`cad-nexar`, `jmouse-cad-plugin-nexar`) and KiCad's installed
libraries (`cad-kicad`). A provider that cannot answer is listed all the same, saying why; one that fails while
searching does not fail the others.

| Setting | Means |
|---|---|
| `jmouse.cad.parts.limit` (24) | how many candidates each provider answers with at most |
| `jmouse.cad.parts.nexar.client-id`, `.client-secret` | a Nexar application's client; without it Nexar is `NOT_CONFIGURED` |
| `jmouse.cad.parts.nexar.limit` (10), `.timeout` (8 s), `.token-url`, `.api-url` | Nexar counts every part a search returns against the application's allowance |
| `jmouse.cad.kicad.symbol-folders`, `.footprint-folders` | KiCad's installed libraries, read on a virtual thread the first time they are asked about |

| Method | Path | Takes | Answers | Capability | State |
|---|---|---|---|---|---|
| GET | `/parts/providers` | — | `ProviderResults[]` — each provider and its `state`, nothing searched | public | ✅ |
| GET | `/parts/search` | `?q= &provider=` | `ProviderResults[]` — `{ "provider", "name", "state", "candidates": PartCandidate[] }` | public | ✅ |
| POST | `/owners/{type}/{id}/parts/fetch` | `{ "provider", "identifier" }` | 201 `{ "open", "created": [], "reused": [], "notes": [] }` — the part written as the owner's documents | OWNER_WRITE, in the service | ✅ |

```json
// GET /parts/search?q=NE555
[ { "provider": "catalogue", "name": "Catalogue", "state": { "status": "READY", "detail": "The published catalogue." },
    "candidates": [ { "kind": "COMPONENT", "identifier": "ne555", "title": "NE555", "slug": "ne555", "fetchable": false, … } ] },
  { "provider": "kicad", "name": "KiCad libraries",
    "state": { "status": "READY", "detail": "21093 symbols, 13820 footprints. Fetched parts stay under CC-BY-SA-4.0 WITH KiCad-libraries-exception." },
    "candidates": [ { "kind": "COMPONENT", "identifier": "symbol:Timer:NE555D", "title": "Timer:NE555D", "fetchable": true, … }, … ] },
  { "provider": "nexar", "name": "Nexar · Octopart",
    "state": { "status": "NOT_CONFIGURED", "detail": "A Nexar application's client id and secret are not set, …" }, "candidates": [] } ]

// POST /owners/PROJECT/7/parts/fetch   { "provider": "kicad", "identifier": "symbol:Timer:NE555D" }
// 201 Created
{ "open": "kicad-ne555d", "created": ["kicad-ne555d"], "reused": [],
  "notes": ["KiCad names its footprint Package_SO:SOIC-8_3.9x4.9mm_P1.27mm; KiCad's footprints are not converted — choose one of ours for the part."] }
```

`state.status` is `READY`, `INDEXING` (ask again shortly), `NOT_CONFIGURED` or `FAILED`, `detail` the words a reader is
shown. ⚠️ **Nexar's terms allow showing, not keeping**: its candidates are never `fetchable`, carry their Octopart page as
`link` for the interface to show, and a fetch from it is 409. A document whose slug is taken already is reused, never
written over. Refusals: 400 for an empty query or a missing `identifier`, 404 for a provider not there.

### 2.8 Plugins

| Method | Route | Takes | Answers | Access | State |
|---|---|---|---|---|---|
| GET | `/plugins` | — | `{ plugins: [{ id, title, version, status: STARTED|REFUSED, reason }], points: [{ name, kind, holds }] }` | public | ✅ |
| GET | `/about` | — | `{ engine: { name, summary, version, author, license, site }, plugins: [{ id, title, version, description, author, icon, running, reason }], runtime: { java }, roadmap: [{ title, state: done|now|next|later, items }] }` | public | ✅ |

The engine runs as the first plugin, `cad-engine`, which opens the points it reads — the language takes its modules
from `cad.language.modules` — and every other plugin stands on it (`jmouse-plugin`; `jmouse.plugins.<id>.enabled:
false` switches one off). An interface turns its own half of a plugin on by this list; a refused plugin says why.

### 2.10 Explorer — the plugin `cad-explorer`

Folders of the server mounted as libraries of files (`jmouse-cad-plugin-explorer`), each a jMouse storage that is a
tree (`FileTree` in `jmouse-storage`; the local store is one). The server's own come from
`jmouse.cad.explorer.mounts` (`id`, `title`, `path`, `writable`, `owner` — none for the installation's); any plugin
may bring a store to `explorer.mounts`, and every change is told on the topic `explorer.changes`. Who may do what is the
engine's seam: the plugin's capabilities `cad-explorer:read` and `cad-explorer:write`, asked across the installation for
its mounts and at the owner for an owner's — the host maps them as it maps the engine's. A mount the reader may not read
is not listed and answers 404. A `key` is where a thing is from its mount's top — `notes/readme.txt` —; one that would
climb out is refused (400). Every listing reads the folder again: what changed on the disk is there at the next look.

| Method | Path (under `/plugins/cad-explorer`) | Takes | Answers | State |
|---|---|---|---|---|
| GET | `/mounts` | — | `[{ "id", "title", "ownerType", "ownerId", "changeable" }]` — those the reader may read | ✅ |
| GET | `/mounts/{mount}/entries` | `?folder=` (none: the top) | `[{ "key", "name", "folder", "sizeBytes", "modifiedAt" }]`, folders first | ✅ |
| GET | `/mounts/{mount}/file` | `?key=` | the file's bytes, its type, inline under its name | ✅ |
| PUT | `/mounts/{mount}/file` | `?key=`, the bytes as the body | the entry written — made or replaced | ✅ |
| POST | `/mounts/{mount}/folders` | `{ "folder" }` | 201; the folders above made too | ✅ |
| POST | `/mounts/{mount}/move` | `{ "from", "to" }` | 204; 409 when something stands at `to` | ✅ |
| DELETE | `/mounts/{mount}/entries` | `?key= &folder=true` | 204; a folder only when empty (409 otherwise) | ✅ |

### 2.9 Not carried over from `/jmouse/parallax/api`

| Old route | Why it is gone |
|---|---|
| `/computations`, `/computations/frames`, `/representations` | the mathematics engine's representations; a circuit's analysis is `/simulations` |
| `/subjects/resolve` | typed formulas and inline mathematical objects |
| `/formula/render`, `/formula/interpret`, `/formula/reference` | read only by the graph and plotter screens |
| `/scenes`, `/comparisons`, `/collections`, `/instruments` | no CAD screen uses them |
| `/jmx/symbol` | nothing calls it |
| `/jmx/*` | renamed `/language/*` — the language is jM, and `jmx` was the old one's name |

---

## 3 · The shapes

### 3.1 `Category`, `CategoryDraft`

```json
// GET /categories?kind=component
[
  { "code": "circuits", "title": "Circuits", "description": "Logic, timers, amplifiers", "position": 1, "documentCount": 214 },
  { "code": "passive",  "title": "Passive",  "description": null,                        "position": 2, "documentCount": 96 }
]
```

`documentCount` counts the **published catalogue** documents filed there (of `kind`, when asked). A draft:

```json
// PUT /categories/sensors
{ "title": "Sensors", "description": "Temperature, light, motion", "position": 7 }
```

A code is 2–40 lowercase letters, digits and hyphens, and never changes: documents point at it. `position` left out puts
it last.

### 3.2 `DocumentSummary`

```json
{
  "id": "6f1c2b9e-1d4a-4f0e-9b65-2a8f1c3d7e10",
  "slug": "and-gate",
  "title": "AND gate",
  "summary": "A two-input AND gate: high only while both inputs are high",
  "kind": "component",
  "category": "circuits",
  "folder": "components/base",
  "featured": false,
  "status": "PUBLISHED",
  "origin": "CATALOGUE",
  "owner": null,
  "tags": ["logic", "cmos"],
  "revision": 4,
  "revisionLabel": "20261027_002",
  "updatedAt": "2026-10-27T14:03:11.204Z",
  "problem": null
}
```

| Field | |
|---|---|
| `origin` | `CATALOGUE` (no owner) or `OWNED` |
| `owner` | `{ "type": "PROJECT", "id": "42" }`, or null for the catalogue |
| `folder` | where a shipped document's file stands under the library's root; null for one somebody wrote |
| `problem` | why the head text no longer reads — the language moved on since it was saved; null when it reads |

### 3.3 `DocumentDetail`

```json
// GET /documents/and-gate
{
  "summary": { …DocumentSummary… },
  "source": "component:and-gate(\"AND gate\", type: gate) {\n    meta { … }\n    use:cmos-hc\n    …\n}\n",
  "version": 7,
  "meta": { "designator": "U", "source": "the gate's function table: Y = A · B", "failure": "short Vcc GND" },
  "related": [
    { "slug": "cmos-hc",         "title": "74HC CMOS", "kind": "family", "relation": "use" },
    { "slug": "and-gate-symbol", "title": "AND gate",  "kind": "symbol", "relation": "symbol" }
  ],
  "sourceRevision": 4,
  "model": { …Component… },
  "shippedUpdatePending": false,
  "writable": true
}
```

| Field | |
|---|---|
| `source` | the text of the revision answered — the head, or the one `@` pinned |
| `version` | the optimistic version a `PUT` sends back (`?version=7`) |
| `meta` | every `meta` field the text writes besides summary, category and tags, as written |
| `sourceRevision` | the revision `source` is |
| `model` | the text read by the engine — `Component`, `Family`, `Circuit`, `SymbolDrawing`, `FootprintDrawing` or `BoardDrawing`, which `summary.kind` names; null when the text does not read (`summary.problem` says why) or is of a kind the engine keeps no model of (`functions`, `category`) |
| `writable` | whether the reader may write its next revision — told before a save is tried |

A **`Component`** model (abridged — every field of `org.jmouse.cad.component.Component`):

```json
{
  "head": { "slug": "and-gate", "title": "AND gate", "type": "gate", "meta": { … } },
  "bases": [],
  "uses": ["cmos-hc"],
  "parameters": [
    { "name": "propagation_delay", "value": { "written": "10 ns" }, "range": null, "step": null,
      "per": null, "flags": [], "description": "input to output at the family's nominal supply" }
  ],
  "pins": [
    { "name": "a",      "index": null, "nature": "digital", "direction": "input",  "drive": null, "numbers": [["1"]], "edge": false, "label": "input A" },
    { "name": "GND",    "index": null, "nature": "ground",  "direction": null,     "drive": null, "numbers": [["3"]], "edge": false, "label": null },
    { "name": "output", "index": null, "nature": "digital", "direction": "output", "drive": null, "numbers": [["4"]], "edge": false, "label": "output Y" }
  ],
  "symbol": { "uses": ["and-gate-symbol"], "box": [], "bindings": [] },
  "seats": [ { "footprint": "sot-23-5", "pads": {} } ],
  "behaviour": "function level()\n    …\nend\n",
  "expectations": [ … ]
}
```

A **`Circuit`** model:

```json
{
  "head": { "slug": "voltage-divider", "title": "Voltage divider", "type": null, "meta": { } },
  "parameters": [ { "name": "U_in", "value": { "written": "9 V" }, "range": { "low": { "written": "0 V" }, "high": { "written": "24 V" } }, … } ],
  "parts": [
    { "reference": "V1", "component": "voltage-source", "connections": [ { "pin": "positive", "net": "in" }, { "pin": "negative", "net": "0" } ],
      "settings": [ { "name": "U_output", "value": { "written": "U_in" } } ], "symbol": null, "footprint": null },
    { "reference": "R1", "component": "resistor@3", "connections": [ { "pin": "positive", "net": "in" }, { "pin": "negative", "net": "out" } ],
      "settings": [ { "name": "resistance", "value": { "written": "10 k" } } ], "symbol": null, "footprint": "axial-0207" }
  ],
  "layout": [ … ],
  "probes": [ { "channel": "A", "reading": "voltage", "subject": "out", … } ],
  "analysis": { "kind": "transient", "duration": 0.02 },
  "frame": "a4-landscape",
  "boundary": { "ports": [], "nets": [] }
}
```

### 3.4 `Revision`

```json
// GET /documents/lm358/revisions
[
  { "revision": 3, "label": "20261027_002", "checksum": "9c1e…", "writtenAt": "2026-10-27T14:03:11Z", "writtenBy": "account-17" },
  { "revision": 2, "label": "20261027_001", "checksum": "41aa…", "writtenAt": "2026-10-27T09:40:52Z", "writtenBy": "account-17" },
  { "revision": 1, "label": "20261019_001", "checksum": "07bd…", "writtenAt": "2026-10-19T08:00:00Z", "writtenBy": null }
]
```

`writtenBy` null is the shipped library writing its own file.

```json
// GET /documents/lm358/revisions/20261027
{ "revision": 3, "label": "20261027_002", "source": "component:lm358(…) { … }\n" }
```

### 3.5 Writing a document

```http
POST /jmouse/cad/api/owners/PROJECT/42/documents
Content-Type: application/json

{ "source": "circuit:divider(\"Divider\") {\n    meta { category: circuits }\n    parts {\n        R1 resistor (in, out) resistance 10 k\n        R2 resistor (out, 0) resistance 10 k\n    }\n}\n" }
```

```http
HTTP/1.1 201 Created

{ "summary": { "slug": "divider", "kind": "circuit", "origin": "OWNED", "owner": { "type": "PROJECT", "id": "42" },
               "revision": 1, "revisionLabel": "20261027_003", … },
  "source": "circuit:divider(\"Divider\") { … }\n", "version": 0, … }
```

What a write checks, as a whole, before anything is kept:

1. the text reads (`invalid-document`, with the line);
2. its slug is free, or is this document's own (`conflict`);
3. its category exists (`invalid-request`);
4. every document it names exists and is of the kind the relation needs — a `symbol` names a symbol (`invalid-document`);
5. a circuit's drawing agrees with its netlist (`layout-disagrees`);
6. the reader may write there.

An updated text equal to the head writes no revision and answers the detail unchanged.

### 3.6 `SheetView`, `SheetInstance`

```json
// GET /owners/PROJECT/42/sheets
[
  { "id": "2d4e…", "slug": "power-supply", "title": "Power supply", "updatedAt": "2026-10-27T10:12:00Z",
    "ports": ["vout", "gnd"], "nets": ["VCC"], "blocks": [], "problem": null },
  { "id": "88a0…", "slug": "main", "title": "Main", "updatedAt": "2026-10-27T11:30:00Z",
    "ports": [], "nets": ["VCC"], "blocks": [ { "block": "S1", "sheet": "power-supply" } ], "problem": null }
]
```

A part counts as a block only when it places another circuit **of the same owner**. An opened block answers its sheet
as drawn and what the circuit's run calls each of its parts and nets — a port the parent's net, a global itself, any
other `D2.mid` —, so a drawing of the sheet shows the run's figures; one deeper is `D1_D2`. A reference that places no
sheet is 400.

```json
// GET /documents/main/sheet?path=D2      (D2 divider (mid, low) R = 2 kΩ)
{ "block": "D2", "sheet": { …Circuit, the sheet as written… },
  "parts": { "R1": "D2_R1", "R2": "D2_R2" },
  "nets":  { "top": "mid", "tap": "low", "0": "0" },
  "values": { "R": 2000.0 } }
```

`values` is each of the sheet's parameters as this block runs it, in SI — its own value, or what the block's line sets —,
so a drawing of the block labels its parts with the block's figures, not the sheet's.

### 3.7 `ComponentReference`, `ComponentView`

```json
// GET /components
[
  { "reference": "2n2222",   "title": "2N2222A",   "type": "bjt",   "origin": "CATALOGUE", "owner": null, "status": "PUBLISHED" },
  { "reference": "my-opamp", "title": "My op-amp", "type": "opamp", "origin": "OWNED", "owner": { "type": "PROJECT", "id": "42" }, "status": "PUBLISHED" }
]
```

Every list of §2.3 takes `?owner=PROJECT:42`: the catalogue's documents the reader may see, then that owner's own — when
the reader may read there — each by title. Without it, the catalogue's alone.

`ComponentView` is the component **with its lineage applied** — its bases' and families' parameters, pins and equations
folded in, as the engine compiles it — plus what drawing it needs resolved in place:

```json
// GET /components/2n2222
{
  "component": { "slug": "2n2222", "title": "2N2222A", "type": "bjt", "parameters": [ …44, in SI… ], "pins": [ …emitter, base, collector… ], … },
  "lineage": ["bipolar-transistor", "npn-transistor", "2n2222"],
  "symbols": [ { "slug": "npn-transistor-symbol", "drawing": { …SymbolDrawing… } } ],
  "portrait": { "slug": "to-92-portrait", "drawing": { …SymbolDrawing… } },
  "footprints": [ { "footprint": "to-92-inline-footprint", "pads": {} },
                  { "footprint": "sot-23-footprint", "pads": { "1": "base", "2": "emitter", "3": "collector" } } ],
  "sheetParameters": []
}
```

`component` is the engine's `SimulatedComponent`. The first of `symbols` and of `footprints` is the default; a
footprint's empty `pads` lands each pin on the pad of its package number. How a symbol's ports bind to the pins is the
component's `symbol` line, inside `component`. A component the engine cannot compile answers 422 `component-refused`.

A **circuit** asked for here answers as a block of it is placed on another sheet: `component.type` is `sheet`, its
`pins` are its `ports` in order (a `power`/`ground` port a pin of that nature, an `input`/`output` one of that
direction), it has nothing to run — a bench unfolds the block into the sheet's parts —, and `sheetParameters` are the
sheet's sliders, each block setting its own on its line (`S2 rc-stage (in: mid, out: out) R: 2.2 kΩ`).

### 3.8 `DrawnDocument`

```json
// GET /footprints  (one row)
{
  "slug": "axial-0207-p10.16",
  "title": "Axial 0207, 10.16 mm pitch",
  "summary": null,
  "origin": "CATALOGUE",
  "owner": null,
  "folder": "footprints/passive/axial",
  "code": "FP.AXIAL:R0207-P10.16-H",
  "provenance": { "source": "JEDEC MS-001", "authors": "jMouse", "license": "CERN-OHL-W-2.0" },
  "drawing": {
    "pads": [ { "number": "1", "type": "through-hole", "shape": "circle", "x": -5.08, "y": 0, "rotation": 0,
                "width": 1.6, "height": 1.6, "drillWidth": 0.8, "drillHeight": 0.8, "roundness": 0, "path": null }, … ],
    "outlines": [ { "layer": "silkscreen", "width": 0.12, "filled": false, "path": "M -3.5 -1.1 H 3.5 V 1.1 H -3.5 Z" } ],
    "fields": [ { "subject": "reference", "x": 0, "y": -2, "height": 1, "rotation": 0, "layer": "silkscreen" } ]
  }
}
```

`drawing` is `FootprintDrawing` for a footprint and `SymbolDrawing` (`width`, `height`, `paths`, `pins`, `labels`,
`indicators`, `inputs`, `elements`) for a symbol, portrait or frame, in millimetres. `provenance` is what the text's
`meta` says, each field as written or null — a KiCad conversion names its library, KiCad's library team and KiCad's
licence. A document whose text no longer reads is listed with `drawing` null, never left out.

### 3.9 `SimulationRequest`, `SimulationResult`, `Job`

```json
// POST /simulations
{
  "circuit": "rc-charging",
  "parameters": { "U": "10 V", "S1.closed": "1" },
  "analysis": "transient",
  "duration": 0.008,
  "samples": 800
}
```

| Field | |
|---|---|
| `circuit` | a stored circuit, by slug or `slug@revision` — **or** `source`, an unsaved text (the bench's edits); exactly one of the two |
| `owner` | whose documents the circuit is found among besides the catalogue, `TYPE:id`; left out: the catalogue's alone |
| `parameters` | other values for the circuit's sliders by name, or a part's setting as `part.setting`, each **as written** (`"10 V"`); a slider is held within its range |
| `analysis` | `transient` or `operating` — the analyses the language writes; left out: the circuit's own `analysis` line |
| `duration` | seconds a transient runs; left out: the analysis line's (`analysis transient for 8 ms`) |
| `samples` | 2–100 000, 2000 when left out; more than `jmouse.cad.simulation.inline-samples` (20 000) is a job |
| `everyPart` | record every part's current and power, not only what has a current of its own |

```json
// 200 OK — a transient (rc-charging: U = 5 V, τ = RC = 1 ms)
{
  "analysis": "transient",
  "circuit": "rc-charging",
  "time": [0, 0.00001, 0.00002, …],
  "voltages": { "in": [5, 5, …], "feed": [5, 5, …], "out": [0, 0.049, …] },
  "lows": { "out": [0, 0.044, …] },
  "highs": { "out": [0.049, 0.098, …] },
  "currents": { "V1.positive": [0.005, …], "V1.negative": [-0.005, …] },
  "powers": { "R1": [0.025, …] },
  "levels": [ {}, {}, … ]
}
```

A column is the circuit's own name — a node, `part.pin`, a part — never `V(out)`; voltages are in volts, currents in
amperes, powers in watts. `lows` and `highs` are each node's least and greatest voltage between one sample and the next:
what a peak-detecting screen draws, so a signal faster than the samples shows as a band rather than a saw. `levels` is
every digital node's level at each sample, empty maps for a circuit with none.

```json
// 200 OK — an operating point
{ "analysis": "operating", "circuit": "rc-charging", "voltages": { "in": 5, "feed": 5, "out": 5 } }
```

A run of more samples than an answer holds answers **202** with a job, read from `GET /simulations/jobs/{id}/result`
when it is done:

```json
{ "id": "d01006db-…", "state": "RUNNING", "progress": 0.42, "queuedAt": "…", "startedAt": "…", "finishedAt": null, "failure": null }
```

`state` is `QUEUED`, `RUNNING`, `DONE`, `FAILED` or `CANCELLED`; the result is refused with 409 while it runs, and 422
`simulation-failed` naming why when it failed. A job is seen by the actor who started it alone — anybody else is told it
does not exist — and kept for `jmouse.cad.simulation.job-retention` (30 minutes) after it ends. ⚠️ Jobs live in the
process, not the database: a restart forgets the ones in flight.

Refused: a slider outside its range or a name the circuit has not (400 `invalid-request`), an analysis the language does
not write, a document that is no circuit, both or neither of `circuit` and `source`; a circuit that does not converge
(422 `simulation-failed`).

### 3.10 `Verdict`

```json
// POST /expectations   { "reference": "rc-charging" }    — or { "source": "…" }, and an optional "owner"
[
  { "title": "after one τ the capacitor holds 63 % of the source", "source": "Vc(τ) = Vs · (1 − e⁻¹) = 5 V · 0.632, τ = RC = 1 ms",
    "measured": "3.1610", "window": "[3.0968, 3.2232]", "met": true, "refusal": null },
  { "title": "the current starts at Vs / R", "source": "I(0) = Vs / R = 5 V / 1 kΩ",
    "measured": "0.0049812", "window": "[0.0049000, 0.0051000]", "met": true, "refusal": null }
]
```

Every expectation a circuit — or a component's datasheet — states of itself, measured on the nominal bench. `measured`
is what was measured as the engine writes it — a value, or a row of them —, null with `refusal` saying why when it could
not be measured.

### 3.11 Editing

`GET /language/schema` answers `LanguageSchema` — read off the grammar the reader reads with, never listed by hand:

```json
{
  "kinds": [ { "word": "component", "blocks": ["behaviour", "meta", "parameters", "pins", …], "repeatable": ["expect"], "lines": [] },
             { "word": "footprint", "blocks": ["meta"], "repeatable": [], "lines": [ …its pad, outline and field lines… ] } ],
  "blocks": [
    { "word": "meta", "form": "META", "header": null, "lines": [] },
    { "word": "pins", "form": "LINES", "header": null, "lines": [
      { "word": null, "term": "pin", "prefixes": [], "ending": "label", "endingValue": "text",
        "slots": [ { "role": "name", "value": "name", "allowed": [], "optional": false, "mark": null, "termWord": null },
                   { "role": "nature", "value": "word", "allowed": ["analog", "digital", "ground", "power"], "optional": false, … },
                   { "role": "direction", "value": "word", "allowed": ["bidirectional", "input", "output"], "optional": true, … } ],
        "clauses": [ { "word": "edge", "value": "flag", "allowed": [], "required": false, "repeatable": false, "nested": null } ],
        "says": "a pin is 'name analog|digital|power|ground [input|output|bidirectional] [open-drain] [numbers …] [edge] [\"label\"]'" } ] },
    { "word": "behaviour", "form": "SCRIPT", "header": null, "lines": [] }
  ]
}
```

A value kind is named as the grammar declares it (`word`, `name`, `quantity`, `point`, `numbers`, `path`…); a line read by
something other than a grammar — an equation — carries only `says`. The same walk of the language
(`LanguageWalk`) makes the editor's TextMate grammar, so the two cannot disagree.

⚠️ **An edit is structural, and the language reads it** (`form`, `edit` and the canvases ✅): an editor sends where a
line stands in the tree and its values by role — `{ "resistance": "22 k" }` —; the server reads each value with the
lexer and the value kind of its slot, builds the line's node, puts it in the tree and answers the text its writer writes.
Nobody concatenates language text, and no span of text is patched.

```json
// POST /language/form   { "source": "component:and-gate(\"AND gate\", type: gate) { … }" }
// 200 OK — EditedDocument
{
  "source": "component:and-gate(\"AND gate\", type: gate) { … }",
  "form": {
    "kind": "component", "slug": "and-gate", "title": "AND gate", "type": "gate", "spelling": "TERMS",
    "statements": [
      { "index": 0, "form": "META", "word": "meta", "fields": [ { "word": "designator", "written": "U" }, … ] },
      { "index": 1, "form": "DECLARATION", "word": "use", "names": ["cmos-hc"] },
      { "index": 2, "form": "BLOCK", "word": "parameters", "header": null, "lines": [
        { "index": 0, "word": "parameter", "prefixes": [], "slots": { "name": "propagation_delay", "value": "= 10 ns" },
          "clauses": [], "ending": "\"input to output at the family's nominal supply\"", "written": null } ] },
      { "index": 3, "form": "BLOCK", "word": "pins", "lines": [ { "index": 0, "slots": { "name": "a", "nature": "digital", "direction": "input", "numbers": "1" }, … } ] },
      { "index": 5, "form": "SCRIPT", "word": "behaviour", "written": "function level() … end …" }
    ]
  },
  "problems": []
}
```

A statement is `BLOCK` (its `header` and `lines`), `LINE` (a drawn kind's line — a symbol's `pin` — standing in the body),
`DECLARATION` (`names`), `META` (`fields`), `SCRIPT` or `OTHER` (`written`, as written). A line's values are as written:
a slot by its role, a clause by its word, the ending by its name. A text that does not read answers `form: null` and its
problem — `{ "line": 2, "column": 10, "detail": "a term's arguments close with ')': …" }` —, so an editor shows a
half-written text instead of refusing it; a text whose model the engine refuses has a form and that problem.

```json
// POST /language/edit
{ "source": "component:and-gate(…) { … }",
  "edit": { "operation": "set-line", "statement": 2, "line": 0, "values": { "value": "12 ns" } } }
// 200 OK — the EditedDocument of the next text:  parameter:propagation_delay(12 ns, "…")
```

| `operation` | Takes | Does |
|---|---|---|
| `set-line` | `statement`, `line` (`-1` for a block's head), `values`, `flags` | new values on a line that stands; an empty value takes an optional one away |
| `add-line` | `statement` (null: a drawn kind's body), `before` (null: after the last), `word`, `values`, `flags` | a new line of the grammar its word or term word names |
| `remove-line` | `statement`, `line` | a line taken away; a `LINE` statement whole |
| `set-field` | `key`, `written` | a `meta` field set, or taken away by an empty value |
| `add-block` | `word`, `values`, `flags` | a block the kind allows and has not — or may write again —, at the end, its head of `values` (`symbol uses …`), its body empty; a script block is written in the text |
| `remove-statement` | `statement` | a statement taken away whole: a block with its lines, a declaration, a line standing in the body |
| `set-title` | `written` | the title; the address stays, written out (`as slug`) when the new title would give another |
| `set-address` | `written` | the address (slug), as a title would give it — lower case, words joined by hyphens —; the title stays |

A value is typed as either spelling writes it — `"12 ns"` as a term's argument or `"= 12 ns"` as a line's —, a text in its
quotes (`"\"enable\""`), and is read by the kind of value its slot takes; the document is written back spelled as it was
read, every comment above and beside a line kept. An edit the language refuses is 400 `invalid-document` naming why —
`'hydraulic' is not one of [analog, digital, ground, power]`, `a pin line cannot go without its 'name'`. A clause
holding a line of its own is not edited yet.

`POST /language/read { source }` answers `ReadText` — `{ kind, slug, title, model, parameters, presentation, problems }`: an
unsaved text read as the document it would be, what a draft is previewed by. `presentation` — here and on every
`DocumentDetail` — says how the document is shown, read off its model: `drawing` (a symbol, portrait, frame), `part` (a
component), `instrument` (a circuit), `footprint`, `board`; null for one that is not shown.

#### The canvases

A canvas sends the text it holds and **what it drew, as the model it was answered** — a `SymbolDrawing`, a
`FootprintDrawing`, a `BoardDrawing` from `/language/drawn` or a document's `model`, or a `CircuitSheet` made of a
circuit's `model` —, changed. The server builds every drawn line on the grammar that reads it, reads every value an
author wrote (a property, a range, a formula) with the language, and writes the drawing into the tree beside what is
written:

| Canvas | Lines matched | So |
|---|---|---|
| `drawing`, `footprint`, `board` | by **class and place** — the third `pin` drawn against the third `pin` written | a pad moved rewrites that pad's line alone |
| `circuit` | by **key**, inside its `parts`, `layout` and `probes` blocks — `R1`, `W3`, `CH1`, `frame`, `time` | a part added is written after the one drawn before it |

A board's offered sizes are lines of their own, drawn with it: `sizes tracks 0.25, 0.4` and `sizes vias drill 0.3, 0.4
diameter 0.6, 0.8` — each via's drill and diameter in the same place —, carried as `BoardDrawing.sizes`.

A line the drawing did not change **stays as written**, comments above and beside it included; a changed one takes its
place and keeps its comments; one fewer drawn takes one away. What no canvas draws — `meta`, declarations, a circuit's
`parameters`, `ports`, `expect`, `analysis` — is never touched. `title`, `summary` and
`category` set the head (null leaves a field as written; a new title keeps the slug, writing `as slug` when the title
would give another). An unchanged drawing answers its text byte for byte.

```json
// POST /language/circuit
{ "source": "circuit \"Charging a capacitor through a resistor\" as rc-charging { … }",
  "circuit": {
    "parts":  [ { "reference": "R2", "component": "resistor", "connections": [ { "pin": null, "net": "out" }, { "pin": null, "net": "0" } ],
                  "settings": [ { "name": "resistance", "value": { "written": "10 kΩ" } } ], "symbol": null, "footprint": null }, … ],
    "layout": [ { "reference": "R2", "unit": null, "at": { "x": 1160, "y": 600 }, "rotation": 90, "mirrored": false,
                  "name": null, "value": null, "readings": null, "size": null, "shows": [] },
                { "reference": "W11", "from": "R2.positive", "to": "Jo.j", "via": [ { "x": 1160, "y": 360 } ] }, … ],
    "frame": null, "probes": [ { "channel": "CH1", "reading": "voltage", "subject": "out", "reference": null,
                                 "scale": { "written": "1 V/div" }, "shows": null } ],
    "timeBase": { "written": "1 ms/div" } },
  "summary": "Two resistors now." }
// 200 OK — EditedDocument; of the text, only these lines changed:
//   summary "Two resistors now."
//   R2 resistor (out, 0) resistance = 10 kΩ        (after VM1, the part drawn before it)
//   R2 at 1160, 600 rotate 90
//   W11 wire R2.positive to Jo.j via 1160 360
//   - CH2 current R1 1 mA/div                      (the probe the sheet no longer has)
```

A layout entry is known by its fields — a wire by `from` and `to`, a mark by `symbol` —, a setting's value as
`{ "written" }` (a formula) or `{ "text" }` (words in quotes); nothing names a type. A drawing's values travel with
their `expressions` (what a browser evaluates) and are read back without them: the server reads the words again.

```json
// POST /language/drawn   { "source": "symbol:resistor-symbol(\"Resistor\") { … }" }
// 200 OK — DrawnText
{ "source": "…", "kind": "symbol", "drawing": { "width": 120, "height": 40, "paths": [ … ], "pins": [ … ], … }, "problems": [] }
```

`drawing` is null on a text that does not read, and `problems` says where — `{ "line": 33, "column": 12, "detail":
"':' is not part of this line: …" }`; a kind that is not drawn (a circuit, a component) is 400.

Refusals are 400: a canvas on another kind — `This editor draws footprint; the text holds a symbol.` —, a drawing that
does not write — `'1 kΩ, closed = 1' holds more than one value: a part is …` —, a path that does not read, two lines
drawn with one key.

### 3.12 Exchange

```json
// POST /exchange/spice/read   { "files": { "rc.cir": "RC low-pass\nV1 in 0 DC 5\nR1 in out 1k\nC1 out 0 1u\n.tran 10u 8m\n.end\n" }, "main": "rc.cir", "title": "RC from SPICE" }
{ "documents": [ { "slug": "rc-from-spice", "kind": "circuit", "source": "circuit \"RC from SPICE\" {\n    meta { summary \"Imported from a SPICE netlist: RC low-pass\" }\n    parts {\n        V1 voltage-source (positive = in, negative = 0) U_output = 5 V\n        R1 resistor (terminal_one = in, terminal_two = out) resistance = 1 kOhm\n        …\n    analysis transient for 8 ms\n}\n",
  } ], "notes": [] }
```

`/exchange/spice/export` answers the deck the converter built as nodes, read back by the SPICE parser before it is answered, and
the notes of what has no SPICE counterpart — `the layout has no SPICE counterpart: a netlist is not drawn; left out`,
`a SPICE element's name begins with its letter: SW1 is XSW1`.

```json
// POST /simulations/engines/ngspice/transient   { "source": "circuit \"Charging a capacitor…\" as rc-charging { … }", "samples": 800 }
{ "result": { "analysis": "transient", "circuit": "rc-charging", "time": [0, 1.0e-5, …],
              "voltages": { "in": […], "feed": […], "out": […] }, "currents": { "V1": […] },
              "lows": {}, "highs": {}, "powers": {}, "levels": [] },
  "deck": "… .tran 1e-5 0.008\n.options temp=25 tnom=25\n.control\nset nobreak\n…\nprint all\n.endc\n.end\n",
  "notes": [ … ] }
```

The run is the engine's `Transient` shape: each net's voltage by its net, each source's current by its part; the peaks,
powers and logic levels are the engine's alone. It starts from SPICE's operating point at time 0. On rc-charging ngspice
puts `out` at 3.1606 V after one τ, the engine 3.1629 V. Refusals: 422 `spice-not-configured` (no
`jmouse.cad.spice.command`), 422 `spice-failed` (ngspice gave no answer), 422 `conversion-refused` (a part the deck cannot
carry — its answer would be another circuit's), 400 `invalid-document` (the text does not read).

```json
// POST /owners/PROJECT/7/exchange/kicad/import   { "main": "rc-charging-copy.kicad_sch", "files": { "rc-charging-copy.kicad_sch": "(kicad_sch …)" } }
// 201 Created
{ "root": "rc-charging-copy", "created": ["rc-charging-copy"], "updated": [], "reused": [], "notes": [] }
// the same import again: nothing written, every document named
{ "root": "rc-charging-copy", "created": [], "updated": [], "reused": ["rc-charging-copy"], "notes": [] }
```

The library map that names which jM component each KiCad symbol is ships with `jmouse-cad-kicad`
(`KiCadLibraryMap.standard()`). An export writes a component with no KiCad counterpart under the library `jmCAD`, its
value parameter in `Value` and every other setting of the part as a field of its parameter's name, each computed over
the circuit's parameters (`resistance = R` → `1k`); an import reads `jmCAD:voltage-source` back as `voltage-source`
and a field back only where the component has a parameter of that name, so a design's own fields (`MPN`) are left
alone. A circuit exported and imported runs as it did: rc-charging at U = 7 V gives 4.428 V after one τ both ways.
KiCad names a net by its labels, so every named net is given one: `out` comes back as `out`. A document of that name not the owner's is 409; a root not among the files is 400.

---

## 4 · The live socket — `/jmouse/cad/live`

⚠️ **A module beside the API**: `jmouse-cad-live`, imported as `CadLiveConfiguration`; the product owes a
`LiveAuthenticator` (token → `LivePrincipal`, which runs each message as its actor). The stepping is the engine's
`LiveCircuitSession` (`jmouse-cad-simulation`), so a socket only carries it. ✅

One running circuit per socket. The server steps it and streams what the probes read; the reader starts, pauses, moves
probes, works live parts and edits the circuit, each as a message. The bearer token rides as the `bearer.<token>`
subprotocol beside `cad.live` — the one place a browser lets a WebSocket handshake carry it.

### 4.1 What the reader sends (JSON text frames)

| `type` | Carries | Does |
|---|---|---|
| `start` | `circuit` or `source`, `owner`, `parameters`, `interval`, `probes[]`, `speed`, `gapless` | elaborates the circuit and starts it, refused unless the reader may run circuits where it belongs |
| `pause` / `resume` / `step` / `reset` | — | as named |
| `probes` | `probes[]` | replaces what is read every sample |
| `pace` | `speed` | runs faster or slower than real time |
| `sample` | `interval` | changes the time between samples |
| `acquire` | `gapless` | every sample in order, slowed to show them — or real time, one block a frame |
| `set` | `part`, `parameter`, `value` | works a live part: a switch, a button, a wiper |
| `restart` | `source`, `parameters` | the edited circuit, from zero |
| `retune` | `source`, `parameters` | the edited circuit, carried on from the state it reached |

What the reader set is kept as the circuit's settings, as written — a slider by its parameter's name (`"U": "7"`), a
live part's value as `"SW1.closed": "0"` — and each change builds the circuit with them as a measurement `with` them
does. ⚠️ A live part inside a placed sheet (`S1_SW1`) is not set yet: it exists in the unfolded circuit only.
| `capture` | `from`, `to` | replays a stretch already passed, finely |

A probe: `{ "channel": "A", "quantity": "VOLTAGE", "positive": "out", "negative": "0" }` — or `"part": "R1",
"terminal": "positive"` for a current, a power, an instrument's reading or a logic level.

### 4.2 What the server sends

JSON text frames:

```json
{ "type": "started", "interval": 0.00001, "nodes": ["in", "out"], "controls": [ { "part": "S1", "parameter": "closed", "label": "S1", "minimum": 0, "maximum": 1, "step": 1, "value": 0, "kind": "TOGGLE" } ], "probes": [ … ] }
{ "type": "changed", "continued": true, "controls": [ … ], "time": 0.0412 }
{ "type": "problem", "message": "The circuit does not converge at 3.1 ms: node 'out' runs away." }
{ "type": "capture-refused", "message": "That stretch is older than the run keeps." }
```

and binary frames — a frame of samples (`frame`) or a replayed stretch (`capture`): a JSON header (channels, units, the
parts' readings, heat and failures, logic levels, the load) followed by the time column and one `Float32` column per
channel, encoded by `FrameEncoding`.

---

## 5 · How a product adopts it

```java
@Configuration
@Import(CadManagementConfiguration.class)
@EntityScan({"org.example.product", "org.jmouse.cad.jpa"})
class CadSetup {

    @Bean
    ExternalAccessRules cadAccessRules() {
        return CadAccessRules.builder()
                .capability(CadCapability.CATALOGUE_WRITE, "catalogue:write", "GLOBAL")
                .capability(CadCapability.OWNER_READ, "project:read", "PROJECT")
                .capability(CadCapability.OWNER_WRITE, "project:write", "PROJECT")
                .build();
    }

    @Bean
    CadAuthority cadAuthority(…) { … }   // who is acting, and what they may do where

    @Bean
    CadPlaces cadPlaces(…) { … }         // what an owner is: its chain, whether it takes new work
}
```

The product also lets `/jmouse/cad/**` through its security chain (the routes decide for themselves) and registers an
access-target resolver for `DocumentOwner`. `CadManagementConfiguration` is imported, never component-scanned.

`jmouse-cad-spring-boot` migrates its own schema (`cad_schema_history`) before the product's Flyway and before every
entity manager factory — so a product with no Flyway of its own works too —, and imports the shipped documents into the
catalogue when the application is ready: `jmouse-cad-library`'s own and the product's from
`jmouse.cad.library.locations`. Category documents are written first and never rewritten after, circuits last, relations
once everything exists. A document a curator has edited keeps the edit and is marked `shippedUpdatePending`; a file
whose text is the one already taken is not read at all (a start where nothing moved: 1499 documents, all unchanged).
⚠️ **A slug shipped by two sources is refused at start, naming both files** — the catalogue keeps one document per
address, and taking either would be a guess that flips on every start.

| Property | Default | | State |
|---|---|---|---|
| `jmouse.cad.management.prefix` | `/jmouse/cad/api` | where these routes answer | ✅ |
| `jmouse.cad.migrations.enabled` | `true` | migrate the library's schema | ✅ |
| `jmouse.cad.library.import-on-start` | `true` | import the shipped documents when the application is ready | ✅ |
| `jmouse.cad.library.include-engine` | `true` | whether `jmouse-cad-library`'s own documents are among them | ✅ |
| `jmouse.cad.library.locations` | — | the product's own shipped documents, as resource locations (`classpath:catalogue`) | ✅ |
| `jmouse.cad.documents.maximum-length` | `1000000` | the longest text a document may be, in characters | ✅ |
| `jmouse.cad.documents.cached-readings` | `2000` | how many read documents are kept by checksum | ✅ |
| `jmouse.cad.revisions.zone` | the system zone | the day a revision label is written in | ✅ |
| `jmouse.cad.live.path` | `/jmouse/cad/live` | where the socket answers (live module) | ✅ |
| `jmouse.cad.simulation.inline-samples` | `20000` | the largest simulation answered at once; larger ones are jobs | ✅ |
| `jmouse.cad.simulation.maximum-samples` | `100000` | the most samples a simulation may ask for | ✅ |
| `jmouse.cad.simulation.workers` | `1` | jobs run at once | ✅ |
| `jmouse.cad.simulation.job-retention` | `PT30M` | how long a finished job and its result are kept | ✅ |
| `jmouse.cad.spice.command` | — | the program that runs a deck from its standard input, for the `ngspice` engine; unset, the engine is listed as off (SPICE plugin) | ✅ |
| `jmouse.cad.xyce.command` | — | the program that runs a deck from its standard input and prints the run's CSV, for the `xyce` engine; unset, the engine is listed as off (Xyce plugin) | ✅ |
| `jmouse.cad.spice.timeout`, `.samples` | `PT60S`, `1000` | how long one run may take; how many steps it is printed in when none are asked | ✅ |
| `jmouse.cad.kicad.symbol-folders`, `.footprint-folders` | — | KiCad's installed libraries: for part search, and the footprints an import looks up (KiCad module) | ✅ |
| `jmouse.cad.parts.limit` | `24` | how many candidates each provider answers with (parts module) | ✅ |
| `jmouse.cad.parts.nexar.*` | — | a Nexar application's `client-id` and `client-secret`; `limit` 10, `timeout` 8 s | ✅ |
