# jMouse Query (`.jmq`) — syntax highlighting

A TextMate grammar for jMQ, in the same shape as `jmouse-access-el/tools/jmp-textmate`.

## Installing it

**VS Code** — copy or symlink this directory into the extensions folder and reload:

```
%USERPROFILE%\.vscode\extensions\jmq-textmate      (Windows)
~/.vscode/extensions/jmq-textmate                  (macOS, Linux)
```

**IntelliJ** — *Settings → Editor → TextMate Bundles → +* and point it at this directory.

## What it colours, and why those things

The grammar is not decoration. Three of its rules exist because of a mistake the language can otherwise
let somebody make silently.

| Coloured | Why |
|---|---|
| ⚠️ the converter pipe — `\| int`, `\| before("\|")` | **bold, so its absence is what stands out.** An ordered comparison over a bag of text without one compares *words*, and as words `"900"` is greater than `"1000"`. A reader scanning a filter should notice a missing pipe, not hunt for it. |
| ⚠️ `unknown` in a `source` block | **marked as invalid-ish, italic.** It is an admission, not a type — nobody has promised what the attribute holds — and it is exactly what makes an ordered comparison refuse. A source file then shows at a glance which attributes are unpromised. |
| ⚠️ reserved words — `group`, `having`, `select`, `insert`… | **greyed out before the parser refuses them.** A word held for a later version must not look like a name that merely happens not to work. |

Everything else is ordinary: keywords, strings, numbers, attribute paths in both shapes
(`entry[quantity]` and `issue.points`), and the `as` / `:` pair in a parameter list — coloured
*differently*, because `as` introduces a **type** and `:` a **default**, and they look alike on a line.

⚠️ Identifiers accept non-ASCII letters. Every document this language was designed against is Cyrillic
(`view "Мої косарки" on inventory`), and an ASCII-only word pattern would break selection and rename on
the first real file.

## Keeping it honest

The grammar duplicates knowledge that lives in `QueryToken` — that is what a TextMate grammar is. When a
keyword is added or a reserved word is spent, both change. ⚠️ The one that silently rots is this file:
the parser will refuse an unknown word loudly, while the editor will simply stop colouring it.
