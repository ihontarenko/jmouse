# jmouse-telegram — the core

> **Agent orientation file.** Written so you do not have to read the module to use it. If something
> here disagrees with the code, the code wins — and fix this file in the same change.
>
> Module: `jmouse-telegram` · Package root: `org.jmouse.telegram` · Java 21
> Depends on: `jmouse-core`, `jmouse-http`, `slf4j-api`. **No Jackson, no Spring, no JPA.**
> Siblings: [`JMOUSE_TELEGRAM_BOT.md`](JMOUSE_TELEGRAM_BOT.md) (the transport),
> [`JMOUSE_TELEGRAM_SPRING_BOOT.md`](JMOUSE_TELEGRAM_SPRING_BOOT.md) (the wiring)
> Tracker: `JMF-326` (epic) · Prose: `../jMouseProjects/.scratch/jmouse-telegram/spec.md`

## What it is in one paragraph

Telegram as a **contract**, not a client library. A product calls one interface, `TelegramGateway`;
behind it `RoutingGateway` resolves a `TelegramIdentity` from an `IdentitySource`, picks the
`TelegramTransport` that speaks that identity's protocol, checks the `Capability` the call needs, and
delegates. Failures arrive as a typed `TelegramRefusal`, never as prose to be matched on.

**It is not a notification framework.** It carries a message and says what happened. *What is worth
telling somebody about* is the product's question — Innoventa already models that as `AttentionSource`
with Telegram as one delivery channel. Never put the word "notification" in this module.

## The two-protocol fact, which shapes everything

Telegram is **two APIs**, and their powers genuinely differ.

| | Bot API (`IdentityKind.BOT`) | MTProto (`IdentityKind.USER`) |
|---|---|---|
| credential | a bot token | phone → code *inside Telegram* → 2FA → a session |
| create a group or channel | ❌ | ✅ |
| inline keyboards, `answerCallbackQuery` | ✅ | ❌ |
| write to somebody first | ❌ they must start the bot | ✅ |
| read history | ❌ no such method exists | ✅ |
| sees a chat | only ones it was **added to**, only from then on | everything the person sees |

⚠️ **No `USER` transport is implemented yet** (`JMF-339`). The kind exists so the contract was built
around both from the first commit. An `IdentitySource` may legitimately answer with a `USER` identity
today — `RoutingGateway` refuses it with a sentence saying no transport is installed, which is the
honest answer and not a defect.

## The map

```
org.jmouse.telegram
├── TelegramGateway          THE interface a product calls
├── RoutingGateway           the only implementation: resolve → route → check → delegate
├── TelegramIdentity         who we speak as (record; toString REDACTS the credential)
├── IdentityKind             BOT | USER
├── IdentitySource           where identities come from — asked PER CALL, keyed by purpose
├── ChatReference            a chat by id or @username, plus an optional forum topic
├── MessageHandle            (chat, messageId) — what to edit or delete
├── MessageDraft             what to send, with no destination · + Builder
├── SentMessage              what came back: messageId + file_ids — MEANT TO BE STORED
├── ParseMode                NONE | MARKDOWN_V2 | HTML
├── MarkupEscaper            escaping, because a caller will forget
├── CallbackAnswer           the reply owed to a button press
├── Capability               what a transport can do at all
├── TelegramRefusal          sealed, 9 cases — why a call did not happen
├── TelegramException        a refusal, thrown
├── markup/                  InlineButton, ReplyMarkup
├── media/                   MediaKind, MediaSource, MediaAttachment
├── pace/                    Pace, TokenBucketPace, PacedTransport
├── spi/TelegramTransport    one way of actually talking to Telegram
├── test/RecordingTransport  a fake, in src/main so products can use it
└── smoke/                   TelegramSmoke, UpdateSmoke, Checks — run their main()
    update/                  Update (sealed), IncomingMessage, TelegramUser,
                             UpdateHandler, UpdateDispatcher
```

## Sending — the whole usage

```java
TelegramGateway gateway = ...;                      // injected; see the Spring Boot doc

SentMessage sent = gateway
        .as("kitsu-notifications")                  // a purpose the PRODUCT names
        .send(ChatReference.of(chatId), MessageDraft.text("Blade Runner was opened"));

// Later — edit rather than sending a second message
gateway.as("kitsu-notifications")
       .edit(sent.handle(), MessageDraft.text("Blade Runner was opened by 2 people"));
```

### `TelegramGateway`

