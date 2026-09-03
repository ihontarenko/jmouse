# jMouse Script — TextMate grammar for `.jms`

Syntax highlighting for jMS documents, in the VS Code extension layout that JetBrains IDEs read
natively. **No plugin to build, no IDE restart, no Gradle.**

It lives here, beside `ScriptToken` and the parsers, because the language belongs to
`jmouse-script-el` rather than to any product that adopts it — the same reason `jmp-textmate` lives in
`jmouse-access-el` and `jmm-textmate` in `jmouse-mapper-el`. Under `tools/` rather than
`src/main/resources/` so it is not packaged into the jar.

## ⚠️ The CodeMirror scanner is the reference

There are two highlighters for this dialect and they must agree:

| Where | What it is for |
|---|---|
| `@jmouse/codemirror` → `jmsSyntax.ts` (UIK-53) | a browser — the script editor a product ships |
| this bundle | an editor outside the browser |

**When the two disagree, the scanner is right.** It has tests, including one asserting that no character
of a fixture is left unscoped; this bundle has an IDE and a pair of eyes. So a word list changes there
first and is copied here.

## ⚠️ It colours; it does not decide

Whether a script is valid — whether the event exists, whether a facade was declared, whether a function
was ever defined — is answered by the parser and the binder on a backend when the file loads. Neither
highlighter knows a catalogue, and neither is going to: a re-implementation of the grammar in a second
language is a second grammar that agrees for about a month.

## Three things this grammar gets right on purpose

1. **A keyword is only a keyword where a keyword can be.** The backend reads every one of the dialect's
   keywords as an ordinary name wherever a name belongs — a host is entitled to an event called `end` or
   a facade method called `do`. `@world.end()` is a call, not a block terminator.
2. **`#` is both a comment and a constant access.** `# a note` opens a comment; the `#` of
   `@player#MAX` does not. They are told apart by what precedes the hash.
3. **`=` is not `==`.** A lone `=` assigns; `==` compares, and writing the second where the first was
   meant is refused at load because it would otherwise evaluate and throw its answer away. They get
   different scopes, because that is the mistake this language expects people to make.

Words that are **operators** rather than control keywords: `and`, `or`, `not`, `is`, `in`. ⚠️ `not` in
particular — it is the engine's second spelling of `!=` and the partner of `is`, never a prefix.
Negation is `!`.

Scope name: `source.jms`. A fixture covering every construction lives in the module's tests, as
`GameShapedFixture`.

## Install

### JetBrains IDEs (IntelliJ IDEA, WebStorm, …)

Settings → Editor → TextMate Bundles → **+** → this directory.

### VS Code

```powershell
New-Item -ItemType SymbolicLink `
  -Path   "$env:USERPROFILE\.vscode\extensions\jms-textmate" `
  -Target (Resolve-Path .)
```
