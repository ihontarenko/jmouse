# jmouse-storage-management

> Spring integration module · Maven artifact `org.jmouse:jmouse-storage-management` · source: [module POM](../jmouse-spring/jmouse-storage-management/pom.xml).

## Purpose and boundaries

Provides managed-file services, directory trees, upload policies inherited from directories, file visibility, trash, and optional REST endpoints. It composes byte storage with jmouse-files-jpa; it does not define a product identity or permission vocabulary.

## How a product adopts it

Add the artifact and supply JPA, transactions and file-owner context. Use FileManagement and DirectoryManagement directly, or deliberately enable their endpoints and declare ExternalAccessRules through FilesAccessRules. Register the file and directory target resolvers and scope hierarchy appropriate to the product before exposing the controllers.

The dependency surface is declared in the [module POM](../jmouse-spring/jmouse-storage-management/pom.xml). This integration module uses these library artifacts:

- `jmouse-storage-spring-boot`.
- `jmouse-files-jpa`.
- `jmouse-access-enforcement`.
- `jmouse-access`.

## Runtime path

FilesManagementAutoConfiguration wires bindings, managed files, directory stores, policy resolution and file services. FileManagement is independent of endpoint enablement, but DirectoryManagement is currently declared inside the endpoint-enabled nested configuration and also requires directories.enabled. Storage administration is a separate opt-in surface; import fetches are another opt-in capability. FilesFlywayAutoConfiguration migrates file metadata after the storage migration integration and before product Flyway.

## Key class responsibilities

| Type | Responsibility |
|---|---|
| [FilesManagementAutoConfiguration](../jmouse-spring/jmouse-storage-management/src/main/java/org/jmouse/files/management/autoconfigure/FilesManagementAutoConfiguration.java) | 📁 Managed files, their filing, their tree — and, if a product asks for them, their endpoints. |
| [FileManagement](../jmouse-spring/jmouse-storage-management/src/main/java/org/jmouse/files/management/FileManagement.java) | 📁 The whole of what a file endpoint does, minus the routing and minus the authorization. |
| [FileManagementContext](../jmouse-spring/jmouse-storage-management/src/main/java/org/jmouse/files/management/FileManagementContext.java) | 🧭 The two things about an upload that the server answers, never the client. |
| [DirectoryManagement](../jmouse-spring/jmouse-storage-management/src/main/java/org/jmouse/files/management/DirectoryManagement.java) | 🌳 The whole of what a directory endpoint does, minus the routing and minus the authorization. |
| [FilesAccessRules](../jmouse-spring/jmouse-storage-management/src/main/java/org/jmouse/files/management/FilesAccessRules.java) | 🔐 What this module's routes require, said in a product's own vocabulary. |
| [FileVisibility](../jmouse-spring/jmouse-storage-management/src/main/java/org/jmouse/files/management/access/FileVisibility.java) | 🔒 Which of the files a listing returns this caller may actually read. |
| [DirectoryVisibility](../jmouse-spring/jmouse-storage-management/src/main/java/org/jmouse/files/management/access/DirectoryVisibility.java) | 🔒 Which folders a caller may actually see, applied to what a listing is about to return. |
| [DirectoryUploadPolicyResolver](../jmouse-spring/jmouse-storage-management/src/main/java/org/jmouse/files/management/DirectoryUploadPolicyResolver.java) | 🛃 A folder decides what may enter it — and so does the nearest one above it that has an opinion. |
| [DirectoryScopeHierarchy](../jmouse-spring/jmouse-storage-management/src/main/java/org/jmouse/files/management/access/DirectoryScopeHierarchy.java) | 🌳 A grant on a directory covers everything below it. |
| [StorageAdministration](../jmouse-spring/jmouse-storage-management/src/main/java/org/jmouse/storage/administration/StorageAdministration.java) | 🗄️ What an operator can ask about storage, and the two things they can do to it. |
| [RemoteFileFetcher](../jmouse-spring/jmouse-storage-management/src/main/java/org/jmouse/files/management/RemoteFileFetcher.java) | 🌐 Downloads a file from a web address on somebody's behalf. |
| [FilesProblemDetails](../jmouse-spring/jmouse-storage-management/src/main/java/org/jmouse/files/management/autoconfigure/FilesProblemDetails.java) | 🚑 What a filing refusal answers with. |

