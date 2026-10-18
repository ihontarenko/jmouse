# jmouse-parallax — mathematical objects, their representations, and the machinery that hosts them

> **Agent orientation file.** Written so you do not have to read the modules to use them. If something
> here disagrees with the code, the code wins — and fix this file in the same change.
>
> Modules: the engine `jmouse-parallax` (`org.jmouse.parallax`, + `jmouse-formula`, `jmouse-script-el`,
> `jackson-annotations`; no Spring) with jMX, the Parallax script language on jMS, in `org.jmouse.parallax.script` ·
> `jmouse-parallax-jpa` · `jmouse-spring/jmouse-parallax-spring-boot` · `jmouse-spring/jmouse-parallax-management` ·
> `jmouse-spring/jmouse-parallax-live` — see "The five modules" · The formula language underneath:
> [JMOUSE_FORMULA.md](JMOUSE_FORMULA.md) · Interface: `jmouse-ui/packages/parallax` (headless) and
> `packages/parallax-react` (their READMEs) · First consumer: Parallax (`jMouseProjects/Parallax`), which supplies
> identity, policy, workspaces and the shipped catalogue `.jmx` files.

## What it is in one paragraph

One model for "a mathematical object you can look at in many ways". An **object** is a sealed
`ObjectDefinition` — 31 kinds, from an explicit curve to a circuit, a program or a graph — plus its
`ParameterDefinition`s. `DefinitionCompiler` turns it, with parameter values, into a `CompiledObject` whose jMF
formulas are compiled once. A **representation** is a `Computation`: one stateless implementation per
`RepresentationKind` (48 of them, in 15 families) that says whether it accepts an object, what settings it takes,
roughly what it costs, and computes a `Dataset` — a sealed, JSON-ready result (mesh, polylines, scalar grid, …) a
renderer draws. Adding a representation is adding one `Computation`; nothing else changes. The four Spring-side
modules store objects as jMX text with revisions, cache datasets by fingerprint, queue the expensive ones, serve it
all under `/jmouse/parallax/api` and run circuits live on a socket — each refusal and each permission asked of a host
through a small SPI.

## The five modules

| Module | Package | Holds | Spring |
|---|---|---|---|
| `jmouse-parallax` | `org.jmouse.parallax.*` | the engine (kinds, computations, datasets, numerics, the circuit engine, checks, files), jMX (`.script`), and the **SPI** a host implements (`authority`, `owner`, `limits`, `live`, `catalogue`, `problem`, `event`) | none |
| `jmouse-parallax-jpa` | `org.jmouse.parallax.jpa.*` | entities and `EntityManager` stores (`ObjectStore`, `CategoryStore`, `InstrumentStore`, `SceneStore`, `ComparisonStore`, `CollectionStore`, `DatasetCache`, `JobQueue`, `EvaluationLedger`), `ParallaxMigrations`, `db/parallax/{mysql,postgresql}` | JPA only |
| `jmouse-spring/jmouse-parallax-spring-boot` | `org.jmouse.parallax.store.*`, `.spring.autoconfigure` | the application services with their `@Transactional` (`Catalogue`, `CatalogueImport`, `Scenes`, `Comparisons`, `Collections`, `Computing`, `Jobs`, `ComputationWorker`, `CatalogueWarmup`, `Evaluations`, `DocumentEditing`, `Subjects`, `FormulaGuide`, `ParallaxStatistics`), the migrator, a scheduler of its own; three auto-configurations | auto-configured |
| `jmouse-spring/jmouse-parallax-management` | `org.jmouse.parallax.management` | every REST route under `/jmouse/parallax/api`, `ParallaxAccessRules`, `ParallaxProblemDetails`, `@OwnerAddress` | **imported** |
| `jmouse-spring/jmouse-parallax-live` | `org.jmouse.parallax.live.socket` | the JSR-356 socket at `/jmouse/parallax/live`: `LiveRun`, `FrameEncoding`, pacing, `LiveCircuits` (also the `LiveActivity` count) | **imported** |

⚠️ **The two web modules are never auto-configured and never component-scanned.** A route nobody declared is a route
nobody gates; `@Import` and the access rules arrive together or the boot stops (see "Hosting it").

⚠️ **The object is not its picture.** A surface has a surface, contours, cross-sections, gradient, normals,
curvature, tangent planes, critical points, projections and a parameter sweep — all computed from the same
definition. Pick representations with `Computations.applicableTo(definition)`; never branch on the kind yourself.

## The map

```
org.jmouse.parallax.object
├── ObjectDefinition (sealed)   31 records, Jackson discriminator "kind" — see "The 31 kinds"
├── ObjectKind                  the 31 names, objectKind() of every definition · nature() → ObjectNature
├── ObjectNature                MATHEMATICAL CIRCUIT PROGRAM COMPONENT SYMBOL GRAPH — derived, never stored
├── Interval                    (minimum, maximum), finite and ordered — refuses [3, -3]
├── ParameterDefinition         name, label, defaultValue, minimum, maximum, step, animatable · clamp(value)
├── DefinitionCompiler          compile(definition, parameters, values) → CompiledObject · variablesOf(definition)
├── CompiledObject              definition + parameter values; function(source, variables) → BoundFunction,
│                               derivative(…), complexFunction(…), constant(source), withParameters(overrides)
└── BoundFunction               a compiled formula with its parameters bound; NOT thread-safe (one buffer)

org.jmouse.parallax
├── Computation           SPI   kind() · accepts(definition) · settings() · estimatedCost(…) · compute(…)
├── Computations                the registry: require(kind) · forObject(kind, definition) · applicableTo(definition) · all()
│                               a FAMILY definition is answered by its member's computation, wrapped (FamilyComputation)
├── StandardComputations        all() · registry() — every computation this module ships; refuses a kind with none
├── RepresentationKind          48 kinds, each with a Family (15)
├── SettingSpecification        name, Type (INTEGER NUMBER BOOLEAN CHOICE VECTOR TEXT), default, bounds, choices, length
├── ComputationSettings         resolve(specifications, given) — checks every value; integer(…) number(…) choice(…) vector(…)
├── ComputationProgress         report(fraction, message) · cancelled() · NONE · ComputationCancelled
├── Sampling · PlaneWindow      shared grid / bounds helpers · a plane raster's window (centre x, centre y, half-width)
├── Dataset (sealed)            14 formats, Jackson discriminator "format" — see "Datasets"
└── analysis/ complex/ dimension/ dynamics/ field/ fractal/ geometry/ linear/ parameter/ probability/ region/ value/
    program/ circuit/           the computations, one package per family

org.jmouse.parallax  — the SPI a host implements (see "Hosting it")
├── authority/                  ParallaxAuthority · ParallaxPlaces · ParallaxPlace (owner chain) · ParallaxCapability (17)
├── owner/ParallaxOwner         (type, id) — the host's word and id; "TYPE:id" via parse/toString; SCENE is the library's
├── limits/ParallaxLimits       budgets and switches, asked at the moment of use
├── live/                       LiveAuthenticator · LivePrincipal · LiveActivity
├── catalogue/                  ShippedDocumentHandler · HomeCategoryPolicy · StandardHomeCategoryPolicy
├── problem/ParallaxProblem     the one refusal type: NotFound Conflict Invalid Refused Unauthenticated LimitExceeded Busy
├── event/                      ObjectCreated · ObjectStatusChanged · EvaluationCompleted · JobFailed · CatalogueWarmed
└── support/ParallaxIdentifiers

org.jmouse.parallax.numerics
├── GridMesh                    a parametric grid → Mesh with normals and seams
├── MarchingSquares             contour polylines of a scalar grid
├── MarchingTetrahedra          an implicit surface → Mesh
├── OdeIntegrator               Dormand–Prince 5(4), adaptive; OrdinarySystem; Solution(times, states, escaped, steps)
├── LinearAlgebra               determinant rank trace solve svd (Jacobi) eigenpairs (Faddeev–LeVerrier + Durand–Kerner)
│                               polynomialRoots nullSpace columnSpace multiply transpose identity
└── NewtonSolver                solve(function, start, …) · rootsInBox(function, minimum, maximum, …)

org.jmouse.parallax.file
├── DatasetFormat         SPI   name · extension · mediaType · accepts(dataset) · write(dataset, writer)
├── DatasetFormats              standard(): obj · stl · ply · csv — byName(…) · applicableTo(dataset)
└── ObjectFiles                 readFormulaFile(.jmf) → objects inferred from their variables · readPointTable(.csv)

org.jmouse.parallax.verification
├── ConsistencyCheck      SPI   code · description · appliesTo(definition) · check(compiled) → CheckResult(outcome, measured, tolerance, detail)
└── StandardChecks              all(): seven independent cross-checks of the model, and a component against its datasheet

org.jmouse.parallax.circuit  the netlist engine behind `component` and `circuit` documents
├── ComponentDocuments          a component / family jMX document → ComponentModel (meta, parameters in SI, pins, equations, measure, display, leads)
├── ModelEquations              a model's equations compiled per unit: pin names as voltages, d(q)/dt, pin.current, the exact Jacobian
├── PartExpression              one expression over a part (measure, display): pin voltages, pin.current, states, parameters, power, time
└── ComponentCheck              a ConsistencyCheck (COMPONENT_DATASHEET), not a representation: every `expect` built on its bench, simulated, measured, PASS / FAIL

org.jmouse.parallax.object.ComponentHierarchy
                                a circuit's component tree resolved and flattened; Assembly reports a part made of parts by its own pins

org.jmouse.parallax.script        jMX itself — ExperienceReader, Kind (which blocks a kind may hold), Block (what each block reads as)
├── reader/                     DocumentReader, DeclarationReader, CircuitReader, SymbolReader, GraphReader, …: the truth about the syntax
├── form/                       the schema and form lines an editor is generated from (`/jmx/schema`, `/jmx/form`, `/jmx/edit`)
└── engine/                     EngineSelection (document → ObjectDefinition, the one place) · DefinitionWriter (and back) ·
                                ExperienceDefinitions · ObjectExplanation · CanvasTexts (a canvas's drawing patched into the text)
```

