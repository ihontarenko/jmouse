# Kiwi — jMouse Framework

> Project-specific notes for the `kiwi` skill. Kiwi is the record; this file is notes.
> Fix it in the same response as any call that contradicts it.
> Verified: 2026-08-25

## Where this codebase writes

⚠️ **There is no `jMouse Framework` root section** — `categories_tree` does not report one. Whatever held
`Access` is gone with the installation it lived in. What exists instead are two parallel trees, both
keyed on the product name at depth two:

- **Decisions** `decisions/jmouse-framework-2` — ⚠️ **note the `-2`.** Settled, current reference
  documents that Ivan reads rather than a record of a moment: no ticket references, no verbatim quotes,
  no cross-links to notes. Created 2026-08-25 at Ivan's request as the home for approved decisions.
  ⚠️ **The display path is `Decisions/jMouse Framework` and passing it is refused** — the slug carries a
  collision suffix because `notes/jmouse-framework` already claimed the plain one. Send
  `decisions/jmouse-framework-2`, always.
- **Notes** `notes/jmouse-framework/<YYYY-MM-DD>/<TYPE>` — thoughts, ideas, questions, decisions **as
  moments**, never edited afterwards. See the `kiwi-note` skill.
- **Documentation** `documentation/jmouse-framework-3/<set>` — ⚠️ **note the `-3`**, the same collision
  suffix story as Decisions' `-2`; the display path is `Documentation/jMouse Framework` and passing it is
  refused. One section per documented library or module; the first is
  `documentation/jmouse-framework-3/jmq`. Manuals: what a type is, how it is obtained, its exact
  signatures, its configuration, worked examples. No ticket keys, no quotes, no links to notes or
  decisions. See the `kiwi-documents` skill.
- ⚠️ **The difference is not size.** A note preserves what was thought on a day; a Decisions page is
  maintained and expected to be correct today. A ruling that changes gets a new note *and* an edit to
  the Decisions page.
- **Addressing** a page created **through the tool** gets a proper slug from its title —
  `jmq-as-a-real-query-engine-sql-in-anything-out-b11110`. The `untitled-<hex>` problem below is the
  BROWSER's, and only the browser's.

⚠️ **`parent` and `scope` take the SLUG path, not the display path.** `categories_tree` reports
`Notes/jMouse Framework`; passing that is refused with `UNKNOWN_SCOPE`, and `notes/jmouse-framework`
works. The refusal does list the slugs, so it is recoverable — but it costs a call every time.

⚠️ **The installation puts one root section per product** — `Tessera`, `Innoventa Manual`,
`Electronics Tools` — so `jMouse Framework` follows that, and the depth is two: product, then
subsystem. The `kiwi` skill's `Projects/<Product>/Documents/…` example is **not** what this
installation does.

## Mirror

`C:/Users/Ivan_Hontarenko/Git/jmouse/.kiwi/` — this file's own directory, one file per page, named for
the address.

⚠️ **Git: the mirror is not free here.** `Git/jmouse` **is** the repository root, so a mirror written
here lands straight in `git status`. It is ignored by `/.kiwi/` in `.gitignore`, beside the `/.tessera/`
line that was already there. Never remove either, and never commit a mirror. This file, being
documentation, is committed normally.

## Pages that matter

- http://localhost:5070/p/4c079f — **jMQ — the language and the engine.** The settled reference
  for jMQ: `structure` / `mapping` / `view`, one invented script carried through a database, a CSV file
  and an in-memory list, the `Translator<T>` seam, the expression catalogue, capabilities and the
  extension points. `Decisions/jMouse Framework`. ⚠️ Read this before re-deciding any of it; the four
  `jmq-*` notes under `Notes/jMouse Framework/` are how it was arrived at, not what it is.
