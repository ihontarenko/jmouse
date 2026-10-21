# jmouse-files-jpa

Jakarta Persistence file metadata, polymorphic bindings and the library directory tree. A ManagedFile is the person-facing name/uploader around a StoredFile; FileBinding says where it is filed; StorageDirectory provides an indexed nested-set hierarchy. These are separate facts and tables.

This is an orientation map of the implementation. The linked source is authoritative when an API or behavior changes.

## Module boundary

The build and declared dependencies are in [pom.xml](../jmouse-files-jpa/pom.xml). Direct dependency declarations include `jmouse-files`, `jmouse-storage-jpa`, `jakarta.persistence-api`, `jackson-databind`, `slf4j-api`, `logback-classic`.

The entry points below are the major contracts, not an assertion that every class is an application service. Internal helpers and executable examples can be followed from their source packages.

## Responsibility map

| Type | Responsibility |
| --- | --- |
| [ManagedFile](../jmouse-files-jpa/src/main/java/org/jmouse/files/jpa/ManagedFile.java) | 📄 A file as a person meets it: a name, an uploader, and the bytes behind it. What it is not It is not the bytes — StoredFile is, and one of those may back several of these. It is not a place either: where a file sits lives in FileBinding, so that a product filing into a directory tree and a product attaching to an issue use one table rather than two shapes. |
| [ManagedFiles](../jmouse-files-jpa/src/main/java/org/jmouse/files/jpa/ManagedFiles.java) | 📄 The file rows: recording one, finding one, renaming one, removing one. What it deliberately does not do It does not take bytes into storage. That is StoredFileIngestion's, and keeping the two apart is what lets a product ingest once and record several times — a document uploaded twice by two people costs one stored object and two of these rows, each with its own name and uploader. It does not authorize anything. Every… |
| [FileBinding](../jmouse-files-jpa/src/main/java/org/jmouse/files/jpa/FileBinding.java) | 🔗 One statement that a file is filed against something. |
| [FileBindingId](../jmouse-files-jpa/src/main/java/org/jmouse/files/jpa/FileBindingId.java) | 🔑 What identifies a binding: the file, and the thing it is filed against. |
| [FileBindings](../jmouse-files-jpa/src/main/java/org/jmouse/files/jpa/FileBindings.java) | 🔗 Filing, unfiling and re-filing — the whole of what a place means here. Transaction demarcation is the caller's, as everywhere else in these libraries. Nothing here opens one, and every method is written to be correct inside whatever transaction the product's own service already started. |
| [StorageDirectory](../jmouse-files-jpa/src/main/java/org/jmouse/files/jpa/directory/StorageDirectory.java) | 🌳 A folder in the library's own tree — the one files are filed into. |
| [StorageDirectories](../jmouse-files-jpa/src/main/java/org/jmouse/files/jpa/directory/StorageDirectories.java) | 🌳 The tree: resolving a root, making a folder, moving one, reading a subtree. |
| [StorageDirectoryConfiguration](../jmouse-files-jpa/src/main/java/org/jmouse/files/jpa/directory/StorageDirectoryConfiguration.java) | 🔧 One folder's answer to one question — a typed configuration document, filed by kind. |
| [StorageDirectoryConfigurations](../jmouse-files-jpa/src/main/java/org/jmouse/files/jpa/directory/StorageDirectoryConfigurations.java) | 🔧 Reading and writing what a folder says about itself. Two reads matter, and the second is why this class exists rather than a repository method: one kind for one directory, and one kind across a list of directories in a single query . Inheritance walks a whole ancestor chain on every upload, and doing that a row at a time is the N+1 that turns a cache from a nicety into the only thing holding the feature up. Transaction… |
| [FilesMigrations](../jmouse-files-jpa/src/main/java/org/jmouse/files/jpa/migration/FilesMigrations.java) | 🚚 Where this library's schema lives, and under what history. Its own history table, separate from the product's and separate from storage_schema_history — the same reasoning that gave storage one. Products in this workspace number their migrations in ranges that do not fit together, and a shared history would force either a reserved range held forever by convention or somebody renumbering a schema that has already shipped. |

## Main flow

- Record a managed file using an already stored registry row. This service does not ingest bytes; call StoredFileIngestion before recording metadata where the product operation needs both. ([ManagedFiles](../jmouse-files-jpa/src/main/java/org/jmouse/files/jpa/ManagedFiles.java))

- bind, unbind and refile associate the same file with an OwnerReference. One file may be filed against several things, which is why location is not a column on ManagedFile. ([FileBindings](../jmouse-files-jpa/src/main/java/org/jmouse/files/jpa/FileBindings.java))

- Resolve roots/paths, create folders, rename or move subtrees through the service so nested-set indices and paths stay coherent. ([StorageDirectories](../jmouse-files-jpa/src/main/java/org/jmouse/files/jpa/directory/StorageDirectories.java))

