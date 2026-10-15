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

## Acting as a person — module `jmouse-telegram-user` (`JMF-339`)

⚠️ **The MTProto library is the PRODUCT's** (decided 2026-09-29): `tdlight-java` is not on Maven Central
and brings natives, so the framework holds the seam and the product implements `UserClient` over it and
declares a `UserClientFactory` bean. Without that bean nothing below exists, and a `USER` identity is
refused by `RoutingGateway` with a sentence — honest, not a defect.

| Type | What |
|---|---|
| `UserClient` | ⚠️ product-implemented: login steps, send/edit/delete, `resolve`, `join` (username or invite link), `history`, `listen`, `createGroup`, `createChannel` |
| `LoginState` | sealed: `WaitingForPhoneNumber` → `WaitingForCode` → (`WaitingForPassword`) → `Ready` · `Closed`; each has a `prompt()` |
| `UserSession` / `UserSessions` | a directory + a database key **derived per session** as HMAC-SHA256(master key, name); the master key comes from the environment and never touches the session disk |
| `UserAccounts` | open clients by session name; `ready(name)` refuses with the next login step |
| `UserTransport` | the `USER` `TelegramTransport` — sends as the person through the ordinary gateway |
| `UserAdministration` | `createGroup` as a person (and `createChannel`, directly) |
| `ChannelReader` | `join`, `resolve`, `history` — what a bot can never do |

```yaml
jmouse.telegram.user.sessions: /var/lib/product/telegram-sessions
jmouse.telegram.user.session-key: ${TELEGRAM_SESSION_KEY}      # base64, 32 bytes, environment ONLY
jmouse.telegram.identities.ivan-reader: { kind: user, token: ivan }   # a USER credential is a SESSION NAME
```

- ⚠️ **A session directory is full control of the account.** Encrypted with its derived key; losing the
  master key means every person logs in again — the price of the key never being written down.
- ⚠️ No buttons and no callback answers as a person; `RoutingGateway` refuses them naming `BOT`.
- Live posts: `accounts.connect(session, dispatcher)` feeds the same `UpdateDispatcher` bots feed.
- ⚠️ Automating a user account is against Telegram's terms; reading is calmer than mass sending, not risk-free.

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
├── TelegramRefusal          sealed, 10 cases — why a call did not happen
├── TelegramException        a refusal, thrown
├── markup/                  InlineButton, ReplyMarkup
├── media/                   MediaKind, MediaSource, MediaAttachment
├── pace/                    Pace, TokenBucketPace, PacedTransport
├── journal/                 JournalEntry, TelegramJournal, MemoryTelegramJournal, JournalingTransport
├── administration/          ChatAdministration, RoutingChatAdministration, rights, permissions, links, topics
├── spi/AdministrationTransport  one way of administering chats
├── spi/TelegramTransport    one way of actually talking to Telegram
├── test/RecordingTransport  a fake, in src/main so products can use it
└── smoke/                   TelegramSmoke, UpdateSmoke, JournalSmoke, Checks — run their main()
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
| `TelegramFile describeFile(String fileId)` | Bot API `getFile` — where a sent file is held, for about an hour; needs `DOWNLOAD_FILES` |
| `InputStream openFile(TelegramFile)` | the file's bytes as a stream; the caller closes it |

⚠️ **A `file_id` is valid only for the bot that received it** — fetch through the gateway of the purpose that
heard the message. ⚠️ The public Bot API hands a bot **at most 20 MB**; a larger file answers `getFile` with a
refusal (`Rejected`), not with a path. ⚠️ The download address carries the bot token, so `TelegramFile` holds
only the path and the transport builds the address at the moment it reads.

Both are `default` methods that refuse, and so is the SPI's `TelegramTransport.describeFile` / `openFile`: a
gateway or transport written before files could be fetched still compiles and says plainly what it cannot do.

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
| `ConversationNotStarted(String chat)` | ❌ | ⚠️ **NOT a block — keep the binding.** The person never pressed Start in *this* bot (typical for a shared notification bot); ask them to, once |
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
`ADMINISTER_CHAT` `MANAGE_TOPICS` `CREATE_CHAT` `INITIATE_CONVERSATION` `READ_HISTORY` `DOWNLOAD_FILES`

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

