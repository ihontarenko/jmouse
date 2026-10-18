# jmouse-parallax-management

> Spring integration module · Maven artifact `org.jmouse:jmouse-parallax-management` · source: [module POM](../jmouse-spring/jmouse-parallax-management/pom.xml).

## Purpose and boundaries

Provides the complete Parallax MVC surface: catalogue, computations and jobs, jMX editing, jMF interpretation and rendering, SPICE import/export, subjects, scenes, comparisons, collections and installation administration. Domain services remain in jmouse-parallax-spring-boot.

## How a product adopts it

Explicitly @Import(ParallaxManagementConfiguration.class) and declare ParallaxAccessRules with the product permission and scope vocabulary. Register target resolvers for ParallaxOwner, Scene and Comparison, configure the security chain for library routes, and align proxy configuration with the selected prefix.

The dependency surface is declared in the [module POM](../jmouse-spring/jmouse-parallax-management/pom.xml). This integration module uses these library artifacts:

- `jmouse-parallax-spring-boot`.
- `jmouse-access-enforcement`.

## Runtime path

The configuration constructs each controller, ParallaxProblemDetails and the OwnerAddress argument resolver. Controllers delegate to catalogue/computation/scene services rather than implementing another persistence layer. Rules are keyed by controller and method, so changing a path does not move the authorization declaration. PublicEndpoint marks handlers whose checks happen within domain services.

## Key class responsibilities

| Type | Responsibility |
|---|---|
| [ParallaxManagementConfiguration](../jmouse-spring/jmouse-parallax-management/src/main/java/org/jmouse/parallax/management/ParallaxManagementConfiguration.java) | 🔌 Parallax's REST API. |
| [ParallaxAccessRules](../jmouse-spring/jmouse-parallax-management/src/main/java/org/jmouse/parallax/management/ParallaxAccessRules.java) | 🔐 What Parallax's routes require, said in a product's own vocabulary. |
| [CatalogueController](../jmouse-spring/jmouse-parallax-management/src/main/java/org/jmouse/parallax/management/CatalogueController.java) | 📚 The catalogue of objects, their categories, an owner's objects, the instruments, the component models and the representation vocabulary. |
| [ComputationController](../jmouse-spring/jmouse-parallax-management/src/main/java/org/jmouse/parallax/management/ComputationController.java) | 🧮 Computing a representation — inline, queued or as an animation's frames — and the jobs a queued one becomes. |
| [DocumentController](../jmouse-spring/jmouse-parallax-management/src/main/java/org/jmouse/parallax/management/DocumentController.java) | The jMX language for editors: its schema, a text as a form, and a form's edit as the next text. |
| [FormulaController](../jmouse-spring/jmouse-parallax-management/src/main/java/org/jmouse/parallax/management/FormulaController.java) | The jMF language: its reference, reading a typed formula, and printing formulas. |
| [SpiceController](../jmouse-spring/jmouse-parallax-management/src/main/java/org/jmouse/parallax/management/SpiceController.java) | SPICE in and out: a netlist as a jMX document, and a jMX circuit or component as a netlist. |
| [SceneController](../jmouse-spring/jmouse-parallax-management/src/main/java/org/jmouse/parallax/management/SceneController.java) | 🎬 Scenes — their documents, revisions, snapshots and forks — and the comparisons that lay several side by side. |
| [CollectionController](../jmouse-spring/jmouse-parallax-management/src/main/java/org/jmouse/parallax/management/CollectionController.java) | 🗂️ Collections: the curated shelves, the formula sets, and a reader's own collections of objects and scenes. |
| [ParallaxAdministrationController](../jmouse-spring/jmouse-parallax-management/src/main/java/org/jmouse/parallax/management/ParallaxAdministrationController.java) | 🛠️ The machinery's administration: every job of the installation, the dataset cache, the catalogue warm-up, what the server is busy with, the statistics a system screen shows, and the model-consistency evaluation runs. |
| [OwnerAddressArgumentResolver](../jmouse-spring/jmouse-parallax-management/src/main/java/org/jmouse/parallax/management/OwnerAddressArgumentResolver.java) | Combines owner or realm path variables into the canonical ParallaxOwner string consumed by service arguments. |
| [ParallaxProblemDetails](../jmouse-spring/jmouse-parallax-management/src/main/java/org/jmouse/parallax/management/ParallaxProblemDetails.java) | 🚑 What a Parallax refusal answers with: an RFC 9457 problem whose status the refusal names, its sentence as the detail, and — beyond what the other libraries send — its code and whatever it carries beside it. |