- Containment uses treeLeft/treeRight and a shared owner key; paths and parent identifiers supplement the indexed tree. ([StorageDirectory](../jmouse-files-jpa/src/main/java/org/jmouse/files/jpa/directory/StorageDirectory.java))

- Find one typed configuration or batch-read the kind over several directories. Write validated payloads and clear/delete configurations through the service. ([StorageDirectoryConfigurations](../jmouse-files-jpa/src/main/java/org/jmouse/files/jpa/directory/StorageDirectoryConfigurations.java))

- Retain private-file state, a held reason and trash metadata alongside the display name and stored-file reference. ManagedFiles exposes metadata lookup, rename, listing and removal. ([ManagedFile](../jmouse-files-jpa/src/main/java/org/jmouse/files/jpa/ManagedFile.java))

## Adoption and extension points

- Use the library files schema and independent files_schema_history with the storage dialect resolver. The file schema is separate from the byte registry schema. ([FilesMigrations](../jmouse-files-jpa/src/main/java/org/jmouse/files/jpa/migration/FilesMigrations.java))

- Supply an EntityManager and identifier supplier. Directory writes and binding changes participate in the caller transaction. ([StorageDirectories](../jmouse-files-jpa/src/main/java/org/jmouse/files/jpa/directory/StorageDirectories.java))

- Provide the registered DirectoryConfigurationKinds and the binder/serialization collaborators accepted by the constructors. ([StorageDirectoryConfigurations](../jmouse-files-jpa/src/main/java/org/jmouse/files/jpa/directory/StorageDirectoryConfigurations.java))

- Use ownersOfAll when rendering many files rather than performing one owner query per row. ([FileBindings](../jmouse-files-jpa/src/main/java/org/jmouse/files/jpa/FileBindings.java))

## Constraints worth preserving

- The nested set itself — ranges, insertion, moves, deletion, renumbering — is [jmouse-tree-jpa](JMOUSE_TREE_JPA.md); StorageDirectories keeps roots, names, slugs, paths, the depth limit and its refusals.

- Every tree write renumbers the nested set. Bypassing the service with individual parent-column changes breaks subtree and ancestor range queries. ([StorageDirectories](../jmouse-files-jpa/src/main/java/org/jmouse/files/jpa/directory/StorageDirectories.java))

- The declared maximum depth is eight. Root handling and move validation are directory-domain constraints, not storage-key formatting concerns. ([StorageDirectory](../jmouse-files-jpa/src/main/java/org/jmouse/files/jpa/directory/StorageDirectory.java))

- The binding identity is the file plus owner kind plus owner identifier. There is no independent binding identifier to hand out. ([FileBindingId](../jmouse-files-jpa/src/main/java/org/jmouse/files/jpa/FileBindingId.java))

- Removing a managed row is not a promise to delete backing bytes immediately; registry reference accounting and the orphan sweeper own byte reclamation. ([ManagedFiles](../jmouse-files-jpa/src/main/java/org/jmouse/files/jpa/ManagedFiles.java))

- This is the file filing tree, separate from a product category tree. Reusing category rows would conflate two independent hierarchies. ([StorageDirectory](../jmouse-files-jpa/src/main/java/org/jmouse/files/jpa/directory/StorageDirectory.java))


## Source navigation

- [`org.jmouse.files.jpa.directory`](../jmouse-files-jpa/src/main/java/org/jmouse/files/jpa/directory/)
- [`org.jmouse.files.jpa`](../jmouse-files-jpa/src/main/java/org/jmouse/files/jpa/)
- [`org.jmouse.files.jpa.migration`](../jmouse-files-jpa/src/main/java/org/jmouse/files/jpa/migration/)

Resource entry points:

- [V000001__create_managed_files.sql](../jmouse-files-jpa/src/main/resources/db/files/mysql/V000001__create_managed_files.sql)
- [V000002__managed_files_held_reason.sql](../jmouse-files-jpa/src/main/resources/db/files/mysql/V000002__managed_files_held_reason.sql)
- [V000003__create_storage_directory_configurations.sql](../jmouse-files-jpa/src/main/resources/db/files/mysql/V000003__create_storage_directory_configurations.sql)
- [V000004__managed_files_trash.sql](../jmouse-files-jpa/src/main/resources/db/files/mysql/V000004__managed_files_trash.sql)
- [V000001__create_managed_files.sql](../jmouse-files-jpa/src/main/resources/db/files/postgresql/V000001__create_managed_files.sql)
- [V000002__managed_files_held_reason.sql](../jmouse-files-jpa/src/main/resources/db/files/postgresql/V000002__managed_files_held_reason.sql)
- [V000003__create_storage_directory_configurations.sql](../jmouse-files-jpa/src/main/resources/db/files/postgresql/V000003__create_storage_directory_configurations.sql)
- [V000004__managed_files_trash.sql](../jmouse-files-jpa/src/main/resources/db/files/postgresql/V000004__managed_files_trash.sql)

