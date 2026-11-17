---
subject: formula-language
title: The formula language (jMF)
summary: How a component writes its physics: every equation, display and meter is written in jMF
---
## A formula is a document

Definitions come first, then one expression.

- Statements are separated by `;`, or by a line break once a definition is complete. An expression may run over several lines.
- `#` starts a comment, which runs to the end of the line.
- The last statement is always the expression.
- A single top-level `=` in the last statement makes it an equation. `==` is a comparison.

```
r = sqrt(x^2 + y^2)
wave(u) = A sin(k u - t)
wave(r) + 0.2 wave(2 r)
```

## Notation

| Construct | Written | Example |
|---|---|---|
| addition, subtraction | `+ -` | `x + y - 1` |
| multiplication | `*` `·` `×`, or two things side by side | `2x`, `a b`, `3(x + 1)`, `π r^2` |
| division, remainder | `/` `÷`, `%` | `1 / (1 + x^2)`, `x % 2` |
| power | `^` `**` or a superscript | `x^2`, `x²`, `2^-x`; `x^y^2 = x^(y^2)` |
| factorial | a trailing `!` | `n!` |
| comparison | `< <= > >= == !=` `≤ ≥ ≠` | `x^2 + y^2 <= 1`: 1 or 0 |
| logic | `and` `or` `!` `&&` `\|\|` `∧ ∨ ¬` | `x > 0 and y > 0` |
| choice | `c ? a : b`, `if(c, a, b)`, `piecewise(c₁, v₁, …, otherwise)` | `if(abs(x) <= 1, 1, 0)` |
| sum, product | `sum(k, from, to, term)`, `product(…)` | `sum(k, 1, n, k^2)` |
| integral | `integral(t, lower, upper, integrand)` | `integral(t, 0, x, exp(-t^2))` |
| derivative | `diff(f, x)`, `diff(f, x, n)` | `diff(sin(x) / x, x)` |
| your own function | `name(p₁, p₂) = body` | `bump(u, s) = exp(-u^2 / (2 s^2))` |
| a local name | `name = body` | `r = sqrt(x^2 + y^2)` |
| imaginary unit | `i` | `exp(i theta)`, in complex formulas |

Two letters side by side need a space: `ab` is one name, `a b` is a product.

### Precedence, tightest first

`n!` · `^` (right to left) · `-x` · `* / %` and side by side · `+ -` · comparisons · `!` · `and` · `or` · `? :` · a top-level `=`.

## Conditions and undefined values

A condition is a number: 1 when it holds, 0 when it does not. Conditions compose with arithmetic: `(x > 0) * sin(x)`.

An **undefined** value — NaN — stays undefined through every operator. `if(c, a)` without its third argument is undefined where `c`
fails: that is how a formula keeps to its own domain, as in `if(x > 0, ln(x))`.

## Your own functions

- A body sees its parameters and the names defined before it, never names bound where it is called.
- A function never calls itself; a sum or a product usually says what a recursion would.
- The language's own names — `sin`, `pi`, `i`, `sum`, `if` — cannot be defined again.
- A function is called with as many arguments as it takes.

## What a component adds

A component's equations speak of its pins and rates:

| Written | Means |
|---|---|
| `current(anode)` or `anode.current` | the current through a pin |
| `d(q)/dt` | the rate of a quantity: the engine integrates it over time |

`diff(f, x)` stays the exact derivative of a formula.

## Typing it

`x²` reads as `x^2`, `−` as `-`, `·` and `×` as `*`, `÷` as `/`, `π` as `pi`, `∞` as `inf`, `≤ ≥ ≠` as `<= >= !=`, `∧ ∨ ¬` as `and or !`.
