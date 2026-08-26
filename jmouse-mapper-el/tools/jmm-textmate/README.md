# jMouse Mapping (`.jmm`) — syntax highlighting

A TextMate grammar for `.jmm`, in the same shape as `jmouse-access-el/tools/jmp-textmate` and
`jmouse-query/tools/jmq-textmate`. **No plugin to build, no IDE restart, no Gradle.**

It lives here, beside `JmmRecognizer` and `JmmParser`, because the language belongs to
`jmouse-mapper-el` rather than to any product that adopts it — and under `tools/` rather than
`src/main/resources/` so it is not packaged into the jar.

## Installing it

**IntelliJ IDEA (and every other JetBrains IDE)** — *Settings → Editor → TextMate Bundles → +* and
point it at **this directory** (`jmouse-mapper-el/tools/jmm-textmate`). Every `.jmm` file is
highlighted from that moment on, no restart.

⚠️ IDEA resolves a bundle by *directory*, so the directory is what you pick — not the grammar file.
The `language-configuration.json` beside it comes along for free: `Ctrl+/` toggles `#` comments,
braces match and auto-close, and a block indents itself.

**VS Code** — the same directory *is* an extension. Symlink or copy it into the extensions folder:

```bash
# macOS / Linux
ln -s "$PWD" ~/.vscode/extensions/jmm-textmate

# Windows (PowerShell, as administrator)
New-Item -ItemType SymbolicLink -Path "$env:USERPROFILE\.vscode\extensions\jmm-textmate" -Target (Get-Location)
```

One grammar, two editors. Changing the language means editing one file, not two.

## ⚠️ This is the second grammar for one language, deliberately

`@jmouse/codemirror`'s `src/jmmSyntax.ts` — a **different repository** — is the scanner every jMouse
interface renders a `.jmm` file with. IDEA cannot load a CodeMirror `StreamParser`, so the cost of a
second implementation is paid on purpose, and it comes with one rule:

> **Change them together, and the CodeMirror scanner is the reference when they disagree.**

A grammar that drifts is worse than no second grammar: a file looks right in the IDE and wrong in the
product's own editor, and nobody can tell which is lying. ⚠️ And the repository boundary makes drift
*likelier*, not less likely — the two files are no longer in the same `git status`, so nothing but this
paragraph will remind anybody.

Neither of them is the language. `JmmRecognizer` and `JmmParser` are, two directories up.

## What it colours, and why those things

The grammar is not decoration. Four of its rules exist because of something the language can otherwise
let somebody read wrongly.

| Coloured | Why |
|---|---|
| ⚠️ `ignore` | **its own constant, italic — not a keyword, not a name.** It is the one value on the right of a `:` that is never evaluated: the property is deliberately left alone. Painted like `from` it would look like a block; painted like an identifier it would look like a variable somebody forgot to define. |
| ⚠️ a filter after `\|` | **bold, so its absence is what stands out.** `reference \| trim \| upper` is not decoration — the same reason jMQ paints its converter bold. |
| ⚠️ the phase words — `source`/`target`, `before`/`after` | **italic bold.** They say *when* a refusal is tested, and a refusal tested on the wrong side of the mapping passes every file it was written to stop. |
| a target property — the word a `:` follows | tested by **look-ahead**, never by position: `target Order` and `unmapped fail` also sit before a colon and neither is a property. The same rule the CodeMirror scanner is built on. |
| ⚠️ a `use` header, against the type a block is about | **two different scopes, and the one place a second colour is spent on what is arguably one kind of thing.** A `use` line is bookkeeping — namespace hue, italic, read once when the file is opened. `target Project` is what a reader scans a file *for* — class hue, bold — because `mapping`, `target` and `from` repeat on every block and carry nothing once you know the language. ⚠️ `Project` occurs in both places in the same document, which is the whole argument. |

Everything else is ordinary: `#` comments, the block openers, the whole type on a `use` line coloured
as **one** thing rather than nine, strings, numbers, and the jME operators on the right-hand side.

⚠️ **Block openers are anchored to the start of a line**, exactly as the CodeMirror scanner is. A source
property called `target`, `from` or `source` is legitimate, and `source.total` has to read as a path.

## ⚠️ The one rule TextMate cannot express

**What stands on each side of a `:` depends on the block.** In a rule block the left is a target
property and the right is an expression; in a `refuse` block the left is a *condition* and the right is
a message. Colouring both left sides alike would say they are the same kind of thing, and they are
opposites — one is a name being written into, the other an expression being tested.

The CodeMirror scanner tracks which block it is in. TextMate has no state across one, so `refuse-block`
is matched as a block of its own and the lines inside it are read by position rather than by knowledge.
That covers the case that occurs. A `refuse` block written in some shape the `begin` pattern does not
match would colour its conditions as target properties — the known limit, written down here rather than
discovered.

## ⚠️ What it does not do, and must never start doing

**It does not decide whether a mapping is valid.** Whether the property exists on the target, whether
the source path can be read, whether a `let` shadows something, whether two files claim one target —
all of that is answered by `JmmReader` and `JmmValidator` on a backend when the file loads, in sentences
a person can act on. A regular expression cannot know any of it, and a highlighter that pretended to
would be a second checker agreeing for about a month, after which the editor calls a file good and the
boot refuses it.

