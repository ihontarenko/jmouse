# jmouse-telegram-spring-boot — the wiring

> **Agent orientation file.** Written so you do not have to read the module to use it. If something
> here disagrees with the code, the code wins — and fix this file in the same change.
>
> Module: `jmouse-spring/jmouse-telegram-spring-boot` · Package: `org.jmouse.telegram.spring` · Java 21
> Depends on: `jmouse-telegram`, `jmouse-telegram-bot`, `spring-boot-autoconfigure` (optional),
> `spring-web` (optional — only the webhook needs it), `jackson-databind`
> Read [`JMOUSE_TELEGRAM.md`](JMOUSE_TELEGRAM.md) and [`JMOUSE_TELEGRAM_BOT.md`](JMOUSE_TELEGRAM_BOT.md) first.
> Tracker: `JMF-333`

## Adopting it — the whole story

```xml
<dependency>
    <groupId>org.jmouse</groupId>
    <artifactId>jmouse-telegram-spring-boot</artifactId>
</dependency>
```

```yaml
jmouse:
  telegram:
    identities:
      general:
        kind: bot
        token: ${TELEGRAM_BOT_TOKEN}
```

```java
@Service
class OpenedNotices {

    private final TelegramGateway gateway;   // injected — nothing else to configure

    void announce(long chatId, String title) {
        gateway.as("kitsu-notifications")
               .send(ChatReference.of(chatId), MessageDraft.text(title + " was opened"));
    }
}
```

That is a **sending** application. Receiving needs one more line — see *Ingestion*.

## The configuration surface

⚠️ **These keys are a published contract.** Every adopting product writes them into its own
configuration, so renaming one is a break in each of them at once.

```yaml
jmouse:
  telegram:
    identities:                        # purpose → identity. The KEY is the purpose.
      general:                         # ⚠️ the fallback purpose (IdentitySource.GENERAL)
        kind: bot                      # bot | user
        token: ${TELEGRAM_BOT_TOKEN}
        api-base:                      # a self-hosted telegram-bot-api server; past the 50 MB ceiling
        enabled: true
      kitsu-notifications:             # a purpose the PRODUCT names; the library never enumerates one
        kind: bot
        token: ${KITSU_BOT_TOKEN}
    updates:
      mode: none                       # none | polling | webhook   ⚠️ default is none
      poll-timeout: 50s
      allowed: [message, callback_query]    # ⚠️ Telegram's own default EXCLUDES chat_member
      webhook:
        path: /jmouse/telegram/webhook
        secret: ${TELEGRAM_WEBHOOK_SECRET}  # ⚠️ startup FAILS without it in webhook mode
    pace:
      enabled: true
      global-per-second: 30
      per-chat-per-minute: 20
```

Bound with Spring's `Binder`, not `@ConfigurationProperties`-scanned, so `TelegramSettings` stays a plain
record a smoke class or a test can construct with no context.

⚠️ **A token belongs in an environment variable.** `TelegramSettings.Identity.toString()` and
`Webhook.toString()` redact theirs, as `TelegramIdentity` does.

## The beans

All are `@ConditionalOnMissingBean` — **every bean steps aside**, so a product that wants its own
declares one and this configuration goes quiet about that one thing only.

| Bean | Default | Replace it when |
|---|---|---|
| `TelegramSettings` | bound from `jmouse.telegram` | never, normally |
| `IdentitySource` | `PropertiesIdentitySource` | you have a store — that is `JMF-334` |
| `BotApiTransport` | `new BotApiTransport()` | you need different timeouts |
| `Pace` | `TokenBucketPace` from `pace.*`, or `Pace.unlimited()` when disabled | ⚠️ you run more than one instance |
| `TelegramGateway` | `RoutingGateway` over every `TelegramTransport`, each wrapped in `PacedTransport` | you have transports this cannot compose |
| `UpdateDispatcher` | `new UpdateDispatcher()` | never, normally |
| `TelegramDiagnostics` | logs one startup line | never |

⚠️ **Pacing wraps each transport inside the gateway bean and is NOT itself a bean.** A `PacedTransport`
published as a bean would be a *second* `TelegramTransport` of the same kind, which `RoutingGateway`
refuses — correctly, since two transports for one protocol would otherwise resolve to whichever was last
in the list. Because the gateway wraps the injected `List<TelegramTransport>`, a product's own transport
**is** picked up and paced like the rest.

## Ingestion — one switch, three states

`jmouse.telegram.updates.mode`:

| Mode | What appears | When |
|---|---|---|
| `none` **(default)** | nothing | an application that only sends |
| `polling` | a `LongPolling` bean, started and stopped with the context | ⚠️ development on this machine |
| `webhook` | `TelegramWebhookController` | production, with public HTTPS |

⚠️ **`none` being the default matters.** A stray polling loop is worse than useless: Telegram delivers a
bot's updates to exactly one consumer, so it would *take* them away from whatever was meant to have them.

Registering handlers is the same in both modes — inject the dispatcher:

```java
@Component
class TelegramHandlers {

    TelegramHandlers(UpdateDispatcher dispatcher, TelegramGateway gateway) {
        dispatcher.onCommand("start", update -> { /* the binding flow */ });
        dispatcher.onCallback("open:", update -> { /* a button press */ });
    }
}
```

