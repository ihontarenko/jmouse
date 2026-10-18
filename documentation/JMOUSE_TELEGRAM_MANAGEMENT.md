# jmouse-telegram-management

> Spring integration module · Maven artifact `org.jmouse:jmouse-telegram-management` · source: [module POM](../jmouse-spring/jmouse-telegram-management/pom.xml).

## Purpose and boundaries

Provides the Telegram administration API for account identities, credentials, invitation bindings, chat administration and delivery journal reads. It composes the Telegram starter and persisted adapters; polling and webhook transport remain in the starter.

## How a product adopts it

Add the artifact and explicitly @Import(TelegramManagementConfiguration.class). Configure the Telegram gateway, journal and chat administration ports through the starter. Supply persisted accounts and bindings where editing is supported, and declare product access rules for the imported controllers.

The dependency surface is declared in the [module POM](../jmouse-spring/jmouse-telegram-management/pom.xml). This integration module uses these library artifacts:

- `jmouse-telegram-spring-boot`.
- `jmouse-telegram-jpa`.

## Runtime path

The configuration constructs account, binding, journal and chat controllers and their problem advice. Accounts and bindings are obtained through ObjectProvider so an installation can explain their unavailability. Write operations use a local TransactionTemplate when the product has a transaction manager; otherwise the adapter uses withoutTransaction().

## Key class responsibilities

| Type | Responsibility |
|---|---|
| [TelegramManagementConfiguration](../jmouse-spring/jmouse-telegram-management/src/main/java/org/jmouse/telegram/management/TelegramManagementConfiguration.java) | The Telegram administration API, mounted by the product: * @Import(TelegramManagementConfiguration.class) ⚠️ Imported, never auto-configured. |
| [TelegramAccountController](../jmouse-spring/jmouse-telegram-management/src/main/java/org/jmouse/telegram/management/TelegramAccountController.java) | Telegram accounts — one identity per purpose, kept in telegram_accounts. |
| [TelegramBindingController](../jmouse-spring/jmouse-telegram-management/src/main/java/org/jmouse/telegram/management/TelegramBindingController.java) | Who is bound to which chat — the deep-link flow's results — and the invitations that start one. |
| [TelegramJournalController](../jmouse-spring/jmouse-telegram-management/src/main/java/org/jmouse/telegram/management/TelegramJournalController.java) | The journal — every call to Telegram, newest first. |
| [TelegramChatController](../jmouse-spring/jmouse-telegram-management/src/main/java/org/jmouse/telegram/management/TelegramChatController.java) | One chat as a purpose's bot sees it — what it is, how many are in it, who administers it, and whether the bot is one of them. |
| [TelegramManagementProblems](../jmouse-spring/jmouse-telegram-management/src/main/java/org/jmouse/telegram/management/TelegramManagementProblems.java) | What a client of these controllers sees when something is refused — and nothing else's. |
| [ManagementPage](../jmouse-spring/jmouse-telegram-management/src/main/java/org/jmouse/telegram/management/ManagementPage.java) | One page of a listing, with what a table needs to draw its pager. |
| [AccountsUnavailableException](../jmouse-spring/jmouse-telegram-management/src/main/java/org/jmouse/telegram/management/AccountsUnavailableException.java) | Accounts or bindings were asked for in an installation that keeps none. |

## Registration and activation

This artifact has no `AutoConfiguration.imports` resource. Its controller/configuration classes must be registered through the adoption path described above.

Conditions below are copied from the linked source. They include configuration-level, nested-configuration and individual bean checks; the row is an inventory, not one conjunction applied to every bean. A classpath match alone does not provide the beans required by a constructor.

| Configuration source | Declared activation, ordering and backoff |
|---|---|
| [TelegramManagementConfiguration](../jmouse-spring/jmouse-telegram-management/src/main/java/org/jmouse/telegram/management/TelegramManagementConfiguration.java) | `@Configuration(proxyBeanMethods = false)` |

## Configuration contract

jmouse.telegram.management.prefix defaults to the shared management root plus /telegram/api. The module has no auto-configuration imports resource and no independent enabled property. Account identity, journal and transport configuration continue to use the settings in jmouse-telegram-spring-boot.

