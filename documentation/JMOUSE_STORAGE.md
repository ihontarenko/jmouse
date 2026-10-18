# jmouse-storage

The neutral byte-storage layer: content acceptance, safe storage keys, backend selection, local disk storage, write receipts and delivery planning. It contains no file registry entity or product HTTP controller. The separate JPA module records stored objects; framework integrations render delivery plans.

This is an orientation map of the implementation. The linked source is authoritative when an API or behavior changes.

## Module boundary

The build and declared dependencies are in [pom.xml](../jmouse-storage/pom.xml). Direct dependency declarations include `jmouse-core`, `jmouse-http`, `slf4j-api`, `logback-classic`.

The entry points below are the major contracts, not an assertion that every class is an application service. Internal helpers and executable examples can be followed from their source packages.

## Responsibility map

| Type | Responsibility |
| --- | --- |
| [FileStore](../jmouse-storage/src/main/java/org/jmouse/storage/FileStore.java) | 🗄️ The byte layer: how content is written, read, described and removed. One method per question, each taking an explicit StorageKey and — for writes — a neutral Content, so a call site reads correctly without opening this interface. Deciding where content goes is not an argument here; it is org.jmouse.storage.key.StorageKeyStrategy. Adding a backend means implementing this interface and registering one bean. There is no… |
| [FileStores](../jmouse-storage/src/main/java/org/jmouse/storage/FileStores.java) | 🗂️ Every backend the application has, with one of them the default. A product does not stay on one backend forever. It starts on local disk, an object store arrives, and from that moment there are files in both — so "the store" has to become a set, or everything written before the move stops opening. Reads follow the object, writes follow the caller Every registered object records which backend wrote it, so a read is… |
| [StandardFileStores](../jmouse-storage/src/main/java/org/jmouse/storage/StandardFileStores.java) | 🗂️ The backends, built once at startup from configuration and held by name. Constructed through FileStoreFactory rather than by knowing about implementations, so this class has no idea what an object store is and a product using only local disk pulls no SDK. A provider nothing claims fails here, at startup, naming what is missing — rather than on the first upload months later. |
| [FileStoreFactory](../jmouse-storage/src/main/java/org/jmouse/storage/FileStoreFactory.java) | 🏭 Builds one kind of FileStore from its configuration. What makes "add a storage backend by implementing one interface and registering one bean" true. An application collects every factory it can find and asks each configured backend's provider which one claims it — so a new provider is a factory plus an enum constant, and no base class, no switch and no existing file changes. It also keeps the object-store SDK off a… |
| [LocalFileStore](../jmouse-storage/src/main/java/org/jmouse/storage/local/LocalFileStore.java) | 💾 The reference FileStore: objects on local disk, the storage key being their path relative to a configured root. Deliberately does not implement resolveDirectLink. The disk is reachable only through this application, so every download is streamed — which is exactly the case every caller has to handle anyway, since a direct link is optional by contract. Writes land on a temporary file in the destination directory and are… |
| [LocalFileStoreFactory](../jmouse-storage/src/main/java/org/jmouse/storage/local/LocalFileStoreFactory.java) | 🏭 Builds local-disk backends. Ships in the core module because local disk is the one backend every application has, needs no credentials and drags nothing onto a classpath. |
| [Content](../jmouse-storage/src/main/java/org/jmouse/storage/Content.java) | 📦 Everything the storage layer needs to ingest a piece of content, and nothing framework-shaped. No multipart type, no servlet type, no Resource — so the same call works from a Spring controller, a jMouse handler, a scheduled import and a plain test. Adapting a framework's upload type into this record is the framework adapter's job, not the library's. Both originalFilename and declaredContentType are claims made by… |
| [ContentStream](../jmouse-storage/src/main/java/org/jmouse/storage/ContentStream.java) | 🚰 Opens the bytes of a piece of Content, on demand. Deliberately a supplier rather than a stream: a backend decides when — and whether more than once — it wants to read, and a caller that hands over an already-open stream cannot express "read this again" for a retry. |
| [StorageKey](../jmouse-storage/src/main/java/org/jmouse/storage/StorageKey.java) | 🔑 The address of a stored object — a forward-slash separated relative path. The local backend resolves a key against its root directory; an object store uses it verbatim as the object name. Keeping one format is what lets an object move between backends without rewriting the key already persisted against it. Safety is a property of the type, not of a call somewhere: a key that could climb out of the storage root cannot be… |
| [StorageKeyStrategy](../jmouse-storage/src/main/java/org/jmouse/storage/key/StorageKeyStrategy.java) | 🗺️ Decides where content is laid out. Separating layout from the FileStore that physically writes is what lets the layout policy — per owner, per content class, content-addressed, flat — change without touching a caller or a backend. It is also the single mechanism: an application must not have one layout for uploads and a different one for documents, which is the situation this replaces. |
| [StorageKeyRequest](../jmouse-storage/src/main/java/org/jmouse/storage/key/StorageKeyRequest.java) | 📐 What a caller knows about content, expressed so a StorageKeyStrategy can decide where it goes. Built rather than constructed positionally on purpose. The mechanism this replaces offered store(stream, a, b) and store(stream, a, b, c) where every parameter was a String, and reading a call site told you nothing about what a and b were — which is how two products ended up disagreeing about it. |
| [OwnerNamespacedKeyStrategy](../jmouse-storage/src/main/java/org/jmouse/storage/key/OwnerNamespacedKeyStrategy.java) | 🏠 Lays content out per owner: {namespace/u/{ownerIdentifier}/{segments…}/{base}.{ext}}. The layout is deliberately unchanged from the one already in production, so adopting this library is a code change and not a data movement — the same inputs produce byte-identical keys, and every key already persisted keeps resolving. One caller does change shape. A product whose upload path bypassed its own layout policy and wrote… |
| [ContentAddressedKeyStrategy](../jmouse-storage/src/main/java/org/jmouse/storage/key/ContentAddressedKeyStrategy.java) | 🧬 Lays content out by what it is : {namespace/sha256/{aa}/{bb}/{digest}}. The layout deduplication needs. Two callers uploading the same bytes compose the same key, so the second write lands on the object the first one made and one copy backs both bindings. |
| [UploadPolicy](../jmouse-storage/src/main/java/org/jmouse/storage/policy/UploadPolicy.java) | 🛃 What may enter storage, and how large it may be. One component in two modes rather than two components: a product that blocks executables and markup, and a product that admits only images, PDF and Office formats, are configuring the same rule in opposite directions. Declared content type and extension are checked independently , because a client controls both and may lie about either — an executable renamed invoice.pdf… |
| [UploadPolicyResolver](../jmouse-storage/src/main/java/org/jmouse/storage/policy/UploadPolicyResolver.java) | 🛃 Which acceptance policy applies to content going here . UploadPolicy answers "may this content be stored"; this answers "by which rule". |
| [StoredObject](../jmouse-storage/src/main/java/org/jmouse/storage/StoredObject.java) | ✅ The receipt for a completed write. Unlike ObjectDescription this carries the digest, because the digest is computed in the same pass as the bytes — establishing it afterwards would mean reading the object a second time. It doubles as a content identity for deduplication and as a strong entity tag for conditional requests, so neither costs an extra column or an extra read. It also names the backend that did the writing.… |
| [DeliveryPlanner](../jmouse-storage/src/main/java/org/jmouse/storage/delivery/DeliveryPlanner.java) | 🧭 Turns "who asked for what" into "what to send", once, for every product. This is the genuinely subtle part of serving a file, and it was previously restated per product — where two restatements are two chances to disagree. Written here as a pure function over values, the whole of it becomes a table of input to expected output: no server to start, no bytes to move, no framework to stand up. |
| [DeliveryPlan](../jmouse-storage/src/main/java/org/jmouse/storage/delivery/DeliveryPlan.java) | 📦 What to do with a request for a file — a closed set, so a renderer that handles every case is a renderer that cannot be surprised. Each variant carries its own resolved #headers(), already including status, content type, disposition, cache lifetime and entity tag. That is the point of planning separately from rendering: the arithmetic that is easy to get wrong — a redirect's lifetime against its signature's, a content… |
| [DeliverableFile](../jmouse-storage/src/main/java/org/jmouse/storage/delivery/DeliverableFile.java) | 📄 What the delivery layer needs to know about a file, and nothing else. Neutral on purpose: the planner lives in a module that knows nothing about persistence, so it cannot take a registry entity — and a product's own binding row, not the registry, is what knows the name this particular reference should present. Assembling this record is where the two meet. #presentedFilename is therefore the binding's name rather than… |
| [DeliveryIntent](../jmouse-storage/src/main/java/org/jmouse/storage/delivery/DeliveryIntent.java) | 🙋 What the client asked for. Everything a delivery decision depends on that comes from the request rather than from the file: whether to show or save, which bytes, what the client already holds, and whether the file is being served publicly or to its owner — the last one deciding how long a cache may keep it. Deliberately not a servlet request. The planner is a pure function so that "what does this request produce" is a… |
| [SpooledContent](../jmouse-storage/src/main/java/org/jmouse/storage/support/SpooledContent.java) | 🥄 Content read once onto disk, so it can be measured, digested, and then read again. A stream answers only one question, and only once: what bytes. Two things need more than that. A content-addressed layout has to know the digest before it can say where the content goes, and deduplication has to ask the registry whether these exact bytes are already stored before deciding to store them at all. Both need the bytes twice,… |
| [StorageSettings](../jmouse-storage/src/main/java/org/jmouse/storage/configuration/StorageSettings.java) | ⚙️ Every setting the storage layer reads, as one record. Bound through Bind rather than a framework annotation, so the same type binds inside a jMouse context and inside a Spring one. @BeanProperties would have been the jMouse- native choice and is deliberately not used: it is applied by a jMouse BeanPostProcessor and would simply never fire in a Spring application. Binding * StorageSettings settings =… |

