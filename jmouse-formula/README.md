# jmouse-formula — jMF: the formula tree, and the language that writes it

The core of jMF with no parser in it: the immutable `Formula` tree, two compilers, exact differentiation,
simplification and printing. The language that produces these trees from text is in the same module, in `org.jmouse.formula.el` —
see [the language](#jmf-the-jmouse-formula-language) below. It was the separate `jmouse-formula-el` until
2026-10-12.

| Type | Role |
|---|---|
| `Formula` | sealed tree: `Constant`, `NamedConstant`, `ImaginaryUnit`, `Variable`, `Negation`, `Binary`, `Call`, `Comparison`, `Logical`, `Not`, `Piecewise`, `Accumulation` (sum/product), `Integral`, `Application` of a `Definition`, `Equation` |
| `Symbols` | the names a compiled function reads and the slot of each: variables first, then parameters |
| `RealCompiler` | a closure tree over `double[]`; constant subtrees folded; binders get slots of their own in a private frame |
| `ComplexCompiler` | the same over `Complex[]`; conditions compare real parts; sums and integrals with complex bodies |
| `Differentiation` | exact derivatives of every node, Leibniz for integrals, the chain rule for definitions |
| `Simplification` | local tidying: constants folded, zeros and ones dropped, constant conditions decided |
| `Substitution` | capture-avoiding replacement of free names |
| `Quadrature` | globally adaptive Gauss–Kronrod with infinite bounds |
| `FormulaPrinter` | text (re-parseable, `document` with definitions) and LaTeX |
| `MathematicalFunction`, `MathematicalConstant` | the built-in vocabulary with real and complex implementations |

`Formula.variables()` answers the **free** names — what a caller must supply; names a sum, an integral or a
definition binds are not among them. `Formula.dependsOn(name)` asks the same for one name.

## jMF, the jMouse Formula language

jMF is the mathematical dialect of jME. It reuses jME's lexer, parser and nodes and replaces only what a formula
needs to mean differently: `^` is a power rather than XOR, a leading minus negates, power is right-associative,
juxtaposition multiplies (`2x`, `3(x + 1)`), and only arithmetic, comparison and `and`/`or` are operators. What it
parses is translated into the immutable `Formula` tree of the core above, which
compiles it (real or complex), differentiates it exactly and prints it as text or LaTeX.

```java
Formula formula = FormulaLanguage.standard().parse("f(u) = u^2 - 1; f(x) * f(y)");
RealFunction compiled = RealCompiler.compile(formula, Symbols.of("x", "y"), source);
double value = compiled.evaluate(new double[]{0.5, 2});
String latex = FormulaPrinter.latex(Differentiation.derivative(formula, "x"));
```

`FormulaLanguage.standard()` is shared and thread-safe. `parse(source, library)` reads a document against
definitions declared elsewhere — the functions an object or a scene declares once for all its formulas — and
`definitions(source)` reads such a library out of a document of definitions.

### A formula is a document

Definitions first, then one expression. Statements are separated by `;`, or by a line break once a definition is
complete; an expression may run over several lines. `#` starts a comment anywhere.

```
# the radial distance, then a function of two arguments, then the formula itself
r = sqrt(x^2 + y^2)
bump(u, s) = exp(-u^2 / (2 s^2))
bump(r - 1, 0.3) * cos(4 atan2(y, x))
```

### Notation

| Kind | Written | Notes |
|---|---|---|
| numbers | `42`, `-3.5`, `6.022e23` | |
| arithmetic | `+ - * / %` | `%` is `mod`; `·`, `×`, `÷`, `−` are read too |
| power | `x^2`, `x**2`, `x²` | right-associative: `x^y^2 = x^(y^2)`; `2^-x` allowed |
| implicit product | `2x`, `a b`, `3(x + 1)`, `(x + 1)(x - 1)` | single letters need a space: `a b`, not `ab` |
| factorial | `n!`, `(n + 1)!`, `3!!` | Γ(n + 1), so `0.5!` is defined |
| comparison | `< <= > >= == !=`, `≤ ≥ ≠` | 1 when it holds, 0 when not |
| logic | `and`, `or`, `&&`, `\|\|`, `!`, `∧ ∨ ¬` | `!x > 0` is `!(x > 0)` |
| conditional | `c ? a : b`, `if(c, a, b)`, `if(c, a)` | `if(c, a)` is **undefined** where `c` fails |
| pieces | `piecewise(c₁, v₁, c₂, v₂, …, otherwise)` | first branch that holds; no `otherwise` means undefined there |
| equation | `x^2 + y^2 = 1` | one top-level `=`; its value is the residual `lhs - rhs`, whose zero set an implicit object draws |
| sum, product | `sum(k, 1, n, k^2)`, `product(k, 1, n, …)` | over the integers ⌈from⌉…⌊to⌋; `to` may be `inf` |
| integral | `integral(t, a, b, f)` | either bound may be `inf` / `-inf`; bounds may depend on free names |
| derivative | `diff(f, x)`, `diff(f, x, 2)` | exact, taken once when the formula is read |
| definition | `f(u, v) = …`, `r = …` | lexically scoped, never recursive |
| constants | `pi π`, `tau`, `e`, `phi`, `inf ∞`, `i` | `i` only in complex formulas |

A truth value is a number, so conditions compose with arithmetic: `if(x > 0, 1, 0) * sin(x)`, or
`(x > 0) * sin(x)`. An undefined value is NaN and stays NaN through every operator and condition — which is how
`if(x^2 + y^2 < 4, sqrt(4 - x^2 - y^2))` is a hemisphere and nothing outside it.

### Functions

`sin cos tan cot sec csc` · `asin acos atan atan2` (also `arcsin arccos arctan`, `tg ctg`) ·
`sinh cosh tanh asinh acosh atanh` · `exp ln log log10 log2` · `sqrt cbrt abs sign sgn` ·
`floor ceil round step min max mod hypot pow` · `re im arg conj` ·
`gamma Γ factorial binomial choose erf`.

In a complex formula every function with a complex form is available — including `gamma`, `factorial` and
`binomial` — and a function without one is refused rather than guessed.

### Binders and scope

`sum`, `product` and `integral` bind their first argument: inside `sum(k, 1, n, x^k)` the name `k` is the index,
and outside it `k` is free again. `diff(f, x)` names the variable to differentiate by.

A definition's body sees its parameters and the names that are free at the top level — never names bound at the
place it is called. `g = k^2; sum(k, 1, 3, g)` is `3 k²` for the free `k`, not `1 + 4 + 9`. That is what makes a
definition mean the same thing everywhere it is used. A body sees only the definitions before it, so recursion is
impossible; `f(x) = f(x)` is refused with that reason. A built-in name (`sin`, `pi`, `sum`, …) cannot be redefined.

### Numerics

| Form | How it is computed |
|---|---|
| finite sum / product | exactly, up to 1 000 000 terms; more is NaN, not a hang |
| `sum(…, inf, …)` | until 8 consecutive terms no longer move the result; a slowly converging series (`1/k²`) is accepted once its last term changes it by less than 1e-8 of itself, a diverging one (`1/k`) is NaN |
| `integral` | globally adaptive Gauss–Kronrod (G7/K15), tolerance 1e-10 relative; infinite bounds mapped onto finite ones; an integrable endpoint singularity (`1/sqrt(t)` at 0) converges because nodes never touch the ends; out of budget, NaN unless the error is below 1e-6 |
| `gamma`, `factorial` | Lanczos (g = 7, n = 9), ~15 digits, exact products for whole arguments up to 170; poles are NaN |
| `binomial` | exact product for whole arguments, Γ otherwise |
| `erf` | Taylor series below 2.5, continued fraction for erfc above |

A formula with no binders evaluates in the caller's array and allocates nothing; one with binders evaluates in a
private frame copied from it, so a compiled function stays thread-safe.

### Derivatives

`Differentiation.derivative` is exact for every form:

- conditions and comparisons are 0 (they are constant wherever they are differentiable);
- a piecewise expression keeps its conditions and differentiates each branch;
- `d/dx ∑ f = ∑ ∂f/∂x`, `d/dx ∏ f = ∏ f · ∑ (∂f/∂x)/f`;
- integrals follow Leibniz: `d/dx ∫ₐ⁽ˣ⁾ᵇ⁽ˣ⁾ f dt = f(b) b' − f(a) a' + ∫ ∂f/∂x dt`;
- a call of the author's function uses the chain rule in terms of new definitions, so `d/dx f(2x)` prints
  `f_u(u) = 3 u^2; 2 * f_u(2 x)` rather than the body written out;
- `sign floor ceil round step gamma factorial` have no exact derivative and are refused.

### Printing

`FormulaPrinter.text` writes an expression; `FormulaPrinter.document` writes the definitions it calls first, and
what it writes parses back to the same tree. `FormulaPrinter.latex` writes KaTeX: `\begin{cases}` for pieces,
`\sum_{k=1}^{\infty}`, `\int_a^b … \, dt`, `\binom{n}{k}`, `n!`, `\Gamma`.

### Files and editor support

[`examples/formulas.jmf`](src/main/resources/org/jmouse/formula/el/examples/formulas.jmf) (also in the jar) holds
114 formulas from `42` to Fresnel integrals; each parses, compiles and round-trips. A `.jmf` file holds one
formula per line with an optional `name:` label. [`tools/jmf-textmate`](tools/jmf-textmate/README.md) is the
TextMate grammar for JetBrains IDEs and VS Code.

### What jMF is not

It is a formula language, not a programming language: no loops beyond `sum`/`product`, no mutable state, no
strings, no recursion. Time-stepping — an ODE system, a circuit's transient, a discrete map — is expressed as the
right-hand side in jMF and integrated by `jmouse-parallax` (`OdeIntegrator`, Dormand–Prince 5(4)).
