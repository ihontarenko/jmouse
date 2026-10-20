# jmouse-formula — jMF, the jMouse Formula language

> **Agent orientation file.** Written so you do not have to read the modules to use them. If something
> here disagrees with the code, the code wins — and fix this file in the same change.
>
> Module: `jmouse-formula` (+ `jmouse-el`) — `org.jmouse.formula`: the tree, compilers, derivatives, printing ·
> `org.jmouse.formula.el`: the language that reads text into the tree, jMF's entry point. One module since
> 2026-10-12; the language was the separate `jmouse-formula-el` before, and its packages did not move.
> Built on top of them: `jmouse-parallax` ([JMOUSE_PARALLAX.md](JMOUSE_PARALLAX.md)).
> First consumer: Parallax (`jMouseProjects/Parallax/BE`).

## What it is in one paragraph

jMF is the mathematical dialect of jME. A formula — `sin(x) * exp(-y^2)`, or a whole document with its own
functions, sums, integrals and cases — is parsed by jME's machinery and translated into an immutable, sealed
`Formula` tree. That tree is the one thing every consumer works with: `RealCompiler` and `ComplexCompiler` turn
it into a closure that evaluates fast enough to sample a 512×512 grid, `Differentiation` gives its exact
derivative as another tree, `Simplification` tidies it, and `FormulaPrinter` writes it back as re-parseable text
or as LaTeX for KaTeX.

⚠️ **jMF is built by extending jME, not by forking it.** It has its own `CoreExtension` (arithmetic, comparison
and `and`/`or` only), its own `^` operator on jME's caret token (power, not XOR), and its own operator-precedence
parser substituted for jME's at lookup time. Nothing in `jmouse-el` was changed for it.

## The map

```
org.jmouse.formula                       (jmouse-formula)
├── Formula (sealed)        Constant · NamedConstant · ImaginaryUnit · Variable · Negation · Binary(Operator) ·
│                           Call(MathematicalFunction) · Comparison(Relation) · Logical(Connective) · Not ·
│                           Piecewise(Branch…, otherwise?) · Accumulation(Accumulator SUM|PRODUCT, index, from, to, body) ·
│                           Integral(variable, lower, upper, body) · Application(Definition, arguments) · Equation
│   └── Definition          name, parameters, body — a function the author defined
├── Symbols                 the names a compiled function reads, and the slot of each
├── RealFunction            double evaluate(double[] values)
├── RealCompiler            compile(Formula, Symbols, source) → RealFunction · fold(Formula) → double
├── ComplexCompiler         compile(Formula, Symbols, source) → ComplexFunction (Complex[] → Complex)
├── Complex                 record (real, imaginary) with the elementary functions
├── Differentiation         derivative(Formula, variable) → Formula, exact and simplified
├── Simplification          simplify(Formula) → Formula
├── Substitution            substitute(Formula, name, replacement), capture-avoiding
├── Quadrature              integrate(DoubleUnaryOperator, lower, upper) — adaptive Gauss–Kronrod
├── FormulaPrinter          text(Formula) · document(Formula) · latex(Formula)
├── MathematicalFunction    the built-in functions, each with a real and (where it has one) a complex form
├── MathematicalConstant    PI TAU E PHI INFINITY
└── FormulaException        message, source, position (-1 when about the whole formula)

org.jmouse.formula.el                    (the language — same module)
├── FormulaLanguage         standard() · parse(source) · parse(source, library) · definitions(source) · evaluate(source, values)
├── FormulaStatements       splits a document into definitions and the expression (package-private)
├── FormulaTranslator       jME tree → Formula; knows the forms if/piecewise/sum/product/integral/diff
├── FormulaCoreExtension    jMF's vocabulary, in place of jME's CoreExtension
├── FormulaExtensionContainer   hands out FormulaOperatorParser wherever jME asks for OperatorParser
├── FormulaOperatorParser   precedence climbing: unary minus, right-associative ^, juxtaposition, n!, !cond
├── FormulaOperator         POWER (on T_CARET) · NEGATE · FACTORIAL
├── FormulaLexer            repairs jME's "sign glued to a number" after an operand: b+2.5
├── FormulaNotation         Unicode → ASCII: x² · − × ÷ π ∞ ≤ ≥ ≠ ∧ ∨ ¬
└── NegationNode · NotNode · FactorialNode   the jME nodes jMF adds
```