## Registration and activation

This artifact has no `AutoConfiguration.imports` resource. Its controller/configuration classes must be registered through the adoption path described above.

Conditions below are copied from the linked source. They include configuration-level, nested-configuration and individual bean checks; the row is an inventory, not one conjunction applied to every bean. A classpath match alone does not provide the beans required by a constructor.

| Configuration source | Declared activation, ordering and backoff |
|---|---|
| [ParallaxManagementConfiguration](../jmouse-spring/jmouse-parallax-management/src/main/java/org/jmouse/parallax/management/ParallaxManagementConfiguration.java) | `@Configuration(proxyBeanMethods = false)` |

## Configuration contract

jmouse.parallax.management.prefix defaults to the shared management root plus /parallax/api. Owner routes use /owners/{ownerType}/{ownerId}; realm routes use /realms/{realmType}/{realmId}. This artifact has no auto-configuration imports resource or management-enabled property; explicit import is the activation mechanism.

The definitions that own these settings and route placeholders are:

- [ParallaxManagementRoutes](../jmouse-spring/jmouse-parallax-management/src/main/java/org/jmouse/parallax/management/ParallaxManagementRoutes.java).

Route constants are expressions resolved by their consumer; `${…}` text is a placeholder, not a literal URL. Shared `ManagementEndpoints.ROOT` may itself be configured by the host.

| Constant | Exact source expression |
|---|---|
| [ParallaxManagementRoutes](../jmouse-spring/jmouse-parallax-management/src/main/java/org/jmouse/parallax/management/ParallaxManagementRoutes.java).SEGMENT | `"/parallax/api"` |
| [ParallaxManagementRoutes](../jmouse-spring/jmouse-parallax-management/src/main/java/org/jmouse/parallax/management/ParallaxManagementRoutes.java).PREFIX | `"${jmouse.parallax.management.prefix:" + ManagementEndpoints.ROOT + SEGMENT + "}"` |
| [ParallaxManagementRoutes](../jmouse-spring/jmouse-parallax-management/src/main/java/org/jmouse/parallax/management/ParallaxManagementRoutes.java).OWNER_TYPE | `"ownerType"` |
| [ParallaxManagementRoutes](../jmouse-spring/jmouse-parallax-management/src/main/java/org/jmouse/parallax/management/ParallaxManagementRoutes.java).OWNER_ID | `"ownerId"` |
| [ParallaxManagementRoutes](../jmouse-spring/jmouse-parallax-management/src/main/java/org/jmouse/parallax/management/ParallaxManagementRoutes.java).REALM_TYPE | `"realmType"` |
| [ParallaxManagementRoutes](../jmouse-spring/jmouse-parallax-management/src/main/java/org/jmouse/parallax/management/ParallaxManagementRoutes.java).REALM_ID | `"realmId"` |
| [ParallaxManagementRoutes](../jmouse-spring/jmouse-parallax-management/src/main/java/org/jmouse/parallax/management/ParallaxManagementRoutes.java).OWNER | `"/owners/{" + OWNER_TYPE + "}/{" + OWNER_ID + "}"` |
| [ParallaxManagementRoutes](../jmouse-spring/jmouse-parallax-management/src/main/java/org/jmouse/parallax/management/ParallaxManagementRoutes.java).REALM | `"/realms/{" + REALM_TYPE + "}/{" + REALM_ID + "}"` |
| [ParallaxManagementRoutes](../jmouse-spring/jmouse-parallax-management/src/main/java/org/jmouse/parallax/management/ParallaxManagementRoutes.java).ADMINISTRATION | `"/administration"` |

## HTTP and protocol surface

Read the class prefix and method mapping together. Constant names resolve through the route definitions above; method parameters and response DTOs are in the linked handler source.

