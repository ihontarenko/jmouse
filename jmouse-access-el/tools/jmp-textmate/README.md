# jMouse Policy — TextMate grammar for `.jmp`

Syntax highlighting for access-policy documents, in the VS Code extension layout that JetBrains IDEs
read natively. **No plugin to build, no IDE restart, no Gradle.**

It lives here, beside `AccessToken` and the parsers, because the language belongs to
`jmouse-access-el` rather than to any product that adopts it. Under `tools/` rather than
`src/main/resources/` so it is not packaged into the jar.

## Install

### JetBrains IDEs (IntelliJ IDEA, WebStorm, …)

`Settings → Editor → TextMate Bundles → +` and pick **this directory**
(`jmouse-access-el/tools/jmp-textmate`). Every `.jmp` file is highlighted from that moment on — no
restart.

The bundled `language-configuration.json` comes along for free: `Ctrl+/` toggles `#` comments, braces
match and auto-close, and `${` closes itself.

### VS Code

The same directory *is* an extension. Symlink or copy it into your extensions folder:

```bash
# macOS / Linux
ln -s "$PWD" ~/.vscode/extensions/jmp-textmate

# Windows (PowerShell, as administrator)
New-Item -ItemType SymbolicLink -Path "$env:USERPROFILE\.vscode\extensions\jmp-textmate" -Target (Get-Location)
```

One grammar, two editors. Changing the language means editing one file, not two.

## What it knows that a keyword list cannot

A keyword list colours a word wherever it appears. This grammar colours it where the *grammar* would
read it as one — which is the whole difference, and the reason `.jmp` is worth a real grammar:

- **`role:read` is a permission, not the `role` keyword.** Exactly the case `AccessToken.nameTokens()`
  exists for: a keyword is only a keyword where the grammar expects one. Permission and action
  patterns are matched ahead of every keyword, so a colon or a dot chain always wins as a whole.
- **Blocks scope their contents.** `place` and `parameter=` mean something inside `scopes { }` and
  nothing outside it; `gate` / `limit` / `quota` are read only inside `capabilities { }`. A word that
  happens to collide with one elsewhere in the file is left alone.
- **Conditions get their own dialect.** Inside `when`, the four roots a condition can see —
  `caller`, `place`, `resource`, `action` — are coloured apart from published values like `purpose`
  or `spaceKind`, so you can see at a glance which is which.
- **The single-line `when` visibly stops at the end of its line**, because the grammar stops there
  too. A rule split over three lines without braces *looks* truncated, which is the point.

### Three mistakes it marks as errors

These are refused by the binder or the vocabulary at load. The grammar refuses them in the editor
first, because "it did not start" is a slower way to find out:

| Written | Why it is wrong |
|---|---|
| `deny`, `allow`, `when` or `grants` inside `role { }` | A role says what a permission is *worth*. Instance, effect and condition are decisions about an account, and the role body refuses to read them. |
| `allow` or `grants` inside `subject * { }` | It can only take away. An allow here grants something to every account that exists and every one that ever will, from a file no screen can undo. |
| `in`, arithmetic, `\|`, `..`, `~`, `->` inside a condition | ⚠️ The worst one. These are not refused by the expression parser — they are **silent**: it stops at the token it cannot use, evaluates the half it understood, and discards the rest without a word. `ConditionVocabulary` catches it at load; this catches it while you type. |

## Tuning

Everything is a standard TextMate scope, so your existing colour scheme already has an opinion about
each one. Two worth knowing:

- **Want `deny` to shout?** It is `keyword.control.effect.deny.jmp`, which your scheme paints like any
  other keyword. Change that `name` to `invalid.illegal.deny.jmp` in `syntaxes/jmp.tmLanguage.json`
  and it goes red everywhere. One line, and it is deliberately not the default — a policy file is
  mostly denials, and a wall of red stops meaning anything.
- **Scope names carry a `.jmp` suffix throughout**, so you can target `.jmp`-only rules in a custom
  scheme without touching how any other language is painted.

## Limits worth knowing

It is a regex grammar, not the parser — approximate on purpose, and cheap for it:

- **Nothing is resolved.** An unknown permission, a role granted before it is declared, a capability
  in `paid` with no line of its own — all of that still surfaces at load, where the checks against
  the code live. A grammar cannot know the product's vocabulary.
- **Unbalanced braces confuse it**, as they do every TextMate grammar: the block that lost its `}`
  swallows the rest of the file. The colour going wrong *is* the signal.
- **Strings are single-line.** So is every string the language actually accepts.

If completion of permission names, go-to-definition from `grants SPACE_READER` to its `role` block,
or renaming a role across files ever becomes the thing that hurts, that is where a real IntelliJ
plugin earns its keep — and it would not need JFlex, because `AccessToken` already implements
`Token.Type` and the lexer in `org.jmouse.access.el.lexer` can be wrapped in a
`com.intellij.lexer.Lexer` adapter rather than having the grammar written a second time.

## Verifying a change

The grammar was checked by tokenising `policy/innoventa.jmp`, `policy/bootstrap.jmp` and
`policy-examples/example.jmp` with `vscode-textmate` — zero unscoped tokens, zero false `invalid`.
Worth repeating after editing it:

```bash
npm install vscode-textmate vscode-oniguruma
# then tokenise a .jmp through source.jmp and grep for tokens scoped bare `source.jmp`
```