The other ordinary limits of a regex grammar apply: unbalanced braces swallow the rest of the file (the
colour going wrong *is* the signal), and strings are single-line — as is every string the language
accepts.

## ⚠️ IntelliJ paints four things and leaves the rest plain — and that is not a bug here

The first `.jmm` file opened in IDEA came out with keywords, strings, `ignore` and comments coloured
and **everything else white** — the type on a `use` line, the type a block is about, the target-property
column. Nothing is wrong with the grammar. IntelliJ does not read a theme from a bundle: it maps each
TextMate scope onto one of its **own** attribute keys, and the colour comes from the active scheme.

That mapping is fixed, and it is small. Here it is in full, with what Darcula gives each key — the
answer to "why is this word not coloured", which is otherwise a guessing game:

| Scope | IntelliJ key | Darcula |
|---|---|---|
| `comment` · `.line` · `.block` · `.documentation` | LINE / BLOCK / DOC_COMMENT | grey ✅ |
| `constant` | CONSTANT | `#9876aa` italic ✅ |
| `constant.number` · `constant.numeric` | NUMBER | blue ✅ |
| `keyword` · `storage` · `storage.type` | KEYWORD | `#cc7832` ✅ |
| `keyword.operator` | OPERATION_SIGN | — plain |
| `string` | STRING | `#6a8759` ✅ |
| `entity.name` · `entity.name.class` | CLASS_NAME | — **plain** |
| `entity.name.function` | FUNCTION_DECLARATION | `#ffc66d` ✅ |
| `entity.other.attribute-name` | MARKUP_ATTRIBUTE | — plain |
| `variable` | LOCAL_VARIABLE | — **plain** |
| `variable.parameter` | PARAMETER | — plain |
| `entity` | IDENTIFIER | plain text colour |
| `support.function` | FUNCTION_CALL | — plain |
| `support.type` | PREDEFINED_SYMBOL | — plain |
| `punctuation` | DOT | — plain |
| `punctuation.definition.tag` | MARKUP_TAG | ✅ |
| `meta.tag` | METADATA | `#bbb529` ✅ |
| `invalid` · `invalid.deprecated` | BAD_CHARACTER · DEPRECATED | red · struck ✅ |
| `markup.*` | BOLD / ITALIC / HEADING / DIFF_* | ✅ |

⚠️ **Darcula treats a class reference as plain text in every language, Java included.** So this is a
house-style disagreement, not a defect — and the wrong way to answer it is to re-scope a type onto
`entity.name.function` or `meta.tag` because those two happen to be the only unspent colours. A scope
name is a shared vocabulary; spending `meta.tag` on a Java class name would mispaint the file in every
editor that is not this one, to fix one that is.

The right way is the scheme, and there is one in `colors/`:

> **Settings → Editor → Color Scheme → the gear → Import Scheme →** `colors/jMouse Dark.icls`, then
> pick **jMouse Dark** in the list.

It inherits from Darcula and names four keys — CLASS_NAME, LOCAL_VARIABLE, FUNCTION_CALL,
PREDEFINED_SYMBOL — in the same house palette `@jmouse/codemirror` uses, so a `.jmm` file in the IDE and
the same file in a product's own editor agree. ⚠️ It is **global**: a colour scheme is not per-language,
so Java class references take the colour too. And it serves `.jmp` and `.jmq` equally — the three
bundles land on the same four unpainted keys.

⚠️ **One consequence to know before reading the table above as a promise:** `entity.name.type` and
`entity.name.namespace` both collapse onto CLASS_NAME, so **in IntelliJ a `use` header and a block's
type share one colour** — the keyword in front is what tells them apart. They are two different colours
in a product's own editor, where the palette is ours. That is a limit of the mapping, not a decision.

⚠️ **`package.json`'s `configurationDefaults` is VS Code only.** The bold on a block's type and the
italic on a `use` header live there, and IntelliJ ignores the file entirely.

## Keeping it honest

The grammar duplicates knowledge that lives in `JmmToken`. When a structural word is added there, three
files change: the token type, the CodeMirror scanner, and this grammar. ⚠️ **This one is the file that
rots silently** — the parser refuses an unknown word loudly, while an editor simply stops colouring it,
which nobody reports as a bug.

Verify a change against the real files rather than a sample invented for the purpose — the two under
`../../src/main/resources/examples/`, and Innoventa's `BE/src/main/resources/mapping/*.jmm`. Between
them they use every construct the grammar claims to know, `refuse`, `fragment`, `always`, `include` and
a filter chain included.

```bash
npm install vscode-textmate vscode-oniguruma
# then tokenise each .jmm through source.jmm and count two things
```

**Two numbers, and both must be zero:** a token scoped bare `source.jmm` and nothing else, and a token
scoped `invalid.*`. The first is a word the grammar failed to claim — which is how `space.id` came to
render with the `.id` painted and the `space` in front of it not, until the two identifier rules at the
bottom of `#expression` were added. The second is the grammar calling a correct file wrong.

As last measured, over all five files: **216 tokens, 0 unscoped, 0 invalid.**