| Verb | Class-level prefix | Method mapping | Handler |
|---|---|---|---|
| GET | `ParallaxManagementRoutes.PREFIX` | `"/component-models"` | [CatalogueController](../jmouse-spring/jmouse-parallax-management/src/main/java/org/jmouse/parallax/management/CatalogueController.java).componentModels |
| GET | `ParallaxManagementRoutes.PREFIX` | `"/component-models/{reference}"` | [CatalogueController](../jmouse-spring/jmouse-parallax-management/src/main/java/org/jmouse/parallax/management/CatalogueController.java).componentModel |
| GET | `ParallaxManagementRoutes.PREFIX` | `"/symbols"` | [CatalogueController](../jmouse-spring/jmouse-parallax-management/src/main/java/org/jmouse/parallax/management/CatalogueController.java).symbols |
| GET | `ParallaxManagementRoutes.PREFIX` | `"/catalogue/categories"` | [CatalogueController](../jmouse-spring/jmouse-parallax-management/src/main/java/org/jmouse/parallax/management/CatalogueController.java).categories |
| PUT | `ParallaxManagementRoutes.PREFIX` | `"/catalogue/categories/{code}"` | [CatalogueController](../jmouse-spring/jmouse-parallax-management/src/main/java/org/jmouse/parallax/management/CatalogueController.java).saveCategory |
| DELETE | `ParallaxManagementRoutes.PREFIX` | `"/catalogue/categories/{code}"` | [CatalogueController](../jmouse-spring/jmouse-parallax-management/src/main/java/org/jmouse/parallax/management/CatalogueController.java).deleteCategory |
| GET | `ParallaxManagementRoutes.PREFIX` | `"/catalogue/objects"` | [CatalogueController](../jmouse-spring/jmouse-parallax-management/src/main/java/org/jmouse/parallax/management/CatalogueController.java).browse |
| POST | `ParallaxManagementRoutes.PREFIX` | `"/catalogue/objects/summaries"` | [CatalogueController](../jmouse-spring/jmouse-parallax-management/src/main/java/org/jmouse/parallax/management/CatalogueController.java).summaries |
| GET | `ParallaxManagementRoutes.PREFIX` | `"/catalogue/objects/{objectId}"` | [CatalogueController](../jmouse-spring/jmouse-parallax-management/src/main/java/org/jmouse/parallax/management/CatalogueController.java).object |
| GET | `ParallaxManagementRoutes.PREFIX` | `"/catalogue/objects/{objectId}/revisions"` | [CatalogueController](../jmouse-spring/jmouse-parallax-management/src/main/java/org/jmouse/parallax/management/CatalogueController.java).revisions |
| GET | `ParallaxManagementRoutes.PREFIX` | `"/catalogue/objects/{objectId}/revisions/{revision}"` | [CatalogueController](../jmouse-spring/jmouse-parallax-management/src/main/java/org/jmouse/parallax/management/CatalogueController.java).revision |
| POST | `ParallaxManagementRoutes.PREFIX` | `"/catalogue/objects"` | [CatalogueController](../jmouse-spring/jmouse-parallax-management/src/main/java/org/jmouse/parallax/management/CatalogueController.java).create |
| PUT | `ParallaxManagementRoutes.PREFIX` | `"/catalogue/objects/{objectId}"` | [CatalogueController](../jmouse-spring/jmouse-parallax-management/src/main/java/org/jmouse/parallax/management/CatalogueController.java).update |
| POST | `ParallaxManagementRoutes.PREFIX` | `"/catalogue/objects/{objectId}/shipped"` | [CatalogueController](../jmouse-spring/jmouse-parallax-management/src/main/java/org/jmouse/parallax/management/CatalogueController.java).takeShippedText |
| PUT | `ParallaxManagementRoutes.PREFIX` | `"/catalogue/objects/{objectId}/status"` | [CatalogueController](../jmouse-spring/jmouse-parallax-management/src/main/java/org/jmouse/parallax/management/CatalogueController.java).changeStatus |
| DELETE | `ParallaxManagementRoutes.PREFIX` | `"/catalogue/objects/{objectId}"` | [CatalogueController](../jmouse-spring/jmouse-parallax-management/src/main/java/org/jmouse/parallax/management/CatalogueController.java).delete |
| GET | `ParallaxManagementRoutes.PREFIX` | `ParallaxManagementRoutes.OWNER + "/objects"` | [CatalogueController](../jmouse-spring/jmouse-parallax-management/src/main/java/org/jmouse/parallax/management/CatalogueController.java).ownedObjects |
| POST | `ParallaxManagementRoutes.PREFIX` | `ParallaxManagementRoutes.OWNER + "/objects"` | [CatalogueController](../jmouse-spring/jmouse-parallax-management/src/main/java/org/jmouse/parallax/management/CatalogueController.java).createOwned |
| GET | `ParallaxManagementRoutes.PREFIX` | `"/instruments"` | [CatalogueController](../jmouse-spring/jmouse-parallax-management/src/main/java/org/jmouse/parallax/management/CatalogueController.java).instruments |
| GET | `ParallaxManagementRoutes.PREFIX` | `"/instruments/{code}"` | [CatalogueController](../jmouse-spring/jmouse-parallax-management/src/main/java/org/jmouse/parallax/management/CatalogueController.java).instrument |
| PUT | `ParallaxManagementRoutes.PREFIX` | `"/instruments/{code}"` | [CatalogueController](../jmouse-spring/jmouse-parallax-management/src/main/java/org/jmouse/parallax/management/CatalogueController.java).saveInstrument |
| GET | `ParallaxManagementRoutes.PREFIX` | `"/representations"` | [CatalogueController](../jmouse-spring/jmouse-parallax-management/src/main/java/org/jmouse/parallax/management/CatalogueController.java).representations |
| GET | `ParallaxManagementRoutes.PREFIX` | `"/collections"` | [CollectionController](../jmouse-spring/jmouse-parallax-management/src/main/java/org/jmouse/parallax/management/CollectionController.java).curated |
| GET | `ParallaxManagementRoutes.PREFIX` | `"/me/collections"` | [CollectionController](../jmouse-spring/jmouse-parallax-management/src/main/java/org/jmouse/parallax/management/CollectionController.java).mine |
| GET | `ParallaxManagementRoutes.PREFIX` | `"/collections/{collectionId}"` | [CollectionController](../jmouse-spring/jmouse-parallax-management/src/main/java/org/jmouse/parallax/management/CollectionController.java).collection |
| POST | `ParallaxManagementRoutes.PREFIX` | `"/collections"` | [CollectionController](../jmouse-spring/jmouse-parallax-management/src/main/java/org/jmouse/parallax/management/CollectionController.java).create |
| PUT | `ParallaxManagementRoutes.PREFIX` | `"/collections/{collectionId}"` | [CollectionController](../jmouse-spring/jmouse-parallax-management/src/main/java/org/jmouse/parallax/management/CollectionController.java).update |
| DELETE | `ParallaxManagementRoutes.PREFIX` | `"/collections/{collectionId}"` | [CollectionController](../jmouse-spring/jmouse-parallax-management/src/main/java/org/jmouse/parallax/management/CollectionController.java).delete |
| POST | `ParallaxManagementRoutes.PREFIX` | `"/collections/{collectionId}/items"` | [CollectionController](../jmouse-spring/jmouse-parallax-management/src/main/java/org/jmouse/parallax/management/CollectionController.java).add |
| DELETE | `ParallaxManagementRoutes.PREFIX` | `"/collections/{collectionId}/items/{itemId}"` | [CollectionController](../jmouse-spring/jmouse-parallax-management/src/main/java/org/jmouse/parallax/management/CollectionController.java).remove |
| PUT | `ParallaxManagementRoutes.PREFIX` | `"/collections/{collectionId}/order"` | [CollectionController](../jmouse-spring/jmouse-parallax-management/src/main/java/org/jmouse/parallax/management/CollectionController.java).reorder |
| POST | `ParallaxManagementRoutes.PREFIX` | `"/computations"` | [ComputationController](../jmouse-spring/jmouse-parallax-management/src/main/java/org/jmouse/parallax/management/ComputationController.java).compute |
| POST | `ParallaxManagementRoutes.PREFIX` | `"/computations/frames"` | [ComputationController](../jmouse-spring/jmouse-parallax-management/src/main/java/org/jmouse/parallax/management/ComputationController.java).frames |
| POST | `ParallaxManagementRoutes.PREFIX` | `"/computations/estimate"` | [ComputationController](../jmouse-spring/jmouse-parallax-management/src/main/java/org/jmouse/parallax/management/ComputationController.java).estimate |
| GET | `ParallaxManagementRoutes.PREFIX` | `ParallaxManagementRoutes.OWNER + "/jobs"` | [ComputationController](../jmouse-spring/jmouse-parallax-management/src/main/java/org/jmouse/parallax/management/ComputationController.java).ownedJobs |
| GET | `ParallaxManagementRoutes.PREFIX` | `"/jobs/{jobId}"` | [ComputationController](../jmouse-spring/jmouse-parallax-management/src/main/java/org/jmouse/parallax/management/ComputationController.java).job |
| GET | `ParallaxManagementRoutes.PREFIX` | `"/jobs/{jobId}/dataset"` | [ComputationController](../jmouse-spring/jmouse-parallax-management/src/main/java/org/jmouse/parallax/management/ComputationController.java).dataset |
| POST | `ParallaxManagementRoutes.PREFIX` | `"/jobs/{jobId}/cancel"` | [ComputationController](../jmouse-spring/jmouse-parallax-management/src/main/java/org/jmouse/parallax/management/ComputationController.java).cancel |
| POST | `ParallaxManagementRoutes.PREFIX` | `"/jobs/{jobId}/retry"` | [ComputationController](../jmouse-spring/jmouse-parallax-management/src/main/java/org/jmouse/parallax/management/ComputationController.java).retry |
| GET | `ParallaxManagementRoutes.PREFIX + "/jmx"` | `"/schema"` | [DocumentController](../jmouse-spring/jmouse-parallax-management/src/main/java/org/jmouse/parallax/management/DocumentController.java).schema |
| POST | `ParallaxManagementRoutes.PREFIX + "/jmx"` | `"/form"` | [DocumentController](../jmouse-spring/jmouse-parallax-management/src/main/java/org/jmouse/parallax/management/DocumentController.java).form |
| POST | `ParallaxManagementRoutes.PREFIX + "/jmx"` | `"/edit"` | [DocumentController](../jmouse-spring/jmouse-parallax-management/src/main/java/org/jmouse/parallax/management/DocumentController.java).edit |
| POST | `ParallaxManagementRoutes.PREFIX + "/jmx"` | `"/schematic"` | [DocumentController](../jmouse-spring/jmouse-parallax-management/src/main/java/org/jmouse/parallax/management/DocumentController.java).schematic |
| POST | `ParallaxManagementRoutes.PREFIX + "/jmx"` | `"/drawing"` | [DocumentController](../jmouse-spring/jmouse-parallax-management/src/main/java/org/jmouse/parallax/management/DocumentController.java).drawing |
| GET | `ParallaxManagementRoutes.PREFIX + "/formula"` | `"/reference"` | [FormulaController](../jmouse-spring/jmouse-parallax-management/src/main/java/org/jmouse/parallax/management/FormulaController.java).reference |
| POST | `ParallaxManagementRoutes.PREFIX + "/formula"` | `"/interpret"` | [FormulaController](../jmouse-spring/jmouse-parallax-management/src/main/java/org/jmouse/parallax/management/FormulaController.java).interpret |
| POST | `ParallaxManagementRoutes.PREFIX + "/formula"` | `"/render"` | [FormulaController](../jmouse-spring/jmouse-parallax-management/src/main/java/org/jmouse/parallax/management/FormulaController.java).render |
| GET | `ParallaxManagementRoutes.PREFIX + ParallaxManagementRoutes.ADMINISTRATION` | `"/jobs"` | [ParallaxAdministrationController](../jmouse-spring/jmouse-parallax-management/src/main/java/org/jmouse/parallax/management/ParallaxAdministrationController.java).allJobs |
| GET | `ParallaxManagementRoutes.PREFIX + ParallaxManagementRoutes.ADMINISTRATION` | `"/computation"` | [ParallaxAdministrationController](../jmouse-spring/jmouse-parallax-management/src/main/java/org/jmouse/parallax/management/ParallaxAdministrationController.java).cache |
| GET | `ParallaxManagementRoutes.PREFIX + ParallaxManagementRoutes.ADMINISTRATION` | `"/computation/activity"` | [ParallaxAdministrationController](../jmouse-spring/jmouse-parallax-management/src/main/java/org/jmouse/parallax/management/ParallaxAdministrationController.java).activity |
| GET | `ParallaxManagementRoutes.PREFIX + ParallaxManagementRoutes.ADMINISTRATION` | `"/computation/warmup"` | [ParallaxAdministrationController](../jmouse-spring/jmouse-parallax-management/src/main/java/org/jmouse/parallax/management/ParallaxAdministrationController.java).warmupStatus |
| POST | `ParallaxManagementRoutes.PREFIX + ParallaxManagementRoutes.ADMINISTRATION` | `"/computation/warmup"` | [ParallaxAdministrationController](../jmouse-spring/jmouse-parallax-management/src/main/java/org/jmouse/parallax/management/ParallaxAdministrationController.java).startWarmup |
| GET | `ParallaxManagementRoutes.PREFIX + ParallaxManagementRoutes.ADMINISTRATION` | `"/statistics"` | [ParallaxAdministrationController](../jmouse-spring/jmouse-parallax-management/src/main/java/org/jmouse/parallax/management/ParallaxAdministrationController.java).statistics |
| GET | `ParallaxManagementRoutes.PREFIX + ParallaxManagementRoutes.ADMINISTRATION` | `"/evaluation/checks"` | [ParallaxAdministrationController](../jmouse-spring/jmouse-parallax-management/src/main/java/org/jmouse/parallax/management/ParallaxAdministrationController.java).checks |
| GET | `ParallaxManagementRoutes.PREFIX + ParallaxManagementRoutes.ADMINISTRATION` | `"/evaluation/runs"` | [ParallaxAdministrationController](../jmouse-spring/jmouse-parallax-management/src/main/java/org/jmouse/parallax/management/ParallaxAdministrationController.java).runs |
| GET | `ParallaxManagementRoutes.PREFIX + ParallaxManagementRoutes.ADMINISTRATION` | `"/evaluation/runs/{runId}"` | [ParallaxAdministrationController](../jmouse-spring/jmouse-parallax-management/src/main/java/org/jmouse/parallax/management/ParallaxAdministrationController.java).report |
| POST | `ParallaxManagementRoutes.PREFIX + ParallaxManagementRoutes.ADMINISTRATION` | `"/evaluation/runs"` | [ParallaxAdministrationController](../jmouse-spring/jmouse-parallax-management/src/main/java/org/jmouse/parallax/management/ParallaxAdministrationController.java).run |
| GET | `ParallaxManagementRoutes.PREFIX` | `ParallaxManagementRoutes.OWNER + "/scenes"` | [SceneController](../jmouse-spring/jmouse-parallax-management/src/main/java/org/jmouse/parallax/management/SceneController.java).ownedScenes |
| POST | `ParallaxManagementRoutes.PREFIX` | `ParallaxManagementRoutes.OWNER + "/scenes"` | [SceneController](../jmouse-spring/jmouse-parallax-management/src/main/java/org/jmouse/parallax/management/SceneController.java).create |
| GET | `ParallaxManagementRoutes.PREFIX` | `ParallaxManagementRoutes.REALM + "/scenes/recent"` | [SceneController](../jmouse-spring/jmouse-parallax-management/src/main/java/org/jmouse/parallax/management/SceneController.java).recent |
| GET | `ParallaxManagementRoutes.PREFIX` | `"/scenes/{sceneId}"` | [SceneController](../jmouse-spring/jmouse-parallax-management/src/main/java/org/jmouse/parallax/management/SceneController.java).scene |
| PUT | `ParallaxManagementRoutes.PREFIX` | `"/scenes/{sceneId}/document"` | [SceneController](../jmouse-spring/jmouse-parallax-management/src/main/java/org/jmouse/parallax/management/SceneController.java).save |
| PUT | `ParallaxManagementRoutes.PREFIX` | `"/scenes/{sceneId}"` | [SceneController](../jmouse-spring/jmouse-parallax-management/src/main/java/org/jmouse/parallax/management/SceneController.java).describe |
| GET | `ParallaxManagementRoutes.PREFIX` | `"/scenes/{sceneId}/revisions"` | [SceneController](../jmouse-spring/jmouse-parallax-management/src/main/java/org/jmouse/parallax/management/SceneController.java).revisions |
| GET | `ParallaxManagementRoutes.PREFIX` | `"/scenes/{sceneId}/revisions/{number}"` | [SceneController](../jmouse-spring/jmouse-parallax-management/src/main/java/org/jmouse/parallax/management/SceneController.java).revision |
| POST | `ParallaxManagementRoutes.PREFIX` | `"/scenes/{sceneId}/revisions/{number}/restore"` | [SceneController](../jmouse-spring/jmouse-parallax-management/src/main/java/org/jmouse/parallax/management/SceneController.java).restore |
| POST | `ParallaxManagementRoutes.PREFIX` | `"/scenes/{sceneId}/fork"` | [SceneController](../jmouse-spring/jmouse-parallax-management/src/main/java/org/jmouse/parallax/management/SceneController.java).fork |
| PUT | `ParallaxManagementRoutes.PREFIX` | `"/scenes/{sceneId}/archived"` | [SceneController](../jmouse-spring/jmouse-parallax-management/src/main/java/org/jmouse/parallax/management/SceneController.java).archive |
| DELETE | `ParallaxManagementRoutes.PREFIX` | `"/scenes/{sceneId}"` | [SceneController](../jmouse-spring/jmouse-parallax-management/src/main/java/org/jmouse/parallax/management/SceneController.java).delete |
| GET | `ParallaxManagementRoutes.PREFIX` | `"/scenes/{sceneId}/snapshots"` | [SceneController](../jmouse-spring/jmouse-parallax-management/src/main/java/org/jmouse/parallax/management/SceneController.java).snapshots |
| POST | `ParallaxManagementRoutes.PREFIX` | `"/scenes/{sceneId}/snapshots"` | [SceneController](../jmouse-spring/jmouse-parallax-management/src/main/java/org/jmouse/parallax/management/SceneController.java).snapshot |
| DELETE | `ParallaxManagementRoutes.PREFIX` | `"/scenes/{sceneId}/snapshots/{snapshotId}"` | [SceneController](../jmouse-spring/jmouse-parallax-management/src/main/java/org/jmouse/parallax/management/SceneController.java).deleteSnapshot |
| GET | `ParallaxManagementRoutes.PREFIX` | `ParallaxManagementRoutes.OWNER + "/comparisons"` | [SceneController](../jmouse-spring/jmouse-parallax-management/src/main/java/org/jmouse/parallax/management/SceneController.java).ownedComparisons |
| POST | `ParallaxManagementRoutes.PREFIX` | `ParallaxManagementRoutes.OWNER + "/comparisons"` | [SceneController](../jmouse-spring/jmouse-parallax-management/src/main/java/org/jmouse/parallax/management/SceneController.java).createComparison |
| GET | `ParallaxManagementRoutes.PREFIX` | `"/comparisons/{comparisonId}"` | [SceneController](../jmouse-spring/jmouse-parallax-management/src/main/java/org/jmouse/parallax/management/SceneController.java).comparison |
| PUT | `ParallaxManagementRoutes.PREFIX` | `"/comparisons/{comparisonId}"` | [SceneController](../jmouse-spring/jmouse-parallax-management/src/main/java/org/jmouse/parallax/management/SceneController.java).updateComparison |
| DELETE | `ParallaxManagementRoutes.PREFIX` | `"/comparisons/{comparisonId}"` | [SceneController](../jmouse-spring/jmouse-parallax-management/src/main/java/org/jmouse/parallax/management/SceneController.java).deleteComparison |
| POST | `ParallaxManagementRoutes.PREFIX + "/spice"` | `"/import"` | [SpiceController](../jmouse-spring/jmouse-parallax-management/src/main/java/org/jmouse/parallax/management/SpiceController.java).importNetlist |
| POST | `ParallaxManagementRoutes.PREFIX + "/spice"` | `"/export"` | [SpiceController](../jmouse-spring/jmouse-parallax-management/src/main/java/org/jmouse/parallax/management/SpiceController.java).exportDocument |
| POST | `ParallaxManagementRoutes.PREFIX + "/spice"` | `"/transient"` | [SpiceController](../jmouse-spring/jmouse-parallax-management/src/main/java/org/jmouse/parallax/management/SpiceController.java).simulateTransient |
| POST | `ParallaxManagementRoutes.PREFIX + "/subjects"` | `"/resolve"` | [SubjectController](../jmouse-spring/jmouse-parallax-management/src/main/java/org/jmouse/parallax/management/SubjectController.java).resolve |