## Registration and activation

Boot discovery file: [AutoConfiguration.imports](../jmouse-spring/jmouse-storage-management/src/main/resources/META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports). Its exact registered classes are:

```text
org.jmouse.files.management.autoconfigure.FilesManagementAutoConfiguration
org.jmouse.files.management.autoconfigure.FilesFlywayAutoConfiguration
```

Conditions below are copied from the linked source. They include configuration-level, nested-configuration and individual bean checks; the row is an inventory, not one conjunction applied to every bean. A classpath match alone does not provide the beans required by a constructor.

| Configuration source | Declared activation, ordering and backoff |
|---|---|
| [FilesFlywayAutoConfiguration](../jmouse-spring/jmouse-storage-management/src/main/java/org/jmouse/files/management/autoconfigure/FilesFlywayAutoConfiguration.java) | `@AutoConfiguration`<br>`@AutoConfigureAfter(name = "org.jmouse.storage.spring.autoconfigure.StorageFlywayAutoConfiguration")`<br>`@ConditionalOnClass({Flyway.class, FilesMigrations.class, StorageMigrations.class, DataSource.class})`<br>`@ConditionalOnProperty(name = "jmouse.files.migrations.enabled", havingValue = "true", matchIfMissing = true)`<br>`@ConditionalOnMissingBean(FilesFlywayMigrator.class)` |
| [FilesManagementAutoConfiguration](../jmouse-spring/jmouse-storage-management/src/main/java/org/jmouse/files/management/autoconfigure/FilesManagementAutoConfiguration.java) | `@AutoConfiguration`<br>`@ConditionalOnClass({EntityManager.class, ManagedFile.class})`<br>`@ConditionalOnMissingBean(name = "filesIdentifierSupplier")`<br>`@ConditionalOnMissingBean`<br>`@ConditionalOnMissingBean(UploadPolicyResolver.class)`<br>`@ConditionalOnProperty(name = IMPORT_ENABLED, havingValue = "true")`<br>`@ConditionalOnProperty(name = ENDPOINTS_ENABLED, havingValue = "true")`<br>`@Configuration(proxyBeanMethods = false)`<br>`@ConditionalOnProperty(name = DIRECTORY_ENDPOINTS_ENABLED, havingValue = "true", matchIfMissing = true)`<br>`@ConditionalOnProperty(name = ADMINISTRATION_ENABLED, havingValue = "true")` |

## Configuration contract

jmouse.files.management.endpoints.enabled is opt-in true; jmouse.files.management.directories.enabled defaults to true within enabled endpoints. jmouse.files.management.import.enabled is opt-in true. jmouse.storage.administration.enabled is opt-in true. jmouse.files.management.namespace defaults to files. jmouse.files.management.import.user-agent is blank by binding default; a blank value selects DEFAULT_IMPORT_USER_AGENT, the browser string in FilesManagementAutoConfiguration. jmouse.files.migrations.enabled defaults to true. Route placeholders are listed below.

The definitions that own these settings and route placeholders are:

- [ManagementRoutes](../jmouse-spring/jmouse-storage-management/src/main/java/org/jmouse/files/management/ManagementRoutes.java).
- [AdministrationRoutes](../jmouse-spring/jmouse-storage-management/src/main/java/org/jmouse/storage/administration/AdministrationRoutes.java).

Route constants are expressions resolved by their consumer; `${…}` text is a placeholder, not a literal URL. Shared `ManagementEndpoints.ROOT` may itself be configured by the host.