## Main flow

- Build the configured backend set from FileStoreFactory implementations and select its named default. Reads can still address the backend recorded on an older object. ([StandardFileStores](../jmouse-storage/src/main/java/org/jmouse/storage/StandardFileStores.java))

- Judge the content type, extension and size claims before accepting a write. The content object supplies a way to open bytes rather than a framework multipart object. ([UploadPolicy](../jmouse-storage/src/main/java/org/jmouse/storage/policy/UploadPolicy.java))

- Compose a StorageKey from the explicit key request. Owner namespacing and content addressing are alternative layout strategies independent of the backend. ([StorageKeyStrategy](../jmouse-storage/src/main/java/org/jmouse/storage/key/StorageKeyStrategy.java))

- Write, read, describe, range-read or delete bytes under an explicit safe key. The completed write returns StoredObject, including measured byte count and digest. ([FileStore](../jmouse-storage/src/main/java/org/jmouse/storage/FileStore.java))

- Resolve presentation and conditional requests first, then ask for an optional direct backend link, then resolve range streaming if the application must serve the bytes. ([DeliveryPlanner](../jmouse-storage/src/main/java/org/jmouse/storage/delivery/DeliveryPlanner.java))

- Pass the closed plan variant and its completed headers to a renderer. Byte fetching is separate from the planner arithmetic. ([DeliveryPlan](../jmouse-storage/src/main/java/org/jmouse/storage/delivery/DeliveryPlan.java))