## Lifecycle and adoption gotchas

ParallaxAccessRules.build() requires rule or PublicEndpoint coverage rather than accepting an incomplete controller gate. Installation administration spans all jobs, caches and evaluation runs and needs installation-wide permissions. Frames computation limits a request to 64 frames and a 4000ms budget. Document and SPICE edits return source; saving still happens through catalogue operations.

## Source and related reading

- [Build and dependencies](../jmouse-spring/jmouse-parallax-management/pom.xml).
- [Underlying library manual](JMOUSE_PARALLAX.md).
- [Spring integration family](JMOUSE_SPRING.md).

## Source reference

Direct dependencies below come from this module's [POM](../jmouse-spring/jmouse-parallax-management/pom.xml); test-only dependencies are omitted. Optional dependencies still require the adopter to supply the feature at runtime.

| Dependency | Declared scope | Optional |
|---|---|---|
| `org.jmouse:jmouse-parallax-spring-boot` | compile | no |
| `org.jmouse:jmouse-access-enforcement` | compile | no |
| `org.springframework:spring-web` | compile | no |
| `org.springframework:spring-webmvc` | compile | no |
| `org.springframework:spring-context` | compile | no |
| `jakarta.servlet:jakarta.servlet-api` | provided | no |

### Public type map by package

Top-level public types in production source, excluding smoke/example drivers. Links point to the implementation; roles summarize source JavaDoc or the declared contract. Nested builders, records and enum members belong to their enclosing type.