`body()` (text, else caption) · `hasText()` · `handle()` · `command()` · `commandArgument()` ·
`attachments()` · `hasAttachments()` · `attachmentFileIds()`

`Attachment` — `fileId` · `fileUniqueId` · `kind` (`PHOTO` `DOCUMENT` `VIDEO` `AUDIO` `ANIMATION` `VOICE`
`VIDEO_NOTE`) · `fileName` · `mimeType` · `sizeBytes` · `fileNameOrDefault()`.

⚠️ **A reference, not the bytes** — fetching is `describeFile` + `openFile`, made only by a handler that wants
the content. ⚠️ `fileName` and `mimeType` are the **sender's client's claims**; a photo has no name at all,
which is what `fileNameOrDefault()` covers (storage refuses nameless content). ⚠️ `fileUniqueId` is what to
deduplicate by and never what to fetch with. A photo is the **largest** size Telegram sent, not the first.

⚠️ `IncomingMessage` carried only `List<String> attachmentFileIds` before; the record component is now
`attachments`, and `attachmentFileIds()` is derived from it — reading code is unchanged, constructing code
passes `List<Attachment>`.

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

## Bot commands — module `jmouse-telegram-command` (`JMF-363`)

```java
@Bean BotCommand search(KnowledgeSearch search) {
    return new BotCommand("search", "Find documents", "<words>",
            context -> context.replyLong(render(search.find(context.subject(), context.arguments()))));
}
@Bean CommandGuard commandGuard(Access access) {                 // ⚠️ REQUIRED — startup refuses without one
    return (subject, command) -> access.permits(subject, "telegram:" + command);
}
```