## Adoption and extension points

- Add another backend by contributing a factory for the configured provider. The core local implementation does not require the S3 SDK. ([FileStoreFactory](../jmouse-storage/src/main/java/org/jmouse/storage/FileStoreFactory.java))

- Resolve a policy by destination, allowing folder/workspace rules without trusting content to choose its own admission policy. ([UploadPolicyResolver](../jmouse-storage/src/main/java/org/jmouse/storage/policy/UploadPolicyResolver.java))

- Expose a store read-only through the resource-loader storage protocol where a resource API is appropriate. ([FileStoreResourceLoader](../jmouse-storage/src/main/java/org/jmouse/storage/resource/FileStoreResourceLoader.java))

- Use the neutral settings record through the jMouse binder or a framework adapter. Configuration binding and servlet adaptation live outside the byte API. ([StorageSettings](../jmouse-storage/src/main/java/org/jmouse/storage/configuration/StorageSettings.java))

## Constraints worth preserving

- Keys are safe relative forward-slash paths. Reject malformed/out-of-root keys at construction rather than passing unchecked paths into a backend. ([StorageKey](../jmouse-storage/src/main/java/org/jmouse/storage/StorageKey.java))

- A supplier can be opened on demand; an already-open single-use stream cannot describe a retry or second read. ([ContentStream](../jmouse-storage/src/main/java/org/jmouse/storage/ContentStream.java))

