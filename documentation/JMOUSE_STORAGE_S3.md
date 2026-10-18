# jmouse-storage-s3

The S3-compatible FileStore implementation for AWS S3, MinIO and Supabase. The backend protocol implementation is shared; endpoint, region and addressing differences arrive through storage configuration. Keeping this adapter separate lets local-only applications avoid the AWS SDK.

This is an orientation map of the implementation. The linked source is authoritative when an API or behavior changes.

## Module boundary

The build and declared dependencies are in [pom.xml](../jmouse-storage-s3/pom.xml). Direct dependency declarations include `jmouse-storage`, `s3`, `slf4j-api`, `logback-classic`.

The entry points below are the major contracts, not an assertion that every class is an application service. Internal helpers and executable examples can be followed from their source packages.

## Responsibility map

| Type | Responsibility |
| --- | --- |
| [S3FileStore](../jmouse-storage-s3/src/main/java/org/jmouse/storage/s3/S3FileStore.java) | 🪣 The FileStore for every S3-compatible provider: AWS S3, self-hosted MinIO and Supabase Storage. One class, not three. The providers speak the same protocol and differ only in endpoint, addressing style and region — all of which arrive as configuration with a per-provider default, so adding a fourth is an entry in org.jmouse.storage.configuration.StorageProvider rather than a branch in here. There is deliberately no if… |
| [S3FileStoreFactory](../jmouse-storage-s3/src/main/java/org/jmouse/storage/s3/S3FileStoreFactory.java) | 🏭 Builds every S3-compatible backend: AWS S3, MinIO and Supabase Storage. One factory for all three, because they are one implementation differing only by configuration. It lives in this module so that a product using only local disk never pulls the AWS SDK onto its classpath — the factory is simply not there to be found. |
| [S3ObjectResource](../jmouse-storage-s3/src/main/java/org/jmouse/storage/s3/S3ObjectResource.java) | 📡 A Resource over an object arriving from the store. Exists for two reasons, both of which are about not reading bytes twice. Length. Resource#getLength() defaults to reading the whole stream to count its bytes, which for a single-use network stream means the object is consumed before anybody gets to read it — the length is measured and the bytes are gone. The store already reported the length in the response headers, so… |

## Main flow

- Claim the supported S3-compatible providers and build their FileStore instances from BackendSettings and S3 settings. ([S3FileStoreFactory](../jmouse-storage-s3/src/main/java/org/jmouse/storage/s3/S3FileStoreFactory.java))

- Receive the explicit StorageKey and neutral Content through the core FileStore contract. Use the configured bucket/client rather than product-specific storage classes. ([S3FileStore](../jmouse-storage-s3/src/main/java/org/jmouse/storage/s3/S3FileStore.java))

- Spool content for the measured upload length and digest, upload it and return a StoredObject receipt for the core registry/ingestion path. ([S3FileStore](../jmouse-storage-s3/src/main/java/org/jmouse/storage/s3/S3FileStore.java))

- Describe or retrieve an object by key; preserve the core distinction between missing objects and other storage failures. ([S3FileStore](../jmouse-storage-s3/src/main/java/org/jmouse/storage/s3/S3FileStore.java))

- Wrap a retrieved response with its known content length and range information so asking for Resource metadata does not consume the network stream. ([S3ObjectResource](../jmouse-storage-s3/src/main/java/org/jmouse/storage/s3/S3ObjectResource.java))

- Offer a time-limited direct link with the requested Presentation. DeliveryPlanner chooses that route when the backend returns it. ([S3FileStore](../jmouse-storage-s3/src/main/java/org/jmouse/storage/s3/S3FileStore.java))

## Adoption and extension points

- Register the factory alongside local factories. The core StandardFileStores chooses only factories needed by configured backends. ([S3FileStoreFactory](../jmouse-storage-s3/src/main/java/org/jmouse/storage/s3/S3FileStoreFactory.java))

- Select provider, endpoint, credentials, bucket, region and addressing through the core settings types. Provider differences are configuration rather than separate subclasses. ([S3FileStore](../jmouse-storage-s3/src/main/java/org/jmouse/storage/s3/S3FileStore.java))

- Treat the returned stream as a network resource and close it through the normal resource-consumption path. ([S3ObjectResource](../jmouse-storage-s3/src/main/java/org/jmouse/storage/s3/S3ObjectResource.java))