| Constant | Exact source expression |
|---|---|
| [ManagementRoutes](../jmouse-spring/jmouse-storage-management/src/main/java/org/jmouse/files/management/ManagementRoutes.java).SEGMENT | `"/files/api"` |
| [ManagementRoutes](../jmouse-spring/jmouse-storage-management/src/main/java/org/jmouse/files/management/ManagementRoutes.java).PREFIX | `"${jmouse.files.management.prefix:" + ManagementEndpoints.ROOT + SEGMENT + "}"` |
| [ManagementRoutes](../jmouse-spring/jmouse-storage-management/src/main/java/org/jmouse/files/management/ManagementRoutes.java).BASE | `PREFIX + "/files"` |
| [ManagementRoutes](../jmouse-spring/jmouse-storage-management/src/main/java/org/jmouse/files/management/ManagementRoutes.java).ONE | `BASE + "/{fileId}"` |
| [ManagementRoutes](../jmouse-spring/jmouse-storage-management/src/main/java/org/jmouse/files/management/ManagementRoutes.java).CONTENT | `ONE + "/content"` |
| [ManagementRoutes](../jmouse-spring/jmouse-storage-management/src/main/java/org/jmouse/files/management/ManagementRoutes.java).IMPORT | `BASE + "/import"` |
| [ManagementRoutes](../jmouse-spring/jmouse-storage-management/src/main/java/org/jmouse/files/management/ManagementRoutes.java).BINDING | `ONE + "/binding"` |
| [ManagementRoutes](../jmouse-spring/jmouse-storage-management/src/main/java/org/jmouse/files/management/ManagementRoutes.java).PRIVACY | `ONE + "/private"` |
| [ManagementRoutes](../jmouse-spring/jmouse-storage-management/src/main/java/org/jmouse/files/management/ManagementRoutes.java).TRASH_ONE | `ONE + "/trash"` |
| [ManagementRoutes](../jmouse-spring/jmouse-storage-management/src/main/java/org/jmouse/files/management/ManagementRoutes.java).RESTORE | `ONE + "/restore"` |
| [ManagementRoutes](../jmouse-spring/jmouse-storage-management/src/main/java/org/jmouse/files/management/ManagementRoutes.java).TRASH | `PREFIX + "/trash"` |
| [ManagementRoutes](../jmouse-spring/jmouse-storage-management/src/main/java/org/jmouse/files/management/ManagementRoutes.java).DIRECTORIES | `PREFIX + "/directories"` |
| [ManagementRoutes](../jmouse-spring/jmouse-storage-management/src/main/java/org/jmouse/files/management/ManagementRoutes.java).DIRECTORY | `DIRECTORIES + "/{directoryId}"` |
| [ManagementRoutes](../jmouse-spring/jmouse-storage-management/src/main/java/org/jmouse/files/management/ManagementRoutes.java).DIRECTORY_SUBTREE | `DIRECTORY + "/subtree"` |
| [ManagementRoutes](../jmouse-spring/jmouse-storage-management/src/main/java/org/jmouse/files/management/ManagementRoutes.java).DIRECTORY_CHILDREN | `DIRECTORY + "/children"` |
| [ManagementRoutes](../jmouse-spring/jmouse-storage-management/src/main/java/org/jmouse/files/management/ManagementRoutes.java).DIRECTORY_PARENT | `DIRECTORY + "/parent"` |
| [ManagementRoutes](../jmouse-spring/jmouse-storage-management/src/main/java/org/jmouse/files/management/ManagementRoutes.java).DIRECTORY_CONFIGURATIONS | `DIRECTORY + "/configurations"` |
| [ManagementRoutes](../jmouse-spring/jmouse-storage-management/src/main/java/org/jmouse/files/management/ManagementRoutes.java).DIRECTORY_CONFIGURATION | `DIRECTORY_CONFIGURATIONS + "/{kind}"` |
| [AdministrationRoutes](../jmouse-spring/jmouse-storage-management/src/main/java/org/jmouse/storage/administration/AdministrationRoutes.java).BASE | `"/api/administration/storage"` |
| [AdministrationRoutes](../jmouse-spring/jmouse-storage-management/src/main/java/org/jmouse/storage/administration/AdministrationRoutes.java).OVERVIEW | `BASE` |
| [AdministrationRoutes](../jmouse-spring/jmouse-storage-management/src/main/java/org/jmouse/storage/administration/AdministrationRoutes.java).REGISTRY | `BASE + "/registry"` |
| [AdministrationRoutes](../jmouse-spring/jmouse-storage-management/src/main/java/org/jmouse/storage/administration/AdministrationRoutes.java).REFERENCES | `BASE + "/references"` |
| [AdministrationRoutes](../jmouse-spring/jmouse-storage-management/src/main/java/org/jmouse/storage/administration/AdministrationRoutes.java).SWEEP_PREVIEW | `BASE + "/sweep/preview"` |
| [AdministrationRoutes](../jmouse-spring/jmouse-storage-management/src/main/java/org/jmouse/storage/administration/AdministrationRoutes.java).SWEEP | `BASE + "/sweep"` |