- A cached redirect must not outlive its signature. The planner subtracts the safety margin and never marks such a redirect immutable. ([DirectLink](../jmouse-storage/src/main/java/org/jmouse/storage/DirectLink.java))

- Allowlist and denylist modes interpret lists differently. An empty allowlist does not mean every format is allowed. ([UploadSettings](../jmouse-storage/src/main/java/org/jmouse/storage/configuration/UploadSettings.java))

- Content addressing needs the digest before the key can be composed, which is why spooling exists; owner-separated keys cannot also provide cross-owner deduplication. ([ContentAddressedKeyStrategy](../jmouse-storage/src/main/java/org/jmouse/storage/key/ContentAddressedKeyStrategy.java))


## Source navigation

- [`org.jmouse.storage.configuration`](../jmouse-storage/src/main/java/org/jmouse/storage/configuration/)
- [`org.jmouse.storage`](../jmouse-storage/src/main/java/org/jmouse/storage/)
- [`org.jmouse.storage.delivery`](../jmouse-storage/src/main/java/org/jmouse/storage/delivery/)
- [`org.jmouse.storage.exception`](../jmouse-storage/src/main/java/org/jmouse/storage/exception/)
- [`org.jmouse.storage.key`](../jmouse-storage/src/main/java/org/jmouse/storage/key/)
- [`org.jmouse.storage.local`](../jmouse-storage/src/main/java/org/jmouse/storage/local/)
- [`org.jmouse.storage.policy`](../jmouse-storage/src/main/java/org/jmouse/storage/policy/)
- [`org.jmouse.storage.resource`](../jmouse-storage/src/main/java/org/jmouse/storage/resource/)
- [`org.jmouse.storage.support`](../jmouse-storage/src/main/java/org/jmouse/storage/support/)

Executable examples are present in the module source tree. They are navigation aids; this document does not report running them:

- [DeliverySmoke](../jmouse-storage/src/main/java/org/jmouse/storage/smoke/DeliverySmoke.java)
- [StorageSmoke](../jmouse-storage/src/main/java/org/jmouse/storage/smoke/StorageSmoke.java)

## Source reference

Direct dependencies below come from this module's [POM](../jmouse-storage/pom.xml); test-only dependencies are omitted. Optional dependencies still require the adopter to supply the feature at runtime.

| Dependency | Declared scope | Optional |
|---|---|---|
| `org.jmouse:jmouse-core` | compile | no |
| `org.jmouse:jmouse-http` | compile | no |
| `org.slf4j:slf4j-api` | compile | no |
| `ch.qos.logback:logback-classic` | compile | yes |

### Public type map by package

Top-level public types in production source, excluding smoke/example drivers. Links point to the implementation; roles summarize source JavaDoc or the declared contract. Nested builders, records and enum members belong to their enclosing type.

#### `org.jmouse.storage`

| Type | Responsibility / contract |
|---|---|
| [Content](../jmouse-storage/src/main/java/org/jmouse/storage/Content.java) | 📦 Everything the storage layer needs to ingest a piece of content, and nothing framework-shaped. |
| [ContentStream](../jmouse-storage/src/main/java/org/jmouse/storage/ContentStream.java) | 🚰 Opens the bytes of a piece of Content, on demand. |
| [ContentTypes](../jmouse-storage/src/main/java/org/jmouse/storage/ContentTypes.java) | 🎨 Resolves the content type an object should be stored and served with. |
| [DirectLink](../jmouse-storage/src/main/java/org/jmouse/storage/DirectLink.java) | 🔗 A time-limited link that lets a client fetch bytes straight from the backend, skipping the application entirely. |
| [Filenames](../jmouse-storage/src/main/java/org/jmouse/storage/Filenames.java) | 📄 Filename parsing shared by keys, content and the acceptance policy. |
| [FileStore](../jmouse-storage/src/main/java/org/jmouse/storage/FileStore.java) | 🗄️ The byte layer: how content is written, read, described and removed. |
| [FileStoreFactory](../jmouse-storage/src/main/java/org/jmouse/storage/FileStoreFactory.java) | 🏭 Builds one kind of FileStore from its configuration. |
| [FileStores](../jmouse-storage/src/main/java/org/jmouse/storage/FileStores.java) | 🗂️ Every backend the application has, with one of them the default. |
| [ObjectDescription](../jmouse-storage/src/main/java/org/jmouse/storage/ObjectDescription.java) | 📋 What a backend knows about a stored object without transferring its bytes. |
| [Presentation](../jmouse-storage/src/main/java/org/jmouse/storage/Presentation.java) | 🎁 How the backend should present bytes it serves directly. |
| [StandardFileStores](../jmouse-storage/src/main/java/org/jmouse/storage/StandardFileStores.java) | 🗂️ The backends, built once at startup from configuration and held by name. |
| [StorageKey](../jmouse-storage/src/main/java/org/jmouse/storage/StorageKey.java) | 🔑 The address of a stored object — a forward-slash separated relative path. |
| [StoredObject](../jmouse-storage/src/main/java/org/jmouse/storage/StoredObject.java) | ✅ The receipt for a completed write. |

