# jmouse-telegram-user — user sessions behind an MTProto adapter

> **Agent orientation file.** Code wins when this description differs.
> Module: `jmouse-telegram-user` · Package: `org.jmouse.telegram.user` · Java 21.

## What it is

The user-account side of the Telegram gateway: login states, session/key handling, user transport,
channel history and user-only administration. The actual MTProto client is an adopter-supplied implementation
of `UserClient`; this artifact does not bundle TDLib/native binaries or provide a working MTProto connection
merely by appearing on the classpath. Bot API transport is [a separate module](JMOUSE_TELEGRAM_BOT.md).

## The map

| Type | Responsibility |
|---|---|
| [UserClient](../jmouse-telegram-user/src/main/java/org/jmouse/telegram/user/UserClient.java) / [UserClientFactory](../jmouse-telegram-user/src/main/java/org/jmouse/telegram/user/UserClientFactory.java) | Product adapter for one MTProto account and the factory that opens it |
| [LoginState](../jmouse-telegram-user/src/main/java/org/jmouse/telegram/user/LoginState.java) | Explicit phone/code/password/ready states for an interactive login flow |
| [UserSessions](../jmouse-telegram-user/src/main/java/org/jmouse/telegram/user/UserSessions.java) / [UserSession](../jmouse-telegram-user/src/main/java/org/jmouse/telegram/user/UserSession.java) | Derive one session directory and encryption key from its name |
| [UserAccounts](../jmouse-telegram-user/src/main/java/org/jmouse/telegram/user/UserAccounts.java) | Lazily open clients, require login readiness, connect updates, log out and close |
| [UserTransport](../jmouse-telegram-user/src/main/java/org/jmouse/telegram/user/UserTransport.java) | Implement the Telegram gateway's USER transport over ready accounts |
| [ChannelReader](../jmouse-telegram-user/src/main/java/org/jmouse/telegram/user/ChannelReader.java) / [HistoryPage](../jmouse-telegram-user/src/main/java/org/jmouse/telegram/user/HistoryPage.java) | Join/read channels and page through history, newest first |
| [UserAdministration](../jmouse-telegram-user/src/main/java/org/jmouse/telegram/user/UserAdministration.java) | User-only chat/group administration through the shared capability interface |

## Session and login flow

1. Construct `UserSessions(root, masterKey)` or `fromBase64Key(root, key)`. The key must have at least 32 bytes.
2. Supply `UserClientFactory` and create `UserAccounts(sessions, factory)`.
3. Drive `accounts.client(name)` through `submitPhoneNumber`, `submitCode` and `submitPassword` according
   to `LoginState`. The API does not block waiting for a person to type.
4. Use `accounts.ready(name)` for operations requiring a signed-in account. It refuses an incomplete login
   with an `Unauthorized` Telegram refusal carrying the next step.
5. Route incoming updates through `accounts.connect(name, dispatcher)` when the same update handlers
   should process bot and user updates.
6. Close `UserAccounts` at shutdown. `logOut(name)` signs out and forgets the cached client.

## Key and transport semantics

Names match `[A-Za-z0-9_-]{1,64}` because they become directory names. Each session key is
`HMAC-SHA256(masterKey, "jmouse-telegram-user:" + name)`. The adapter receives that key and directory in
`UserSession`; implementing encrypted session storage is the adapter's obligation. The master key is not
written by `UserSessions`. Losing it makes existing encrypted sessions unusable and requires login again.

A USER identity's credential is the session name, not the master key or login code. `UserTransport` uses
the accounts registry to reach its client. `history(chat, olderThan, limit)` uses the preceding page's
message id as a cursor, with null selecting the newest page. `join` accepts a public name or invitation.
Adapters report failures as `TelegramException` with the common refusal taxonomy, including a precise
`FloodWait` delay. Preserve that delay rather than turning every failure into an immediate retry.

No generic product authentication screen is mounted here. Wire the factory, session policy and account
lifecycle explicitly or follow the optional USER wiring in [the Telegram starter](JMOUSE_TELEGRAM_SPRING_BOOT.md).

## Source reference

Direct dependencies below come from this module's [POM](../jmouse-telegram-user/pom.xml); test-only dependencies are omitted. Optional dependencies still require the adopter to supply the feature at runtime.

| Dependency | Declared scope | Optional |
|---|---|---|
| `org.jmouse:jmouse-telegram` | compile | no |
| `org.slf4j:slf4j-api` | compile | no |
| `ch.qos.logback:logback-classic` | compile | yes |

### Public type map by package

Top-level public types in production source, excluding smoke/example drivers. Links point to the implementation; roles summarize source JavaDoc or the declared contract. Nested builders, records and enum members belong to their enclosing type.

#### `org.jmouse.telegram.user`

| Type | Responsibility / contract |
|---|---|
| [ChannelReader](../jmouse-telegram-user/src/main/java/org/jmouse/telegram/user/ChannelReader.java) | Reading channels and groups as a person — what a bot cannot do at all: join by itself, or read anything posted before it arrived. |
| [HistoryPage](../jmouse-telegram-user/src/main/java/org/jmouse/telegram/user/HistoryPage.java) | A slice of a chat's history, newest first. |
| [LoginState](../jmouse-telegram-user/src/main/java/org/jmouse/telegram/user/LoginState.java) | Where a person's login stands — explicit states rather than a method that blocks, because two of the steps wait for somebody to type something. |
| [UserAccounts](../jmouse-telegram-user/src/main/java/org/jmouse/telegram/user/UserAccounts.java) | Every person's account this application has open, by session name. |
| [UserAdministration](../jmouse-telegram-user/src/main/java/org/jmouse/telegram/user/UserAdministration.java) | Chat administration as a person — today only what a bot cannot: creating a group . |
| [UserClient](../jmouse-telegram-user/src/main/java/org/jmouse/telegram/user/UserClient.java) | One person's Telegram account, signed in over MTProto — ⚠️ implemented by the product . |
| [UserClientFactory](../jmouse-telegram-user/src/main/java/org/jmouse/telegram/user/UserClientFactory.java) | Opens a UserClient for a session — the one thing the product's MTProto adapter has to provide. |
| [UserSession](../jmouse-telegram-user/src/main/java/org/jmouse/telegram/user/UserSession.java) | What an MTProto library needs to keep one person's login: a directory for its database and a key to encrypt that database with. |
| [UserSessions](../jmouse-telegram-user/src/main/java/org/jmouse/telegram/user/UserSessions.java) | Where sessions live, and the key each one is encrypted with. |
| [UserTransport](../jmouse-telegram-user/src/main/java/org/jmouse/telegram/user/UserTransport.java) | The USER transport: sending as a person, through the gateway every product already calls. |

### Verification boundary

This documentation was checked against the current source tree. No Java build, database migration, external API call or runtime scenario is claimed by this documentation change.
