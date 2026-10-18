# jmouse-files

Small file-management vocabulary independent of persistence: directory paths and slugs, typed directory-configuration kinds, upload configuration and owner references, plus explicit domain refusals. Actual directory/file/binding rows live in jmouse-files-jpa and bytes in the storage modules.

This is an orientation map of the implementation. The linked source is authoritative when an API or behavior changes.

## Module boundary

The build and declared dependencies are in [pom.xml](../jmouse-files/pom.xml). Direct dependency declarations include `jmouse-storage`, `jmouse-core`, `slf4j-api`, `logback-classic`.

The entry points below are the major contracts, not an assertion that every class is an application service. Internal helpers and executable examples can be followed from their source packages.

## Responsibility map

| Type | Responsibility |
| --- | --- |
| [DirectoryPath](../jmouse-files/src/main/java/org/jmouse/files/directory/DirectoryPath.java) | 🧭 A place in the tree, written the way a person writes one: innoventa/files. |
| [DirectorySlugs](../jmouse-files/src/main/java/org/jmouse/files/directory/DirectorySlugs.java) | 🏷️ A directory's name, as something that can appear in a path and a storage key. Both products that grew a tree of their own wrote this, and wrote it the same way, because the requirement is the same: a person types "Річні звіти 2026" and the address has to survive being put in a URL, a bucket key and a log line. |
| [DirectoryConfigurationKind](../jmouse-files/src/main/java/org/jmouse/files/directory/DirectoryConfigurationKind.java) | 🔧 One kind of thing a folder can be configured to do, and the shape of that configuration. A directory carries at most one configuration per kind, and a kind is contributed by whoever cares about it: upload says what may enter, and a later retention or naming would say something else entirely. The point of naming them rather than adding columns is that the second kind costs a record and a registry entry, and no migration… |
| [DirectoryConfigurationKinds](../jmouse-files/src/main/java/org/jmouse/files/directory/DirectoryConfigurationKinds.java) | 🗂️ Every kind of configuration a folder may carry, and the one place a name is checked against. |
| [DirectoryUploadConfiguration](../jmouse-files/src/main/java/org/jmouse/files/directory/DirectoryUploadConfiguration.java) | 🛃 What a folder accepts — the upload kind of directory configuration. A folder carrying one of these replaces the installation's acceptance policy for everything filed into it and into everything beneath it that carries none of its own. It does not add to it, and it does not merge with an ancestor's: the nearest configuration wins entirely, because a rule assembled out of four ancestors is a rule nobody can read off one… |
| [EffectiveUploadRule](../jmouse-files/src/main/java/org/jmouse/files/directory/EffectiveUploadRule.java) | 🛃 The upload rule that actually applies to a folder, as a screen has to draw it. DirectoryUploadConfiguration is what somebody wrote ; this is what a folder gets — after inheritance, with the installation's size limit filled in where the folder stated none, and with the one fact a screen cannot work out for itself already computed. |
| [OwnerReference](../jmouse-files/src/main/java/org/jmouse/files/OwnerReference.java) | 🔗 What a file is filed against — a kind and an identifier, and nothing else. |
| [DirectoryException](../jmouse-files/src/main/java/org/jmouse/files/exception/DirectoryException.java) | 🌳 Something about the directory tree could not be done. A name that will not fit, a move that would put a directory inside itself, a depth beyond what the installation allows, a root asked to be renamed. Every one of them is a refusal rather than a failure — the tree is left exactly as it was. |
| [FileBindingException](../jmouse-files/src/main/java/org/jmouse/files/exception/FileBindingException.java) | 🔗 A binding that could not be made, or could not be read as one. Raised before anything is written, so a refused binding leaves no half-filed row behind. |
| [FileHeldException](../jmouse-files/src/main/java/org/jmouse/files/exception/FileHeldException.java) | 🔒 Something in the product is holding this file, so it may not stop being reachable. |
| [ManagedFileNotFoundException](../jmouse-files/src/main/java/org/jmouse/files/exception/ManagedFileNotFoundException.java) | 🔍 No managed file under that identifier. ⚠️ Distinct from the storage layer's ObjectNotFoundException, and the difference is worth keeping: this one means the row is missing, that one means the row exists and the bytes it points at do not. They arrive at a caller as the same 404, but they mean opposite things to whoever is asked to fix it. |
| [ManagedFileTooLargeException](../jmouse-files/src/main/java/org/jmouse/files/exception/ManagedFileTooLargeException.java) | 📏 The file is real and readable, and reading it whole is more than the caller asked to be handed. |
| [RemoteFetchException](../jmouse-files/src/main/java/org/jmouse/files/exception/RemoteFetchException.java) | 🌐 A file could not be fetched from the address it was asked for. Every reason is the caller's or the remote server's rather than this installation's: an address that does not parse, a host that does not resolve, a host that resolves inside this network, an error status, an HTML page where a file was expected, or a declared size over the limit. All of them are a bad request, and all of them say which. |