#### `org.jmouse.storage.configuration`

| Type | Responsibility / contract |
|---|---|
| [BackendSettings](../jmouse-storage/src/main/java/org/jmouse/storage/configuration/BackendSettings.java) | 🔌 One backend, fully described: which kind it is and everything that kind needs. |
| [CacheSettings](../jmouse-storage/src/main/java/org/jmouse/storage/configuration/CacheSettings.java) | ⏳ How long clients may hold on to what storage serves them. |
| [S3Settings](../jmouse-storage/src/main/java/org/jmouse/storage/configuration/S3Settings.java) | 🪣 Everything an S3-compatible backend needs to connect, for every provider that speaks S3. |
| [StorageProvider](../jmouse-storage/src/main/java/org/jmouse/storage/configuration/StorageProvider.java) | 🔌 The backends storage can be pointed at. |
| [StorageSettings](../jmouse-storage/src/main/java/org/jmouse/storage/configuration/StorageSettings.java) | ⚙️ Every setting the storage layer reads, as one record. |
| [SweeperSettings](../jmouse-storage/src/main/java/org/jmouse/storage/configuration/SweeperSettings.java) | 🧹 When unreferenced objects are reclaimed. |
| [UploadProfile](../jmouse-storage/src/main/java/org/jmouse/storage/configuration/UploadProfile.java) | 🚦 The acceptance configurations that ship ready to use. |
| [UploadSettings](../jmouse-storage/src/main/java/org/jmouse/storage/configuration/UploadSettings.java) | 🚦 What may enter storage. |

#### `org.jmouse.storage.delivery`

| Type | Responsibility / contract |
|---|---|
| [ContentDispositions](../jmouse-storage/src/main/java/org/jmouse/storage/delivery/ContentDispositions.java) | 🎁 Decides how a browser should present a file. |
| [DeliverableFile](../jmouse-storage/src/main/java/org/jmouse/storage/delivery/DeliverableFile.java) | 📄 What the delivery layer needs to know about a file, and nothing else. |
| [DeliveryIntent](../jmouse-storage/src/main/java/org/jmouse/storage/delivery/DeliveryIntent.java) | 🙋 What the client asked for. |
| [DeliveryPlan](../jmouse-storage/src/main/java/org/jmouse/storage/delivery/DeliveryPlan.java) | 📦 What to do with a request for a file — a closed set, so a renderer that handles every case is a renderer that cannot be surprised. |
| [DeliveryPlanner](../jmouse-storage/src/main/java/org/jmouse/storage/delivery/DeliveryPlanner.java) | 🧭 Turns "who asked for what" into "what to send", once, for every product. |

#### `org.jmouse.storage.exception`

| Type | Responsibility / contract |
|---|---|
| [ObjectNotFoundException](../jmouse-storage/src/main/java/org/jmouse/storage/exception/ObjectNotFoundException.java) | 🔍 Nothing is stored under the requested key. |
| [StorageException](../jmouse-storage/src/main/java/org/jmouse/storage/exception/StorageException.java) | 💥 Base of every failure the storage layer reports. |
| [StorageKeyException](../jmouse-storage/src/main/java/org/jmouse/storage/exception/StorageKeyException.java) | 🚫 A storage key is malformed, or resolves to a location outside the storage root. |
| [UploadRejectedException](../jmouse-storage/src/main/java/org/jmouse/storage/exception/UploadRejectedException.java) | ⛔ Content was refused by the acceptance policy and was never written. |