## The whole usage

```java
FormulaLanguage language = FormulaLanguage.standard();            // shared, thread-safe

// 1. read
Formula formula = language.parse("""
        r = sqrt(x^2 + y^2)
        bump(u, s) = exp(-u^2 / (2 s^2))
        A * bump(r - 1, sigma) * cos(4 atan2(y, x))
        """);

// 2. compile once — variables first, then parameters; the array is laid out in that order
Symbols symbols = Symbols.of(List.of("x", "y"), List.of("A", "sigma"));
RealFunction surface = RealCompiler.compile(formula, symbols, source);
double height = surface.evaluate(new double[]{0.5, 0.25, 1.0, 0.3});

// 3. differentiate exactly, print
Formula slope = Differentiation.derivative(formula, "x");
String latex = FormulaPrinter.latex(slope);                         // for KaTeX
String text = FormulaPrinter.document(slope);                       // parses back to the same tree

// 4. complex
Formula mapping = language.parse("(z - 1) / (z + 1)");
ComplexCompiler.ComplexFunction function = ComplexCompiler.compile(mapping, Symbols.of("z"), "(z - 1) / (z + 1)");
Complex value = function.evaluate(new Complex[]{new Complex(0.5, 0.5)});

// 5. one-off value
double basel = language.evaluate("sum(k, 1, inf, 1/k^2)", Map.of());   // 1.6449…
```

`formula.variables()` answers the **free** names — exactly what the caller must put in `Symbols`. Names that a
sum, an integral or a definition binds are not among them. `Symbols.requireDeclares` refuses a formula that
mentions a name the table does not declare, and the message lists the names it does know.

## A formula is a document

Definitions first, then one expression.

- Statements are separated by `;`, or by a line break **once a definition is complete** — so an expression may
  run over several lines, and so may a definition inside parentheses or after a line ending in an operator.
- `#` starts a comment anywhere and runs to the end of the line.
- The last statement is always the expression; a document that ends with a definition is refused.
- A single top-level `=` in the last statement makes it an **equation** (see below); `==` is a comparison.

```
# one line, separated by ';'
f(u) = u^2 - 1; f(x) * f(y)

# several lines: each definition ends at its line break, the expression may continue
r = sqrt(x^2 + y^2)
wave(u) = A sin(k u - t)
wave(r) +
    0.2 wave(2 r)
```

## Notation — every construct

