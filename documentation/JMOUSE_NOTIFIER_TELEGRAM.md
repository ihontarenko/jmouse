# jmouse-notifier-telegram — notifications delivered through Telegram

> **Agent orientation file.** Source code is authoritative.
> Module: `jmouse-notifier-telegram` · Package: `org.jmouse.notifier.telegram` · Java 21.

## What it is

A [notifier](JMOUSE_NOTIFIER.md) channel that resolves product subjects to Telegram addresses and sends
formatted drafts through the [Telegram gateway](JMOUSE_TELEGRAM.md). Recipient resolution and publication
idempotency belong to notifier core; Telegram transport, pacing and identities belong to Telegram core.

## The map

| Type | Responsibility |
|---|---|
| [TelegramChannel](../jmouse-notifier-telegram/src/main/java/org/jmouse/notifier/telegram/TelegramChannel.java) | Implement `Channel`, send under a purpose and translate Telegram refusals to delivery outcomes |
| [TelegramAddressBook](../jmouse-notifier-telegram/src/main/java/org/jmouse/notifier/telegram/TelegramAddressBook.java) | Product subject → reachable Telegram chats and unreachable-address feedback |
| [BindingsAddressBook](../jmouse-notifier-telegram/src/main/java/org/jmouse/notifier/telegram/BindingsAddressBook.java) | Address book over the authenticated binding records in `jmouse-telegram-jpa` |
| [TelegramAddresses](../jmouse-notifier-telegram/src/main/java/org/jmouse/notifier/telegram/TelegramAddresses.java) | Encode/decode chat references and optional forum topic identifiers |
| [TelegramMessageFormat](../jmouse-notifier-telegram/src/main/java/org/jmouse/notifier/telegram/TelegramMessageFormat.java) | HTML draft formatting, escaping, severity behavior and message-length handling |

## Wiring and delivery

```java
Channel telegram = new TelegramChannel(gateway, "notifications",
        new BindingsAddressBook(bindings));
```

Pass the channel to `Notifier` alongside the desired `NotificationStore` and `RecipientResolver`.
The purpose selects the Telegram identity; it is not a recipient or a notifier channel name.
`BindingsAddressBook` requires the optional Telegram JPA dependency at runtime. Implement the address-book
interface instead if the product keeps reachable chats elsewhere.

The binding subject must be the same identifier the notifier resolves. Binding an account id while
publishing to a different profile id yields no addresses, without making Telegram itself fail.
`TelegramAddresses` carries signed chat ids or usernames and an optional `#` topic suffix; use its
helpers rather than inventing a second serialization.

## Outcome mapping

| Telegram result | Notifier behavior |
|---|---|
| Successful send | `Delivered` receipt identifying chat/message for subsequent references |
| `FloodWait` | Retry after Telegram's supplied delay |
| `TransportFailure` | Retry after 30 seconds |
| `BotBlocked` / `ChatNotFound` | Permanent failure and address-book unreachable feedback |
| `ConversationNotStarted` | Failure while keeping the binding; the person must start the bot |
| Other refusal | Failed delivery with its refusal message |

Formatting escapes values as HTML and cuts by escaped length. INFO is silent; WARN/ALARM are audible.
The channel does not create its own retry thread or outbox; the notifier's retry operation/worker owns that.
A successful gateway send can precede a failed database update, so durable delivery remains at least once.
For automatic channel registration, see [the notifier starter](JMOUSE_NOTIFIER_SPRING_BOOT.md).

## Source reference

Direct dependencies below come from this module's [POM](../jmouse-notifier-telegram/pom.xml); test-only dependencies are omitted. Optional dependencies still require the adopter to supply the feature at runtime.

| Dependency | Declared scope | Optional |
|---|---|---|
| `org.jmouse:jmouse-notifier` | compile | no |
| `org.jmouse:jmouse-telegram` | compile | no |
| `org.jmouse:jmouse-telegram-jpa` | compile | yes |
| `org.slf4j:slf4j-api` | compile | no |
| `ch.qos.logback:logback-classic` | compile | yes |

### Public type map by package

Top-level public types in production source, excluding smoke/example drivers. Links point to the implementation; roles summarize source JavaDoc or the declared contract. Nested builders, records and enum members belong to their enclosing type.

#### `org.jmouse.notifier.telegram`

| Type | Responsibility / contract |
|---|---|
| [BindingsAddressBook](../jmouse-notifier-telegram/src/main/java/org/jmouse/notifier/telegram/BindingsAddressBook.java) | An address book over the person bindings jmouse-telegram-jpa keeps — the deep-link flow's result. |
| [TelegramAddressBook](../jmouse-notifier-telegram/src/main/java/org/jmouse/notifier/telegram/TelegramAddressBook.java) | Where a person is reachable on Telegram. |
| [TelegramAddresses](../jmouse-notifier-telegram/src/main/java/org/jmouse/notifier/telegram/TelegramAddresses.java) | How a Telegram chat is written as a notifier Address and read back. |
| [TelegramChannel](../jmouse-notifier-telegram/src/main/java/org/jmouse/notifier/telegram/TelegramChannel.java) | Notifications delivered as Telegram messages. |
| [TelegramMessageFormat](../jmouse-notifier-telegram/src/main/java/org/jmouse/notifier/telegram/TelegramMessageFormat.java) | How a publication looks as a Telegram message. |

### Verification boundary

This documentation was checked against the current source tree. No Java build, database migration, external API call or runtime scenario is claimed by this documentation change.

Existing checks/examples to consult when changing this API:

- [TelegramChannelSmoke.java](../jmouse-notifier-telegram/src/main/java/org/jmouse/notifier/telegram/smoke/TelegramChannelSmoke.java).
