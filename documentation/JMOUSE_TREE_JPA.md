# jmouse-tree-jpa

A nested set ([jmouse-tree](JMOUSE_TREE.md)) kept in a host's own table over Jakarta Persistence: the range reads
it exists for, and the writes that keep its numbering true. The host keeps its entity, its table, its columns and
its own rules — names, which names may stand side by side, what a move rewrites besides the shape. This module is
the shape alone.

The hosts are `jmouse-files-jpa` (`StorageDirectories`, one tree per owner key) and `jmouse-taxonomy-jpa` (`Taxonomies`, one tree per taxonomy).

## Responsibility map

| Type | Responsibility |
| --- | --- |
| [NestedSetMapping](../jmouse-tree-jpa/src/main/java/org/jmouse/tree/jpa/NestedSetMapping.java) | 🗺️ How a host's entity is a tree: the entity class, the attribute that separates one tree from the next (`ownerKey`), the same value read from a node, and the order siblings stand in when renumbered. |
| [NestedSetTree](../jmouse-tree-jpa/src/main/java/org/jmouse/tree/jpa/NestedSetTree.java) | 🌳 Reads: `find`, `subtreeOf`, `ancestorsOf` (the whole chain, root first), `childrenOf`, `roots`, `subtreesOf` (one query for many anchors, each node once), `subtreeHeight`. Writes: `insertRoot`, `insertUnder`, `move`, `delete`, `renumber`. |

## Adopting it

1. The entity implements `NestedSetNode`, with attributes named `id`, `parentId`, `treeLeft`, `treeRight`, `depth`,
   `sortOrder` — the columns behind them are the host's to choose — and one attribute naming its tree.
2. Build a `NestedSetMapping` once, and a `NestedSetTree` over the persistence context with it.
3. A new node is built by the host at the place the tree made: `tree.insertUnder(parent, place -> new Folder(…,
   place.parentId(), place.treeLeft(), place.treeRight(), place.depth(), place.sortOrder()))`.
4. Whatever else a move changes in the host's row (a path, a slug) is set on the node before `tree.move`, and is
   flushed with it.

## Constraints worth preserving

- **Every write runs inside the caller's transaction.** None is opened here; a renumbering is only safe inside one.
- **Every write clears the persistence context and returns the node read anew.** Insertion shifts bounds with bulk
  statements that bypass the context, so anything loaded before a write carries stale numbers — a caller keeping
  one writes them back. Pending changes are flushed before.
- **Every read and write is bounded by the scope.** Trees in one table are numbered apart; without the bound one
  person adding a node renumbers everyone's tree, and a range read answers with somebody else's nodes.
- **A move never crosses trees, never lands beneath itself** (`TreeException`). A host refuses first in its own
  words; this is the floor beneath it.
- **Whether a node with children may be deleted is the host's question.** `delete` removes the whole subtree it is
  given.