With Spring Boot nothing else: `TelegramCommandAutoConfiguration` builds a `CommandRouter` over every
`BotCommand` bean, replying as `jmouse.telegram.updates.purpose` (the product's own bot).

- ⚠️ **A handler acts as `context.subject()`** — the product subject the sender is **bound** as
  (`TelegramBindings.subjectOf(telegramUserId)`, by numeric id, never username). Unbound → one sentence, no
  handler, no list of commands. Bound as two subjects → refused rather than guessed.
- ⚠️ **`CommandGuard` is asked before every handler and before every page turn.** `CommandGuard.everyone()`
  exists, as a choice somebody writes down.
- Handlers run on a virtual-thread executor, never the polling thread. A throw is logged and answered with
  one sentence (`CommandPhrases`, replaceable for the product's language).
- `/start` is the binding flow's, `/help` (and any unknown command) lists `BotCommand.usage()`.
- `context.replyLong(text)` pages at ~3800 characters with ‹ › buttons, kept an hour in memory, turnable
  only by the person they were made for. `context.replyDocument(...)` sends a file instead.

### Files people send — `TelegramFiles`, over `jmouse-storage`

```java
@Bean BotCommand push(Documents documents, FileStores stores) {
    return new BotCommand("push", "Add a note or a file", "<text>", context -> {
        for (Attachment attachment : context.attachments()) {
            StoredObject kept = context.files().store(attachment, stores.defaultStore(),
                    StorageKey.of("inbox/" + attachment.fileNameOrDefault()));
        }
    }).receivingFiles();                       // ⚠️ a file dropped with NO command comes here too
}
```

- `context.attachments()` — what the message carries; empty for a typed or pressed command.
- `context.files()` — a `TelegramFiles` over the gateway that heard the message.
  - `Content contentOf(Attachment)` — the file as **`jmouse-storage` `Content`**: named, typed as claimed, sized
    as Telegram reports, and **streaming from Telegram only when a store reads it**. Any `FileStore.write` takes
    it, so a chat file goes through the same acceptance policy, digest and backend as an upload from a form.
  - `StoredObject store(Attachment, FileStore, StorageKey)` — download straight into a store.
- ⚠️ **The size is checked twice**: against what Telegram reports *before* any transfer (over the ceiling →
  `Rejected(413)`, nothing downloaded), and by storage against the bytes that really arrive. The ceiling is
  `TelegramFiles.BOT_API_LIMIT_BYTES` (20 MB) unless the product constructs `new TelegramFiles(gateway, bytes)`.
- ⚠️ **`BotCommand.receivingFiles()`** — a message with an attachment and no command in its caption goes to that
  command, the caption as its arguments. **At most one command per router** may receive files; two make the
  router refuse to start, because a file with no command has nowhere else to say where it was meant to go. A file
  whose caption *is* a command (`/push`) goes to that command as usual.
- ⚠️ `jmouse-telegram-command` now depends on `jmouse-storage` (a plain dependency, not optional: `files()` is on
  every context).

## Chat administration — `administration/` (`JMF-337`)

```java
ChatAdministration administration = ...;            // its own bean — NOT on TelegramGateway

administration.as("raccoon-commands").ban(chat, userId, null, false);
ForumTopic topic = administration.createTopic(group, "Kitsu");
gateway.send(topic.topic(), MessageDraft.text("posted into the topic"));
InviteLink link = administration.createInviteLink(group, new InviteLinkTerms("ops", null, 5, false));
```

| Area | Methods |
|---|---|
| chat | `details` · `memberCount` · `rename` · `describe` · `changePhoto` (upload only) · `removePhoto` · `setPermissions` |
| members | `member` · `administrators` · `promote` · `restrict` · `ban` · `unban` |
| invitations | `createInviteLink` · `editInviteLink` · `revokeInviteLink` · `approveJoinRequest` · `declineJoinRequest` |
| pins | `pin` · `unpin` · `unpinAll` |
| topics | `createTopic` · `renameTopic` · `closeTopic` · `reopenTopic` · `deleteTopic` (topic = chat in its thread) |
| creation | `createGroup` — ⚠️ refused for a bot (`CapabilityUnavailable(CREATE_CHAT)`), no Bot API method exists |

- ⚠️ **A separate interface on purpose**: the code that notifies and the code that bans are never the same
  code. Inject `ChatAdministration` only where the product administers.
- ⚠️ Rights and permissions are **sets of enums** (`AdministratorRight`, `ChatPermission`) and are always sent
  **complete** — an absent one is sent `false`. `AdministratorRights.none()` **demotes**.
- ⚠️ `unban` sends `only_if_banned=true` — Telegram's default would remove a member who is in the chat.
- ⚠️ `restrict` and `setPermissions` send `use_independent_chat_permissions=true`, or Telegram derives the
  media permissions itself.
- ⚠️ `InviteLink.toString()` omits the link — it is a credential to the chat.
- A bot administers only chats it was added to **as an administrator**; anything else is
  `InsufficientRights`.
- Not journaled and not paced — the journal answers "why did a message not arrive", and administration is
  low-volume. Verified by `AdministrationSmoke` against a local server shaped like the Bot API, **not** a
  real group.

## The journal — `journal/` (`JMF-336`)

One `JournalEntry` per call: when, which identity (by **name**), which operation, which chat and topic,
which message id, delivered or refused, the refusal's case name and sentence, how long it took.

```java
TelegramTransport transport = new JournalingTransport(new PacedTransport(bot, pace), journal);
JournalPage refusals = journal.list(JournalQuery.refusals());     // "why did nothing arrive"
```

| Type | What |
|---|---|
| `TelegramJournal` | `record` · `list(JournalQuery) → JournalPage(entries, total)` · `prune(before)` · `none()` |
| `MemoryTelegramJournal` | bounded (1000), newest first, forgotten on restart |
| `JpaTelegramJournal` | in `jmouse-telegram-jpa`, table `telegram_journal` |
| `JournalingTransport` | the decorator that writes entries |

- ⚠️ **No message text, and never the credential.** The question needs destination, identity, time and
  refusal — keeping the words would make a diagnostic table a copy of every private message.
- ⚠️ **Wrap it OUTSIDE `PacedTransport`**, so an entry is the final outcome: a flood wait retried into a
  delivery is one `DELIVERED` entry, not a refusal for a message that arrived.
- ⚠️ **A journal that fails to record never fails the call** — logged and swallowed.
- ⚠️ A call refused **before** it reaches a transport (no credential, a missing capability) is not
  journaled; it is thrown to the caller, and `TelegramDiagnostics` warns about a missing identity at startup.
- The delivery outbox is **not** here — it is the notifier's (`JMF-354`), shared by every channel.

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