| Construct | Written | Example | Notes |
|---|---|---|---|
| number | `42` `-3.5` `.5` `6.022e23` | `6.022e23 * n` | |
| addition, subtraction | `+ -` | `x + y - 1` | `−` (U+2212) and `–` read as `-` |
| multiplication | `*` `·` `×` `⋅` | `2 * x` | |
| implicit multiplication | juxtaposition | `2x`, `a b`, `3(x + 1)`, `(x + 1)(x - 1)`, `π r^2` | two letters need a space: `ab` is one name |
| division | `/` `÷` | `1 / (1 + x^2)` | |
| remainder | `%` | `x % 2` | the same as `mod(x, 2)`, always non-negative for a positive divisor |
| power | `^` `**` superscripts | `x^2`, `x**2`, `x²`, `2^-x`, `x^y^2` | right-associative: `x^y^2 = x^(y^2)`; `-x^2 = -(x^2)` |
| negation | leading `-` | `-x^2`, `2 * -x` | |
| factorial | trailing `!` | `n!`, `(n + 1)!`, `3!!` | Γ(n + 1), defined for real n; `3!! = (3!)!` |
| comparison | `< <= > >= == !=`, `≤ ≥ ≠` | `x^2 + y^2 <= 1` | value 1 or 0 |
| conjunction | `and` `&&` `∧` | `x > 0 and y > 0` | |
| disjunction | `or` `\|\|` `∨` | `x < -1 or x > 1` | |
| negation of a condition | `!` `¬` | `!(x > 0)`, `!x > 0` | `!x > 0` means `!(x > 0)` |
| ternary | `c ? a : b` | `x > 0 ? sqrt(x) : 0` | |
| conditional | `if(c, a, b)` · `if(c, a)` | `if(abs(x) <= 1, 1, 0)` | without the third argument it is **undefined** where `c` fails |
| cases | `piecewise(c₁, v₁, c₂, v₂, …, otherwise)` | `piecewise(x < 0, -1, x < 1, 0, 1)` | the first branch that holds; an odd last argument is the otherwise |
| equation | `lhs = rhs` | `x^2 + y^2 = r^2` | value is `lhs - rhs`; one `=` only |
| sum | `sum(k, from, to, term)` | `sum(k, 1, n, k^2)` | `k` is bound; `to` may be `inf` |
| product | `product(k, from, to, factor)` | `product(k, 1, inf, 4k^2 / (4k^2 - 1))` | |
| integral | `integral(t, lower, upper, integrand)` · `integrate(…)` | `integral(t, 0, x, exp(-t^2))` | `t` is bound; bounds may be `inf`, `-inf`, or depend on free names |
| derivative | `diff(f, x)` · `diff(f, x, n)` · `derivative(…)` | `diff(sin(x) / x, x)`, `diff(exp(-x^2), x, 2)` | exact, taken once when the formula is read; order 1–8 |
| function definition | `name(p₁, p₂) = body` | `bump(u, s) = exp(-u^2 / (2 s^2))` | |
| local name | `name = body` | `r = sqrt(x^2 + y^2)` | a definition with no parameters |
| constants | `pi π` `tau` `e` `phi` `inf ∞` | `2 pi r`, `sum(k, 0, inf, …)` | `phi` is the golden ratio |
| imaginary unit | `i` | `exp(i theta)` | complex formulas only; a real compile refuses it |

### Precedence, tightest first

| Level | Operators |
|---|---|
| postfix | `n!` |
| power | `^` `**` (right-associative; the right side may carry a sign: `2^-x`) |
| unary | `-x` |
| multiplicative | `*` `/` `%` and juxtaposition |
| additive | `+` `-` |
| relation | `< <= > >= == !=` |
| not | `!` (takes a whole relation) |
| and | `and` `&&` |
| or | `or` `\|\|` |
| ternary | `? :` |
| equation | top-level `=` |

## Truth values and undefined values

A condition is a number: 1 when it holds, 0 when it does not, and a value holds when it is not 0. So conditions
compose with arithmetic as well as with `if`:

```
(x > 0) * sin(x)                     # sin(x) on the right half, 0 on the left
(x^2 + y^2 < 1) + (x^2 + y^2 < 4)    # 2 in the disc, 1 in the annulus, 0 outside
```

An **undefined** value is NaN, and NaN stays NaN through every operator, comparison and condition — a condition
that is NaN makes the whole `if`/`piecewise` NaN rather than choosing a branch. That is how a formula restricts its
own domain, and every representation in `jmouse-parallax` treats NaN as "draw nothing here":

```
if(x^2 + y^2 < 4, sqrt(4 - x^2 - y^2))      # a hemisphere and nothing outside the disc
if(x > 0, ln(x))                            # ln, but only where it is real
```

## Your own functions

```
f(u) = u^2 - 1; f(x) * f(-x)                              # one parameter
bump(u, s) = exp(-u^2 / (2 s^2)); bump(x - 1, 0.5)          # two
g(u) = u^3 - u; h(u) = g(g(u)); h(x)                      # composition: h sees g
wave(u) = A sin(k u - t); wave(x) + wave(y)               # the body may use the object's parameters A, k, t
s(u) = if(u <= 0, 0, if(u >= 1, 1, 3u^2 - 2u^3)); s(x)     # a smooth step, by cases
f(t) = t^2; integral(t, 0, x, f(t))                       # inside a binder
```