- The runnable smoke targets a real compatible object store. Read its endpoint/bucket overrides before using it against an installation. ([S3StorageSmoke](../jmouse-storage-s3/src/main/java/org/jmouse/storage/s3/smoke/S3StorageSmoke.java))

## Constraints worth preserving

- Resource default length discovery would read the stream; this wrapper uses the response length instead. A network response cannot safely be consumed to count bytes and then read again. ([S3ObjectResource](../jmouse-storage-s3/src/main/java/org/jmouse/storage/s3/S3ObjectResource.java))

- Direct-link presentation must match streamed presentation, including content disposition. Sign the presentation supplied by the planner instead of re-deciding it in an HTTP controller. ([S3FileStore](../jmouse-storage-s3/src/main/java/org/jmouse/storage/s3/S3FileStore.java))

- Adding the module makes a factory available; it does not configure a bucket or credentials for the application. ([S3FileStoreFactory](../jmouse-storage-s3/src/main/java/org/jmouse/storage/s3/S3FileStoreFactory.java))

- The smoke performs object operations on its configured backend and is not an isolated unit test. ([S3StorageSmoke](../jmouse-storage-s3/src/main/java/org/jmouse/storage/s3/smoke/S3StorageSmoke.java))

## Backend lifecycle

The implementation owns both the S3 client and presigner and implements AutoCloseable. Close it when its application lifecycle ends:

```java
try (S3FileStore store = new S3FileStore(backendSettings)) {
    StoredObject receipt = store.write(key, content);
    // Keep the receipt for the registry; the bytes now live in the configured bucket.
}
```

The public endpoint is used by the presigner and may differ from the internal API endpoint. A signed host cannot be rewritten afterward. Presigning failure returns no direct link and permits the delivery layer to stream instead. The current delete implementation logs runtime failures instead of propagating them; callers cannot infer confirmed remote deletion solely from its return. See [S3FileStore](../jmouse-storage-s3/src/main/java/org/jmouse/storage/s3/S3FileStore.java).

## Source navigation

- [`org.jmouse.storage.s3`](../jmouse-storage-s3/src/main/java/org/jmouse/storage/s3/)

Executable examples are present in the module source tree. They are navigation aids; this document does not report running them:

- [S3StorageSmoke](../jmouse-storage-s3/src/main/java/org/jmouse/storage/s3/smoke/S3StorageSmoke.java)

## Source reference

Direct dependencies below come from this module's [POM](../jmouse-storage-s3/pom.xml); test-only dependencies are omitted. Optional dependencies still require the adopter to supply the feature at runtime.

| Dependency | Declared scope | Optional |
|---|---|---|
| `org.jmouse:jmouse-storage` | compile | no |
| `software.amazon.awssdk:s3` | compile | no |
| `org.slf4j:slf4j-api` | compile | no |
| `ch.qos.logback:logback-classic` | compile | yes |

### Public type map by package

Top-level public types in production source, excluding smoke/example drivers. Links point to the implementation; roles summarize source JavaDoc or the declared contract. Nested builders, records and enum members belong to their enclosing type.

#### `org.jmouse.storage.s3`

| Type | Responsibility / contract |
|---|---|
| [S3FileStore](../jmouse-storage-s3/src/main/java/org/jmouse/storage/s3/S3FileStore.java) | 🪣 The FileStore for every S3-compatible provider: AWS S3, self-hosted MinIO and Supabase Storage. |
| [S3FileStoreFactory](../jmouse-storage-s3/src/main/java/org/jmouse/storage/s3/S3FileStoreFactory.java) | 🏭 Builds every S3-compatible backend: AWS S3, MinIO and Supabase Storage. |
| [S3ObjectResource](../jmouse-storage-s3/src/main/java/org/jmouse/storage/s3/S3ObjectResource.java) | 📡 A Resource over an object arriving from the store. |

### Verification boundary

This documentation was checked against the current source tree. No Java build, database migration, external API call or runtime scenario is claimed by this documentation change.

Existing checks/examples to consult when changing this API:

- [S3StorageSmoke.java](../jmouse-storage-s3/src/main/java/org/jmouse/storage/s3/smoke/S3StorageSmoke.java).