## Main flow

- Parse a human-readable path into the directory vocabulary. Its first segment identifies the application, making similarly named roots in different products distinguishable. ([DirectoryPath](../jmouse-files/src/main/java/org/jmouse/files/directory/DirectoryPath.java))

- Normalize a person-entered directory name into the shared slug spelling used in paths and storage keys. ([DirectorySlugs](../jmouse-files/src/main/java/org/jmouse/files/directory/DirectorySlugs.java))

- Declare a kind name and the type/shape of the configuration document allowed under that name. ([DirectoryConfigurationKind](../jmouse-files/src/main/java/org/jmouse/files/directory/DirectoryConfigurationKind.java))

- Collect known configuration kinds and reject unknown names rather than letting persistence accumulate untyped payloads. ([DirectoryConfigurationKinds](../jmouse-files/src/main/java/org/jmouse/files/directory/DirectoryConfigurationKinds.java))

- Describe a folder-specific upload rule to replace the inherited admission rule below that folder. ([DirectoryUploadConfiguration](../jmouse-files/src/main/java/org/jmouse/files/directory/DirectoryUploadConfiguration.java))

- Present the resolved rule together with inheritance and the installation size ceiling, rather than mistaking the locally written document for the effective answer. ([EffectiveUploadRule](../jmouse-files/src/main/java/org/jmouse/files/directory/EffectiveUploadRule.java))

## Adoption and extension points

- Use a product-chosen kind plus opaque identifier for the thing holding a file. The library deliberately has no enum of every product entity. ([OwnerReference](../jmouse-files/src/main/java/org/jmouse/files/OwnerReference.java))

- Contribute another typed directory configuration kind for a consuming module; do not add unrelated nullable columns for every future feature. ([DirectoryConfigurationKind](../jmouse-files/src/main/java/org/jmouse/files/directory/DirectoryConfigurationKind.java))

- Bind and validate folder admission configuration through the declared kind before passing it to persistence. ([DirectoryUploadConfiguration](../jmouse-files/src/main/java/org/jmouse/files/directory/DirectoryUploadConfiguration.java))

- Map these domain refusal types at the product/framework boundary rather than translating every failure to a storage backend error. ([DirectoryException](../jmouse-files/src/main/java/org/jmouse/files/exception/DirectoryException.java))

## Constraints worth preserving

- A held file refusal expresses a product reference/retention rule even when the user has permission. It is not an authorization denial. ([FileHeldException](../jmouse-files/src/main/java/org/jmouse/files/exception/FileHeldException.java))

- Missing managed metadata and missing bytes are distinct failures. The storage ObjectNotFoundException answers the latter question. ([ManagedFileNotFoundException](../jmouse-files/src/main/java/org/jmouse/files/exception/ManagedFileNotFoundException.java))

- Whole-file reads that exceed the caller limit are refused; silently returning a truncated value would hide data loss. ([ManagedFileTooLargeException](../jmouse-files/src/main/java/org/jmouse/files/exception/ManagedFileTooLargeException.java))

- Remote import failures include address/network/refused-host cases and remote-server behavior. They are expressed separately from a local backend failure. ([RemoteFetchException](../jmouse-files/src/main/java/org/jmouse/files/exception/RemoteFetchException.java))