The definitions that own these settings and route placeholders are:

- [ManagementRoutes](../jmouse-spring/jmouse-telegram-management/src/main/java/org/jmouse/telegram/management/ManagementRoutes.java).

Route constants are expressions resolved by their consumer; `${…}` text is a placeholder, not a literal URL. Shared `ManagementEndpoints.ROOT` may itself be configured by the host.

| Constant | Exact source expression |
|---|---|
| [ManagementRoutes](../jmouse-spring/jmouse-telegram-management/src/main/java/org/jmouse/telegram/management/ManagementRoutes.java).SEGMENT | `"/telegram/api"` |
| [ManagementRoutes](../jmouse-spring/jmouse-telegram-management/src/main/java/org/jmouse/telegram/management/ManagementRoutes.java).DEFAULT_PREFIX | `ManagementEndpoints.DEFAULT_ROOT + SEGMENT` |
| [ManagementRoutes](../jmouse-spring/jmouse-telegram-management/src/main/java/org/jmouse/telegram/management/ManagementRoutes.java).PREFIX | `"${jmouse.telegram.management.prefix:" + ManagementEndpoints.ROOT + SEGMENT + "}"` |

## HTTP and protocol surface

Read the class prefix and method mapping together. Constant names resolve through the route definitions above; method parameters and response DTOs are in the linked handler source.

| Verb | Class-level prefix | Method mapping | Handler |
|---|---|---|---|
| GET | `ManagementRoutes.PREFIX + "/accounts"` | `(base)` | [TelegramAccountController](../jmouse-spring/jmouse-telegram-management/src/main/java/org/jmouse/telegram/management/TelegramAccountController.java).accounts |
| GET | `ManagementRoutes.PREFIX + "/accounts"` | `"/{purpose}"` | [TelegramAccountController](../jmouse-spring/jmouse-telegram-management/src/main/java/org/jmouse/telegram/management/TelegramAccountController.java).account |
| PUT | `ManagementRoutes.PREFIX + "/accounts"` | `"/{purpose}"` | [TelegramAccountController](../jmouse-spring/jmouse-telegram-management/src/main/java/org/jmouse/telegram/management/TelegramAccountController.java).save |
| PUT | `ManagementRoutes.PREFIX + "/accounts"` | `"/{purpose}/credential"` | [TelegramAccountController](../jmouse-spring/jmouse-telegram-management/src/main/java/org/jmouse/telegram/management/TelegramAccountController.java).rotate |
| PUT | `ManagementRoutes.PREFIX + "/accounts"` | `"/{purpose}/enabled"` | [TelegramAccountController](../jmouse-spring/jmouse-telegram-management/src/main/java/org/jmouse/telegram/management/TelegramAccountController.java).enable |
| DELETE | `ManagementRoutes.PREFIX + "/accounts"` | `"/{purpose}"` | [TelegramAccountController](../jmouse-spring/jmouse-telegram-management/src/main/java/org/jmouse/telegram/management/TelegramAccountController.java).remove |
| POST | `ManagementRoutes.PREFIX + "/accounts"` | `"/{purpose}/test-message"` | [TelegramAccountController](../jmouse-spring/jmouse-telegram-management/src/main/java/org/jmouse/telegram/management/TelegramAccountController.java).test |
| GET | `ManagementRoutes.PREFIX + "/bindings"` | `(base)` | [TelegramBindingController](../jmouse-spring/jmouse-telegram-management/src/main/java/org/jmouse/telegram/management/TelegramBindingController.java).bindings |
| POST | `ManagementRoutes.PREFIX + "/bindings"` | `"/invitations"` | [TelegramBindingController](../jmouse-spring/jmouse-telegram-management/src/main/java/org/jmouse/telegram/management/TelegramBindingController.java).invite |
| DELETE | `ManagementRoutes.PREFIX + "/bindings"` | `(base)` | [TelegramBindingController](../jmouse-spring/jmouse-telegram-management/src/main/java/org/jmouse/telegram/management/TelegramBindingController.java).remove |
| GET | `ManagementRoutes.PREFIX + "/chats"` | `"/{chat}"` | [TelegramChatController](../jmouse-spring/jmouse-telegram-management/src/main/java/org/jmouse/telegram/management/TelegramChatController.java).chat |
| GET | `ManagementRoutes.PREFIX + "/journal"` | `(base)` | [TelegramJournalController](../jmouse-spring/jmouse-telegram-management/src/main/java/org/jmouse/telegram/management/TelegramJournalController.java).journal |