## HTTP and protocol surface

Read the class prefix and method mapping together. Constant names resolve through the route definitions above; method parameters and response DTOs are in the linked handler source.

| Verb | Class-level prefix | Method mapping | Handler |
|---|---|---|---|
| GET | `(none; method uses complete route constant)` | `ManagementRoutes.DIRECTORIES` | [DirectoryController](../jmouse-spring/jmouse-storage-management/src/main/java/org/jmouse/files/management/DirectoryController.java).roots |
| GET | `(none; method uses complete route constant)` | `ManagementRoutes.DIRECTORY_SUBTREE` | [DirectoryController](../jmouse-spring/jmouse-storage-management/src/main/java/org/jmouse/files/management/DirectoryController.java).subtree |
| GET | `(none; method uses complete route constant)` | `ManagementRoutes.DIRECTORY_CHILDREN` | [DirectoryController](../jmouse-spring/jmouse-storage-management/src/main/java/org/jmouse/files/management/DirectoryController.java).children |
| POST | `(none; method uses complete route constant)` | `ManagementRoutes.DIRECTORIES` | [DirectoryController](../jmouse-spring/jmouse-storage-management/src/main/java/org/jmouse/files/management/DirectoryController.java).create |
| PUT | `(none; method uses complete route constant)` | `ManagementRoutes.DIRECTORY` | [DirectoryController](../jmouse-spring/jmouse-storage-management/src/main/java/org/jmouse/files/management/DirectoryController.java).rename |
| PUT | `(none; method uses complete route constant)` | `ManagementRoutes.DIRECTORY_PARENT` | [DirectoryController](../jmouse-spring/jmouse-storage-management/src/main/java/org/jmouse/files/management/DirectoryController.java).move |
| DELETE | `(none; method uses complete route constant)` | `ManagementRoutes.DIRECTORY` | [DirectoryController](../jmouse-spring/jmouse-storage-management/src/main/java/org/jmouse/files/management/DirectoryController.java).delete |
| GET | `(none; method uses complete route constant)` | `ManagementRoutes.DIRECTORY` | [DirectoryController](../jmouse-spring/jmouse-storage-management/src/main/java/org/jmouse/files/management/DirectoryController.java).read |
| GET | `(none; method uses complete route constant)` | `ManagementRoutes.DIRECTORY_CONFIGURATIONS` | [DirectoryController](../jmouse-spring/jmouse-storage-management/src/main/java/org/jmouse/files/management/DirectoryController.java).configurations |
| GET | `(none; method uses complete route constant)` | `ManagementRoutes.DIRECTORY_CONFIGURATION` | [DirectoryController](../jmouse-spring/jmouse-storage-management/src/main/java/org/jmouse/files/management/DirectoryController.java).configuration |
| PUT | `(none; method uses complete route constant)` | `ManagementRoutes.DIRECTORY_CONFIGURATION` | [DirectoryController](../jmouse-spring/jmouse-storage-management/src/main/java/org/jmouse/files/management/DirectoryController.java).configure |
| DELETE | `(none; method uses complete route constant)` | `ManagementRoutes.DIRECTORY_CONFIGURATION` | [DirectoryController](../jmouse-spring/jmouse-storage-management/src/main/java/org/jmouse/files/management/DirectoryController.java).clearConfiguration |
| GET | `(none; method uses complete route constant)` | `ManagementRoutes.BASE` | [FileController](../jmouse-spring/jmouse-storage-management/src/main/java/org/jmouse/files/management/FileController.java).list |
| POST | `(none; method uses complete route constant)` | `value = ManagementRoutes.BASE, consumes = MediaType.MULTIPART_FORM_DATA_VALUE` | [FileController](../jmouse-spring/jmouse-storage-management/src/main/java/org/jmouse/files/management/FileController.java).upload |
| POST | `(none; method uses complete route constant)` | `ManagementRoutes.IMPORT` | [FileController](../jmouse-spring/jmouse-storage-management/src/main/java/org/jmouse/files/management/FileController.java).importFrom |
| GET | `(none; method uses complete route constant)` | `ManagementRoutes.ONE` | [FileController](../jmouse-spring/jmouse-storage-management/src/main/java/org/jmouse/files/management/FileController.java).read |
| GET | `(none; method uses complete route constant)` | `ManagementRoutes.CONTENT` | [FileController](../jmouse-spring/jmouse-storage-management/src/main/java/org/jmouse/files/management/FileController.java).content |
| PATCH | `(none; method uses complete route constant)` | `ManagementRoutes.PRIVACY` | [FileController](../jmouse-spring/jmouse-storage-management/src/main/java/org/jmouse/files/management/FileController.java).setPrivate |
| PUT | `(none; method uses complete route constant)` | `ManagementRoutes.ONE` | [FileController](../jmouse-spring/jmouse-storage-management/src/main/java/org/jmouse/files/management/FileController.java).rename |
| PUT | `(none; method uses complete route constant)` | `ManagementRoutes.BINDING` | [FileController](../jmouse-spring/jmouse-storage-management/src/main/java/org/jmouse/files/management/FileController.java).refile |
| DELETE | `(none; method uses complete route constant)` | `ManagementRoutes.ONE` | [FileController](../jmouse-spring/jmouse-storage-management/src/main/java/org/jmouse/files/management/FileController.java).delete |
| POST | `(none; method uses complete route constant)` | `ManagementRoutes.TRASH_ONE` | [FileController](../jmouse-spring/jmouse-storage-management/src/main/java/org/jmouse/files/management/FileController.java).trash |
| POST | `(none; method uses complete route constant)` | `ManagementRoutes.RESTORE` | [FileController](../jmouse-spring/jmouse-storage-management/src/main/java/org/jmouse/files/management/FileController.java).restore |
| GET | `(none; method uses complete route constant)` | `ManagementRoutes.TRASH` | [TrashController](../jmouse-spring/jmouse-storage-management/src/main/java/org/jmouse/files/management/TrashController.java).list |
| DELETE | `(none; method uses complete route constant)` | `ManagementRoutes.TRASH` | [TrashController](../jmouse-spring/jmouse-storage-management/src/main/java/org/jmouse/files/management/TrashController.java).empty |
| GET | `(none; method uses complete route constant)` | `AdministrationRoutes.OVERVIEW` | [StorageAdministrationController](../jmouse-spring/jmouse-storage-management/src/main/java/org/jmouse/storage/administration/StorageAdministrationController.java).overview |
| GET | `(none; method uses complete route constant)` | `AdministrationRoutes.REGISTRY` | [StorageAdministrationController](../jmouse-spring/jmouse-storage-management/src/main/java/org/jmouse/storage/administration/StorageAdministrationController.java).registry |
| GET | `(none; method uses complete route constant)` | `AdministrationRoutes.REFERENCES` | [StorageAdministrationController](../jmouse-spring/jmouse-storage-management/src/main/java/org/jmouse/storage/administration/StorageAdministrationController.java).references |
| POST | `(none; method uses complete route constant)` | `AdministrationRoutes.SWEEP_PREVIEW` | [StorageAdministrationController](../jmouse-spring/jmouse-storage-management/src/main/java/org/jmouse/storage/administration/StorageAdministrationController.java).previewSweep |
| POST | `(none; method uses complete route constant)` | `AdministrationRoutes.SWEEP` | [StorageAdministrationController](../jmouse-spring/jmouse-storage-management/src/main/java/org/jmouse/storage/administration/StorageAdministrationController.java).sweep |