#### `org.jmouse.storage.key`

| Type | Responsibility / contract |
|---|---|
| [ContentAddressedKeyStrategy](../jmouse-storage/src/main/java/org/jmouse/storage/key/ContentAddressedKeyStrategy.java) | 🧬 Lays content out by what it is : {namespace/sha256/{aa}/{bb}/{digest}}. |
| [OwnerNamespacedKeyStrategy](../jmouse-storage/src/main/java/org/jmouse/storage/key/OwnerNamespacedKeyStrategy.java) | 🏠 Lays content out per owner: {namespace/u/{ownerIdentifier}/{segments…}/{base}.{ext}}. |
| [StorageKeyRequest](../jmouse-storage/src/main/java/org/jmouse/storage/key/StorageKeyRequest.java) | 📐 What a caller knows about content, expressed so a StorageKeyStrategy can decide where it goes. |
| [StorageKeyStrategy](../jmouse-storage/src/main/java/org/jmouse/storage/key/StorageKeyStrategy.java) | 🗺️ Decides where content is laid out. |

#### `org.jmouse.storage.local`

| Type | Responsibility / contract |
|---|---|
| [LocalFileStore](../jmouse-storage/src/main/java/org/jmouse/storage/local/LocalFileStore.java) | 💾 The reference FileStore: objects on local disk, the storage key being their path relative to a configured root. |
| [LocalFileStoreFactory](../jmouse-storage/src/main/java/org/jmouse/storage/local/LocalFileStoreFactory.java) | 🏭 Builds local-disk backends. |

#### `org.jmouse.storage.policy`

| Type | Responsibility / contract |
|---|---|
| [AcceptanceMode](../jmouse-storage/src/main/java/org/jmouse/storage/policy/AcceptanceMode.java) | 🚦 How an UploadPolicy reads its lists of content types and extensions. |
| [FixedUploadPolicy](../jmouse-storage/src/main/java/org/jmouse/storage/policy/FixedUploadPolicy.java) | 🛃 One rule, everywhere — the answer for an installation that has exactly one. |
| [UploadPolicy](../jmouse-storage/src/main/java/org/jmouse/storage/policy/UploadPolicy.java) | 🛃 What may enter storage, and how large it may be. |
| [UploadPolicyResolver](../jmouse-storage/src/main/java/org/jmouse/storage/policy/UploadPolicyResolver.java) | 🛃 Which acceptance policy applies to content going here . |

#### `org.jmouse.storage.resource`

| Type | Responsibility / contract |
|---|---|
| [FileStoreResourceLoader](../jmouse-storage/src/main/java/org/jmouse/storage/resource/FileStoreResourceLoader.java) | 🔌 Exposes a FileStore through the jMouse resource-loader registry, under a storage: protocol. |

#### `org.jmouse.storage.support`

| Type | Responsibility / contract |
|---|---|
| [ByteRanges](../jmouse-storage/src/main/java/org/jmouse/storage/support/ByteRanges.java) | ✂️ Turns a requested Range into concrete byte positions against a known object length. |
| [ContentDigests](../jmouse-storage/src/main/java/org/jmouse/storage/support/ContentDigests.java) | 🧮 Copies content somewhere while measuring and digesting it in the same pass. |
| [Digested](../jmouse-storage/src/main/java/org/jmouse/storage/support/Digested.java) | 📊 What one digesting copy established about the bytes. |
| [SpooledContent](../jmouse-storage/src/main/java/org/jmouse/storage/support/SpooledContent.java) | 🥄 Content read once onto disk, so it can be measured, digested, and then read again. |
| [TemporaryFiles](../jmouse-storage/src/main/java/org/jmouse/storage/support/TemporaryFiles.java) | 🧹 Disposes of the working files a write leaves behind. |

### Verification boundary

This documentation was checked against the current source tree. No Java build, database migration, external API call or runtime scenario is claimed by this documentation change.

Existing checks/examples to consult when changing this API:

- [DeliverySmoke.java](../jmouse-storage/src/main/java/org/jmouse/storage/smoke/DeliverySmoke.java).
- [StorageSmoke.java](../jmouse-storage/src/main/java/org/jmouse/storage/smoke/StorageSmoke.java).
