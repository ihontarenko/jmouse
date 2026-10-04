# jmouse-telegram-bot — the Bot API transport

> **Agent orientation file.** Written so you do not have to read the module to use it. If something
> here disagrees with the code, the code wins — and fix this file in the same change.
>
> Module: `jmouse-telegram-bot` · Package: `org.jmouse.telegram.bot` · Java 21
> Depends on: `jmouse-telegram`, `jackson-databind` (the wire only), `slf4j-api`.
> **The HTTP client is the JDK's — there is no web stack and no Spring.**
> Read [`JMOUSE_TELEGRAM.md`](JMOUSE_TELEGRAM.md) first; this module implements its SPI.
> Tracker: `JMF-328` (transport), `JMF-332` (polling)

## What it is

`BotApiTransport` — the only public class worth knowing. Construct one, hand it to a `RoutingGateway`,
and everything else here is its machinery. It speaks HTTPS + JSON against
`{apiBase}/bot{token}/{method}`.

```java
BotApiTransport transport = new BotApiTransport();

TelegramGateway gateway = new RoutingGateway(
        IdentitySource.fixed(TelegramIdentity.bot("alerts", System.getenv("BOT_TOKEN"))),
        List.of(new PacedTransport(transport, TokenBucketPace.defaults())));
```

⚠️ **It holds no configuration.** The identity arrives as a parameter on every call — purposes,
fallbacks and rotation all stay in the gateway above it.

## The map

| File | Visibility | What it carries |
|---|---|---|
| `BotApiClient` | `public abstract` | everything every method call has in common |
| `BotApiTransport` | `public` | the `TelegramTransport` + `UpdateFetcher` |
| `BotApiErrors` | package-private | ⚠️ **the only place a Telegram error description is read** |
| `BotApiPayload` | package-private | value types → wire fields |
| `BotApiUpdates` | `public` | update JSON → the core's `Update` model |
| `MultipartBody` | package-private | hand-rolled `multipart/form-data` |
| `UpdateFetcher` | `public` | `fetchUpdates` as a type, so the loop is testable |
| `LongPolling` | `public`, `AutoCloseable` | the `getUpdates` loop |
| `smoke/PollingSmoke` | `public` | 10 checks over the loop, no network |

## `BotApiClient` — the base, and the rule about it

Protected API for a subclass:

```java
JsonNode call(TelegramIdentity, String method, Map<String,Object> fields)
JsonNode call(TelegramIdentity, String method, Map<String,Object> fields, Duration timeout)
JsonNode upload(TelegramIdentity, String method, MultipartBody body)
String   writeValueAsString(Object value)
```

It returns the `result` member of Telegram's reply, or throws. It handles timeouts, the credential
check, the URL, JSON both ways, and the translation of the three ways a call fails.

⚠️ **A new method must be three small things: its name, its fields, how to read the reply.** If adding
`sendVenue` means touching anything in `BotApiClient`, the split has been got wrong and the twentieth
method will be a twentieth copy of the error handling. (This mirrors `HttpChatModel` in
`jmouse-ai-provider` deliberately.)

Timeouts: `DEFAULT_CONNECT_TIMEOUT = 10s`, `DEFAULT_READ_TIMEOUT = 30s`. Both finite and set here — a
hung endpoint otherwise holds the calling thread forever, and enough of those make one slow integration
look like the whole application being down. ⚠️ The read timeout is a **parameter** because long polling
legitimately needs a much longer one.

## `BotApiTransport`

`kind()` → `BOT`. Supported capabilities: `SEND_MESSAGE` `EDIT_MESSAGE` `DELETE_MESSAGE` `SEND_MEDIA`
`REPLY_MARKUP` `ANSWER_CALLBACK` `ADMINISTER_CHAT` `MANAGE_TOPICS`.

⚠️ **Three are absent and no configuration adds them:** `CREATE_CHAT` (MTProto only — a bot cannot
create a group or channel), `INITIATE_CONVERSATION` (a person must start the bot, which is why binding
is a flow), `READ_HISTORY` (a bot sees only what arrives while it is listening).

### How `send` routes

| Draft | Method used |
|---|---|
| text only | `sendMessage` |
| one attachment | `MediaKind.method()` — `sendPhoto`, `sendDocument`, … |
| more than one | `sendMediaGroup` |

Non-upload sources (`FileId`, `RemoteUrl`) go as a JSON body; `Bytes` and `LocalFile` go as multipart.
In a media group an upload is referenced as `attach://fileN`.

