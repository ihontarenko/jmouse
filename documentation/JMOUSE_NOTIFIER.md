# jmouse-notifier — publishing notifications, and its channels

> **Agent orientation file.** Written so you do not have to read the module to use it. If something
> here disagrees with the code, the code wins — and fix this file in the same change.
>
> Modules: `jmouse-notifier` (`org.jmouse.notifier`, depends on `slf4j-api` only) ·
> `jmouse-notifier-telegram` (`org.jmouse.notifier.telegram`, + `jmouse-telegram`, optional `jmouse-telegram-jpa`) ·
> `jmouse-notifier-jpa` · `jmouse-notifier-web` · `jmouse-spring/jmouse-notifier-spring-boot`
> Tracker: `JMF-354` (epic) · Prose: `../jMouseProjects/.scratch/jmouse-notifier/QUEUE.md`

## What it is in one paragraph

One publishing API for every product. A product says **who** (`Sender`), **to whom** in its own terms
(`Recipient`) and **what** (`Notification`); `Notifier` keeps it in a `NotificationStore`, asks the
product's `RecipientResolver` who that means, asks every installed `Channel` where it reaches each of
them, and makes one delivery attempt per address. The notifier is the composite — it holds the
channels; `jmouse-notifier-telegram | web | email | sms` are channel implementations.

⚠️ **Decided with Ivan, 2026-10-06:** the notifier is the abstraction, a channel is a stateless
implementation, and **delivery state (attempts, retries, idempotency, receipt) belongs to the notifier**,
shared by every channel. A channel keeps nothing.

## The map

```
org.jmouse.notifier
├── Notifier                  publish(...) · retryDue() — the only orchestrator
├── Notification              title?, body, Level, links — NO destination, NO sender
├── Level                     INFO (silent) | WARN | ALARM (audible)
├── Sender (sealed)           System(name) · Rule(name) · Person(subjectId, displayName)
├── Recipient (sealed)        Everyone(scope) · Subject · Group · Device · Except(audience, device) · Direct(Address)
├── RecipientResolver   SPI   the product: Recipient → List<Addressee>
├── Addressee                 (subject, deviceId?)
├── Channel             SPI   name() · addressesOf(Addressee) · deliver(Publication, Address) → DeliveryOutcome
├── Address                   (channel, value) — value is the channel's own notation
├── DeliveryOutcome (sealed)  Delivered(receipt) · RetryAfter(delay, reason) · Failed(reason)
├── Publication               a notification once published: id, idempotencyKey?, sender, recipient, time
├── Delivery                  state of one publication at one address: PENDING | DELIVERED | FAILED
├── PublicationReport         what publishing came to; repeated = idempotency key seen before
├── NotificationStore   SPI   register · find · save · deliveriesOf · due
├── InMemoryNotificationStore bounded (1000 by default), oldest forgotten first
├── test/RecordingChannel     a fake channel, in src/main so products can use it
└── smoke/                    Checks, NotifierSmoke — run its main()

org.jmouse.notifier.telegram
├── TelegramChannel           Channel over TelegramGateway.as(purpose)
├── TelegramAddressBook       subject → chats; told when a chat is unreachable
├── BindingsAddressBook       over jmouse-telegram-jpa's TelegramBindings (the deep-link flow)
├── TelegramAddresses         ChatReference ⇄ Address: "-1001234567890", "@name", "-100…#7" (topic)
├── TelegramMessageFormat     <b>title</b> · escaped body · <i>sender</i> · links as URL buttons
└── smoke/TelegramChannelSmoke
```

## The whole usage

```java
TelegramChannel telegram = new TelegramChannel(gateway, "kitsu-notifications", new BindingsAddressBook(bindings));

Notifier notifier = new Notifier(
        new InMemoryNotificationStore(),
        recipient -> switch (recipient) {               // the product's half — the library knows no people
            case Recipient.Subject subject -> List.of(Addressee.of(subject.id()));
            default -> accounts.resolve(recipient);
        },
        List.of(telegram));

notifier.publish(new Sender.Rule("downloads"),
                 new Recipient.Subject(accountId),
                 Notification.info("Blade Runner finished downloading").linking("Open", filmUrl),
                 "download:" + filmId);            // optional idempotency key

// A raw chat — an operators' group — bypassing the resolver:
notifier.publish(new Sender.System("Kitsu"),
                 new Recipient.Direct(TelegramAddresses.of(ChatReference.of(-1001234567890L))),
                 Notification.alarm("Disk is 95% full"));

// Somebody's scheduler, every few seconds:
notifier.retryDue();
```

## The rules that bite

- ⚠️ **The sender is set by the publishing code, never by a caller's text.** A rule that could name its
  own sender can impersonate the product.
- ⚠️ **`Recipient.Direct` bypasses the resolver**, so who may publish one is the product's question.
- ⚠️ **Retries need somebody to call `retryDue()`.** Nothing here owns a thread. No caller, no retry.
- ⚠️ **A failing channel never takes the publish down.** A refusal or even an exception is recorded on
  its own delivery; the others carry on. Only invalid arguments and the product's resolver throwing
  propagate.
