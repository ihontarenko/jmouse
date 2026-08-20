# Tessera — jMouse Framework

> Project-specific notes for the `tessera` skill. The tracker is the record; this file is notes.
> Fix it in the same response as any call that contradicts it.
> Verified: 2026-08-17

## Project

- **Key** `JMF` — every issue reads `JMF-<n>` forever; the key cannot be changed.
- **Name** `jMouse Framework` — this is the `scope` argument, verbatim, spaces and lower-case `j`
  included.
- **Lead** `SU`
- **Board** every open issue (not sprint-scoped)
- **Sprints** not used — `planning: board`.
- **Story points** used — set the field on every issue, epics excepted (see the skill's execution
  recommendation rule). `JMF-20` carries 8, `JMF-22`…`JMF-32` carry 2–8.

## Issue types

| Type | Used for |
|---|---|
| `Epic` | `JMF-1` — condition functions |
| `Story` | ordinary library work |
| `Bug` | a parser or formatting defect — these are the ones that bite silently |
| `Task`, `Sub-task` | legal, installation-wide |
| `UI changes` | legal but meaningless here — a library has no interface |

⚠️ **`Papercut` and `Nit` do not exist as rows yet**, installation-wide. Until they are created by hand
on `/administration` → Issue types, §1's "work nobody asked to file" rule files under `Task` here.

## Statuses and the path through them

`To Do` → `WIP` → `In Review` → `Done`

- ⚠️ The in-flight status is **`WIP`**, not "In Progress". `In Progress` exists in the installation's
  catalog but is not on this path, and naming it is refused.
- Finishing is **two transitions**, not one. Read `canMoveTo` before each.

## Resolutions

`Done`, `Won't Do`, `Duplicate`, `Cannot Reproduce` — required when moving into a Done status, ignored
everywhere else.

## Mirror

`C:/Users/Ivan_Hontarenko/Git/jmouse/.tessera/` — this file's own directory, one file per issue.

⚠️ **Git: the mirror is not free here.** `Git/jmouse` **is** the repository root, so a mirror written
here lands straight in `git status`. It is ignored by `/.tessera/` at the bottom of `.gitignore`. Never
remove that line, and never commit the mirror. This file, being documentation, is committed normally.

It lived at `jMouseProjects/.tesseraMirror/JMF/` until 2026-08-17, back when the framework was treated
as having no folder. Anything pointing there is stale, not a second copy.

## Prose that belongs to this project

- `../jMouseProjects/.scratch/jmouse-*/` — the extraction and library clusters (storage,
  storage-management, access, access-policy, ai, mcp-authorization). Most are finished and none are
  imported here.
- ⚠️ `jmouse-storage-management/spec.md` is the **prose half of epic `JMF-21`** — the decisions and
  their reasoning. The eleven tickets under that epic are deliberately thin without it.

## Local notes

- ⚠️ **The code is here; the products that consume it are not.** `Git/jmouse` is a sibling checkout of
  `Git/jMouseProjects`, so a session started in either one is missing half the picture. Paths between
  them are `../jmouse` and `../jMouseProjects`.
- ⚠️ **This project tracks libraries, not a deployed product.** Work that spans a library *and* the
  product consuming it is usually two tickets in two projects, and the library one lands first.
- The engine's expression gotchas (`in` precedence, one-element `in`, `minusDays`/null) return wrong
  answers **silently** rather than failing — a bug here is rarely a stack trace.
