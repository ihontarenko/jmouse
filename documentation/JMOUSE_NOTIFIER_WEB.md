# jmouse-notifier-web — transient notices collected once per device

> **Agent orientation file.** The code is authoritative.
> Module: `jmouse-notifier-web` · Package: `org.jmouse.notifier.web` · Java 21.

## What it is

A screen channel for [notifier](JMOUSE_NOTIFIER.md). Delivery deposits a notice in an inbox; a product's
authenticated route collects it for a browser, television or another device. Despite the artifact name,
the module does not mount a servlet/Spring controller or determine who the caller is.

## The map

| Type | Responsibility |
|---|---|
| [ScreenChannel](../jmouse-notifier-web/src/main/java/org/jmouse/notifier/web/ScreenChannel.java) | Translate notifier deliveries to screen inbox notices |
| [Inbox](../jmouse-notifier-web/src/main/java/org/jmouse/notifier/web/Inbox.java) | Deposit and collect contract, including the device identity |
| [MemoryInbox](../jmouse-notifier-web/src/main/java/org/jmouse/notifier/web/MemoryInbox.java) | Bounded expiring process-local inbox with per-device collection tracking |
| [Notice](../jmouse-notifier-web/src/main/java/org/jmouse/notifier/web/Notice.java) | Value returned to the product UI: the displayed notification shape |

## Wiring and use

```java
Inbox inbox = new MemoryInbox();
Channel screen = new ScreenChannel(inbox);
// In the product's authenticated collect route:
List<Notice> fresh = inbox.collect(subject, deviceId);
```

Register `screen` with `Notifier`. `subject` must come from the authenticated caller and `deviceId`
must identify the collecting device. The endpoint, authentication, polling schedule and rendering belong
to the product. Do not accept another person's subject from an untrusted query parameter.

A subject-only address reaches all of that subject's devices; a `subject|device` address targets one.
Each notice is collected once per device, so a browser collecting does not consume the television's copy.
Notifier exclusions rely on this device-specific form. It is deliberately different from a people-only
channel such as Telegram, where multiple device resolutions still mean one reachable person.

`inbox.recent(subject, limit)` reads retained notices newest first without marking them collected;
it is a recent-history view within the same bounded lifetime, not a durable notification archive.

## Lifetime and extension

`MemoryInbox` defaults to capacity 500 and ten-minute lifetime. Expired and evicted notices, collection
tracking and uncollected notices disappear on restart. Separate application instances have separate
inboxes; choose routing or a shared implementation of `Inbox` if that is incompatible with deployment.
An accepted deposit is not an acknowledgement that a person has seen the notice.

Use a persistent domain notification history separately when a durable record or read acknowledgement
is required. The inbox is a transient nudge. [The notifier starter](JMOUSE_NOTIFIER_SPRING_BOOT.md)
registers a screen channel when the module is present and lets the adopter configure notice lifetime.

## Source reference

Direct dependencies below come from this module's [POM](../jmouse-notifier-web/pom.xml); test-only dependencies are omitted. Optional dependencies still require the adopter to supply the feature at runtime.

| Dependency | Declared scope | Optional |
|---|---|---|
| `org.jmouse:jmouse-notifier` | compile | no |
| `org.slf4j:slf4j-api` | compile | no |
| `ch.qos.logback:logback-classic` | compile | yes |

### Public type map by package

Top-level public types in production source, excluding smoke/example drivers. Links point to the implementation; roles summarize source JavaDoc or the declared contract. Nested builders, records and enum members belong to their enclosing type.

#### `org.jmouse.notifier.web`

| Type | Responsibility / contract |
|---|---|
| [Inbox](../jmouse-notifier-web/src/main/java/org/jmouse/notifier/web/Inbox.java) | Where screen notices wait to be collected. |
| [MemoryInbox](../jmouse-notifier-web/src/main/java/org/jmouse/notifier/web/MemoryInbox.java) | Screen notices in this process's memory — bounded, expiring, collected once per device. |
| [Notice](../jmouse-notifier-web/src/main/java/org/jmouse/notifier/web/Notice.java) | One notification as a screen shows it — the shape a collect route answers with. |
| [ScreenChannel](../jmouse-notifier-web/src/main/java/org/jmouse/notifier/web/ScreenChannel.java) | Notifications shown on the product's own screens — a browser, a television. |

### Verification boundary

This documentation was checked against the current source tree. No Java build, database migration, external API call or runtime scenario is claimed by this documentation change.
