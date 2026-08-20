# WiQi — jMouse Framework

> Project-specific notes for the `wiqi` skill. WiQi is the record; this file is notes.
> Fix it in the same response as any call that contradicts it.
> Verified: 2026-08-18

## Where this codebase writes

- **Section** `projects/jmouse-framework/documents-3` — the `scope` argument, **verbatim**.
- **Addressing** slug with a short id → `tool-permissions-d83acd`.
- **Subsections in use** none yet. Add an `ADR` child if the framework ever mints numbered decisions;
  it has no `docs/adr/` today.

## ⚠️ Paths are SLUGS, not the display names the tree prints

`categories_tree` reports a `path` that reads like the display name — `Projects/WiQi` — but every action
matches the **slug** path, and a call sent the pretty one is refused as an unknown scope:

```
categories_create(name: "Documents", parent: "Projects/jMouse Framework")
  → refused: UNKNOWN_SCOPE. It can see: projects/jmouse-framework, projects/wiq/documents, …
```

Read the slugs out of the **refusal**, or out of what `categories_create` answers, and send those. The
refusal listing every visible section is the reliable source.

## ⚠️ A section's slug is globally unique, so a name can arrive numbered

Creating `Documents` under `Projects/jMouse Framework` minted **`documents-3`**, because two other
branches already held a `Documents`. The *display name* is still `Documents`; only the slug counts up.
Do not treat the number as a mistake and do not try to create it again to get a nicer one.

## Mirror

`C:/Users/Ivan_Hontarenko/Git/jmouse/.wiqi/` — one file per page, named for its address.

⚠️ **Git: the mirror is not free here.** `Git/jmouse` **is** the repository root, so a mirror written
here lands straight in `git status`. It is ignored by `/.wiqi/` at the bottom of `.gitignore`, added
2026-08-18 beside the existing `/.tessera/`. Never remove either line, and never commit either mirror.
This file, being documentation, is committed normally.

## Pages that matter

- `tool-permissions-d83acd` — **Tool permissions · Пермішини тулів.** How an agent is authorized in
  every jMouse product: one tool = one permission, derived from the action's own name, declared only in
  `policy/tools.jmp`; what `INHERITED` and `RESTRICTED` actually mean; and what decides *which rows* as
  opposed to *which action*. Bilingual. ⚠️ Mirrors `jmouse/docs/tool-permissions.md`, which is the copy
  that survives WiQi being down — change both.

## Local notes

- ⚠️ **The code is here; the products that consume it are not.** `Git/jmouse` is a sibling checkout of
  `Git/jMouseProjects`, so a page about how a product *uses* the framework may belong in that product's
  section instead. The rule: if the mechanism lives in the library, it is filed here.
- **Authorship is the agent's, authorization is the owner's.** A page written through the protocol is
  attributed to `Claude Code (wiq) · this machine` rather than to the person who approved the client —
  that is `WIQ-14` working as designed, not a mix-up.
