# jMouse Sheet (`.sht`) — syntax highlighting

A TextMate grammar for sheet files — the tables of facts jMouse reads: a part's data sheet, a package's dimensions, a
library map, a taxonomy. Same shape as `jmouse-query/tools/jmq-textmate`.

`.sht` is the extension every file is written with. `.dsh` (data sheet) and `.jmt` (table) are recognised as well, so
a file named either way is coloured the same.

## Installing it

**VS Code** — copy or symlink this directory into the extensions folder and reload:

```
%USERPROFILE%\.vscode\extensions\sheet-textmate      (Windows)
~/.vscode/extensions/sheet-textmate                  (macOS, Linux)
```

**IntelliJ** — *Settings → Editor → TextMate Bundles → +* and point it at this directory.

## What it colours

| Coloured | Example |
|---|---|
| the document's head | `sheet "74HC595" as 74hc595 {` |
| an entry, its title and where it was read | `supply-voltage "Supply voltage" source page 5 table "6.3" {` |
| a field's name | `range`, `pitch`, `members` |
| a number and its unit apart | `2 V .. 6 V`, `0.35 mm` |
| a link to another sheet's entry or field | `../logic/hc.sht#levels/high-input-minimum` |
| words, texts, `=` and `..` | `14 = data`, `"Texas Instruments"` |

A unit is coloured apart from its number on purpose: a table read at a glance shows which values carry a unit and
which were left bare — and a bare length is a value the reader refuses.