| Method | Notes |
|---|---|
| `SentMessage send(ChatReference, MessageDraft)` | ⚠️ returns a handle — store it |
| `SentMessage edit(MessageHandle, MessageDraft)` | an unchanged edit → `MessageNotModified` (benign) |
| `void delete(MessageHandle)` | ⚠️ Telegram allows this only within 48 h |
| `void answerCallback(CallbackAnswer)` | needs `ANSWER_CALLBACK` |
| `boolean supports(Capability)` | transport power, **never permission** |
| `TelegramGateway as(String purpose)` | a view bound to a purpose; bare methods mean `GENERAL` |

⚠️ **Chat administration is deliberately NOT on this interface** — it is a separate one in `JMF-337`.
The code that sends notifications and the code that bans members are never the same code.

### `MessageDraft` — built, not constructed

```java
MessageDraft draft = MessageDraft.builder()
        .text("*%s* was opened".formatted(MarkupEscaper.markdownV2(title)), ParseMode.MARKDOWN_V2)
        .attach(MediaAttachment.document(MediaSource.LocalFile.of(path)).withCaption("poster"))
        .markup(ReplyMarkup.InlineKeyboard.row(
                InlineButton.callback("Watched", "watched:" + filmId),
                InlineButton.url("Open", url)))
        .silent()
        .build();
```

Builder methods: `text(String)`, `text(String, ParseMode)`, `attach(MediaAttachment)`,
`attach(List<MediaAttachment>)`, `markup(ReplyMarkup)`, `replyTo(int)`, `silent()`,
`protectedContent()`, `withoutPreview()`, `build()`. Shortcut: `MessageDraft.text("…")`.

⚠️ **The draft carries no destination.** One draft goes to fifty chats — that is the shape of a
notification — so putting a `ChatReference` inside it would mean rebuilding the body per recipient.

### `ChatReference`

`of(long)` · `of(String username)` (adds a missing `@`) · `inThread(int)` · `inChat()` ·
`hasThread()` · `wireValue()`

⚠️ **A chat id is a signed 64-bit number and is often negative** — supergroups are large negatives,
conventionally `-100…`. `long`, `BIGINT`, never unsigned. Truncation delivers to a *different chat*
rather than failing.

⚠️ **`threadId` is a forum topic**, present from the first commit because Telegram carries
`message_thread_id` on *every* send method. It is how one group serves several products.

### `SentMessage` and `MessageHandle`

`SentMessage(chat, messageId, sentAt, mediaIds)` · `handle()` → `MessageHandle`

⚠️ **`handle()` strips the thread.** `message_thread_id` routes a *new* message into a topic; Telegram
**refuses** it on an edit or a delete. `MessageHandle`'s constructor throws on a threaded chat, so the
mistake cannot reach the wire.

⚠️ `mediaIds` are `file_id`s — re-send the same bytes for free instead of uploading twice. **Bot
specific:** one bot cannot use another's.

## Failure — `TelegramRefusal` (sealed)

Catch `TelegramException` and switch on `refusal()`, never on the exception type.

```java
try {
    gateway.send(chat, draft);
} catch (TelegramException exception) {
    switch (exception.refusal()) {
        case TelegramRefusal.BotBlocked blocked -> bindings.deactivate(chat);   // ⚠️ not a retry
        case TelegramRefusal.FloodWait wait     -> outbox.deferBy(wait.retryAfter());
        default                                 -> outbox.failed(exception.refusal());
    }
}
```

| Case | `retryable()` | What the caller should do |
|---|---|---|
| `FloodWait(Duration retryAfter)` | ✅ | wait **exactly** that long — never invent a backoff |
| `BotBlocked(String chat)` | ❌ | ⚠️ **deactivate the binding**; it will never succeed |
| `ChatNotFound(String chat)` | ❌ | the destination is wrong |
| `MessageNotModified()` | ❌ | benign — the edit was a no-op |
| `Unauthorized(String identityName, String detail)` | ❌ | an administrator must act |
| `InsufficientRights(String chat, String detail)` | ❌ | in the chat, but not an admin of it |
| `CapabilityUnavailable(Capability, String identityName, IdentityKind, IdentityKind capableKind)` | ❌ | use a different identity kind |
| `Rejected(int errorCode, String description)` | ❌ | Telegram said no for a reason we do not model |
| `TransportFailure(String detail)` | ✅ | nothing answered |

⚠️ **Sealed, so a `switch` over these breaks at compile time when a case is added.** Every member has
`message()` — one sentence somebody can act on.

