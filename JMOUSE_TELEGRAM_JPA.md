# jmouse-telegram-jpa — accounts and bindings

> **Agent orientation file.** Written so you do not have to read the module to use it. If something
> here disagrees with the code, the code wins — and fix this file in the same change.
>
> Module: `jmouse-telegram-jpa` · Package: `org.jmouse.telegram.jpa` · Java 21
> Depends on: `jmouse-telegram`, `jakarta.persistence-api`, `slf4j-api`.
> **No Spring Data and no Spring** — transaction demarcation belongs to whoever calls the store.
> Read [`JMOUSE_TELEGRAM.md`](JMOUSE_TELEGRAM.md) first; wiring is in
> [`JMOUSE_TELEGRAM_SPRING_BOOT.md`](JMOUSE_TELEGRAM_SPRING_BOOT.md).
> Tracker: `JMF-334` (accounts), `JMF-335` (bindings) · `JMF-336` adds an outbox and a journal

## What it is

Two things a properties file cannot do:

1. **Accounts as rows**, so a token is rotated, an account disabled during an incident, or a purpose
   added — without a deploy and without a restart.
2. **Bindings**, which are the precondition of every notification: *which chat reaches which of the
   product's subjects*.

## The map

```
org.jmouse.telegram.jpa
├── TelegramAccount           entity — one identity per purpose
├── TelegramAccounts          port: list/find/upsert/rotate/setEnabled/remove + Description
├── JpaTelegramAccounts        its implementation
├── JpaIdentitySource          IdentitySource over the table — why the table exists
├── CredentialCipher           seals a credential before it is written
├── AesGcmCredentialCipher     AES-256-GCM, fresh nonce per value
├── TelegramBinding            entity — one reachable chat per (subject, chat)
├── TelegramBindingToken       entity — an invitation waiting to be accepted
├── TelegramBindings           port: invite/complete/deliverableFor/allFor/deactivate/remove
├── JpaTelegramBindings        its implementation
├── BindingCommand             a ready UpdateHandler for /start <token>
├── migration/TelegramDialect  MYSQL | POSTGRESQL, resolved from the DataSource
├── migration/TelegramMigrations  history table, locations, migrator bean name
└── smoke/CipherSmoke          14 checks over the cipher, no database
```

Tables: `telegram_accounts`, `telegram_bindings`, `telegram_binding_tokens`.
History table: **`telegram_schema_history`** — the library's own.

## ⚠️ Mapping it in a product

The entities live outside a product's package, so the default scan does not reach them:

```java
@EntityScan({"net.innoventa", "org.jmouse.telegram.jpa"})
```

## Accounts

```java
TelegramAccounts accounts = ...;                    // injected

accounts.upsert("kitsu-notifications", IdentityKind.BOT, rawToken, null);
accounts.rotate("kitsu-notifications", newToken);   // ⚠️ use this, not upsert, to rotate
accounts.setEnabled("kitsu-notifications", false);  // switch off, keep the row
```

| Method | Notes |
|---|---|
| `List<Description> list()` | ⚠️ unpaged: one row per purpose, a handful, never a page |
| `Optional<Description> find(String purpose)` | |
| `Description upsert(purpose, kind, credential, apiBase)` | credential **in the clear**; sealed on the way in |
| `Description rotate(purpose, credential)` | ⚠️ credential only — cannot re-enable a disabled account or drop `apiBase` by accident |
| `Description setEnabled(purpose, boolean)` | |
| `boolean remove(purpose)` | |

⚠️ **`Description` has no credential field at all.** That is what lets an administration endpoint list
accounts without a reviewer having to check whether it masked something — there is nothing to mask.

⚠️ **`locate()` and `openCredential()` are package-private.** A public method answering with the entity
would put the sealed value within reach of every caller, and the step after that is somebody logging it.

## ⚠️ The credential is sealed, and there is no plaintext cipher