## Lifecycle and adoption gotchas

The controllers carry no product-specific access requirements; enablement must arrive with external access declarations. Remote import is an HTTP fetch, with bounded redirects and timeouts in RemoteFileFetcher. Directory policy inheritance and visible-tree filtering must use product access context. AdministrationRoutes uses /api/administration/storage independently from the configurable managed-file prefix. Fresh-schema Flyway baseline version requires the same care as storage.

## Source and related reading

- [Build and dependencies](../jmouse-spring/jmouse-storage-management/pom.xml).
- [Boot registration metadata](../jmouse-spring/jmouse-storage-management/src/main/resources/META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports).
- [Spring integration family](JMOUSE_SPRING.md).

## Source reference

Direct dependencies below come from this module's [POM](../jmouse-spring/jmouse-storage-management/pom.xml); test-only dependencies are omitted. Optional dependencies still require the adopter to supply the feature at runtime.

| Dependency | Declared scope | Optional |
|---|---|---|
| `org.jmouse:jmouse-storage-spring-boot` | compile | no |
| `org.jmouse:jmouse-files-jpa` | compile | no |
| `org.jmouse:jmouse-access-enforcement` | compile | no |
| `org.jmouse:jmouse-access` | compile | no |
| `org.springframework.boot:spring-boot-autoconfigure` | compile | yes |
| `org.springframework:spring-web` | compile | no |
| `org.flywaydb:flyway-core` | compile | yes |
| `org.springframework:spring-tx` | compile | yes |
| `org.springframework:spring-context` | compile | yes |
| `jakarta.servlet:jakarta.servlet-api` | provided | no |
| `org.slf4j:slf4j-api` | compile | no |
| `ch.qos.logback:logback-classic` | compile | yes |

