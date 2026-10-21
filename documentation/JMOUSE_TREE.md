# jmouse-tree

A hierarchy kept as a nested set: a parent pointer on every node, and a numbered range beside it that holds the
ranges of everything beneath. The range is what makes "everything under this node" one indexed query — what an
authorization engine granting a subtree asks on every read. The pointer is the authority; the numbering is derived
from it and can always be rebuilt.

No dependencies. The persistence half is [jmouse-tree-jpa](JMOUSE_TREE_JPA.md).

## Responsibility map

| Type | Responsibility |
| --- | --- |
| [NestedSetNode](../jmouse-tree/src/main/java/org/jmouse/tree/NestedSetNode.java) | 🌳 What a node of a nested set is: `id`, `parentId`, `treeLeft`, `treeRight`, `depth`, `sortOrder` — getters and setters, because the host's entity carries them as attributes and a query names them so. `contains(other)` is the range test. |
| [NestedSetNumbering](../jmouse-tree/src/main/java/org/jmouse/tree/NestedSetNumbering.java) | 🔢 One tree's whole numbering rebuilt from its parent pointers: roots one after another, each subtree depth first, siblings in the order the host gives. Left bounds start at one, a root's depth is one. |
| [NestedSetPlace](../jmouse-tree/src/main/java/org/jmouse/tree/NestedSetPlace.java) | 📍 Where a new node goes — the parent, the bounds made free, the depth, the sibling order — handed to the host that builds the node. |
| [TreeException](../jmouse-tree/src/main/java/org/jmouse/tree/TreeException.java) | 🌳 A change refused because the result would not be a tree: a node moved beneath itself or into another tree, a pointer to nowhere, pointers in a circle. |

## Constraints worth preserving

- **Rebuilt, never patched.** A move is a new parent plus a renumbering of the whole tree from the pointers. Four
  hand-written range updates that must all be right are how a plausible-looking, wrong numbering is born — and a
  wrong numbering answers subtree questions wrongly, which where access hangs on subtrees is a silent
  authorization bug.
- **A whole tree, never part of one.** `NestedSetNumbering.number` is given every node of one tree. A node whose
  parent is not among them, or pointers going round in a circle, is refused rather than left unnumbered: an
  unnumbered node keeps the bounds it had, and answers range queries for a place it no longer stands in.