- The application prefix is part of the path contract. Removing it makes product roots collide semantically. ([DirectoryPath](../jmouse-files/src/main/java/org/jmouse/files/directory/DirectoryPath.java))


## Source navigation

- [`org.jmouse.files.directory`](../jmouse-files/src/main/java/org/jmouse/files/directory/)
- [`org.jmouse.files.exception`](../jmouse-files/src/main/java/org/jmouse/files/exception/)
- [`org.jmouse.files`](../jmouse-files/src/main/java/org/jmouse/files/)

## Source reference

Direct dependencies below come from this module's [POM](../jmouse-files/pom.xml); test-only dependencies are omitted. Optional dependencies still require the adopter to supply the feature at runtime.

| Dependency | Declared scope | Optional |
|---|---|---|
| `org.jmouse:jmouse-storage` | compile | no |
| `org.jmouse:jmouse-core` | compile | no |
| `org.slf4j:slf4j-api` | compile | no |
| `ch.qos.logback:logback-classic` | compile | yes |

### Public type map by package

Top-level public types in production source, excluding smoke/example drivers. Links point to the implementation; roles summarize source JavaDoc or the declared contract. Nested builders, records and enum members belong to their enclosing type.

#### `org.jmouse.files`

| Type | Responsibility / contract |
|---|---|
| [OwnerReference](../jmouse-files/src/main/java/org/jmouse/files/OwnerReference.java) | 🔗 What a file is filed against — a kind and an identifier, and nothing else. |

#### `org.jmouse.files.directory`

| Type | Responsibility / contract |
|---|---|
| [DirectoryConfigurationKind](../jmouse-files/src/main/java/org/jmouse/files/directory/DirectoryConfigurationKind.java) | 🔧 One kind of thing a folder can be configured to do, and the shape of that configuration. |
| [DirectoryConfigurationKinds](../jmouse-files/src/main/java/org/jmouse/files/directory/DirectoryConfigurationKinds.java) | 🗂️ Every kind of configuration a folder may carry, and the one place a name is checked against. |
| [DirectoryPath](../jmouse-files/src/main/java/org/jmouse/files/directory/DirectoryPath.java) | 🧭 A place in the tree, written the way a person writes one: innoventa/files. |
| [DirectorySlugs](../jmouse-files/src/main/java/org/jmouse/files/directory/DirectorySlugs.java) | 🏷️ A directory's name, as something that can appear in a path and a storage key. |
| [DirectoryUploadConfiguration](../jmouse-files/src/main/java/org/jmouse/files/directory/DirectoryUploadConfiguration.java) | 🛃 What a folder accepts — the upload kind of directory configuration. |
| [EffectiveUploadRule](../jmouse-files/src/main/java/org/jmouse/files/directory/EffectiveUploadRule.java) | 🛃 The upload rule that actually applies to a folder, as a screen has to draw it. |

#### `org.jmouse.files.exception`

| Type | Responsibility / contract |
|---|---|
| [DirectoryException](../jmouse-files/src/main/java/org/jmouse/files/exception/DirectoryException.java) | 🌳 Something about the directory tree could not be done. |
| [FileBindingException](../jmouse-files/src/main/java/org/jmouse/files/exception/FileBindingException.java) | 🔗 A binding that could not be made, or could not be read as one. |
| [FileHeldException](../jmouse-files/src/main/java/org/jmouse/files/exception/FileHeldException.java) | 🔒 Something in the product is holding this file, so it may not stop being reachable. |
| [ManagedFileNotFoundException](../jmouse-files/src/main/java/org/jmouse/files/exception/ManagedFileNotFoundException.java) | 🔍 No managed file under that identifier. |
| [ManagedFileTooLargeException](../jmouse-files/src/main/java/org/jmouse/files/exception/ManagedFileTooLargeException.java) | 📏 The file is real and readable, and reading it whole is more than the caller asked to be handed. |
| [RemoteFetchException](../jmouse-files/src/main/java/org/jmouse/files/exception/RemoteFetchException.java) | 🌐 A file could not be fetched from the address it was asked for. |

### Verification boundary

This documentation was checked against the current source tree. No Java build, database migration, external API call or runtime scenario is claimed by this documentation change.
