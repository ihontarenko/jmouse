# jmouse-telegram-command — commands acting as a bound subject

> **Agent orientation file.** Source code wins.
> Module: `jmouse-telegram-command` · Package: `org.jmouse.telegram.command` · Java 21.

## What it is

The command layer over [Telegram updates and the gateway](JMOUSE_TELEGRAM.md). It turns bot commands,
action buttons and optional attachment intake into product handlers, after resolving a bound subject and
asking the product's command guard. It neither trusts a Telegram username as a product credential nor owns
the product's authorization policy.

## The map

| Type | Responsibility |
|---|---|
| [BotCommand](../jmouse-telegram-command/src/main/java/org/jmouse/telegram/command/BotCommand.java) | Command declaration, presentation, handler and optional file-receiver capability |
| [CommandRouter](../jmouse-telegram-command/src/main/java/org/jmouse/telegram/command/CommandRouter.java) | Register update routes, resolve senders, authorize and submit handlers to an executor |
| [SenderResolver](../jmouse-telegram-command/src/main/java/org/jmouse/telegram/command/SenderResolver.java) / [BindingsSenderResolver](../jmouse-telegram-command/src/main/java/org/jmouse/telegram/command/BindingsSenderResolver.java) | Turn a verified Telegram binding into the product's subject |
| [CommandGuard](../jmouse-telegram-command/src/main/java/org/jmouse/telegram/command/CommandGuard.java) | Product authorization before handlers and subsequent page turns |
| [CommandHandler](../jmouse-telegram-command/src/main/java/org/jmouse/telegram/command/CommandHandler.java) / [CommandContext](../jmouse-telegram-command/src/main/java/org/jmouse/telegram/command/CommandContext.java) | Work and its authenticated subject, arguments, chat, attachments and reply helpers |
| [CommandAction](../jmouse-telegram-command/src/main/java/org/jmouse/telegram/command/CommandAction.java) | Inline button representing another command invocation |
| [CommandPhrases](../jmouse-telegram-command/src/main/java/org/jmouse/telegram/command/CommandPhrases.java) | Replaceable sentences for help, refusal and failure |
| [TelegramFiles](../jmouse-telegram-command/src/main/java/org/jmouse/telegram/command/TelegramFiles.java) | Download attachments as storage content or store them in a `FileStore` |

## Installation and execution

Create `CommandRouter(gateway, senderResolver, guard, commands)` and call `install(updateDispatcher)`.
Use `gateway.as(purpose)` when commands must send under a particular identity. The default router uses
a virtual-thread-per-task executor and English phrases; the longer constructor accepts both collaborators.
Provide an application-managed executor when its lifecycle or context propagation matters.

The router indexes declarations once, preserving order for help and menu output. `/start` is reserved for
binding and is left alone by the command router; `/help` is library-owned. Duplicate names and declaring
either reserved command fail construction. At most one declaration can receive files without a command.
For those messages, the caption becomes arguments and the attachments remain available on the context.

An incoming invocation resolves `SenderResolver.subjectOf(message.from())`, refuses an unbound sender,
checks `CommandGuard.permits(subject, command)` and then runs the handler off the polling thread. Unknown
commands show help. `context.subject()` is the bound subject; use it in domain authorization rather than
substituting a Telegram sender id. Handler exceptions are logged and answered with the configured failure phrase.

## Replies and boundaries

`reply`, `replyLong`, `replyWithActions` and `replyDocument` supply the common output shapes.
Long replies keep page state in memory; a page-turn callback checks both the original subject and current
command permission again. Button data uses the router's prefixes and passes through the command path,
so a forwarded button does not grant its recipient the original sender's access.

`menu()` returns menu entries; the caller still publishes them through the Telegram API. `TelegramFiles`
integrates the storage byte contract; file naming, accepted content and product metadata remain the adopter's
policy. Use [Telegram bindings](JMOUSE_TELEGRAM_JPA.md) for authenticated linking and
[Spring wiring](JMOUSE_TELEGRAM_SPRING_BOOT.md) when appropriate.

## Source reference

Direct dependencies below come from this module's [POM](../jmouse-telegram-command/pom.xml); test-only dependencies are omitted. Optional dependencies still require the adopter to supply the feature at runtime.

| Dependency | Declared scope | Optional |
|---|---|---|
| `org.jmouse:jmouse-telegram` | compile | no |
| `org.jmouse:jmouse-storage` | compile | no |
| `org.jmouse:jmouse-telegram-jpa` | compile | yes |
| `org.slf4j:slf4j-api` | compile | no |
| `ch.qos.logback:logback-classic` | compile | yes |

### Public type map by package

Top-level public types in production source, excluding smoke/example drivers. Links point to the implementation; roles summarize source JavaDoc or the declared contract. Nested builders, records and enum members belong to their enclosing type.

#### `org.jmouse.telegram.command`

| Type | Responsibility / contract |
|---|---|
| [BindingsSenderResolver](../jmouse-telegram-command/src/main/java/org/jmouse/telegram/command/BindingsSenderResolver.java) | Senders resolved through the person bindings jmouse-telegram-jpa keeps. |
| [BotCommand](../jmouse-telegram-command/src/main/java/org/jmouse/telegram/command/BotCommand.java) | One /command a product offers through its bot. |
| [CommandAction](../jmouse-telegram-command/src/main/java/org/jmouse/telegram/command/CommandAction.java) | A button under an answer that runs a command — "Open" beside a document runs /read . |
| [CommandContext](../jmouse-telegram-command/src/main/java/org/jmouse/telegram/command/CommandContext.java) | Everything a handler needs: who asked, as whom they act, what they wrote, and how to answer. |
| [CommandGuard](../jmouse-telegram-command/src/main/java/org/jmouse/telegram/command/CommandGuard.java) | The product's word on whether a bound person may run a command — asked before every handler, and again before every page of a long answer is shown. |
| [CommandHandler](../jmouse-telegram-command/src/main/java/org/jmouse/telegram/command/CommandHandler.java) | What a command does. |
| [CommandPhrases](../jmouse-telegram-command/src/main/java/org/jmouse/telegram/command/CommandPhrases.java) | The sentences the router says on its own — replace them to speak the product's language. |
| [CommandRouter](../jmouse-telegram-command/src/main/java/org/jmouse/telegram/command/CommandRouter.java) | Bot commands that act as the bound person. |
| [SenderResolver](../jmouse-telegram-command/src/main/java/org/jmouse/telegram/command/SenderResolver.java) | Who a Telegram sender is in the product. |
| [TelegramFiles](../jmouse-telegram-command/src/main/java/org/jmouse/telegram/command/TelegramFiles.java) | The files people send a bot, as jMouse storage content. |

### Verification boundary

This documentation was checked against the current source tree. No Java build, database migration, external API call or runtime scenario is claimed by this documentation change.