Captions: an explicit caption on the attachment wins over the draft's text, because it was set closer to
the thing it describes. For an album the caption goes on the **first** entry only — that is where
Telegram shows it.

### Refusals this module raises itself

| Refused, with a sentence | Why here rather than at Telegram |
|---|---|
| editing a message's **media** | `editMessageMedia` has its own rules about which kinds may replace which; guessing works for a photo and fails silently for audio |
| a media group carrying **buttons** | Telegram refuses `reply_markup` on `sendMediaGroup` with a generic 400 |
| a group mixing incompatible kinds | only photos and videos may mix; documents and audio group with their own kind. Telegram reports both violations identically |
| an upload over **50 MB** | ⚠️ only when `apiBase` is Telegram's own — a self-hosted `telegram-bot-api` server accepts 2 GB, and refusing there would break the one arrangement that lifts the limit |

⚠️ **An invalid `apiBase` is re-thrown without the address.** The JDK's own `IllegalArgumentException`
quotes the whole URL — which contains the bot token, and would therefore put it in a log.

⚠️ **A photo's `file_id` comes from the LAST entry of the sizes array.** The first is a thumbnail, and a
`file_id` for a thumbnail re-sends a thumbnail. Same trap on the receiving side in `BotApiUpdates`.

## `BotApiErrors` — the containment

Telegram answers a refusal as `{"ok":false,"error_code":403,"description":"Forbidden: bot was blocked by
the user"}`. The code is coarse: **400 covers a missing chat, an unchanged edit, a malformed keyboard and
an over-long caption**, so the description is the only thing distinguishing them — and it is an English
sentence written for a person, not a stable identifier.

Matching on it is therefore unavoidable, and **containing that match to one file is the point.** The day
a phrase changes there is one file to fix, rather than a `description.contains("blocked")` in every
product, each subtly different and each silently wrong.

| Detected | Becomes |
|---|---|
| `429` | `FloodWait(parameters.retry_after)` — ⚠️ read from the body, not a header |
| `401` | `Unauthorized` |
| `404` | ⚠️ `Unauthorized` — **Telegram reports an invalid token as 404 on the method path**, which reads as "this method does not exist" and sends people looking in the wrong place |
| "bot was blocked by the user" · "user is deactivated" · "bot was kicked" | `BotBlocked` |
| "chat not found" · "peer_id_invalid" · "user not found" | `ChatNotFound` |
| "message is not modified" | `MessageNotModified` |
| "not enough rights" · "need administrator rights" · … | `InsufficientRights` |
| anything else | `Rejected(code, description)` — ⚠️ **not retryable**, conservatively |

## `BotApiUpdates` — JSON → `Update`

`public static Update read(JsonNode)` · `public static List<Update> readAll(JsonNode array)`

Public because the **webhook endpoint parses the same shape as polling** — Telegram posts an identical
`Update` object either way.

Recognised members: `message`, `edited_message`, `channel_post`, `edited_channel_post`,
`callback_query`, `my_chat_member`, `chat_member`, `chat_join_request`.

⚠️ **Anything else becomes `Update.Unknown(updateId, kind)` rather than throwing.** Telegram adds update
types on its own schedule; a parser that refused one would stop an ingestion loop on the day of a
Telegram release, over an update nobody wanted. `kind` names the field so a decision to model it can be
taken with evidence.

⚠️ **`my_chat_member` vs `chat_member`** — the first is *this identity's own* membership changing, i.e.
the bot was added to or removed from a chat. Telegram delivers it as a separate kind because it means
something different; the model carries that as `MembershipChanged.mine`.

⚠️ **`callback_query.message` may be absent** — for a button on an inline-mode result, and for a message
too old for Telegram to hold. `CallbackPressed.message` is then `null`.

⚠️ **`message_thread_id` is carried through**, so a reply lands in the topic it answers rather than at
the top of the group — the single most visible way a forum integration looks broken.

## `LongPolling` — the loop

```java
try (LongPolling polling = new LongPolling(transport, identities, dispatcher)) {
    polling.start();
    // ... the application runs ...
}
```

Constructors:

```java
LongPolling(UpdateFetcher, IdentitySource, UpdateDispatcher)
LongPolling(UpdateFetcher, IdentitySource, UpdateDispatcher,
            String purpose, Duration pollTimeout, Duration failureBackoff, Set<String> allowedUpdates)
```