## `Capability` — why it exists

`SEND_MESSAGE` `EDIT_MESSAGE` `DELETE_MESSAGE` `SEND_MEDIA` `REPLY_MARKUP` `ANSWER_CALLBACK`
`ADMINISTER_CHAT` `MANAGE_TOPICS` `CREATE_CHAT` `INITIATE_CONVERSATION` `READ_HISTORY`

One interface in front of genuinely different powers is exactly where pretending parity produces the
worst failure: code that compiles, ships, and throws `UnsupportedOperationException` with no sentence.
`RoutingGateway` checks before the call and refuses with `CapabilityUnavailable`, which names the
identity, the capability, and the kind that **would** have worked — computed from the transports
actually installed, not from a table, so the advice is always followable.

⚠️ **It describes transport power, never permission.** That a bot *can* ban says nothing about whether
it administers that chat (Telegram answers that at call time) or whether the person driving it may
(the product's question, `jmouse-access`).

## `IdentitySource` — per call, keyed by purpose

```java
public interface IdentitySource {
    String GENERAL = "general";
    TelegramIdentity identity();
    default TelegramIdentity identity(String purpose) { return identity(); }
    static IdentitySource fixed(TelegramIdentity identity) { ... }
}
```

⚠️ **Asked per call, deliberately.** An administrator who rotates a token expects the next send to use
it; resolving at startup means a restart instead — and a restart is exactly what nobody performs the
moment a token is found to be leaking. An implementation that wants a cache is free to be one.

⚠️ **A purpose is the product's own word and this module never enumerates one.** `"kitsu-notifications"`
means nothing here. Unknown purposes fall back to `GENERAL`, which is the whole migration story: naming
a purpose in code does not require configuring one first.

## Escaping — `MarkupEscaper`

`markdownV2(String)` · `html(String)` · `forMode(ParseMode, String)`

⚠️ MarkdownV2 reserves **eighteen** characters — ``_*[]()~`>#+-=|{}.!`` — and every one must be escaped
*anywhere* in the text, not only where formatting was meant. `Blade Runner (1982)` fails the whole send
with a parse error, and the code that composed it looks obviously correct.

⚠️ **Escape the values, not the finished message.** Escaping a string that already holds intentional
markup destroys the markup — that is what escaping is for.

⚠️ The **em dash `—` is not reserved; the ASCII hyphen `-` is.** This confusion already produced one
wrong assertion here, and it is the same one that produces a failed send in production.

## The inbound half — `update/`

```java
dispatcher
    .onCommand("start",  update -> bindings.begin(update))          // /start and /start@thebot
    .onCallback("open:", update -> openWhatWasPressed(update))      // prefix, not equality
    .on(Update.ChannelPost.class, update -> index(update));
```

`Update` is **sealed with an `Unknown` member**:

`MessageReceived` · `MessageEdited` · `ChannelPost` · `ChannelPostEdited` ·
`CallbackPressed(updateId, queryId, from, message, data)` ·
`MembershipChanged(updateId, chat, about, from, status, mine)` ·
`JoinRequested(updateId, chat, from, inviteLink)` · `Unknown(updateId, kind)`

⚠️ **Telegram's set of update types is NOT closed** — business messages, reactions, boosts and paid
media all arrived after bots did. A hierarchy with no room for an unrecognised one would throw inside an
ingestion loop on the day of a Telegram release. So: sealed for the compiler check that a product wants,
plus `Unknown` so it survives Telegram growing. **`Unknown` is not an error** — treating it as one turns
every Telegram feature release into an incident.

### `UpdateDispatcher`

`on(String description, Predicate<Update>, UpdateHandler)` · `on(Class<T>, UpdateHandler)` ·
`onCommand(String, UpdateHandler)` · `onCallback(String prefix, UpdateHandler)` ·
`int dispatch(Update)`

⚠️ **Every matching handler runs and none can consume an update.** Telegram delivers a bot's updates to
**exactly one** consumer, so two products sharing a bot share one ingress and fan out here. A handler
able to swallow an update would make "which product sees this press" depend on registration order.

⚠️ A throwing **handler** is isolated and logged; so is a throwing **filter** (a bad predicate would
otherwise abort the dispatch and silently stop every *later* handler).

### `IncomingMessage`

`body()` (text, else caption) · `hasText()` · `handle()` · `command()` · `commandArgument()`

⚠️ `command()` **strips the `@botname` suffix** — in a group Telegram appends it so several bots can
share a command, so a naive `equals("/start")` never matches in exactly the place where bots coexist.

⚠️ `commandArgument()` is the **deep link payload**: `t.me/<bot>?start=<token>` arrives as
`/start <token>`. `JMF-335`'s binding flow reads precisely this.

### `TelegramUser`

`id` `firstName` `lastName` `username` `languageCode` `bot` · `hasLanguage()` · `displayName()`

⚠️ **`languageCode` is captured here because there is no second chance** — Telegram reports it on the
update and offers no endpoint to ask later. A binding that does not record it has lost it.
⚠️ `username` is optional and changeable — **never an identifier**. `id` is the only stable handle.

## Pacing — `pace/`

```java
TelegramTransport paced = new PacedTransport(transport, TokenBucketPace.defaults());
```

| Type | What |
|---|---|
| `Pace` | `awaitTurn(chat)`, `penalise(chat, retryAfter)`, `Pace.unlimited()` |
| `TokenBucketPace` | global 30/s + per-chat 20/min over `jmouse-core`'s `RateLimiter.smooth`, plus a penalty clock |
| `PacedTransport` | a **decorator** over any transport: paces, and retries what is retryable |

⚠️ **A `Pace` waits where a rate limiter refuses.** `jmouse-ai`'s `CallerRateLimiter` returns a boolean
because it guards against a model in a loop. Here the caller is a message somebody expects: dropping it
loses the message, and sending anyway earns a penalty that delays every *other* message.

⚠️ `TokenBucketPace` is **per process**. Two instances share a bot but not these buckets while Telegram
counts the total — a multi-instance deployment needs a `Pace` over a shared cache. The seam is there;
the implementation is not.

⚠️ `PacedTransport` does **not** pace or retry `answerCallback` — it is owed within seconds while the
person's client shows a spinner.

## Testing — `test/` and `smoke/`

```java
RecordingTransport transport = new RecordingTransport()
        .willRefuse(new TelegramRefusal.FloodWait(Duration.ofMillis(120)));   // queued, not set

TelegramGateway gateway = new RoutingGateway(
        IdentitySource.fixed(TelegramIdentity.bot("test", "token")), List.of(transport));
```

`RecordingTransport` **behaves**: message ids increment, bodies are remembered, and a second identical
edit genuinely raises `MessageNotModified`. `willRefuse` **queues**, so "refused, refused, then fine"
is expressible. `calls()` / `lastCall()` / `reset()`.

⚠️ It lives in `src/main/java`, not a test jar — this repository has **no JUnit tests** and a consuming
product needs the fake on its own compile path.

```bash
cd Git/jmouse
mvn -o install -pl jmouse-telegram -Dgpg.skip=true
mvn -o -q dependency:build-classpath -pl jmouse-telegram -Dmdep.outputFile=/tmp/tg-cp.txt -DincludeScope=runtime
java -cp "jmouse-telegram/target/classes;$(cat /tmp/tg-cp.txt)" org.jmouse.telegram.smoke.TelegramSmoke   # 34 checks
java -cp "jmouse-telegram/target/classes;$(cat /tmp/tg-cp.txt)" org.jmouse.telegram.smoke.UpdateSmoke     # 18 checks
```

## Invariants enforced in constructors

Each of these otherwise comes back as a generic Telegram `400` whose description does not mention the
real cause:

| Refused | Where |
|---|---|
| a draft with neither text nor attachment | `MessageDraft` |
| text over 4096 · a media group over 10 | `MessageDraft` |
| callback data over 64 **bytes** (⚠️ bytes — Cyrillic hits it at 32 characters) | `InlineButton.Action.Callback` |
| a chat with both an id and a username, or neither | `ChatReference` |
| a handle carrying a forum topic | `MessageHandle` |
| a callback notice over 200 characters | `CallbackAnswer` |

## Do not

- **Do not add Jackson, Spring or JPA here.** The wire format belongs to a transport; wiring belongs to
  the starter; storage belongs to `jmouse-telegram-jpa`.
- **Do not name a product concept.** No "user", "workspace", "film". A purpose is a string.
- **Do not add "notification" vocabulary.** See the first paragraph.
- **Do not make sending `void`.** `SentMessage` is what makes editing possible, forever, for everyone.
- **Do not match on a Telegram error description.** That happens in exactly one file, and it is in the
  bot module.
- **Do not treat `Update.Unknown` as an error.**
