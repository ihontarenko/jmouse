# jmouse-java-index-store

Persistence contract for a caller-defined index scope, plus the standard disk implementation.
Coordinates: `org.jmouse:jmouse-java-index-store:1.0.1`. Java 21; depends on `jmouse-java-index` only.
No Spring, database, filesystem watcher, background executor or product jobs are required by the contract.

## Contract

`IndexStore` exposes:

- `read(source) -> Optional<FileIndex>`: the active contribution of a source;
- `catalog() -> List<FileSymbols>`: immutable lightweight search metadata;
- `write(file, contentHash)`: replace the source contribution, with the SHA-256 hash of its source bytes;
- `remove(source)`: forget the source contribution;
- `revision()`: a monotonic modification counter within the scope.

The caller chooses what a scope means: an Atomic project or a Raccoon corpus/generation, for example.
An adapter owns its durability and transaction boundaries. Database generations, leases and permissions
are product concerns. Raccoon's future Postgres adapter implements this interface on its own side;
it does not need the disk cache or Atomic's project lifecycle.

## Disk implementation

`new DiskIndexStore(cacheRoot, scope, JavaParser.Level.EXPRESSIONS)` opens a writer-locked scope.
The adapter implements `IndexStore` and `AutoCloseable`.

- `index(source, bytes, modifiedNanoseconds) -> IndexedFile(file, parsed)` reuses a content stub or parses
  on a cache miss. `parsed` distinguishes a cache hit. Stored contributions are path-neutral and rebased
  on read, so renamed files and identical content in another scope reuse the same stub.
- `isCurrent(source, modifiedNanoseconds, size)` is a scan fast path. Watch notifications should force
  content hashing, even when filesystem timestamps match.
- `flush()` persists pending metadata deltas and compacts them when needed. `close()` flushes and releases
  the writer lock. Mutating methods are immediately visible within this adapter; durability follows flush.
- `read` returns empty for a missing/corrupt stub and logs a warning; the caller can rebuild from source.
  Invalid project metadata is discarded and rebuilt by the caller's normal source scan.

The cache namespace includes parser revision, FileIndex revision, codec version and parsing depth.
The closed binary record schema has a separate fingerprint and CRC32C checksums. Decoding uses a fixed
schema derived from the index record types; cached bytes cannot request arbitrary class loading.
Repeated strings and record identities are shared, including repeated expression subtrees.

Files below `<revision-hash>/`:

| Path | Purpose |
|---|---|
| `stubs/<content-sha256>.stub` | immutable, path-neutral FileIndex cache shared between scopes |
| `projects/<scope-sha256>/manifest` | active segment names and modification counter; atomically replaced |
| `projects/<scope-sha256>/index-<uuid>.segment` | snapshot/delta search metadata and content pointers |
| `projects/<scope-sha256>/writer.lock` | one writer per scope, released by closing the adapter |

Segments are append-only, checksummed frames; a truncated final append is ignored. Restoration reads them
through read-only mmap and rebuilds the small name/candidate lookup tables. Expressions and full file
declarations are loaded lazily from stubs. All lookup metadata shares one segment stream rather than four
duplicated tables. Compaction publishes a fresh snapshot through an atomic manifest replacement.
Old mapped segments are reclaimed on the next open, avoiding forced unmapping on Windows.
Content stubs are retained for reuse across source versions and scopes; there is no eviction policy yet.

## Connecting the index

`new CodeIndex(store.catalog(), source -> store.read(source).orElseThrow())` restores type/name search
without parsing or loading every stub. A product should provide a missing-stub recovery callback when it
can rebuild sources. `CodeIndex.fileSymbols(source)` reads only lightweight metadata, useful for file-tree
glyphs; `file(source)` loads the full contribution on demand.

Atomic owns recursive watching, ordered background updates, progress notifications, debounce, source-root
validation and repair. A database adopter owns its corresponding lifecycle.

## Implementation ownership (quality revision, A6)

SegmentName owns generation/validation of canonical index-<uuid>.segment filenames. ContentHash validates SHA-256
identities; IndexLayout owns the directory/file layout. IndexJournal owns writer locking, restore, deltas and compaction;
FileIndexCache owns content-addressed contributions and source rebasing. DiskIndexStore is the synchronized facade.
Wire tags and defensive codec limits are named in the codec; the existing on-disk schema remains compatible.
Atomic also uses a declaration-only scope for lazily read JDK/JAR APIs. Unsaved editor buffers never enter this cache.

## Phase-one hardening (2026-10-18)

Parser/index revisions invalidate old disk namespaces (parser 4, FileIndex 3). The closed record
schema also fingerprints diagnostic phase and retained JavaDoc; incompatible frames are rejected.
IndexBinaryCodec exposes typed file/catalog encoding and a format/schema revision for the JPA
adapter, without filesystem access or arbitrary class loading. Encoded size, nesting, collection
counts, checksums and schema are bounded/validated. Set encoding is ordered for repeatability.
Disk rebasing delegates to FileIndex.withSource, preserving documentation and member identity.

IndexStoreContractTest verifies deep payload round-trip, corruption rejection/rebuild, lazy catalog
navigation, rename reuse, persisted removal and separation of declaration/expression namespaces.
The standard disk adapter remains Atomic's implementation. A database deployment uses the separate
jmouse-java-index-jpa module; it does not add JPA or database dependencies to this module.