A bot token is complete control of the bot; a user session is complete control of a **person's account**.
A plaintext column puts that in every backup, every replica, and every dump somebody takes to debug
something — none of which a rotation can reach afterwards.

So `CredentialCipher` has one implementation and **no pass-through default**, because a pass-through would
be the state an installation reaches by *forgetting* a key. A product that genuinely wants plaintext
writes the two-line implementation itself — a decision somebody has to type out.

```java
CredentialCipher cipher = AesGcmCredentialCipher.fromBase64Key(System.getenv("TELEGRAM_KEY"));
```

Key: exactly **32 bytes** (AES-256) — `openssl rand -base64 32`. ⚠️ A short key is **refused**, not padded
or hashed into shape: silently accepting one lets an installation believe it configured encryption it did
not.

| Property | Why it matters here specifically |
|---|---|
| **GCM, not CBC** | authenticated — a tampered value **fails to open** rather than decrypting to rubbish. Rubbish would be sent to Telegram as a token, and the refusal reads as *"the credential was revoked"*, sending somebody to rotate one that was fine |
| **fresh random nonce per value**, prepended | reusing a nonce under one key in GCM is a **break**, not a weakening: it leaks the XOR of plaintexts and allows forgery |
| **no key rotation** | rotating means opening every value with the old key and sealing with the new — a migration somebody runs. Changing the key without that gives an `IllegalStateException` per account, which is the correct visible failure |

Opening happens in **exactly one place**: `JpaIdentitySource`, at the moment of use.

## `JpaIdentitySource`

Resolution: the row for `purpose` if enabled → the row for `general` if enabled → refuse with
`Unauthorized` naming what to configure.

⚠️ **A disabled purpose falls through to `general` rather than refusing.** Disabling one purpose during an
incident should degrade it to the general bot, not silence the product; an installation wanting silence
disables `general` too.

⚠️ **It reads per call, deliberately.** One query against one indexed row beside an HTTPS round trip that
takes a thousand times longer. Caching defeats the only thing the table is for: a token rotated at 03:00
because it leaked must take effect on the next message, not the next restart.

## Bindings — the flow

```java
// 1. the product asks for an invitation
TelegramBindings.Invitation invitation = bindings.invite(subjectId);     // default 24 h
String link = invitation.deepLink("kitsu_bot");   // https://t.me/kitsu_bot?start=<token>

// 2. show the link or a QR code · 3. the person opens it · 4. the bot receives /start <token>
dispatcher.onCommand("start", new BindingCommand(bindings, gateway));

// 5. later — who can be reached for this subject
for (TelegramBindings.Binding binding : bindings.deliverableFor(subjectId)) {
    gateway.send(binding.chat(), draft);
}
```

⚠️ **`deepLink` takes the bot's username** because this library does not know it: an identity carries a
token, not a name. Get it from Telegram's `getMe` or from the product's configuration — passing it in keeps
a network call out of building a link.

⚠️ **A "subject" is the product's own identifier and this module never interprets one.** A person, a
workspace, a team, a channel — all a string. The moment this module knows what a subject *is*, it has
learned a product's domain.

### The three invitation rules

| Rule | Why |
|---|---|
| **single use** | `accept()` **throws** on a second attempt rather than overwriting — silently re-stamping lets a replayed link bind a second chat |
| **expires** (default 24 h) | a link that works forever works after being forwarded or screenshotted |
| **opaque token** | 24 random bytes, base64**url**, unpadded. ⚠️ Never the subject in any encoding — a token derived from an identifier is one anybody can compute for anybody. `+`, `/` and `=` break inside a `t.me/…?start=` link |

⚠️ **Telegram caps a `/start` payload at 64 characters**, so token length is not a free choice. 32 sits well
inside it.

⚠️ **Issuing an invitation deletes the subject's previous unused one.** Several live tokens means several
links in the wild, any of which binds whoever finds it.

⚠️ **`complete` answers empty for unknown, expired and already-used alike.** Distinguishing them lets
somebody probe for live tokens, and no legitimate caller treats the three differently.