#### `org.jmouse.parallax.management`

| Type | Responsibility / contract |
|---|---|
| [CatalogueController](../jmouse-spring/jmouse-parallax-management/src/main/java/org/jmouse/parallax/management/CatalogueController.java) | 📚 The catalogue of objects, their categories, an owner's objects, the instruments, the component models and the representation vocabulary. |
| [CollectionController](../jmouse-spring/jmouse-parallax-management/src/main/java/org/jmouse/parallax/management/CollectionController.java) | 🗂️ Collections: the curated shelves, the formula sets, and a reader's own collections of objects and scenes. |
| [ComputationController](../jmouse-spring/jmouse-parallax-management/src/main/java/org/jmouse/parallax/management/ComputationController.java) | 🧮 Computing a representation — inline, queued or as an animation's frames — and the jobs a queued one becomes. |
| [DocumentController](../jmouse-spring/jmouse-parallax-management/src/main/java/org/jmouse/parallax/management/DocumentController.java) | The jMX language for editors: its schema, a text as a form, and a form's edit as the next text. Nothing is stored here; an editor saves the text through the catalogue. |
| [FormulaController](../jmouse-spring/jmouse-parallax-management/src/main/java/org/jmouse/parallax/management/FormulaController.java) | The jMF language: its reference, reading a typed formula, and printing formulas. Formula sets are collections of catalogue objects (/collections?purpose=FORMULA_SET), not a store of their own. |
| [OwnerAddress](../jmouse-spring/jmouse-parallax-management/src/main/java/org/jmouse/parallax/management/OwnerAddress.java) | 🏷️ This argument is the owner the address names, as one string — TYPE:id, the form org.jmouse.parallax.owner.ParallaxOwner#parse(String) reads. |
| [ParallaxAccessRules](../jmouse-spring/jmouse-parallax-management/src/main/java/org/jmouse/parallax/management/ParallaxAccessRules.java) | 🔐 What Parallax's routes require, said in a product's own vocabulary. |
| [ParallaxAdministrationController](../jmouse-spring/jmouse-parallax-management/src/main/java/org/jmouse/parallax/management/ParallaxAdministrationController.java) | 🛠️ The machinery's administration: every job of the installation, the dataset cache, the catalogue warm-up, what the server is busy with, the statistics a system screen shows, and the model-consistency evaluation runs. |
| [ParallaxManagementConfiguration](../jmouse-spring/jmouse-parallax-management/src/main/java/org/jmouse/parallax/management/ParallaxManagementConfiguration.java) | 🔌 Parallax's REST API. Imported by a product, never auto-configured — the routes appear because somebody typed @Import, and the same product then gates them: |
| [ParallaxManagementRoutes](../jmouse-spring/jmouse-parallax-management/src/main/java/org/jmouse/parallax/management/ParallaxManagementRoutes.java) | 🛣️ Where Parallax's routes live. |
| [ParallaxProblemDetails](../jmouse-spring/jmouse-parallax-management/src/main/java/org/jmouse/parallax/management/ParallaxProblemDetails.java) | 🚑 What a Parallax refusal answers with: an RFC 9457 problem whose status the refusal names, its sentence as the detail, and — beyond what the other libraries send — its code and whatever it carries beside it. |
| [SceneController](../jmouse-spring/jmouse-parallax-management/src/main/java/org/jmouse/parallax/management/SceneController.java) | 🎬 Scenes — their documents, revisions, snapshots and forks — and the comparisons that lay several side by side. |
| [SpiceController](../jmouse-spring/jmouse-parallax-management/src/main/java/org/jmouse/parallax/management/SpiceController.java) | SPICE in and out: a netlist as a jMX document, and a jMX circuit or component as a netlist. Nothing is stored here — an editor saves what comes back through the catalogue —, and what a part names is read from the components the reader may see, as an editor's checks read them. |
| [SubjectController](../jmouse-spring/jmouse-parallax-management/src/main/java/org/jmouse/parallax/management/SubjectController.java) | A subject — a catalogue object or a typed formula — as the one object description every screen draws from. |

### Verification boundary

This documentation was checked against the current source tree. No Java build, database migration, external API call or runtime scenario is claimed by this documentation change.