### `polling`

`@Bean(initMethod = "start")`, and `LongPolling` is `AutoCloseable`, so **Spring stops it on shutdown**.
⚠️ Without that, a dev restart leaves the old loop holding the bot's updates and the new one appears to
receive half of everything at random.

### `webhook`

Path default: **`/jmouse/telegram/webhook`**. ⚠️ Under `/jmouse` rather than `/telegram` for the reason
`org.jmouse.core.management.ManagementEndpoints` gives: a library that publishes a controller is
publishing into somebody else's URL space, and two controllers on one path is an **ambiguous mapping —
the context refuses to start**. That has already cost this codebase time twice.

## ⚠️ The webhook secret — startup fails without it

`setWebhook` takes a `secret_token`; Telegram echoes it in `X-Telegram-Bot-Api-Secret-Token`.

Without checking it, the endpoint is a **public route that accepts anything shaped like an update** —
meaning anybody who finds the address can make the application believe a message arrived from a chat id
of their choosing, and every handler downstream treats it as genuine.

Defaulting to "no check" would make that the state an installation reaches by **forgetting a property**,
which is the worst possible way to arrive at it. So `TelegramWebAutoConfiguration` throws at startup and
names the property. An application that genuinely wants no check declares its own controller bean — a
decision somebody has to write down.

The comparison is **constant-time** (`MessageDigest.isEqual`): a short-circuiting `equals` leaks the
matching prefix length through timing, and this is a value somebody may guess against at any rate.

## ⚠️ The endpoint answers 200 even when a handler failed

Telegram retries a non-2xx with escalating delay and **disables a webhook that keeps failing**. A handler
defect would otherwise become endless redelivery and then a switched-off integration. `UpdateDispatcher`
already isolates and logs a failing handler; this endpoint's job is to confirm receipt.

## `PropertiesIdentitySource`

Resolution order for a purpose: the purpose's own entry (if `enabled`) → the `general` entry → refuse with
`Unauthorized` naming the property to set.

⚠️ It re-reads the map on every call, which looks pointless against immutable configuration and **is the
contract**: per-call resolution is what lets a database-backed source be substituted without any caller
changing.

## ⚠️ When this does not run, the symptom is silence

Boot 4 registers autoconfiguration through
`META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports`. When that does not
happen — a missing entry, an exclusion, a classpath without a conditional class — **nothing is logged and
nothing throws**. The application starts and the first symptom is a notification that never arrives.

So `TelegramDiagnostics` logs one line at startup:

```
jmouse-telegram ready: transports=[BOT], purposes=[general, kitsu-notifications], updates=POLLING, pacing=30/s global, 20/min per chat
```

**If that line is absent, the autoconfiguration did not run** — establish that before reading any Java.
An installation with no identity gets a `WARN` rather than a failure, because an application may
legitimately start before its token is provisioned.

Registered, in order:

```
org.jmouse.telegram.spring.autoconfigure.TelegramAutoConfiguration
org.jmouse.telegram.spring.autoconfigure.TelegramPollingAutoConfiguration
org.jmouse.telegram.spring.autoconfigure.TelegramWebAutoConfiguration
```

## ⚠️ No `ProblemDetail` advice here, deliberately

There is a house pattern — a `@RestControllerAdvice` at `ProblemDetailAdvices.LIBRARY_PRECEDENCE`, whose
Javadoc explains that lowest precedence is *tied*, not last, so a product's catch-all swallows a
library's careful 400. Two reasons it is not used in this module:

1. A Telegram refusal only reaches an HTTP response if a **controller** sends synchronously, and this
   module has none that do.
2. The precedence constant lives in `org.jmouse.storage.spring`, so using it would mean depending on the
   storage starter from the Telegram one.

The mapping therefore belongs in `jmouse-telegram-management` (`JMF-338`), where the controllers are.
⚠️ `ProblemDetailAdvices` arguably belongs in `jmouse-core` — noted, not done.

## Build notes

- Compiled against **Boot 4.0.7**; every Spring type used here is unchanged from 3.3, so the jar works on
  a Boot 3 application.
- `spring-web` and `jakarta.servlet-api` are **optional/provided** — an application that polls, or that
  only sends, never acquires a servlet API it did not ask for.
- ⚠️ `jmouse-telegram` and `jmouse-telegram-bot` are declared in **`jmouse-dependencies`** (the BOM). A
  new module here fails to build until it is added there.

```bash
cd Git/jmouse
mvn -o install -pl jmouse-telegram,jmouse-telegram-bot,jmouse-spring/jmouse-telegram-spring-boot \
    -Dgpg.skip=true
```

## Do not

- **Do not default the webhook to an unchecked secret.**
- **Do not publish a `PacedTransport` as a bean.**
- **Do not make `polling` or `webhook` the default mode.**
- **Do not put the webhook at a plausible top-level path** such as `/telegram/webhook`.
- **Do not remove the `initMethod`/`AutoCloseable` pairing on `LongPolling`.**
- **Do not add a global `@RestControllerAdvice` from this module.**
