# jMouse Formula — TextMate grammar for `.jmf`

Syntax highlighting for jMF formulas, in the VS Code extension layout that JetBrains IDEs read natively
(Settings → Editor → TextMate Bundles → add this directory). Under `tools/` rather than `src/main/resources/`
so it is not packaged into the jar — the same place `jms-textmate` has in `jmouse-script-el`.

`examples/formulas.jmf` holds formulas from the simplest to the very complex, for checking the highlighting;
the same file is shipped in the jar (`org/jmouse/formula/el/examples/formulas.jmf`), and every formula in it parses.

## It colours; it does not decide

Whether a formula means anything — whether a name is a variable of its object or a declared parameter, whether a
function has a complex form — is decided by the parser and the compiler when the formula is used. The grammar
knows the function and constant words and nothing else.

## A `.jmf` file

One formula per line; `name:` at the start of a line labels the formula after it, which belongs to the file format
rather than to a formula. A formula may be a whole document on its line — `f(u) = u^2; f(x) + f(y)` — and `#`
starts a comment anywhere.

## What is coloured

| Scope | What |
|---|---|
| `keyword.control.form.binding.jmf` · `variable.parameter.bound.jmf` | `sum`, `product`, `integral` and the name each binds |
| `keyword.control.form.jmf` | `if`, `piecewise`, `diff` |
| `entity.name.function.definition.jmf` · `variable.parameter.definition.jmf` | `f(u, v) =` — the name being defined and its parameters |
| `entity.name.function.jmf` · `entity.name.function.user.jmf` | built-in functions · any other name called with `(` |
| `keyword.operator.comparison.jmf` · `keyword.operator.logical(.word).jmf` | `< <= == !=` … · `and or ! && ||` |
| `keyword.operator.factorial.jmf` · `keyword.operator.ternary.jmf` · `keyword.operator.assignment.jmf` | `n!` · `? :` · `=` |
| `keyword.control.power.jmf` | `^`, `**`, superscripts |
| `constant.language.math.jmf` · `constant.language.imaginary.jmf` | `pi e tau phi inf` · `i` |
| `variable.parameter.coordinate.jmf` · `variable.other.field(.greek).jmf` | `x y z w t u v n` · parameters |

After changing the grammar, remove and re-add the bundle in the IDE — JetBrains caches it.