## Source reference

Direct dependencies below come from this module's [POM](../jmouse-files-jpa/pom.xml); test-only dependencies are omitted. Optional dependencies still require the adopter to supply the feature at runtime.

| Dependency | Declared scope | Optional |
|---|---|---|
| `org.jmouse:jmouse-files` | compile | no |
| `org.jmouse:jmouse-storage-jpa` | compile | no |
| `jakarta.persistence:jakarta.persistence-api` | compile | no |
| `com.fasterxml.jackson.core:jackson-databind` | compile | no |
| `org.slf4j:slf4j-api` | compile | no |
| `ch.qos.logback:logback-classic` | compile | yes |

### Public type map by package

Top-level public types in production source, excluding smoke/example drivers. Links point to the implementation; roles summarize source JavaDoc or the declared contract. Nested builders, records and enum members belong to their enclosing type.

#### `org.jmouse.files.jpa`

| Type | Responsibility / contract |
|---|---|
| [FileBinding](../jmouse-files-jpa/src/main/java/org/jmouse/files/jpa/FileBinding.java) | 🔗 One statement that a file is filed against something. |
| [FileBindingId](../jmouse-files-jpa/src/main/java/org/jmouse/files/jpa/FileBindingId.java) | 🔑 What identifies a binding: the file, and the thing it is filed against. |
| [FileBindings](../jmouse-files-jpa/src/main/java/org/jmouse/files/jpa/FileBindings.java) | 🔗 Filing, unfiling and re-filing — the whole of what a place means here. |
| [ManagedFile](../jmouse-files-jpa/src/main/java/org/jmouse/files/jpa/ManagedFile.java) | 📄 A file as a person meets it: a name, an uploader, and the bytes behind it. |
| [ManagedFiles](../jmouse-files-jpa/src/main/java/org/jmouse/files/jpa/ManagedFiles.java) | 📄 The file rows: recording one, finding one, renaming one, removing one. |

#### `org.jmouse.files.jpa.directory`

| Type | Responsibility / contract |
|---|---|
| [StorageDirectories](../jmouse-files-jpa/src/main/java/org/jmouse/files/jpa/directory/StorageDirectories.java) | 🌳 The tree: resolving a root, making a folder, moving one, reading a subtree. |
| [StorageDirectory](../jmouse-files-jpa/src/main/java/org/jmouse/files/jpa/directory/StorageDirectory.java) | 🌳 A folder in the library's own tree — the one files are filed into. |
| [StorageDirectoryConfiguration](../jmouse-files-jpa/src/main/java/org/jmouse/files/jpa/directory/StorageDirectoryConfiguration.java) | 🔧 One folder's answer to one question — a typed configuration document, filed by kind. |
| [StorageDirectoryConfigurations](../jmouse-files-jpa/src/main/java/org/jmouse/files/jpa/directory/StorageDirectoryConfigurations.java) | 🔧 Reading and writing what a folder says about itself. |

#### `org.jmouse.files.jpa.migration`

| Type | Responsibility / contract |
|---|---|
| [FilesMigrations](../jmouse-files-jpa/src/main/java/org/jmouse/files/jpa/migration/FilesMigrations.java) | 🚚 Where this library's schema lives, and under what history. |

### Packaged resources

- [db/files/mysql/V000001__create_managed_files.sql](../jmouse-files-jpa/src/main/resources/db/files/mysql/V000001__create_managed_files.sql).
- [db/files/mysql/V000002__managed_files_held_reason.sql](../jmouse-files-jpa/src/main/resources/db/files/mysql/V000002__managed_files_held_reason.sql).
- [db/files/mysql/V000003__create_storage_directory_configurations.sql](../jmouse-files-jpa/src/main/resources/db/files/mysql/V000003__create_storage_directory_configurations.sql).
- [db/files/mysql/V000004__managed_files_trash.sql](../jmouse-files-jpa/src/main/resources/db/files/mysql/V000004__managed_files_trash.sql).
- [db/files/postgresql/V000001__create_managed_files.sql](../jmouse-files-jpa/src/main/resources/db/files/postgresql/V000001__create_managed_files.sql).
- [db/files/postgresql/V000002__managed_files_held_reason.sql](../jmouse-files-jpa/src/main/resources/db/files/postgresql/V000002__managed_files_held_reason.sql).
- [db/files/postgresql/V000003__create_storage_directory_configurations.sql](../jmouse-files-jpa/src/main/resources/db/files/postgresql/V000003__create_storage_directory_configurations.sql).
- [db/files/postgresql/V000004__managed_files_trash.sql](../jmouse-files-jpa/src/main/resources/db/files/postgresql/V000004__managed_files_trash.sql).

### Verification boundary

This documentation was checked against the current source tree. No Java build, database migration, external API call or runtime scenario is claimed by this documentation change.