### Deactivation

```java
// what a BotBlocked refusal must do
catch (TelegramException exception) {
    if (exception.refusal() instanceof TelegramRefusal.BotBlocked) {
        bindings.deactivate(chat);
    }
}
```

⚠️ **`deactivate` takes a CHAT, not a subject** — that is what the refusal knows, and one chat may serve
several subjects, all of which stopped being deliverable at once.

⚠️ **A blocked binding is kept, not deleted**, so a screen can say "this person turned the bot off" rather
than showing nothing — which is indistinguishable from never having bound. Re-accepting an invitation from
the same chat **revives** the row.

⚠️ **Nothing calls `deactivate` automatically yet.** The gateway cannot — the core must not depend on
persistence — so that link belongs to `JMF-336`'s outbox, which sees the refusal and owns the retry
decision. Until then, a product that sends directly must do it itself.

## `BindingCommand`

`new BindingCommand(bindings, gateway)` — or with custom `accepted` / `refused` / `greeting` text.

⚠️ **Not registered for you.** Claiming `/start` in every application holding this jar would take a command
products routinely want for a greeting or onboarding.

⚠️ It **always answers**, including on refusal — silence makes somebody conclude the product is broken and
try again. A bare `/start` is a **greeting**, not a failed binding: people find bots through search.

⚠️ **A failed confirmation does not fail the binding.** The row is already written and the transaction is
the caller's; propagating a send failure would roll the binding back over a message that does not matter,
and the person would then follow the link again and be told it is invalid — which by then it would be.

## ⚠️ Migrations are append-only from first release

`db/telegram/{mysql,postgresql}/`, own history table `telegram_schema_history`. The workspace rule that
Flyway files may be edited in place applies to a product whose database can be dropped, **never** to a
library other people's data has already run.

Four details carried over from `jmouse-storage-jpa` rather than rediscovered — all of them in the starter's
`TelegramFlywayAutoConfiguration`:

| ⚠️ | Why |
|---|---|
| `baselineVersion("0")` | Flyway's default of 1 inserts a marker and **skips every migration at or below it**, so this library would baseline at 1 and never run its own `V000001` |
| order the product's Flyway by bean **name** | Boot's initializer type moved package between Boot 3 and 4; the name did not |
| no `@ConditionalOnBean(DataSource)` | conditions evaluate in registration order, so a later-contributed data source is not there yet, the condition quietly fails, and nothing is logged |
| a product using `baseline-on-migrate` needs `spring.flyway.baseline-version: 0` | its schema is no longer empty by the time its own Flyway runs |

⚠️ MySQL notes that have bitten this workspace: a `CHECK` cannot carry a foreign-key action, and the
collation is accent-blind.

## Running the checks

```bash
cd Git/jmouse
mvn -o install -pl jmouse-telegram,jmouse-telegram-jpa -Dgpg.skip=true
mvn -o -q dependency:build-classpath -pl jmouse-telegram-jpa -Dmdep.outputFile=/tmp/tgj-cp.txt -DincludeScope=runtime
java -cp "jmouse-telegram-jpa/target/classes;jmouse-telegram/target/classes;$(cat /tmp/tgj-cp.txt)" \
     org.jmouse.telegram.jpa.smoke.CipherSmoke      # 14 checks
```

⚠️ **The stores themselves have no checks** — they need a database, and this repository has no test
infrastructure for one. The cipher is covered because its failures are silent and severe.

## Do not

- **Do not add a plaintext `CredentialCipher`.**
- **Do not return a credential from a read**, sealed or otherwise.
- **Do not make `locate()` or `openCredential()` public.**
- **Do not cache in `JpaIdentitySource`** without owning the decision that rotation takes that long.
- **Do not derive a binding token from the subject.**
- **Do not distinguish the three refusal reasons in `complete`.**
- **Do not delete a blocked binding.**
- **Do not edit a migration that has shipped.**
- **Do not open a transaction in this module.**