Rules:

- **Lexical scope.** A body sees its parameters and the names free at the top level — never names bound at the
  place it is called. `g = k^2; sum(k, 1, 3, g)` is `3 k²` for the *free* `k`, not `1 + 4 + 9`. A definition
  therefore means the same thing everywhere it is used.
- **No recursion.** A body sees only the definitions before it. `f(x) = f(x)` is refused with exactly that reason;
  a sum or a product usually says what a recursion would.
- **Built-ins cannot be redefined**: `sin = 2` is refused, so are `pi`, `i`, `sum`, `if` and the rest.
- **Arity is checked**: `f(x) = x; f(1, 2)` → `f takes 1 argument(s), not 2.` A one-parameter definition used
  bare (`f` instead of `f(x)`) is refused too.
- **Libraries.** `parse(source, library)` reads a document against definitions declared elsewhere — the functions
  an object or a scene declares once for all its formulas. `definitions(source)` reads such a library out of a
  document of definitions alone. A document's own definition hides the library's.

Compiled, a call does not inline its body: the arguments are evaluated once, written into the parameters' slots,
and the body — compiled once per call site — reads them. `f(expensive) * f(expensive)` evaluates each argument once
per call.

## Binders: sums, products, integrals

```
sum(k, 1, 10, k)                              # 55
sum(k, 1, n, k^2)                             # n is free: a parameter of the object
sum(k, 1, inf, 1 / k^2)                       # π²/6, a series
sum(k, 0, inf, x^k / k!)                      # e^x
4 / pi * sum(k, 0, N, sin((2k + 1) x) / (2k + 1))   # a Fourier partial sum, N a parameter
product(k, 1, 5, k)                           # 120
product(k, 1, inf, 4k^2 / (4k^2 - 1))         # Wallis: π/2
integral(t, 0, pi, sin(t))                    # 2
integral(t, -inf, inf, exp(-t^2))             # √π
integral(t, 0, 1, 1 / sqrt(t))                # 2 — an integrable singularity at an end
integral(t, 0, x, integral(s, 0, t, 1))       # nested: x²/2
integral(t, 0, inf, t^(x - 1) exp(-t))        # Γ(x), by its definition
```

- The first argument must be a free name; `sum(2, 1, 3, k)` and `sum(e, …)` are refused.
- A sum runs over the integers from ⌈from⌉ to ⌊to⌋ (with 1e-9 slack, so `sum(k, 0.9999999999, 3, …)` starts at 1).
  An empty range is 0 for a sum and 1 for a product.
- Bounds are ordinary expressions and may depend on free names: `sum(k, 1, n, …)`, `integral(t, a, b, …)`.
- An inner binder with the same name shadows the outer one.

## Functions

Every function call checks its arity. Real implementations follow `java.lang.Math` unless noted.