## Lifecycle and adoption gotchas

Import the configuration beside the rule protecting its controllers: the module deliberately has no universal gate. Its local TransactionOperations is not published as a bean, avoiding replacement of the host TransactionTemplate. Account management and bindings require their backing adapters; adding these controllers does not manufacture those tables or a usable account editor.

## Source and related reading

- [Build and dependencies](../jmouse-spring/jmouse-telegram-management/pom.xml).
- [Underlying library manual](JMOUSE_TELEGRAM_SPRING_BOOT.md).
- [Spring integration family](JMOUSE_SPRING.md).

## Source reference

Direct dependencies below come from this module's [POM](../jmouse-spring/jmouse-telegram-management/pom.xml); test-only dependencies are omitted. Optional dependencies still require the adopter to supply the feature at runtime.

| Dependency | Declared scope | Optional |
|---|---|---|
| `org.jmouse:jmouse-telegram-spring-boot` | compile | no |
| `org.jmouse:jmouse-telegram-jpa` | compile | no |
| `org.springframework:spring-web` | compile | no |
| `org.springframework:spring-context` | compile | no |
| `org.springframework:spring-tx` | compile | no |
| `org.slf4j:slf4j-api` | compile | no |

### Public type map by package

Top-level public types in production source, excluding smoke/example drivers. Links point to the implementation; roles summarize source JavaDoc or the declared contract. Nested builders, records and enum members belong to their enclosing type.

#### `org.jmouse.telegram.management`

| Type | Responsibility / contract |
|---|---|
| [AccountsUnavailableException](../jmouse-spring/jmouse-telegram-management/src/main/java/org/jmouse/telegram/management/AccountsUnavailableException.java) | Accounts or bindings were asked for in an installation that keeps none. |
| [ManagementPage](../jmouse-spring/jmouse-telegram-management/src/main/java/org/jmouse/telegram/management/ManagementPage.java) | One page of a listing, with what a table needs to draw its pager. |
| [ManagementRoutes](../jmouse-spring/jmouse-telegram-management/src/main/java/org/jmouse/telegram/management/ManagementRoutes.java) | Where the Telegram administration API is served. |
| [TelegramAccountController](../jmouse-spring/jmouse-telegram-management/src/main/java/org/jmouse/telegram/management/TelegramAccountController.java) | Telegram accounts — one identity per purpose, kept in telegram_accounts. |
| [TelegramBindingController](../jmouse-spring/jmouse-telegram-management/src/main/java/org/jmouse/telegram/management/TelegramBindingController.java) | Who is bound to which chat — the deep-link flow's results — and the invitations that start one. |
| [TelegramChatController](../jmouse-spring/jmouse-telegram-management/src/main/java/org/jmouse/telegram/management/TelegramChatController.java) | One chat as a purpose's bot sees it — what it is, how many are in it, who administers it, and whether the bot is one of them. |
| [TelegramJournalController](../jmouse-spring/jmouse-telegram-management/src/main/java/org/jmouse/telegram/management/TelegramJournalController.java) | The journal — every call to Telegram, newest first. The "why did nothing arrive" screen. |
| [TelegramManagementConfiguration](../jmouse-spring/jmouse-telegram-management/src/main/java/org/jmouse/telegram/management/TelegramManagementConfiguration.java) | The Telegram administration API, mounted by the product: |
| [TelegramManagementProblems](../jmouse-spring/jmouse-telegram-management/src/main/java/org/jmouse/telegram/management/TelegramManagementProblems.java) | What a client of these controllers sees when something is refused — and nothing else's. |

### Verification boundary

This documentation was checked against the current source tree. No Java build, database migration, external API call or runtime scenario is claimed by this documentation change.