## The whole usage

```java
// 1. an object: a definition (any of the 31 kinds) and its parameters
ObjectDefinition definition = new ObjectDefinition.ExplicitSurface(
        "if(x^2 + y^2 < 4, A sin(k (x^2 + y^2)))", Interval.of(-3, 3), Interval.of(-3, 3));
List<ParameterDefinition> parameters = List.of(
        new ParameterDefinition("A", "Amplitude", 1, 0, 3, 0.1, true),
        new ParameterDefinition("k", "Frequency", 2, 0.5, 6, 0.1, true));

// 2. compile it with values (missing ones take their defaults, all are clamped to their range)
CompiledObject object = DefinitionCompiler.compile(definition, parameters, Map.of("A", 1.5));

// 3. what can it be shown as?
Computations computations = StandardComputations.registry();
List<RepresentationKind> available = computations.applicableTo(definition);   // SURFACE, LEVEL_SET, GRADIENT, …

// 4. compute one
Computation levelSet = computations.forObject(RepresentationKind.LEVEL_SET, definition).orElseThrow();
ComputationSettings settings = ComputationSettings.resolve(levelSet.settings(), Map.of("levels", 20));
long cost = levelSet.estimatedCost(object, settings);          // decide: answer now, or queue a job
Dataset dataset = levelSet.compute(object, settings, ComputationProgress.NONE);

// 5. export, if it is a mesh or a table
DatasetFormats.standard().byName("obj").orElseThrow().write(dataset, writer);
```

Datasets and definitions are Jackson-ready: `ObjectDefinition` carries `kind`, `Dataset` carries `format`, so the
same records travel as JSON between a server and a renderer.

## The 31 kinds

Every formula below is jMF and may use everything the language has — cases, own functions, sums, integrals. The
**variables** column is what each formula is a function of; every other name it mentions is a parameter. ⚠️ Variables
are named by the author: the curve, surface, implicit, parametric, complex, region, sequence and distribution records
carry `variables` (`t` or `θ`, `u, v`, `k`…), and the column gives the conventional names used when none are written.
An interval keeps the name of the picture's axis it spans, never the variable's. jMX documents become these records
through `EngineSelection` — a document says what it is by its structure, never by a family word.

| Kind | Record fields | Variables | Example definition (JSON) |
|---|---|---|---|
| `EXPLICIT_CURVE` | expression, x | x | `{"kind":"EXPLICIT_CURVE","expression":"sin(x)/x","x":{"minimum":-10,"maximum":10}}` |
| `EXPLICIT_SURFACE` | expression, x, y | x, y | `{"kind":"EXPLICIT_SURFACE","expression":"x^2 - y^2","x":{…},"y":{…}}` |
| `IMPLICIT_CURVE` | expression, x, y | x, y | `{"kind":"IMPLICIT_CURVE","expression":"x^3 + y^3 = 3 a x y","x":{…},"y":{…}}` |
| `IMPLICIT_SURFACE` | expression, x, y, z | x, y, z | `"sin(x) cos(y) + sin(y) cos(z) + sin(z) cos(x)"` (gyroid) |
| `IMPLICIT_HYPERSURFACE` | expression, x, y, z, w | x, y, z, w | `"x^2 + y^2 + z^2 + w^2 = 1"` (3-sphere) |
| `PARAMETRIC_CURVE` | components[], t, closed | t | `{"components":["cos(3t)","sin(2t)"],"t":{"minimum":0,"maximum":6.2832},"closed":true}` |
| `PARAMETRIC_SURFACE` | components[], u, v, gluing | u, v | torus: `["(R + r cos(v)) cos(u)","(R + r cos(v)) sin(u)","r sin(v)"]`, gluing `TORUS` |
| `VECTOR_FIELD` | components[], bounds[] | x, y (z) | `["-y","x"]` |
| `DIFFERENTIAL_SYSTEM` | variables[], equations[], time, bounds[], starts[] | the state variables and t | Lorenz: variables `["x","y","z"]`, equations `["sigma (y - x)","x (rho - z) - y","x y - beta z"]`; `white_noise()` makes it stochastic (Euler–Maruyama from `starts`) |
| `DISCRETE_MAP` | variables[], updates[], bounds[], starts[], stepValues[ StepValue(name, expression) ] | the state variables and n | logistic: `["x"]`, `["r x (1 - x)"]`; a step value (`local axis = random_integer(0, 1)`) is drawn once per step and read by every update |
| `COMPLEX_MAPPING` | expression, real, imaginary | z | `"(z - 1) / (z + 1)"`, `"sum(k, 0, 20, z^k / k!)"` |
| `ESCAPE_TIME_FRACTAL` | state, start, iteration, input, bailout, maximumIterations, real, imaginary | the state and the input | Mandelbrot: state `z`, start `0`, iteration `"z^2 + c"`, input `c`; Julia: the input is the state, `c` a parameter — nothing names a fractal |
| `NEWTON_FRACTAL` | state, iteration, maximumIterations, tolerance, real, imaginary | the state | `"p(z) = z^3 - 1"` in front of `"z - p(z) / diff(p(z), z)"` |
| `ITERATED_FUNCTION_SYSTEM` | dimension, maps[ AffineMap(matrix, translation, weight) ] | — | Barnsley fern: four affine maps with weights |
| `LINDENMAYER_SYSTEM` | axiom, rules{}, angle, iterations | — | `{"axiom":"F","rules":{"F":"F+F-F-F+F"},"angle":90,"iterations":4}` |
| `MATRIX` | entries[][] (jMF, so parameters work) | — | `[["cos(a)","-sin(a)"],["sin(a)","cos(a)"]]` |
| `AFFINE_TRANSFORMATION` | matrix[][], translation[] | — | a shear plus a shift |
| `NONLINEAR_TRANSFORMATION` | components[], bounds[] | x, y (z) | `["x + a sin(y)","y + a sin(x)"]` |
| `POINT_SET` | dimension, points[][] | — | read from a CSV by `ObjectFiles.readPointTable` |
| `GEOMETRIC_CONSTRUCTION` | dimension, elements[ GeometricElement(id, type, references, coordinates, label) ] | — | types: POINT MIDPOINT LINE SEGMENT RAY CIRCLE CIRCLE_THROUGH_THREE PLANE SPHERE INTERSECTION PERPENDICULAR PARALLEL TANGENT REFLECTION; coordinates are jMF |
| `POLYTOPE` | dimension, vertexRule, vertices[][], edgeCondition, edges[][] | — | `cartesian(-1, 1, dimension)` + `hamming(first, second) = 1`; rules `axis_points`, `regular_simplex`; measures `dot`, `distance` (`PolytopeSkeleton`) |
| `PROBABILITY_DISTRIBUTION` | variables[], density, support[] | as named | any jMF density, e.g. `exp(-((x - mu) / sigma)^2 / 2) / (sigma sqrt(2 pi))` |
| `REGION` | condition, variables[2], from[2], to[2], integer, exactIntegers{} | as named | Tupper: integer inputs, exact `k`, decided by `ExactRationalEvaluator` |
| `SEQUENCE` | expression, first, last | n | `"sum(k, 1, n, 1/k) - ln(n)"` (→ γ) |
| `VALUE` | name, expression | — | one number of the parameters: `2^3^2`, the Basel sum; shown as a measurement named `name` |
| `CIRCUIT` | elements[ CircuitElement ], time, layout?, conditions? (`BenchConditions`), probes[] | t | a netlist; every value jMF over the parameters — see "jMX: components and circuits" |
| `PROGRAM` | formulas, program, integerInputs{} | — | jMF definitions called from a jMS program that returns a number, a list, points or named columns |
| `COMPONENT` | source | — | a component's jMX text, read when saved; used by circuits by reference (`slug@revision`) |
| `SYMBOL` | drawing | — | a symbol's jMX drawing (`size`, `path`, `pin … at`, `label … at`, `indicator`), under 100 000 characters |
| `GRAPH` | formulas, clock?, layout, panes[], lines[], checks[] | — | the Graph saved: lines on panes moved by one clock; nothing is computed for the graph itself |
| `FAMILY` | index, from, to, member | the member's | one member per whole `index` from `from` to `to` (`y = sin((2k - 1) x) / (2k - 1)  k in 1 .. N integer`) |