| Function | Arguments | Example | Value | Complex | Exact derivative |
|---|---|---|---|---|---|
| `sin` `cos` `tan` | 1 | `sin(pi / 6)` | 0.5 | yes | yes |
| `cot` `sec` `csc` | 1 | `sec(0)` | 1 | yes | yes |
| `asin` `acos` `atan` (`arcsin` `arccos` `arctan`) | 1 | `atan(1)` | π/4 | — | yes |
| `tg` `ctg` | 1 | `tg(x)` | aliases of `tan`, `cot` | yes | yes |
| `atan2` | 2 (y, x) | `atan2(1, -1)` | 3π/4 | — | yes |
| `sinh` `cosh` `tanh` | 1 | `cosh(0)` | 1 | yes | yes |
| `asinh` `acosh` `atanh` | 1 | `asinh(1)` | 0.8814… | — | yes |
| `exp` | 1 | `exp(1)` | e | yes | yes |
| `ln` (`log`) | 1 | `ln(e^2)` | 2 | yes | yes |
| `log10` `log2` | 1 | `log2(8)` | 3 | — | yes |
| `sqrt` `cbrt` | 1 | `cbrt(-8)` | -2 | `sqrt` only | yes |
| `abs` | 1 | `abs(-3)` | 3 | modulus | sign(x) |
| `sign` (`sgn`) `step` | 1 | `step(0)` | 1 | — | refused |
| `floor` `ceil` `round` | 1 | `round(2.5)` | 2 (to even) | — | refused |
| `min` `max` | 2 | `max(x, 0)` | the ReLU | — | refused |
| `mod` | 2 | `mod(-1, 3)` | 2 | — | refused |
| `hypot` | 2 | `hypot(3, 4)` | 5 | — | yes |
| `pow` | 2 | `pow(2, 10)` | 1024 | yes | yes |
| `re` `im` `arg` `conj` | 1 | `arg(i)` | π/2 | yes | — |
| `gamma` (`Gamma`, `Γ`) | 1 | `gamma(0.5)` | √π | yes | refused |
| `factorial` (`n!`) | 1 | `factorial(5)`, `0.5!` | 120, √π/2 | yes | refused |
| `binomial` (`choose`) | 2 (n, k) | `binomial(10, 3)` | 120 | yes | refused |
| `erf` | 1 | `erf(1)` | 0.8427… | — | 2/√π·e^(−x²) |

A function with no complex form is refused by the complex compiler with its name (`atan has no complex form
here.`), never silently evaluated on the real part.

Real-valued conventions worth knowing:

- `(-8)^(1/3)` is `-2`: a negative base under an odd-denominator rational exponent keeps its real root, so
  `x^(1/3)` draws on both sides of zero. `(-8)^0.5` is NaN.
- Small whole exponents (`x^2` … `x^8`) multiply rather than call `pow`, and stay exact for a negative base.
- `gamma` is NaN at its poles (0, −1, −2, …); whole arguments up to 170 are multiplied out exactly.
- `binomial(n, k)` with whole arguments is an exact product, 0 for k outside 0…n.

## Equations

A top-level single `=` makes the last statement an `Equation`. Its value is the **residual** `lhs − rhs`, whose zero
set is the solution set — which is what an implicit curve, surface or hypersurface draws:

```
x^2 + y^2 = 1                 # a circle
x^3 + y^3 = 3 a x y           # the folium of Descartes
(x^2 + y^2 - 1)^3 = x^2 y^3   # a heart
```

Only one `=` is allowed (`x = 1 = 2` → `An equation has one '='; compare with '==' instead.`). An `=` inside
parentheses or a call is a comparison, as is `==` everywhere.

## Numerics

| Form | How | Limits |
|---|---|---|
| finite `sum` / `product` | exactly, term by term | 1 000 000 terms; more is NaN at once, never a hang |
| `sum(…, inf, …)` | until 8 consecutive terms no longer change the result in the 16th digit | out of the 1 000 000-term budget, the result is kept if the last term changed it by less than 1e-8 relative (`1/k²` passes, error ~1e-6), otherwise NaN (`1/k` diverges → NaN) |
| `product(…, inf, …)` | the same rule on the factors | |
| `integral` | globally adaptive Gauss–Kronrod G7/K15: the interval with the largest error is halved next | tolerance max(1e-12, 1e-10·\|I\|); 2000 intervals; out of budget, kept if the error is under 1e-6, otherwise NaN |
| infinite bounds | `[a, ∞)`: t = a + s/(1−s); `(−∞, b]`: t = b − s/(1−s); `(−∞, ∞)`: t = s/(1−s²) | |
| `gamma`, `factorial` | Lanczos g = 7, n = 9 (~15 digits), reflection left of ½; complex the same way | |
| `erf` | Taylor series for \|x\| < 2.5, continued fraction for erfc beyond, ±1 past 6 | ~1e-15 |

**Allocation and threads.** A formula without binders compiles to closures that read the caller's array and
allocate nothing. A formula with binders gets one slot per binder occurrence after the caller's symbols and
evaluates in a private copy of the array, so a compiled function is safe to share between threads either way.
Constant subtrees are folded once at compile time — including whole sums and integrals with constant bounds.