### Public type map by package

Top-level public types in production source, excluding smoke/example drivers. Links point to the implementation; roles summarize source JavaDoc or the declared contract. Nested builders, records and enum members belong to their enclosing type.

#### `org.jmouse.files.management`

| Type | Responsibility / contract |
|---|---|
| [DirectoryConfigurationResolver](../jmouse-spring/jmouse-storage-management/src/main/java/org/jmouse/files/management/DirectoryConfigurationResolver.java) | 🔧 Whoever turns a folder's stored rows into the rule that actually applies to it. |
| [DirectoryConfigurationView](../jmouse-spring/jmouse-storage-management/src/main/java/org/jmouse/files/management/DirectoryConfigurationView.java) | 🔧 What applies to a folder, of one kind, and where it came from. |
| [DirectoryController](../jmouse-spring/jmouse-storage-management/src/main/java/org/jmouse/files/management/DirectoryController.java) | 🌳 The directory endpoints — a separate set from the file ones, deliberately. |
| [DirectoryManagement](../jmouse-spring/jmouse-storage-management/src/main/java/org/jmouse/files/management/DirectoryManagement.java) | 🌳 The whole of what a directory endpoint does, minus the routing and minus the authorization. |
| [DirectoryUploadPolicyResolver](../jmouse-spring/jmouse-storage-management/src/main/java/org/jmouse/files/management/DirectoryUploadPolicyResolver.java) | 🛃 A folder decides what may enter it — and so does the nearest one above it that has an opinion. |
| [DirectoryView](../jmouse-spring/jmouse-storage-management/src/main/java/org/jmouse/files/management/DirectoryView.java) | Value record: `(String id, String parentId, String name, String path, boolean root, int depth, boolean hasChildren, Map configurations)`. |
| [FileController](../jmouse-spring/jmouse-storage-management/src/main/java/org/jmouse/files/management/FileController.java) | 📁 The file endpoints, shipped once. |
| [FileManagement](../jmouse-spring/jmouse-storage-management/src/main/java/org/jmouse/files/management/FileManagement.java) | 📁 The whole of what a file endpoint does, minus the routing and minus the authorization. |
| [FileManagementContext](../jmouse-spring/jmouse-storage-management/src/main/java/org/jmouse/files/management/FileManagementContext.java) | 🧭 The two things about an upload that the server answers, never the client. |
| [FileManagementEvent](../jmouse-spring/jmouse-storage-management/src/main/java/org/jmouse/files/management/FileManagementEvent.java) | 📣 Something happened to a managed file, said once so no product has to watch for it itself. |
| [FilesAccessRules](../jmouse-spring/jmouse-storage-management/src/main/java/org/jmouse/files/management/FilesAccessRules.java) | 🔐 What this module's routes require, said in a product's own vocabulary. |
| [FileView](../jmouse-spring/jmouse-storage-management/src/main/java/org/jmouse/files/management/FileView.java) | 📄 A file as a screen draws it. |
| [ImportFileRequest](../jmouse-spring/jmouse-storage-management/src/main/java/org/jmouse/files/management/ImportFileRequest.java) | 🌐 Fetch this address and keep what comes back. |
| [ManagementRoutes](../jmouse-spring/jmouse-storage-management/src/main/java/org/jmouse/files/management/ManagementRoutes.java) | 🛣️ Where this module's routes live. |
| [MoveDirectoryRequest](../jmouse-spring/jmouse-storage-management/src/main/java/org/jmouse/files/management/MoveDirectoryRequest.java) | 📦 Where a directory should go. |
| [PrivacyRequest](../jmouse-spring/jmouse-storage-management/src/main/java/org/jmouse/files/management/PrivacyRequest.java) | 🙈 Whether a file should be listed and served only to whoever may already reach it. |
| [RefileFileRequest](../jmouse-spring/jmouse-storage-management/src/main/java/org/jmouse/files/management/RefileFileRequest.java) | 📦 Where a file should be filed instead. |
| [RemoteFileFetcher](../jmouse-spring/jmouse-storage-management/src/main/java/org/jmouse/files/management/RemoteFileFetcher.java) | 🌐 Downloads a file from a web address on somebody's behalf. |
| [RenameFileRequest](../jmouse-spring/jmouse-storage-management/src/main/java/org/jmouse/files/management/RenameFileRequest.java) | 🏷️ What to call a file instead. |
| [SaveDirectoryRequest](../jmouse-spring/jmouse-storage-management/src/main/java/org/jmouse/files/management/SaveDirectoryRequest.java) | 📁 A directory to make, or a new name for one. |
| [TrashController](../jmouse-spring/jmouse-storage-management/src/main/java/org/jmouse/files/management/TrashController.java) | 🗑️ The trash, for the installation as a whole: what is in it, and emptying it. |
| [TrashedFileView](../jmouse-spring/jmouse-storage-management/src/main/java/org/jmouse/files/management/TrashedFileView.java) | 🗑️ One file in the trash, as the trash screen renders it. |
| [UploadAllowance](../jmouse-spring/jmouse-storage-management/src/main/java/org/jmouse/files/management/UploadAllowance.java) | 🚦 Whether this upload may happen at all — asked before a byte is written. |