- **The jMQ documentation set** — `documentation/jmouse-framework-3/jmq`, eight pages, written from the
  source and verified by running it. Start at the overview, which links the rest:
  - http://localhost:5070/p/450805 — **jMQ — overview** (what it is, the modules, what it does not do)
  - http://localhost:5070/p/685a66 — **Getting started** (coordinates; one script compiled to SQL and run over rows)
  - http://localhost:5070/p/d00772 — **The language** (`structure` / `mapping` / `source` / `view` / `function`, every clause)
  - http://localhost:5070/p/407b6b — **Expressions** (operators, tests, converters, aggregates, durations)
  - http://localhost:5070/p/0dd0ca — **Java API · reading and translating** (`jmouse-query`)
  - http://localhost:5070/p/6e0868 — **Java API · SQL** (`jmouse-query-sql`)
  - http://localhost:5070/p/c3cee4 — **Building a filter from rows** (`org.jmouse.query.compose`)
  - http://localhost:5070/p/454995 — **Saved queries** (`jmouse-query-store`)
  - http://localhost:5070/p/49dc08 — **Troubleshooting** (every refusal, reproduced)

- **The mapping documentation set** — `documentation/jmouse-framework-3/mapping` (no collision suffix on
  this one), four pages for `org.jmouse.core.mapping`, written from the source with every example
  compiled and run. Start at the overview, which links the rest:
  - http://localhost:5070/p/6ac1dd — **Mapping — overview** (what it is, the cost, what it will not do)
  - http://localhost:5070/p/0c74da — **Getting started** (one program that runs; what the defaults already handle)
  - http://localhost:5070/p/00ec29 — **Rules** (the per-pair DSL, and what a cycle actually produces)
  - http://localhost:5070/p/21a068 — **Policies and configuration** (six policies, eleven config keys, defaults)
  - http://localhost:5070/p/df6125 — **Java API reference** (every public type, exact signatures, and what is internal)

  ⚠️ These are the **manual**, and `http://localhost:5070/p/4c079f` is the **standard**. Where they
  disagree, the manual describes what the code does today and says so; the standard describes what it is
  to become. Both are current on purpose.
- ⚠️ `untitled-af62d5` — **Access conditions — the whole vocabulary, worked through.** Every function and
  every test a policy condition may use, worked through four invented installations that grow from one
  space to four. Mirrors `docs/access-conditions.md`, which is the record. **Verified 2026-08-25: this
  page no longer resolves and no search finds its title** — the mirror in `.kiwi/` is all that is left,
  and `docs/access-conditions.md` is the record either way. Recreate it from the mirror if it is wanted
  back, and put its short link here rather than its address.

## Local notes

- ⚠️ **Every page here is `untitled-<hex>`, and the address is permanent.** Both creation paths — the
  section's `New page` menu item and the top-level `New page` dialog — create the page **immediately**,
  with the title `Untitled` and no field to change it first. The address is minted at that moment and
  is never re-minted, so renaming afterwards leaves the address saying `untitled-…` forever. Nothing
  about this is recoverable except deleting and recreating, which produces another `untitled-…`.
- ⚠️ **The title commits on `Enter`, not on `Save`.** Typing a title and clicking `Save` saves the body
  and silently keeps the old title — the activity log says *Saved as "Untitled"*. Press `Enter` in the
  title field first, then save the body.
- ⚠️ **Setting the title from script does not work**, even with a native value setter plus `input` and
  `change` events. It has to be typed with real key events. The body, by contrast, can be set through
  CodeMirror directly: `document.querySelector('.cm-content').cmTile.view.dispatch({changes: …})`.
- ⚠️ **A long body cannot be typed.** The editor is CodeMirror 6 with bracket closing, so typing
  markdown corrupts it. Getting a 25 KB document in means dispatching a CM transaction, and getting the
  text into the page means serving it same-origin — `Kiwi/UI/public/` is served by Vite at the root, so
  a file copied there is fetchable as `/name.md` and deleted afterwards.
- ⚠️ **The tab freezes CDP briefly after opening a popper or a dialog** — a screenshot right after a
  click times out once and succeeds on the retry. Not a hang; retry once before diagnosing.
- ⚠️ **Never right-click in this UI while automating.** It opens Chrome's own context menu, which blocks
  every subsequent tool call until `Escape`. The row menus are ordinary buttons — find them by
  accessibility name (`Manage <section>`) rather than by coordinates.