`start()` · `close()` · `isRunning()` · `offset()`.
Defaults: `DEFAULT_POLL_TIMEOUT = 50s`, `DEFAULT_FAILURE_BACKOFF = 5s`.

### The five things this loop gets right, and why each matters

1. ⚠️ **The offset advances only AFTER an update has been dispatched, and only forwards.** The next
   request carrying a higher offset is what *acknowledges* the previous batch, so raising it first
   **loses** updates instead of redelivering them. This is the rule whose failure is silent.
2. ⚠️ **`Unauthorized` stops the loop; everything else retries.** A revoked token will never start
   working, and retrying it every five seconds buries the one line an administrator needs.
3. A `FloodWait` waits **Telegram's own figure**.
4. ⚠️ **`close()` interrupts, it does not only set a flag.** The thread spends its life blocked inside a
   request Telegram holds open for fifty seconds. A flag alone means a minute-long shutdown — and on a
   dev machine, a restart where the old poller steals updates from the new one.
5. The thread is **named** (`telegram-long-polling`) and is **not a daemon** — named so a thread dump
   answers "why is this making a request a minute", non-daemon so an unclosed poller keeps the JVM alive,
   which is a visible bug rather than a silent one.

### ⚠️ Exactly one of these may run per bot

Telegram delivers a bot's updates to **one** consumer. A second poller does not get copies; it takes
updates the first will then never see, and the symptom is each instance handling roughly half of
everything at random. Two products sharing a bot share one `LongPolling` and fan out through the
`UpdateDispatcher`.

### ⚠️ Polling is what development runs on here, not a fallback

A Telegram webhook needs a publicly reachable **HTTPS** address. This workspace's machine serves plain
HTTP from behind a router, so a webhook cannot be registered against it at all.

### `allowed_updates`

Configurable, empty by default. ⚠️ **Telegram's default EXCLUDES `chat_member`**, so an application that
wants to know who joined must name it.

### Redelivery after a crash is normal

The offset is in memory, and Telegram keeps undelivered updates for 24 hours — so a restart resumes from
what was not acknowledged and the last batch can arrive twice. A handler that must not act twice needs
idempotency. That is `JMF-336`'s outbox, not this loop's.

## `UpdateFetcher` — why it exists

```java
List<Update> fetchUpdates(TelegramIdentity identity, long offset, Duration pollTimeout, Set<String> allowed);
```

`BotApiTransport` implements it. It exists so `LongPolling` can be built around a script instead of a
real HTTP client — the part worth checking is not the request, it is **when the offset advances**.

⚠️ **Deliberately not on `TelegramTransport`.** Long polling is a Bot API arrangement; MTProto pushes
updates down a socket it already holds. A method on the shared SPI would be one only half its
implementations could mean.

## `MultipartBody`

The JDK's HTTP client has no multipart publisher, and adding a client library for one wire format would
be a large dependency. ~110 lines, no transitive cost.

⚠️ **Bytes throughout, never a `String`.** A body mixing UTF-8 text with the contents of a JPEG cannot be
assembled as text: the file bytes are not valid in any charset, and converting corrupts the upload in a
way that fails at Telegram rather than here.

⚠️ A quote or newline in a file name would end the header early and corrupt every part after it — names
arrive from users and from disk, so both are escaped.

## Running the checks

```bash
cd Git/jmouse
mvn -o install -pl jmouse-telegram,jmouse-telegram-bot -Dgpg.skip=true
mvn -o -q dependency:build-classpath -pl jmouse-telegram-bot -Dmdep.outputFile=/tmp/tgb-cp.txt -DincludeScope=runtime
java -cp "jmouse-telegram-bot/target/classes;jmouse-telegram/target/classes;$(cat /tmp/tgb-cp.txt)" \
     org.jmouse.telegram.bot.smoke.PollingSmoke      # 10 checks
```

⚠️ **No call has ever been made against real Telegram.** No installation configures an identity yet, so
the wire format is verified by construction, not by Telegram accepting it. Worth knowing before trusting
a field name.

## Do not

- **Do not read a Telegram error description anywhere but `BotApiErrors`.**
- **Do not put HTTP handling in a method implementation.** See the rule about `BotApiClient`.
- **Do not add `getUpdates` to `TelegramTransport`.**
- **Do not enforce the 50 MB ceiling against a configured `apiBase`.**
- **Do not make `LongPolling` a daemon thread**, and do not let `close()` merely set a flag.
- **Do not advance the offset before dispatching.**