Parametric surface gluing (`SurfaceGluing`): `NONE CYLINDER TORUS MOBIUS KLEIN_BOTTLE PROJECTIVE_PLANE SPHERE` — the
mesh then carries `seams` so a topology view can draw and animate how the edges are identified.

## Representations

`accepts` is decided by the computation itself; the third column is what it accepts. Settings are
`name type default[minimum–maximum]`; a `vec` with `[]` default means "derived from the object when not given"
(an initial state, a window, a point). `ComputationSettings.resolve` refuses an unknown name, a wrong type or an
out-of-range value with a sentence (`IllegalArgumentException`, which an API answers as 422).

| Representation | Family | Accepts | Settings |
|---|---|---|---|
| `SURFACE` | GEOMETRY | EXPLICIT_SURFACE, PARAMETRIC_SURFACE | resolution int 96[8–512] |
| `CURVE` | GEOMETRY | EXPLICIT_CURVE, PARAMETRIC_CURVE | samples int 512[16–20000] |
| `IMPLICIT_SURFACE` | GEOMETRY | IMPLICIT_SURFACE, IMPLICIT_HYPERSURFACE (w held at `slice`) | resolution int 48[8–160]; slice num 0.0[-1000000–1000000] |
| `LEVEL_SET` | GEOMETRY | IMPLICIT_CURVE (f = 0), EXPLICIT_SURFACE (evenly spaced levels + heights), IMPLICIT_SURFACE (f = 0 with `axis` held at `offset`) | resolution int 200[16–1024]; levels int 12[1–100]; axis choice Z[X, Y, Z]; offset num 0.0[-1000000–1000000]; window vec [][-1000000000000–1000000000000] |
| `POINTS` | GEOMETRY | POINT_SET | — |
| `CONSTRUCTION` | GEOMETRY | GEOMETRIC_CONSTRUCTION | extent num 10.0[0.1–1000000] |
| `TURTLE_PATH` | GEOMETRY | LINDENMAYER_SYSTEM | maximumSymbols int 400000[1000–2000000] |
| `CROSS_SECTION` | ANALYSIS | EXPLICIT_SURFACE (a vertical cut; a horizontal one is a `LEVEL_SET`) | axis choice Y[X, Y, Z]; offset num 0.0[-1000000–1000000]; resolution int 400[16–4000] |
| `PROJECTION` | ANALYSIS | EXPLICIT_SURFACE, PARAMETRIC_CURVE, PARAMETRIC_SURFACE | projection choice ORTHOGRAPHIC[ORTHOGRAPHIC, PERSPECTIVE, STEREOGRAPHIC, POLAR, CYLINDRICAL, SPHERICAL, MERCATOR]; azimuth num 45.0[-360–360]; elevation num 30.0[-90–90]; distance num 6.0[0.1–1000]; resolution int 64[8–256] |
| `GRADIENT` | ANALYSIS | EXPLICIT_SURFACE | resolution int 16[4–64] |
| `NORMALS` | ANALYSIS | EXPLICIT_SURFACE, PARAMETRIC_SURFACE | resolution int 16[4–64] |
| `CURVATURE` | ANALYSIS | EXPLICIT_CURVE, EXPLICIT_SURFACE, PARAMETRIC_CURVE, PARAMETRIC_SURFACE | measure choice GAUSSIAN[GAUSSIAN, MEAN, PRINCIPAL_MAXIMUM, PRINCIPAL_MINIMUM]; resolution int 96[8–512] |
| `TANGENT_PLANE` | ANALYSIS | EXPLICIT_CURVE, EXPLICIT_SURFACE | at vec [][-1000000–1000000]; size num 1.0[0.01–10000]; secant num 0.5[-10000–10000] |
| `CRITICAL_POINTS` | ANALYSIS | EXPLICIT_CURVE, EXPLICIT_SURFACE | seeds int 12[2–40] |
| `DERIVATIVE` | ANALYSIS | EXPLICIT_CURVE, EXPLICIT_SURFACE, PARAMETRIC_CURVE | order int 1[1–4]; samples int 512[16–20000] |
| `INTEGRAL` | ANALYSIS | EXPLICIT_CURVE, EXPLICIT_SURFACE, PARAMETRIC_CURVE | bounds vec [][-1000000–1000000]; rectangles int 16[1–2000]; rule choice MIDPOINT[LEFT, RIGHT, MIDPOINT, TRAPEZOID]; samples int 400[16–20000] |
| `VECTOR_ARROWS` | FIELDS | DIFFERENTIAL_SYSTEM, VECTOR_FIELD | resolution int 16[4–64] |
| `STREAMLINES` | FIELDS | DIFFERENTIAL_SYSTEM, VECTOR_FIELD | seeds int 8[2–32]; length num 4.0[0.01–1000]; samples int 120[8–2000] |
| `PARTICLES` | FIELDS | DIFFERENTIAL_SYSTEM, VECTOR_FIELD | count int 400[1–20000]; frames int 120[2–2000]; duration num 4.0[0.01–1000]; seed int 7[0–2147483647] |
| `SCALAR_FIELD` | FIELDS | DIFFERENTIAL_SYSTEM, VECTOR_FIELD | measure choice MAGNITUDE[MAGNITUDE, DIVERGENCE, CURL]; resolution int 128[8–1024]; slice num 0.0[-1000000–1000000]; window vec [][-1000000000000–1000000000000] |
| `TRAJECTORY` | DYNAMICS | DIFFERENTIAL_SYSTEM | initial vec [][-1000000000–1000000000]; time vec [][-1000000000–1000000000]; samples int 4000[16–200000] |
| `PHASE_PORTRAIT` | DYNAMICS | DIFFERENTIAL_SYSTEM | starts int 6[2–24]; duration num 10.0[0.01–10000]; samples int 300[16–5000] |
| `EQUILIBRIA` | DYNAMICS | DIFFERENTIAL_SYSTEM, DISCRETE_MAP | seeds int 8[2–30] |
| `ORBIT` | DYNAMICS | DISCRETE_MAP, ESCAPE_TIME_FRACTAL (one `point`'s orbit until it escapes) | initial vec [][-1000000000–1000000000]; iterations int 500[1–1000000]; transient int 0[0–1000000]; point vec [][-1000000000000–1000000000000] |
| `BIFURCATION` | DYNAMICS | DIFFERENTIAL_SYSTEM, DISCRETE_MAP | parameter text ; range vec [][-1000000000–1000000000]; steps int 600[2–20000]; transient int 500[0–100000]; kept int 120[1–5000]; initial vec [][-1000000000–1000000000] |
| `DOMAIN_COLORING` | COMPLEX | COMPLEX_MAPPING | resolution int 256[16–2048] |
| `RIEMANN_SPHERE` | COMPLEX | COMPLEX_MAPPING | resolution int 96[8–512] |
| `ESCAPE_TIME` | FRACTALS | ESCAPE_TIME_FRACTAL | resolution int 400[16–4096]; window vec [][-1000000000000–1000000000000]; iterations int 0[0–100000] |
| `NEWTON_BASINS` | FRACTALS | NEWTON_FRACTAL | resolution int 400[16–4096]; window vec [][-1000000000000–1000000000000] |
| `CHAOS_GAME` | FRACTALS | ITERATED_FUNCTION_SYSTEM | points int 100000[100–5000000]; seed int 11[0–2147483647] |
| `TRANSFORMED_GRID` | LINEAR_ALGEBRA | MATRIX, AFFINE_TRANSFORMATION, NONLINEAR_TRANSFORMATION, COMPLEX_MAPPING (as a map of the plane) | lines int 11[2–64]; extent num 3.0[0.1–10000]; samples int 64[2–1000]; morph num 1.0[0–1] |
| `MATRIX_ANALYSIS` | LINEAR_ALGEBRA | MATRIX, AFFINE_TRANSFORMATION | eigenvectors bool false; from vec [][-1000000000000–1000000000000]; steps int 50[1–100000]; side choice RIGHT[RIGHT, LEFT] |
| `SVD_DECOMPOSITION` | LINEAR_ALGEBRA | MATRIX, AFFINE_TRANSFORMATION | samples int 128[8–2000] |
| `POLYTOPE_PROJECTION` | HIGHER_DIMENSIONS | POLYTOPE | rotations vec [][-360–360]; projection choice PERSPECTIVE[PERSPECTIVE, ORTHOGRAPHIC]; distance num 3.0[1.01–1000] |
| `POLYTOPE_SLICE` | HIGHER_DIMENSIONS | POLYTOPE | rotations vec [][-360–360]; offset num 0.0[-1000000–1000000] |
| `PARAMETER_SWEEP` | PARAMETERS | DIFFERENTIAL_SYSTEM, DISCRETE_MAP, EXPLICIT_CURVE, EXPLICIT_SURFACE | first text ; second text ; steps int 120[2–1000]; behaviour choice MAXIMUM[MAXIMUM, MINIMUM, MEAN, VALUE_AT_POINT, LYAPUNOV, PERIOD, ATTRACTOR_SIZE]; point vec [][-1000000000–1000000000]; iterations int 400[10–100000] |
| `GRADIENT_DESCENT` | OPTIMIZATION | EXPLICIT_CURVE, EXPLICIT_SURFACE | start vec [][-1000000000–1000000000]; method choice GRADIENT[GRADIENT, MOMENTUM, NEWTON]; rate num 0.1[1.0E-9–100]; momentum num 0.9[0–0.999]; iterations int 200[1–100000]; tolerance num 1.0E-8[0–1] |
| `DENSITY` | PROBABILITY | PROBABILITY_DISTRIBUTION | resolution int 200[8–2000] |
| `SAMPLES` | PROBABILITY | PROBABILITY_DISTRIBUTION | count int 10000[1–2000000]; bins int 60[2–1000]; seed int 42[0–2147483647] |
| `RANDOM_WALK` | PROBABILITY | DISCRETE_MAP, DIFFERENTIAL_SYSTEM calling `random_*` / `white_noise()` (`Randomness`) | paths int 20[1–2000]; steps int 1000[2–1000000]; seed int 42[0–2147483647] |
| `SEQUENCE_TERMS` | SEQUENCES | SEQUENCE | — |
| `REGION` | GEOMETRY | REGION | resolution int 400[8–1024] (real inputs only) |
| `VALUE` | VALUES | VALUE | — |
| `TRANSIENT_RESPONSE` | CIRCUITS | CIRCUIT | samples int 1000[50–100000]; parts bool false; levels bool false |
| `FREQUENCY_RESPONSE` | CIRCUITS | CIRCUIT | from num 1.0[0.001–1000000000]; to num 1000000.0[0.001–1000000000]; points int 200[10–5000] |
| `OPERATING_POINT` | CIRCUITS | CIRCUIT | — |
| `DC_SWEEP` | CIRCUITS | CIRCUIT with a source | source text ""; from num 0.0[-1000000–1000000]; to num 5.0[-1000000–1000000]; points int 101[3–5000] |
| `PROGRAM_OUTPUT` | PROGRAMS | PROGRAM | steps int 1000000[1000–50000000]; seed int 42[0–2147483647] |

Every representation also answers a `FAMILY` whose member it accepts — `Computations` wraps it; the table does not
repeat that. ⚠️ Removed on 2026-10-13 (60 → 48), so a note naming them is older: `LOGIC_TIMING` (now
`TRANSIENT_RESPONSE levels true`), `CONTOURS` and `IMPLICIT_CURVE` (now `LEVEL_SET`), `MARKOV_EVOLUTION` (now
`MATRIX_ANALYSIS` with `from` / `steps` / `side`), and the rest of `parallax-engine-primitives-audit.md` batch 1.

What some of them compute, where the name does not say:

- `CURVATURE` colours a surface by Gaussian, mean or a principal curvature (from the exact second fundamental form)
  and a curve by its signed curvature.
- `CRITICAL_POINTS` finds them by Newton from a grid of seeds and classifies each from the Hessian: minimum,
  maximum, saddle, degenerate.
- `INTEGRAL` draws Riemann rectangles by the chosen rule beside the exact (adaptive) value, so the approximation
  error is visible.
- `EQUILIBRIA` classifies each fixed point from the Jacobian's eigenvalues: `STABLE_NODE`, `UNSTABLE_NODE`,
  `SADDLE`, `STABLE_FOCUS`, `UNSTABLE_FOCUS`, `CENTER`, …
- `BIFURCATION` sweeps one parameter and keeps the attractor's points after a transient.
- `PARAMETER_SWEEP` maps one or two parameters to a behaviour: maximum, minimum, mean, value at a point, Lyapunov
  exponent, period, attractor size.
- `GRADIENT_DESCENT` records the path of plain gradient descent, momentum or Newton's method from a start point.
- `MATRIX_ANALYSIS` with `eigenvectors true` adds the eigenvectors as arrows (a real one as λv, a complex pair as the
  two axes of the plane it turns, scaled by |λ|); with `from [..]` it carries that vector `steps` times (`side RIGHT`
  x ← Ax, `side LEFT` x ← xA) and reports the eigenvector of eigenvalue 1, scaled to sum 1, as `stationary` — a
  Markov chain is a matrix whose document asks for `from [1, 0, 0] side LEFT`, the engine knows no chain.
- `TRANSIENT_RESPONSE` answers columns named as the circuit names them, with a unit — `out` [V], `R1` [A],
  `U1.output` [A] (`TimeSeries.units`), never `V(out)`. With `parts true` it adds every part's current; with
  `levels true` it answers the digital nodes' logic levels instead, each column named by its node in unit `level`
  (1, 0, −1 unknown, −2 released) — the interface picks the timing view from that unit.
- `RIEMANN_SPHERE` maps the complex plane onto the sphere and colours it by the mapping.
- `POLYTOPE_PROJECTION` / `POLYTOPE_SLICE` rotate a 4-D (or n-D) polytope in its planes and project or cut it to 3-D.

## Datasets

`Dataset` is sealed; each record has a `format` discriminator and `elementCount()` (what a budget counts).

| `format` | Record | Drawn as |
|---|---|---|
| `MESH` | dimension, positions[], normals[], indices[], scalars[]?, scalarName, minimum[], maximum[], seams[] | triangle mesh, optionally coloured by a scalar |
| `POLYLINES` | dimension, lines[ Polyline(points[], scalars[]?, label, closed) ], minimum[], maximum[] | curves, contours, trajectories, construction lines |
| `SCALAR_GRID` | columns, rows, x, y, values[], minimum, maximum, … | heat map, density, divergence |
| `BINARY_GRID` | columns, rows, values[] (one per pixel, row-major) | a region's pixels, a program's bitmap |
| `VECTOR_SAMPLES` | dimension, origins[], vectors[], magnitudes[], maximumMagnitude | arrows, normals, gradient |
| `POINT_CLOUD` | dimension, points[], values[]?, valueName | samples, chaos game, orbits, particles |
| `FEATURE_POINTS` | features[ Feature(position[], value?, classification, facts{}) ] | critical points, equilibria, intersections |
| `COMPLEX_GRID` | columns, rows, real, imaginary, modulus[], phase[] | domain colouring |
| `ITERATION_GRID` | columns, rows, real, imaginary, values[], basins[], … | escape time, Newton basins |
| `TIME_SERIES` | axis, axisValues[], columns[], values[][], units[] | plots over time or index; a circuit's columns |
| `MATRIX_ANALYSIS` | rows, columns, matrix, determinant, rank, trace, eigenpairs[], … | the numbers of a matrix |
| `SYMBOLIC` | text{}, latex{} | derivatives and closed forms, for KaTeX |
| `MEASUREMENTS` | values{}, notes{}, units{} | integrals, estimates, a value, an operating point |
| `COMPOSITE` | parts{ name → Dataset } | several of the above together (a surface with its tangent plane) |

Arrays are `float[]` (and `int[]` for indices) laid out flat — `positions` is `x0 y0 z0 x1 y1 z1 …` — which is what
a WebGL buffer takes without copying. NaN samples are left out of meshes and polylines (a polyline breaks there).

## Files

| Format | `name` | Carries | Use |
|---|---|---|---|
| Wavefront OBJ | `obj` | meshes (with normals), polylines as `l` | Blender, any 3-D tool |
| ASCII STL | `stl` | meshes, one normal per facet | 3-D printing |
| ASCII PLY | `ply` | meshes with the per-vertex scalar as a property | MeshLab, ParaView |
| CSV | `csv` | time series, point clouds, scalar grids, feature points | spreadsheets, notebooks |

`DatasetFormats.applicableTo(dataset)` lists the formats a dataset can be written in, parts of a composite
included. Reading: `ObjectFiles.readFormulaFile(content)` reads a `.jmf` file — one formula per line, optional
`name:` labels — and infers each object from its variables (`z` alone or `i` → complex mapping, `w` →
hypersurface, `z` → implicit surface, `y` → explicit surface, `x` → explicit curve, `n` → sequence); every other
name becomes a parameter with range −10…10. Lines that do not read come back as `Refusal(line, text, reason)`, not
as an exception. `ObjectFiles.readPointTable(content)` reads a CSV of numbers into a `POINT_SET`, skipping a header.

## Verification

`StandardChecks.all()` cross-checks the model against itself, independently of any renderer:

| Code | Checks |
|---|---|
| `PARAMETER_RANGE` | the object evaluates to finite numbers at both ends of every parameter's range |
| `DERIVATIVE_AGREEMENT` | exact derivatives agree with central differences |
| `SECTION_AGREEMENT` | a vertical section's profile lies on its surface |
| `CRITICAL_GRADIENT` | the gradient vanishes at the critical points that were found |
| `TRAJECTORY_REVERSIBILITY` | integrating forwards then backwards returns to the start |
| `EIGEN_RESIDUAL` | real eigenpairs satisfy A v = λ v |
| `MESH_FINITENESS` | a surface's mesh is finite almost everywhere |
| `COMPONENT_DATASHEET` | (`circuit.ComponentCheck`, not in `all()`) every `expect` of a component, built on its bench and measured |

Each returns `CheckResult(outcome PASSED|WARNED|FAILED|SKIPPED, measured, tolerance, detail)`. `Evaluations` runs them
over the whole catalogue (`/administration/evaluation/runs`) and records them in `evaluation_runs` /
`evaluation_results`.

## Numerics

| Tool | Method |
|---|---|
| `OdeIntegrator` | Dormand–Prince 5(4) with adaptive steps; stops and marks `escaped` when the state leaves its bounds or blows up |
| `MarchingSquares` | contours of a scalar grid, NaN cells skipped |
| `MarchingTetrahedra` | implicit surfaces, no ambiguous cases |
| `GridMesh` | parametric grids with vertex normals; seams for glued domains |
| `LinearAlgebra.svd` | one-sided Jacobi |
| `LinearAlgebra.eigenpairs` | characteristic polynomial by Faddeev–LeVerrier, roots by Durand–Kerner, vectors from the null space |
| `NewtonSolver` | damped Newton with a numeric Jacobian; `rootsInBox` seeds a grid and deduplicates |
| integrals inside formulas | `org.jmouse.formula.Quadrature` (see [JMOUSE_FORMULA.md](JMOUSE_FORMULA.md)) |

## Simulations: what fits and how

The module is a computation library, not a simulator, but anything whose behaviour is an ODE system, a discrete
map or a function of time fits the existing kinds — which means every representation of those kinds (trajectory,
phase portrait, time series, equilibria with their stability, bifurcation diagrams, parameter sweeps) works on it
with no new code. An RLC circuit's transient, for instance, is a `DIFFERENTIAL_SYSTEM`:

```json
{"kind":"DIFFERENTIAL_SYSTEM",
 "variables":["q","current"],
 "equations":["current", "(if(t > 0.001, 5, 0) - R current - q / C) / L"],
 "time":{"minimum":0,"maximum":0.05},
 "bounds":[{"minimum":-1,"maximum":1},{"minimum":-10,"maximum":10}]}
```

with the source voltage written in jMF — here a 5 V step at 1 ms; a sine `5 sin(2 pi f t)` or a square wave
`5 sign(sin(2 pi f t))` works the same — and R, L, C, f as parameters. (`i` is the imaginary unit, so a current
needs another name, and the equations of a system are separate formulas, so a helper such as `V(t)` is written
in each one that uses it.) A diode or a transistor is a nonlinear term
(`Is (exp(v / Vt) - 1)`). That is the hand-written route. A circuit drawn as components and nodes is a `circuit`
document instead, solved by the netlist engine in `org.jmouse.parallax.circuit` from each component's own
equations — see "jMX: components and circuits" below.

## jMX: components and circuits

jMX is the Parallax script language, built on jMS, and the one format of every stored object (`.jmx`; the stored
text is the truth, everything else is derived from it); its reader is `org.jmouse.parallax.script` and its syntax
highlighting is `jmouse-parallax/tools/jmx-textmate`. This section covers the two electronics kinds. A block a kind
does not hold is refused with its line (`Kind.blocks()`): a `component` may hold `meta parameters formulas pins nodes
state equations behaviour symbol parts nets view expect display measure leads`; a `circuit` `meta parameters formulas
sheet parts nets layout ports probes analysis symbol view expect`.

### A component

```jmx
component "Ammeter" as ammeter type meter {
    meta {
        designator A
        summary "Reads the current through it, put in the break of a wire"
        category circuits
    }
    parameters {
        R_shunt = 50 mOhm "the shunt it reads across: what it drops, on a realistic bench"
    }
    pins {
        in analog 1 "where the current enters"
        out analog 2 "where it leaves"
    }
    equations {
        # Ideal it is a short: the current passes and nothing drops. On a realistic bench it is its shunt.
        voltage in -> out = realistic * R_shunt * in.current
    }
    leads {
        in green stripes white
        out blue stripes white
    }
    measure {
        reading = in.current unit A signal
    }
    symbol uses ammeter-symbol {
        a in
        b out
    }
    expect "drops across its shunt on a realistic bench" from "50 mOhm in series with 10 Ohm from 10 V" {
        bench for 1 ms realistic {
            V1 voltage-source (top, 0) U_output = 10 V
            U1 self (in = top, out = load)
            R1 resistor (load, 0) resistance = 10 Ohm
        }
        measure final load
        near 9.9502 V +- 0.01%
    }
}
```

**`meta`** — `summary`, `source`, `category`, plus three fields the engine reads:

| Field | Means |
|---|---|
| `designator A` | what a new part of it is called on a sheet before its number (`A1`, `A2`); one to three capital letters, anything else is refused |
| `failure open` \| `failure short` | how it fails when it overheats past `T_maximum` on a realistic bench (open when not written) |
| `package "DO-35"` | the package, as text |

**`parameters`** — `name = value [unit] "description"`, each converted to SI (`50 mOhm`, `100 nF`, `200 ppm/K`,
`350 K/W`); a value may be an expression over the parameters above it. Two names are the bench's and are refused as
parameters: `temperature` and `realistic`. The thermal model is read from parameters too: `power_rating`,
`thermal_resistance`, `thermal_time_constant`, `T_maximum = 200 °C`. A resistor's or capacitor's `tolerance` and
`temperature_coefficient` are applied by the bench.

**`pins`** — `name nature [output] number… "description"`, nature `analog | digital | power | ground`; one number
for all units, or one per unit declared by `units A, B`. **`nodes`** — internal nodes, `j "the junction"`.

**`equations`** — each line contributes one branch:

| Line | Means |
|---|---|
| `current a -> b = …` | the current from pin/node `a` to `b` |
| `voltage a -> b = …` | `a − b` held at the value; the branch carries a current of its own (an unknown) |
| `d(x)/dt = …` | a state's rate |
| `local name = …` | a named sub-expression the lines below may use |

- **A pin's or node's name is its voltage**: `(anode - j) / R_series`. A parameter, local or state may not take a pin's
  name.
- **`d(q)/dt` is the rate of a charge** and may stand inside a current: `I_diode + d(C_junction * V1)/dt`. Not inside a
  `local`, and a charge may not hold another `d(…)/dt`.
- **`pin.current` is the current into a pin**: the branch current of every `voltage` equation on that pin, plus the
  right side of every `current` equation on it, written out (`current a -> b` enters at `a`). A `current` equation
  inlined so may not itself read `current(…)` — that is refused with both lines named. So the ammeter's
  `voltage in -> out = realistic * R_shunt * in.current` is an exact short on an ideal bench and its shunt on a
  realistic one.
- **`temperature`** is the bench's, in kelvin (25 °C unless the bench says otherwise). Write a drift against
  `T_nominal`, the parameter naming the temperature the datasheet figures were measured at:
  `local warmth = temperature / T_nominal`.
- **`realistic`** is 1 on a realistic bench and 0 on an ideal one: how much an instrument loads what it reads is written
  with it — `current plus -> minus = realistic * (plus - minus) / R_input`.
- `time` is the simulated time. The SPICE-style `V(a, b)`, `I(…)` and `ddt(q)` are refused, each with what to write
  instead.

A component whose `type` is `resistor capacitor inductor diode voltage-source current-source` and that has no
`equations`, `behaviour` or `parts` is a primitive: the solver supplies its device, and the document carries its
parameters (the first is its value), pins and drawing. A modelled part keeps its role in `type` without becoming one —
the 1N4148 is `type diode` and is solved from its own equations.

**`state` and `behaviour`** — discrete states and jMS events over them; the frequency meter counts edges itself:

```jmx
state {
    last_edge = -1 discrete "when the input last rose through the level; -1 before the first"
    frequency = 0 discrete "one over the time between the last two rising edges"
}
behaviour {
    on above 'input - reference - threshold' do
        if last_edge >= 0 then
            frequency = 1 / (time - last_edge)
        end
        last_edge = time
    end
}
```

**`parts`** — a component may be a subcircuit: its internal parts in the same netlist notation as a circuit (below),
wired to its pins and nodes. It is flattened for the solver (`ComponentHierarchy.expand`), and a part made of several
parts still reports by its own pins: the current into each pin is the sum of what enters its inner parts' terminals on
that pin's net (`ComponentHierarchy.Assembly.pinCurrents`), so `pin.current`, readings and the drawn currents see it
as one part.

**`measure`** — makes the component an instrument. `name = expression unit U [signal]`; the expression reads what an
equation reads — pin voltages, `pin.current`, states, parameters — plus `power` and `time`. `signal` marks a signal
the bench can show as DC, AC or RMS; without it the figure is shown as it is (a frequency, a duty cycle). The engine
knows no instrument; each is its document:

```jmx
measure { reading = plus - minus unit V signal }               # voltmeter
measure { reading = (plus - minus) * in.current unit W signal } # wattmeter
measure { reading = frequency unit Hz }                         # frequency meter: a state its behaviour keeps
```

**`display`** — what the part shows on its symbol while it runs: `name = expression [unit U]`, an expression over its
pin voltages, its parameters and `power` (`PartDisplays`), the name matching an
`indicator` of its symbol drawing (`indicator level from -4, -14 to 4, 14` in a `symbol` document). With a `unit` the
figure is also written beside the part.

```jmx
display {
    level = terminal_one - terminal_two
    charge = 100 * (terminal_one - terminal_two) / voltage_rating unit %
}
```

**`leads`** — an instrument's lead colours: `pin colour [stripes colour]`, colours `red black white green blue yellow
grey`; a pin the component does not have is refused.

**`symbol`** — `symbol uses <slug>[, <slug>…] { port pin … }` draws it with `symbol` documents (the first is the
default); each line maps the drawing's port to a pin, and a port left out is the pin of the same name. The other forms
are a shape drawn from its sides (`symbol box { left d, clk edge;  right q }`) and a drawing of its own.

**`expect`** — a datasheet claim, checked by `ComponentCheck`:

```jmx
expect "forward voltage falls as it warms" from "VF temperature coefficient, about -2 mV/K" {
    bench for 1 ms at 85 °C {
        X1 forward current = 10 mA
    }
    measure operating anode
    within 0.54 V .. 0.64 V
}
```

- `bench for <time> [at <temperature>] [realistic] { parts }` — how long it runs, the temperature (°C, given to the
  equations in kelvin) and whether instruments load what they read. The parts are netlist lines; `self` is the
  component under test (`U1 self (in = top, out = load)`, settings override its parameters). `X1 forward current = …`
  and `X1 reverse voltage = …` are whole two-terminal benches on their own.
- `measure` — `operating`, `final`, `maximum`, `minimum`, `average`, `frequency … above|below <level>`, `duty … above|below
  <level>`, each optionally `after <time>`, of a signal: a node (`load`), a difference (`out - ret`), a part's current
  (`current V1`), a digital level (`level clock`); `final levels a, b` for digital nodes.
- the criterion — `near 6.86 kHz +- 5%`, `within 26 V .. 30 V` (either end may be left open), `is 1`.

### A circuit

```jmx
circuit "Bridge rectifier supply" as bridge-supply {
    parameters {
        Vm = 12 V in 2 .. 24 step 0.5 "Peak input voltage"
        C = 470 µF in 10 .. 2200 step 10 "Reservoir capacitance"
        R = 100 Ω in 10 .. 1000 step 10 "Load resistance"
    }
    parts {
        AC1 sine-generator (positive = a, negative = b) amplitude = Vm, frequency = 50
        BR1 bridge-rectifier (ac1 = a, ac2 = b, plus = p, minus = 0)
        C1 capacitor (p, 0) capacitance = C
        A1 ammeter (in = p, out = rl)
        RL resistor (rl, 0) resistance = R
        VM1 voltmeter (plus = rl, minus = 0)
    }
    layout {
        AC1 at 300, 300 rotate 90
        A1 at 760, 240 rotate 90
        Jp junction at 640, 200
        Jm junction at 640, 400
        GND1 ground at 640, 460
        W1 wire AC1.positive to BR1.ac1 via 300 220, 400 220, 400 300
        W5 wire Jp.j to A1.in
        W12 wire Jm.j to GND1.ground
    }
    probes {
        CH1 p 5 V/div
        CH2 a against b 10 V/div
    }
    analysis transient for 0.1 s
    view {
        transient_response primary
        operating_point
    }
}
```

**`parts`** — one notation for a circuit's parts, a component's internal parts and a bench:
`ID reference (connections) [settings] [symbol <slug>]`.

- A primitive (`resistor capacitor inductor diode voltage-source current-source`) is connected in pin order `(a, b)`;
  any other component by pin name `(pin = net, …)`, never both. A unit's pin is named with its unit:
  `U1 lm358@3 (A.output = out, A.inverting_input = out, Vcc = vcc, GND = 0)`; `@3` pins the component's revision.
- Settings are `name = value`, commas optional, or bare words (`closed`). A value may be written with its unit —
  `resistance = 100 kΩ`, `capacitance = 4.7 nF` — and is converted to SI when the circuit is built; the document keeps
  it as written. An expression over the parameters is kept as an expression.
- A source that alternates is a generator component (`sine-generator`, `square-generator`), not a `voltage-source`
  with a waveform.
- `symbol resistor-iec` draws the part with another of its component's symbols. `place …` on a part line is refused:
  positions live in `layout`.
- A meter is a part: an ammeter in the break of a wire, a voltmeter across what it reads, a wattmeter both.

**`layout`** — where things are drawn; it changes nothing that is computed. Each id once:

| Line | Means |
|---|---|
| `ID at x, y [rotate d] [mirror] [name at dx, dy] [value at dx, dy] [readings at dx, dy] [shows dc\|ac\|rms figure\|graph]` | a part; text offsets are from its centre; `shows` sets an instrument's display, each word once |
| `ID ground at x, y` | a ground mark — node 0, terminal `ID.ground` |
| `ID ground agnd at x, y` | another ground: the net it names, kept apart from node 0 |
| `ID common at x, y` | the 0 V of a split supply — node 0 drawn as a common mark, terminal `ID.common` |
| `ID junction at x, y` | a junction dot, terminal `ID.j` |
| `ID wire A.pin to B.pin [via x y, x y …]` | a wire from terminal to terminal through its corners |

A terminal is `PART.pin` — a model's pin by name, `positive`/`negative` on a two-terminal primitive.

**`probes`** — the oscilloscope's channels `CH1`…`CH4` and logic levels: `CH1 node [against ref] [scale]`
(`CH2 a against b 10 V/div`), `L1 level clock`. A meter written as a probe (`A1 current R1`, `M1 power RL`,
`M1 voltage p`) is refused with its part line; a display mode on a channel is refused too — a meter's display is
`shows` in `layout`.

**`analysis`** — `analysis transient for 0.1 s` (or `from 1 ms to 10 ms`) sets the span a circuit is simulated over
and is required; the reader also takes `ac`, `operating` and `dc_sweep` with `from to points source step`.

**`view`** — the representations offered, one per line, `primary` marking the first shown: `transient_response`,
`operating_point`, `frequency_response`, `dc_sweep` — and `transient_response levels true` for the digital nodes' logic levels.

`nets` (`name [ground] [route x,y …]`), `ports` (`input|output|power|ground|analog net`) and `sheet 1200 x 760 grid 20`
are also read.

## Hosting it — the SPI

The library keeps no accounts, no owners table, no policy and no seeds. It asks the host, through these interfaces
in `jmouse-parallax`, and **refuses to start** without the first three (`jmouse-parallax needs a ParallaxAuthority
bean: …`) — a default that allowed everything would make every refusal vanish.

| Interface | Required | The host answers | Refuses with |
|---|---|---|---|
| `authority.ParallaxAuthority` | yes | `actor()` · `requireActor()` · `permits(capability, place)` · `require(capability, place)` · `permitsGlobally` / `requireGlobally` (at `ParallaxPlace.installation()`) | `Unauthenticated` nobody signed in · `Refused` may not · `NotFound` the place is hidden from them · `Conflict` the place no longer allows it · `Invalid` a question it cannot decide |
| `authority.ParallaxPlaces` | yes | `requireOwner(owner)` → the owner's whole chain, outermost first, ending with the owner (`WORKSPACE:w`, `PROJECT:p`) · `requireAcceptsNew(owner)` (asked after access) · `recordChange(owner, when)` | `NotFound` no such owner · `Conflict` takes nothing new (archived) · `Unauthenticated` / `Refused` when it checks access on the way |
| `limits.ParallaxLimits` | yes | `inlineComputationBudget` · `maximumJobCost` · `maximumDatasetBytes` · `jobAttempts` · `datasetRetention` · `anonymousComputationAllowed` · `publicQueueLimit` (0 = off) · `catalogueWarmupEnabled` · `maximumSceneDocumentBytes` · `maximumSceneObjects` | — (asked at the moment of use; never cache it) |
| `live.LiveAuthenticator` | with `-live` (boot fails without) | `authenticate(token)` → `LivePrincipal` (`actorId()`, `runAs(work)`); empty for unknown, expired, disabled | an empty answer closes the socket |
| `catalogue.ShippedDocumentHandler` | no (0…n) | `handles(Kind)` · `apply(documents)` → `Applied(written, summary)`; adds what is missing, never rewrites | a shipped document no handler takes fails the import |
| `catalogue.HomeCategoryPolicy` | no (default `StandardHomeCategoryPolicy`) | `homeOf(ObjectKind)` → section code; `categoryOf` falls back to `GENERAL` — ⚠️ declared as a bean, but no service calls it yet: an object with no category is filed under `GENERAL` | — |
| `live.LiveActivity` | no (the live module implements it) | `running()` — circuits stepping now; 0 without the module | — |

⚠️ **The refusal contract: every refusal inside Parallax is a `ParallaxProblem`, the host's included.** A host
whose own exceptions differ translates them at this boundary through the public constructors, keeping the message and
the code — so a service catches one type only, and the web layer renders one type only.

`ParallaxCapability` (17) — what the library asks; the host decides which of its permissions each means and where:
`CATALOGUE_WRITE` `CATALOGUE_PUBLISH` `OWNER_READ` `SCENE_READ` `SCENE_WRITE` `SCENE_DELETE` `SCENE_SHARE` `COMPUTE`
`JOB_READ` `JOB_CANCEL` `JOB_ADMINISTER` `COLLECTION_CREATE` `COLLECTION_CURATE` `EVALUATION_READ` `EVALUATION_RUN`
`MANAGEMENT_READ` `MANAGEMENT_WRITE`.

**Owners.** `ParallaxOwner(type, id)` — the host's word (`WORKSPACE`, `PROJECT`, `ACCOUNT`) and an id; the library
stores the pair (`owner_type` / `owner_id`, plus a `realm` on scenes, comparisons, jobs, collections) and never joins
to a host table. A null owner on an object is the shared catalogue. `SCENE` is the one type the library defines.

## Problems

`ParallaxProblemDetails` (management module, `@Order(LOWEST_PRECEDENCE - 100)`) answers RFC 9457 with `detail`, the
status the problem names, **`code`**, and every entry of `details()` as a property. A product keeps no handler of its
own for `ParallaxProblem`.

| `code` | Status | Raised by | Carries |
|---|---|---|---|
| `not-found` | 404 | `ParallaxProblem.notFound(what)` | — |
| `conflict` | 409 | `conflict(message)` | — |
| `stale-revision` | 409 | `staleRevision(what)` — a save whose base is not the head | — |
| `invalid` | 422 | `invalid(message)` / `invalid(code, message)` | — |
| `invalid-document` | 422 | `invalidDocument(message, line, column)` — jMX that does not read or compile | `line`, `column` (0 = line only) |
| `refused` | 403 | `refused(message)` | — |
| `unauthenticated` | 401 | `Unauthenticated` | — |
| `limit-exceeded` | 422 | `limit(message)` | — |
| `busy` | 503 | `busy(message)` — a shared capacity is taken; retry | — |
| `invalid-formula` | 422 | a `FormulaException` | `position` |
| `program-budget-exceeded` | 422 | a jMS `ScriptBudgetExceededException` | — |

## Events

Published through Spring's `ApplicationEventPublisher` (records in `org.jmouse.parallax.event`); a host journals them.

| Event | Fields | Published by |
|---|---|---|
| `ObjectCreated` | objectId, slug, actorId | `Catalogue` on a create |
| `ObjectStatusChanged` | objectId, slug, status, actorId | `Catalogue.changeStatus` |
| `EvaluationCompleted` | runId, actorId, passed, warned, failed | `Evaluations` |
| `JobFailed` | jobId, failureType | `ComputationWorker` |
| `CatalogueWarmed` | computed, queued, cached, failed | `CatalogueWarmup` |

## Properties — `jmouse.parallax.*`

| Property | Default | Means |
|---|---|---|
| `jmouse.parallax.migrations.enabled` | `true` | the library's own Flyway |
| `jmouse.parallax.computation.worker-threads` | 0 | queued jobs this process runs at once; 0 = an API that never computes in the background |
| `jmouse.parallax.computation.poll-interval` | `PT1S` | how often the worker looks |
| `jmouse.parallax.computation.lease` | `PT2M` (≥ 10 s) | how long a claim holds without a heartbeat |
| `jmouse.parallax.catalogue.locations` | empty | resource patterns of the shipped `.jmx` (objects, sections, collections, instruments, and what a handler takes) |
| `jmouse.parallax.catalogue.shared-document-locations` | empty | logic families and function libraries components share |
| `jmouse.parallax.management.prefix` | `${jmouse.management.prefix:/jmouse}/parallax/api` | where the REST routes answer |
| `jmouse.parallax.live.path` | `${jmouse.management.prefix:/jmouse}/parallax/live` | where the socket answers |

Budgets are not properties: they are `ParallaxLimits`, so a host can change them at run time. The worker's poll and
maintenance (every 30 s) and the dataset forgetting (04:41 daily) run on the library's own scheduler — the host needs
no `@EnableScheduling`. The warm-up starts on `ApplicationReadyEvent` when `catalogueWarmupEnabled()`.
`CatalogueImport.apply()` is the host's to call (Parallax: a start-up step re-run when `checksum()` moves).

## Routes — `/jmouse/parallax/api`

`OWNER` = `/owners/{ownerType}/{ownerId}` (what an owner holds), `REALM` = `/realms/{realmType}/{realmId}` (what is
merely seen there). **Gate** is the capability `ParallaxAccessRules` requires; *service* means `@PublicEndpoint` and
the service decides per row (published to anybody, drafts to editors, owned rows to the owner's readers).

| Method · path | Gate |
|---|---|
| `GET /catalogue/categories` | service (public) |
| `PUT` · `DELETE /catalogue/categories/{code}` | `CATALOGUE_WRITE` |
| `GET /catalogue/objects` (`text`, …) · `POST /catalogue/objects/summaries` | service |
| `GET /catalogue/objects/{idOrSlug}` · `…/revisions` · `…/revisions/{n}` | service (`ObjectAccess`) |
| `POST /catalogue/objects` | `CATALOGUE_WRITE` |
| `PUT /catalogue/objects/{id}?version=` · `DELETE …` · `POST …/shipped` | service (`ObjectAccess.requireWritable`) |
| `PUT /catalogue/objects/{id}/status` | `CATALOGUE_PUBLISH` |
| `GET` · `POST OWNER/objects` | `SCENE_READ` · `SCENE_WRITE` about the owner |
| `GET /component-models` · `/component-models/{reference}` · `/symbols` | service |
| `GET /instruments` · `/instruments/{code}` · `PUT /instruments/{code}` | public · `CATALOGUE_WRITE` |
| `GET /representations` | public |
| `POST /computations` · `/computations/frames` · `/computations/estimate` | service (`Computing.authorize`) |
| `GET /jobs/{id}` · `…/dataset` · `POST …/cancel` · `…/retry` | service (`Jobs`) |
| `GET OWNER/jobs` | `JOB_READ` about the owner |
| `GET` · `POST OWNER/scenes` · `GET REALM/scenes/recent` | `SCENE_READ` · `SCENE_WRITE` · service (filtered) |
| `GET /scenes/{id}` · `…/revisions` · `…/revisions/{n}` · `…/snapshots` | `SCENE_READ` about the scene |
| `PUT /scenes/{id}/document` · `PUT /scenes/{id}?version=` · `POST …/revisions/{n}/restore` · `POST` · `DELETE …/snapshots[/{s}]` | `SCENE_WRITE` about the scene |
| `POST /scenes/{id}/fork` | `SCENE_READ` on the source; writing at the target is the service's |
| `PUT /scenes/{id}/archived` · `DELETE /scenes/{id}` | `SCENE_DELETE` |
| `GET` · `POST OWNER/comparisons` · `GET` · `PUT` · `DELETE /comparisons/{id}` | `SCENE_READ` · `SCENE_WRITE` · read · write · `SCENE_DELETE` |
| `GET /collections` (`purpose`) · `/me/collections` · `/collections/{id}` | service |
| `POST /collections` | `COLLECTION_CREATE` |
| `PUT` · `DELETE /collections/{id}` · `POST …/items` · `DELETE …/items/{item}` · `PUT …/order` | service (owner or curator) |
| `GET /jmx/schema` · `POST /jmx/form` · `/jmx/edit` · `/jmx/schematic` · `/jmx/drawing` | public — texts the caller sent |
| `GET /formula/reference` · `POST /formula/interpret` · `/formula/render` | public |
| `POST /subjects/resolve` (`catalogue:<slug>` · `inline:<jMF>` · `document:<jMX>`) | service |
| `GET /administration/jobs` | `JOB_ADMINISTER` |
| `GET /administration/computation` · `…/activity` · `…/warmup` · `/statistics` | `MANAGEMENT_READ` |
| `POST /administration/computation/warmup` | `MANAGEMENT_WRITE` |
| `GET /administration/evaluation/checks` · `…/runs` · `…/runs/{id}` · `POST …/runs` | `EVALUATION_READ` · `EVALUATION_RUN` |

## Access — `ParallaxAccessRules`

Capabilities in, `ExternalAccessRules` out, by controller class and method — never by path, so moving the prefix
cannot un-gate a route. `build()` **fails the boot** naming each capability left unmapped and each handler with
neither a rule nor `@PublicEndpoint`. Twelve must be mapped: `CATALOGUE_WRITE` `CATALOGUE_PUBLISH` `SCENE_READ`
`SCENE_WRITE` `SCENE_DELETE` `JOB_READ` `JOB_ADMINISTER` `COLLECTION_CREATE` `EVALUATION_READ` `EVALUATION_RUN`
`MANAGEMENT_READ` `MANAGEMENT_WRITE` (the other five are asked through `ParallaxAuthority` only).

```java
@Configuration(proxyBeanMethods = false)
@Import({ParallaxManagementConfiguration.class, ParallaxLiveConfiguration.class})
class ParallaxAccessConfiguration {

    @Bean
    ExternalAccessRules parallaxAccessRules() {
        return ParallaxAccessRules.builder()
                .capability(ParallaxCapability.CATALOGUE_WRITE,   "catalogue:write",   "GLOBAL")
                .capability(ParallaxCapability.CATALOGUE_PUBLISH, "catalogue:publish", "GLOBAL")
                .capability(ParallaxCapability.SCENE_READ,        "scene:read",        "WORKSPACE")
                .capability(ParallaxCapability.SCENE_WRITE,       "scene:write",       "WORKSPACE")
                .capability(ParallaxCapability.SCENE_DELETE,      "scene:delete",      "WORKSPACE")
                .capability(ParallaxCapability.JOB_READ,          "job:read",          "WORKSPACE")
                .capability(ParallaxCapability.JOB_ADMINISTER,    "job:administer",    "GLOBAL")
                .capability(ParallaxCapability.COLLECTION_CREATE, "collection:create", "GLOBAL")
                .capability(ParallaxCapability.EVALUATION_READ,   "evaluation:read",   "GLOBAL")
                .capability(ParallaxCapability.EVALUATION_RUN,    "evaluation:run",    "GLOBAL")
                .capability(ParallaxCapability.MANAGEMENT_READ,   "system:read",       "GLOBAL")
                .capability(ParallaxCapability.MANAGEMENT_WRITE,  "system:write",      "GLOBAL")
                .build();
    }

    // ⚠️ and one AccessTargetResolver per row type the rules name — without them the engine has a requirement and no target:
    //   ParallaxOwner (argument "owner", as "TYPE:id" — @OwnerAddress joins the two path segments),
    //   Scene ("sceneId"), Comparison ("comparisonId") → the host's place (workspace, project in it)
}
```

## Migrations

| | |
|---|---|
| Location | `classpath:db/parallax/{mysql,postgresql}` (`ParallaxMigrations.locationFor(dataSource)`), `V000001`…`V000004` — catalogue, scenes, collections, computation |
| History | its own table, `parallax_schema_history`; baseline version **0** |
| Order | bean `parallaxFlywayMigrator` runs first; the auto-configuration makes the product's `flywayInitializer` depend on it by name — product tables may reference library tables |
| Tables | `object_categories` `objects` `object_sources` `object_relations` `object_tags` `instruments` `instrument_object_kinds` `scenes` `scene_revisions` `scene_object_references` `scene_snapshots` `comparisons` `comparison_slots` `collections` `collection_items` `computed_datasets` `computation_jobs` `worker_heartbeats` `evaluation_runs` `evaluation_results` — no `parallax_` prefix, the names they had in Parallax |
| Seeds | none — the shipped catalogue is the host's `.jmx`, applied by `CatalogueImport` |

⚠️ A product with `baseline-on-migrate` must set `spring.flyway.baseline-version: 0`: the library makes the schema
non-empty first, and the default `1` silently skips the product's own first migration. ⚠️ These migrations are
append-only from first release. ⚠️ The entities sit outside the product's package — widen the scan once:
`@EntityScan({"org.example.product", "org.jmouse.parallax.jpa"})`.

## The live socket — `/jmouse/parallax/live`

JSR-356 on the servlet container (no `spring-websocket`), registered after the servlet context is up. Subprotocols:
`parallax.live` and `bearer.<token>` — a browser sets no header on a handshake. `LiveAuthenticator` is asked once at
the handshake; every message then runs as that `LivePrincipal`, decided by `ParallaxAuthority` as a request would be.
One socket = one `LiveRun`.

| Direction | Messages |
|---|---|
| in (JSON text, `type`) | `start` · `pause` · `resume` · `step` · `reset` · `probes` · `pace` (`speed`, a share of real time) · `sample` (`interval`) · `set` (a live part or a parameter, from now) · `restart` (an edited text, at once, with `probes`) |
| out (JSON text) | `started` · `changed` (`continued`, `controls`, `time`) · `problem` (`message`) |
| out (binary) | `frame` — `FrameEncoding`: int32 header length, JSON header (`channels`, `channelUnits`, `levels`, `load`, `parts`, `trace`, `traceUnits`), padding to 8, float64 first time and step, int32 count, float32 readings per channel, then the traced moments (float64 times, float32 columns) |

## Wiring checklist for a host

1. Depend on `jmouse-parallax-spring-boot` (pulls the engine and `-jpa`), plus `jmouse-parallax-management` and, for
   live circuits, `jmouse-parallax-live`.
2. `@EntityScan` includes `org.jmouse.parallax.jpa`; `spring.flyway.baseline-version: 0` if `baseline-on-migrate`.
3. Beans: `ParallaxAuthority`, `ParallaxPlaces`, `ParallaxLimits`; `LiveAuthenticator` with the live module;
   `ShippedDocumentHandler`s for document kinds it keeps itself; `HomeCategoryPolicy` if its sections differ.
4. `@Import(ParallaxManagementConfiguration.class[, ParallaxLiveConfiguration.class])` beside an
   `ExternalAccessRules` from `ParallaxAccessRules`, and `AccessTargetResolver`s for `ParallaxOwner`, `Scene`,
   `Comparison`.
5. Security chain lets `/jmouse/parallax/**` through (the routes decide); the socket's handshake authenticates by the
   `bearer.<token>` subprotocol.
6. `jmouse.parallax.catalogue.locations` (and `shared-document-locations`) at its `.jmx`; call `CatalogueImport.apply()`
   at start-up; `jmouse.parallax.computation.worker-threads` > 0 on a process that should compute.
7. Dev-server proxy forwards `/jmouse` with WebSockets on; the interface uses the same prefix and path
   (`PARALLAX_API_PREFIX`, `PARALLAX_LIVE_PATH` in `@jmouse/parallax`) — nothing checks that they agree.
8. Delete any product `@ExceptionHandler(ParallaxProblem.class)`: `ParallaxProblemDetails` is the one advice.

## Gotchas

- ⚠️ **`BoundFunction` is not thread-safe** — it reuses one argument buffer. Compile one per thread (compiling is
  cheap) or use the jMF `RealFunction` directly.
- ⚠️ **Parameter values are clamped, not refused**: `withParameters(Map.of("A", 99))` gives the parameter's maximum.
  An unknown parameter name *is* refused.
- ⚠️ **NaN means "no value here", everywhere.** A formula undefined outside its domain draws nothing there; it is
  not an error.
- ⚠️ **`estimatedCost` is an estimate in function evaluations**, meant for "answer now or queue". It does not
  bound the time of a formula with large sums or integrals inside — those multiply the real cost.
- ⚠️ **A representation's "Accepts" can depend on more than the kind**: `TRANSFORMED_GRID` wants a 2×2 or 3×3
  matrix, `PHASE_PORTRAIT` two or three variables. Ask `accepts`, do not mirror the table.