- ⚠️ **`RetryAfter` is obeyed exactly.** A flood wait of 17 s is retried at 17 s, not on a guessed backoff.
  After `maximumAttempts` (5 by default) a retry becomes `FAILED` with "gave up after N attempts".
- ⚠️ **Addresses are a set per publication.** One person resolved through two devices gets one Telegram
  message. `Except(device)` drops addressees on that device; a channel that reaches people, not devices,
  still reaches the person.
- ⚠️ **The in-memory store forgets on restart and beyond capacity** — pending retries and idempotency keys
  included. Per process. A database store is `JMF-356` (parked).

## The Telegram channel

| Telegram said | Outcome |
|---|---|
| sent | `Delivered("<chat>:<messageId>")` — the receipt an edit needs later |
| `FloodWait` | `RetryAfter(exactly its delay)` |
| `TransportFailure` | `RetryAfter(30 s)` |
| `BotBlocked`, `ChatNotFound` | `Failed` **and `addressBook.unreachable(chat)`** → the binding is deactivated |
| `ConversationNotStarted` | `Failed`, binding **kept** — the person has to press Start in the shared bot once |
| anything else | `Failed(refusal.message())` |

- ⚠️ **The purpose decides the bot.** Today one shared bot behind every purpose; a bot per product later
  is identity-source configuration — nothing in the channel changes.
- ⚠️ **HTML, every value escaped**, and an overlong body is cut **by its escaped length** — the draft
  counts what is sent, and `&` is five characters once escaped.
- ⚠️ `INFO` is sent silently; `WARN` and `ALARM` make a sound.
- ⚠️ A `BindingsAddressBook` reaches a person only if the **same subject** was passed to
  `TelegramBindings.invite(...)`. Binding by account and resolving by profile reaches nobody, silently.
- ⚠️ URL buttons: Telegram may refuse addresses it cannot open from a phone (`localhost`). That arrives as
  a `Rejected` refusal → `Failed`, with Telegram's sentence.

## The database outbox — `jmouse-notifier-jpa` (`JMF-356`)

`JpaNotificationStore` over `notifier_publications` and `notifier_deliveries` (own migrations, history table
`notifier_schema_history`, MySQL + PostgreSQL). Entity scan must include `org.jmouse.notifier.jpa`.

- ⚠️ **Its own transaction per call** — a delivery cannot be un-sent, so its record must not roll back with
  a failed save. Consequence: **publish after your own commit** (an after-commit event listener).
- ⚠️ **`due()` claims** with a compare-and-set on `next_attempt_at` (5-minute lease), so several processes
  share one outbox without attempting the same row twice. A dead process's claim lapses by itself.
- ⚠️ **At least once.** A crash between delivering and recording sends again; the idempotency key (UNIQUE in
  the table) stops a second *publication*, not a second attempt of one.
- `Notifier.publish` records each delivery **pending before** its first attempt, so a durable store never
  loses an address it owed.

## The screen channel — `jmouse-notifier-web` (`JMF-357`)

Kitsu's `NoticeBoard`, generalised: `ScreenChannel` deposits a `Notice` into an `Inbox`; the product's own
route collects.

```java
List<Notice> fresh = inbox.collect(caller.subject(), caller.deviceId());   // ⚠️ the AUTHENTICATED caller
```

- **Once per device**, not per person — the television and the browser both see it; the same television
  asking again does not.
- Address `subject` reaches every device; `subject|device` only that one (what `Except` relies on).
- `MemoryInbox`: bounded (500), notices expire (10 min) — a nudge, not a record.
- No web framework in the module: the product writes the 5-line route behind its own authentication.

## Spring Boot — `jmouse-spring/jmouse-notifier-spring-boot` (`JMF-359`)

```yaml
jmouse:
  notifier:
    store: memory            # memory | database
    maximum-attempts: 5
    retry-interval: 10s      # the retry worker's thread — the notifier owns none
    telegram:
      purpose: notifications # ⚠️ the SHARED notification bot's identity purpose
    screen:
      lifetime: 10m
```

- ⚠️ **Startup fails without a `RecipientResolver` bean**, with a sentence — a default meaning nobody would
  make every notification vanish.
- Channels appear for the modules on the classpath: Telegram when `jmouse-notifier-telegram` **and** a
  `TelegramGateway` bean exist (address book = `BindingsAddressBook` when `TelegramBindings` exists, else
  nobody, with a warning); screen when `jmouse-notifier-web` is present. Any `Channel` bean of the
  product's own joins them.
- `store: database` without `jmouse-notifier-jpa` refuses to start rather than keeping retries in memory.
- Startup line: `jmouse-notifier ready: store=…, channels=[telegram, screen]`.

Kitsu's migration onto this is a product ticket, not taken (Ivan, 2026-10-06: «по продуктам поки нічого»).

## Verify

```sh
mvn -o -q -pl jmouse-notifier,jmouse-notifier-telegram -am install -DskipTests -Dgpg.skip
# then run org.jmouse.notifier.smoke.NotifierSmoke and
#          org.jmouse.notifier.telegram.smoke.TelegramChannelSmoke  (no network, no token)
```