#### `org.jmouse.files.management.access`

| Type | Responsibility / contract |
|---|---|
| [DirectoryRefusals](../jmouse-spring/jmouse-storage-management/src/main/java/org/jmouse/files/management/access/DirectoryRefusals.java) | 🔒 Which of these folders the caller may not read — answered by the product, once per listing. |
| [DirectoryScopeHierarchy](../jmouse-spring/jmouse-storage-management/src/main/java/org/jmouse/files/management/access/DirectoryScopeHierarchy.java) | 🌳 A grant on a directory covers everything below it. |
| [DirectoryVisibility](../jmouse-spring/jmouse-storage-management/src/main/java/org/jmouse/files/management/access/DirectoryVisibility.java) | 🔒 Which folders a caller may actually see, applied to what a listing is about to return. |
| [FileRefusals](../jmouse-spring/jmouse-storage-management/src/main/java/org/jmouse/files/management/access/FileRefusals.java) | 🔒 Which of these files the caller may not read, beyond what their folder already decided. |
| [FileVisibility](../jmouse-spring/jmouse-storage-management/src/main/java/org/jmouse/files/management/access/FileVisibility.java) | 🔒 Which of the files a listing returns this caller may actually read. |
| [SeveralTreesScopeHierarchy](../jmouse-spring/jmouse-storage-management/src/main/java/org/jmouse/files/management/access/SeveralTreesScopeHierarchy.java) | 🌲🌳 A product with more than one tree, as the one hierarchy the engine takes. |

