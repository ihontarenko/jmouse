# jm-textmate

Syntax highlighting for the jmCAD document language (`.jm`): `component`, `circuit`, `family`, `functions`,
`category`, `symbol`, `portrait`, `frame`, `footprint` and `pcb`, in both of its spellings — blocks
(`circuit "RC" as rc { parts { … } }`) and terms (`component:npn-transistor("NPN transistor", type: bjt) { … }`).

## ⚠️ Generated from the language, never edited

Nothing in `syntaxes/jm.tmLanguage.json` is listed by hand. `TextMateGeneration` (in `jmouse-cad-library`) walks the
language the reader reads — the kinds and blocks every `DocumentModule` brings, each block's head and the lines its body
holds, every line's word and its clauses, the words its values may be, the lines that open with a name, the equations
jMF reads — and the units table (`jmouse-units`) for quantities. So the grammar cannot drift from the reader: change a
`LineGrammar`, and `check` says the bundle is stale.

From the jmouse root, with the library module's classpath:

```
java -cp "jmouse-cad-library/target/classes;<its dependencies>" org.jmouse.cad.library.TextMateGeneration \
     write jmouse-script-el/tools/jms-textmate jmouse-cad-library/tools/jm-textmate

java … org.jmouse.cad.library.TextMateGeneration check jmouse-script-el/tools/jms-textmate jmouse-cad-library/tools/jm-textmate
```

`check` writes nothing and names every stale file.

jMS's own grammar is the base: every rule of `jms.tmLanguage.json`, its scope renamed to `.jm`, so `behaviour` and `run`
bodies are coloured by jMS's rules unchanged; a document rule that would take a jMS rule's name is refused.

It colours; it does not decide. Whether a block belongs in a kind, whether a reference exists, whether a unit adds up —
the reader and the engines answer that when the file loads.

## Installing

- JetBrains IDEs: Settings → Editor → TextMate Bundles → **+** → this directory.
- VS Code: link this directory into the extensions folder and reload the window:

  ```powershell
  New-Item -ItemType SymbolicLink `
    -Path   "$env:USERPROFILE\.vscode\extensions\jm-textmate" `
    -Target (Resolve-Path jmouse-cad-library\tools\jm-textmate)
  ```

⚠️ jmouse-parallax's `jmx-textmate` claims `.jm` too; install one or the other.
