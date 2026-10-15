# jmx-textmate

Syntax highlighting for jMX — the jMouse document language (`.jmx`): every kind of document a catalogue holds, from
`curve`, `surface`, `dynamics`, `iteration`, `region` and `polytope` to `component`, `circuit`, `symbol`, `family`,
`functions`, `category`, `collection` and `course`, with their titles, `as slug` and a component's `type role`.

What it colours:

- comments `# …`, strings `"…"` and `'…'`, and `""" … """` text that spans lines;
- the top-level lines `family ref`, `units A, B`, `uses a, b`, and `meta { }` with its known fields (`summary`,
  `description`, `category`, `tags`, `source`, `authors`, `license`, `links { "label" "url" }`) and its free ones;
- every block of `Kind.java`: `parameters` and their flags (`in`, `step`, `log`, `integer`, `animate`, `live`,
  `per unit`), `inputs`, `outputs` and their types, `formulas` (jMF definitions, or the slugs of formula sets),
  `state`, `equations` (`d(x)/dt =`, `x_next =`, `x_start =`, `current a -> b =`, `voltage a -> b =`, `0 =`, `local name =`,
  `d(q)/dt`; a pin's name is its voltage), `view` (representations, `primary`, `preset "…" { }`, `camera`, `explain`, `without`), `expect` with its
  `bench`, `measure` and criterion (`near … +- …`, `within … .. …`, `is …`), `pins`, `nodes`, `symbol` in its three
  forms, `parts`, `layout` (`at x, y rotate`, `ground`, `junction`, `wire A.pin to B.pin via x y`), `nets`, `ports`, `probes`, `analysis`, `sheet`, `maps`, `axiom`, `rules`, `turtle`, `steps`,
  `vertices`, `edges`, `points`, `items` and a course's `step "…" { }`; a `graph`'s `clock t from … to … speed … loop`,
  `panes rows|columns|grid { name plane|space|circuit|values … }` (`x`, `y`, `z`, `uses`, `over`, `camera`) and `lines`
  (a line's name, a label `"… {value} …"`, `until`, `at`, `in pane`, `color n`, `hidden`);
- values with their units as one token pair — `2.52 nA`, `0.3 V/us`, `2 mA/V^2`, `3 1/s`, `10 kOhm`, `10 kΩ`,
  `52.4%`, `90 deg`, `25 °C` — ranges `a .. b`, tolerances `+- 5%` and the imaginary unit after a number (`1.5i`).

jMX is a document language around two embedded ones, and the grammar is built the same way: everything inside
`behaviour` and `run` is highlighted by jMS's own rules, copied from `jmouse-script-el/tools/jms-textmate` with the
scope renamed to `source.jmx`. **No jMX word is reserved there** — `points`, `state`, `view` and `set` are ordinary
jMS names inside a jMS body, exactly as the reader treats them. Everywhere else an expression stands, it is coloured
as jMF.

The truth about the syntax is the reader in `org.jmouse.parallax.script` (`Kind`, `DocumentReader`,
`DeclarationReader`, `CircuitReader`, `SymbolReader`, `PresentationReader`, `StructureReader`, `Quantity`,
`ExperienceNotation`). When a word list changes there, it changes in `generate.js`.

⚠️ The grammar is generated, not hand-edited: when jMS's grammar changes, regenerate this one rather than patching
both: `node jmouse-parallax/tools/jmx-textmate/generate.js jmouse-script-el/tools/jms-textmate jmouse-parallax/tools/jmx-textmate`
from the jmouse root. It reads `jms.tmLanguage.json`, adds the jMX rules and writes `syntaxes/jmx.tmLanguage.json`,
`package.json` and `language-configuration.json`. It refuses to run if a jMX rule would overwrite a jMS one.

It colours; it does not decide. Whether a block is allowed in a kind, whether a reference exists, whether a unit adds
up — the reader and the engines answer that when the file loads.

Install as any local VS Code / IntelliJ TextMate bundle: point the editor at this directory.

- JetBrains IDEs: Settings → Editor → TextMate Bundles → **+** → this directory.
- VS Code: link the directory into the extensions folder and reload the window:

  ```powershell
  New-Item -ItemType SymbolicLink `
    -Path   "$env:USERPROFILE\.vscode\extensions\jmx-textmate" `
    -Target (Resolve-Path jmouse-parallax\tools\jmx-textmate)
  ```