#### `org.jmouse.files.management.autoconfigure`

| Type | Responsibility / contract |
|---|---|
| [ConfiguredFileManagementContext](../jmouse-spring/jmouse-storage-management/src/main/java/org/jmouse/files/management/autoconfigure/ConfiguredFileManagementContext.java) | 🧭 One namespace for everything, and nobody named as the uploader. |
| [FilesFlywayAutoConfiguration](../jmouse-spring/jmouse-storage-management/src/main/java/org/jmouse/files/management/autoconfigure/FilesFlywayAutoConfiguration.java) | 🚚 The library's own schema, migrated before anything asks for it. |
| [FilesFlywayMigrator](../jmouse-spring/jmouse-storage-management/src/main/java/org/jmouse/files/management/autoconfigure/FilesFlywayMigrator.java) | 🚚 Runs this library's migrations, against its own history table. |
| [FilesManagementAutoConfiguration](../jmouse-spring/jmouse-storage-management/src/main/java/org/jmouse/files/management/autoconfigure/FilesManagementAutoConfiguration.java) | 📁 Managed files, their filing, their tree — and, if a product asks for them, their endpoints. |
| [FilesProblemDetails](../jmouse-spring/jmouse-storage-management/src/main/java/org/jmouse/files/management/autoconfigure/FilesProblemDetails.java) | 🚑 What a filing refusal answers with. |

#### `org.jmouse.storage.administration`

| Type | Responsibility / contract |
|---|---|
| [AdministrationRoutes](../jmouse-spring/jmouse-storage-management/src/main/java/org/jmouse/storage/administration/AdministrationRoutes.java) | 🛣️ Where the storage administration surface lives. |
| [ReferenceSourceStatus](../jmouse-spring/jmouse-storage-management/src/main/java/org/jmouse/storage/administration/ReferenceSourceStatus.java) | 🔗 One reference source, and whether it is answering. |
| [RegistryEntry](../jmouse-spring/jmouse-storage-management/src/main/java/org/jmouse/storage/administration/RegistryEntry.java) | 📦 One stored object, as an administrator sees it. |
| [StorageAdministration](../jmouse-spring/jmouse-storage-management/src/main/java/org/jmouse/storage/administration/StorageAdministration.java) | 🗄️ What an operator can ask about storage, and the two things they can do to it. |
| [StorageAdministrationController](../jmouse-spring/jmouse-storage-management/src/main/java/org/jmouse/storage/administration/StorageAdministrationController.java) | 🗄️ The storage administration surface. |
| [StorageAdministrationDiagnostics](../jmouse-spring/jmouse-storage-management/src/main/java/org/jmouse/storage/administration/StorageAdministrationDiagnostics.java) | 🔎 Says out loud where the administration surface is mounted. |
| [StorageOverview](../jmouse-spring/jmouse-storage-management/src/main/java/org/jmouse/storage/administration/StorageOverview.java) | 🗄️ What this installation is actually configured to do with files. |
| [SweepOutcome](../jmouse-spring/jmouse-storage-management/src/main/java/org/jmouse/storage/administration/SweepOutcome.java) | 🧹 What a sweep did — or, for a preview, what it would have done. |

### Packaged resources

- [META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports](../jmouse-spring/jmouse-storage-management/src/main/resources/META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports).

### Verification boundary

This documentation was checked against the current source tree. No Java build, database migration, external API call or runtime scenario is claimed by this documentation change.