## Derivatives

`Differentiation.derivative(formula, variable)` is symbolic and exact, then simplified.

| Node | Rule |
|---|---|
| arithmetic, power | the usual rules; `u^v` with both varying uses `u^v (v' ln u + v u'/u)` |
| functions | the table above; refused where no exact derivative exists |
| comparison, logic, not | 0 — constant wherever differentiable |
| piecewise | same conditions, each branch differentiated: `if(x > 0, x^2, -x)` → `if(x > 0, 2x, -1)` |
| equation | `lhs' − rhs'` |
| sum | `∑ ∂f/∂x`; bounds move in integer steps and contribute nothing |
| product | `∏ f · ∑ (∂f/∂x)/f` |
| integral | Leibniz: `f(b) b' − f(a) a' + ∫ ∂f/∂x dt`, e.g. `d/dx ∫₀ˣ t² a dt = x² a` |
| definition call | chain rule in terms of new definitions: `d/dx f(2x)` with `f(u) = u^3` → `f_u(u) = 3 u^2; 2 * f_u(2x)`; a top-level name in the body gives `f_a` |
| differentiating by a bound name | 0 inside that binder |

Anything that does not depend on the variable is 0 immediately, so `diff(floor(a), x)` is 0 rather than refused.

## What a host adds — host calls, members, rates

A host may extend the notation without forking it; every one of these is read in the tree, never out of the text.

| Declared with | Written | Becomes |
|---|---|---|
| `withHostCalls("current", …)` | `current(anode)` | `HostCall("current", [anode])`, for the host to bind (`Substitution.replaceHostCalls`) |
| the same declaration | `anode.current` | the same `HostCall` — a member its host declared is the call written as something the word has; any other property path is refused |
| — | `current(in)`, `in.current` | a keyword alone as a call argument, or before a member, is a word: a pin may be called `in` |
| `withRate("t", "d/dt")` | `d(q)/dt` | `HostCall("d/dt", [q])` — a rate the host integrates, not a derivative taken here. `polarity * d(q)/dt` (read as `(polarity * d(q)) / dt`) and `-d(q)/dt` are rates too; a `d(…)` not over `dt`, and `dt` not under a `d(…)`, are refused |

The circuit engine declares all three; `diff(f, x)` stays the exact derivative taken by jMF itself.

## Printing

```java
FormulaPrinter.text(formula)      // the expression alone
FormulaPrinter.document(formula)  // the definitions it calls, then the expression — parses back to the same tree
FormulaPrinter.latex(formula)     // KaTeX
```

| Formula | `document` | `latex` |
|---|---|---|
| `f(u) = u^2 + 1; f(x) * f(-x)` | `f(u) = u^2 + 1; f(x) * f(-x)` | `f\left(x\right) \cdot f\left(-x\right)` |
| `piecewise(x < 0, -x, x < 1, x^2, 1)` | same | `\begin{cases} -x & x < 0 \\ x^{2} & x < 1 \\ 1 & \text{otherwise} \end{cases}` |
| `sum(k, 1, inf, 1/k^2)` | `sum(k, 1, inf, 1 / k^2)` | `\sum_{k=1}^{\infty} \frac{1}{k^{2}}` |
| `integral(t, 0, x, exp(-t^2))` | same | `\int_{0}^{x} \exp\left(-t^{2}\right) \, dt` |
| `n! / (k! (n - k)!)` | `n! / (k! * (n - k)!)` | `\frac{n!}{k! \cdot \left(n - k\right)!}` |
| `!(x > 0) or y >= 2` | same | `\lnot \left(x > 0\right) \lor y \ge 2` |

Greek names print as Greek (`sigma` → `\sigma`), `x_1` as `x_{1}`, `binomial` as `\binom{n}{k}`, `gamma` as `\Gamma`,
`floor`/`ceil` with brackets.

## Refusals

Every refusal is a `FormulaException` with a sentence for the reader and, where it is about one place, a character
position. Representative ones:

| Source | Message |
|---|---|
| `sni(x, y)` | `'sni' is not a function.` |
| `sni(x)` with symbols x | `'sni' is not defined here. Known names: x. …` (a one-argument unknown call reads as a product) |
| `ab` with symbols a, b | `'ab' is not defined here … Multiplication of single letters needs an operator or a space: x*y.` |
| `sin(1, 2)` | `sin takes 1 argument(s), not 2.` |
| `sin = 2; sin` | `'sin' is built into jMF and cannot be redefined.` |
| `f(x) = f(x); f(1)` | `f cannot call itself; jMF definitions are not recursive, and a sum or a product usually says the same thing.` |
| `sum(2, 1, 3, k)` | `The first argument of sum names the variable it runs over, such as k or t; '2' is not a free name.` |
| `x = 1 = 2` | `An equation has one '='; compare with '==' instead.` |
| `diff(x, x, 9)` | `The order of a derivative is a whole number from 1 to 8.` |
| `exp(i x)` compiled as real | `This expression is real-valued; 'i' belongs to complex expressions.` |
| `a.b` | `'a.b' is a property path; a formula names symbols only.` |
| longer than 16 000 characters | `A formula is at most 16000 characters long.` |

## Unicode the reader types

`x²` → `x^2` · `−` `–` → `-` · `·` `×` `⋅` → `*` · `÷` → `/` · `π` → `pi` · `∞` → `inf` · `≤ ≥ ≠` → `<= >= !=` ·
`∧ ∨ ¬` → `and or !`. jME itself stays ASCII; `FormulaNotation` does this before the lexer sees the text.

## Files and editors

- `jmouse-formula/src/main/resources/org/jmouse/formula/el/examples/formulas.jmf` (in the jar): 114 formulas
  from `42` to Fresnel integrals, grouped by what they exercise; every one parses, compiles and round-trips
  through `FormulaPrinter.document`.
- A `.jmf` file holds one formula per line, optionally labelled `name:`; a line may be a whole document
  (`f(u) = u^2; f(x) + f(y)`). `jmouse-parallax`' `ObjectFiles.readFormulaFile` reads it into objects.
- `jmouse-formula/tools/jmf-textmate` is the TextMate grammar (JetBrains: Settings → Editor → TextMate Bundles;
  VS Code: the same directory as an extension). Re-add it after changing the grammar — JetBrains caches it.

## Gotchas

- ⚠️ **`not`, `ne`, `eq`, `gt`, `lt`, `ge`, `le`, `and`, `or` are operator words in jME's lexer** and cannot be
  variable names. `not` is the logical negation, as `!` and `¬` are (`not x > 0` is `not (x > 0)`); inequality is
  `!=`, `ne` or `neq`. A factorial is the mark alone: `n!`, never `n not`. (Until 2026-10-20 `not` was an alias of
  `!=`.)
- ⚠️ **`e` is Euler's number and `i` the imaginary unit** — neither can be a parameter or an index.
- ⚠️ **A one-argument call of an unknown name is a product**: `rho(x - 1)` is `rho * (x - 1)`. That is what makes
  `a(b + c)` work; a typo such as `sni(x)` is still caught, by the symbol table.
- ⚠️ **`diff` is taken when the formula is read**, so `FormulaPrinter` shows the derivative, not `diff(…)`.
- ⚠️ **A formula's value outside its definition is NaN, not an exception.** Sampling code must treat NaN as "no
  value here" — which every `jmouse-parallax` computation does.
- ⚠️ **Definitions are not shared between separate `parse` calls** unless passed as a library. Two components of
  a parametric curve that need the same helper either repeat it or share a library.

## What jMF is not

A formula language, not a programming language: no loops beyond `sum`/`product`, no mutable state, no strings,
no recursion, no I/O. Behaviour over time — an ODE system, a circuit's transient response, a discrete map — is
written as its right-hand side in jMF and integrated by `jmouse-parallax` (`OdeIntegrator`, Dormand–Prince
5(4), `DiscreteMap` iteration).
