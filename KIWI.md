# Kiwi — jMouse Framework

> Project-specific notes for the `kiwi` skill. Kiwi is the record; this file is notes.
> Fix it in the same response as any call that contradicts it.
> Verified: 2026-08-24

## Where this codebase writes

⚠️ **There is no `jMouse Framework` root section any more** — verified 2026-08-24, `categories_tree`
does not report one. Whatever held `Access` is gone with the installation it lived in.

- **Documents** — nothing here writes them at the moment. Create `jMouse Framework` as a root section
  when there is one to file, and put subsystems under it (`Access`, `Storage`, `Files`, `AI`).
- **Notes** `notes/jmouse-framework/<YYYY-MM-DD>` — thoughts, ideas, questions. See the `kiwi` skill's
  Notes section for what belongs there and what does not.
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

- `untitled-af62d5` — **Access conditions — the whole vocabulary, worked through.** Every function and
  every test a policy condition may use, worked through four invented installations that grow from one
  space to four. Mirrors `docs/access-conditions.md`, which is the record.

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
