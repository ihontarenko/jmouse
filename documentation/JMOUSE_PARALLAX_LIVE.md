# jmouse-parallax-live

> Spring integration module · Maven artifact `org.jmouse:jmouse-parallax-live` · source: [module POM](../jmouse-spring/jmouse-parallax-live/pom.xml).

## Purpose and boundaries

Serves a live-circuit WebSocket with one running circuit per socket, using the host LiveAuthenticator at handshake time. Circuit engine semantics remain in Parallax; this adapter handles transport, stepping cadence, output framing and session cleanup.

## How a product adopts it

Explicitly @Import(ParallaxLiveConfiguration.class), provide LiveAuthenticator, the Parallax store services and servlet WebSocket support. Configure the host security chain to allow the handshake path and understand its token transport, and enable WebSocket forwarding in development and deployment proxies.

The dependency surface is declared in the [module POM](../jmouse-spring/jmouse-parallax-live/pom.xml). This integration module uses these library artifacts:

- `jmouse-parallax-spring-boot`.
- `jmouse-core`.

## Runtime path

The configuration waits for servlet context initialization before registering the container endpoint, because ServerContainer becomes available then. LiveCircuitSocket authenticates and manages per-socket state; LiveCircuits owns shared stepping threads; LiveRun owns the session run; LiveOutput and FrameEncoding construct protocol frames.

## Key class responsibilities

| Type | Responsibility |
|---|---|
| [ParallaxLiveConfiguration](../jmouse-spring/jmouse-parallax-live/src/main/java/org/jmouse/parallax/live/socket/ParallaxLiveConfiguration.java) | 🔌 The live-circuit socket. |
| [LiveCircuitSocket](../jmouse-spring/jmouse-parallax-live/src/main/java/org/jmouse/parallax/live/socket/LiveCircuitSocket.java) | Authenticates the WebSocket handshake, creates one LiveRun per connection and closes it on error or disconnect. |
| [LiveCircuits](../jmouse-spring/jmouse-parallax-live/src/main/java/org/jmouse/parallax/live/socket/LiveCircuits.java) | What a running circuit needs from the stores: the object it runs, checked as the caller may read and run it, the circuit compiled with the catalogue's components, and the controls its live parts offer. |
| [LiveRun](../jmouse-spring/jmouse-parallax-live/src/main/java/org/jmouse/parallax/live/socket/LiveRun.java) | Dispatches live commands, owns a circuit session and schedules bounded stepping and output for one socket. |
| [LiveOutput](../jmouse-spring/jmouse-parallax-live/src/main/java/org/jmouse/parallax/live/socket/LiveOutput.java) | Abstracts text and binary writes from the session engine to the WebSocket transport. |
| [FrameEncoding](../jmouse-spring/jmouse-parallax-live/src/main/java/org/jmouse/parallax/live/socket/FrameEncoding.java) | Encodes frames as little-endian binary data with JSON metadata, channel samples, peaks and trace values. |
| [ParallaxLiveRoutes](../jmouse-spring/jmouse-parallax-live/src/main/java/org/jmouse/parallax/live/socket/ParallaxLiveRoutes.java) | 🛣️ Where the live socket answers: jmouse.parallax.live.path, by default /jmouse/parallax/live — beside the REST API at /jmouse/parallax/api, under the same shared root. |

## Registration and activation

This artifact has no `AutoConfiguration.imports` resource. Its controller/configuration classes must be registered through the adoption path described above.

Conditions below are copied from the linked source. They include configuration-level, nested-configuration and individual bean checks; the row is an inventory, not one conjunction applied to every bean. A classpath match alone does not provide the beans required by a constructor.

| Configuration source | Declared activation, ordering and backoff |
|---|---|
| [ParallaxLiveConfiguration](../jmouse-spring/jmouse-parallax-live/src/main/java/org/jmouse/parallax/live/socket/ParallaxLiveConfiguration.java) | `@Configuration(proxyBeanMethods = false)`<br>`@ConditionalOnClass(ServerContainer.class)` |

## Configuration contract

jmouse.parallax.live.path defaults to the shared management root plus /parallax/live. ParallaxLiveRoutes.PATH is a Spring placeholder resolved by this configuration, not by MVC mapping registration. Stepping thread count is fixed at 2 in LiveCircuits; frame timing and runtime bounds are constants in LiveRun, not published Boot properties.

The definitions that own these settings and route placeholders are:

- [ParallaxLiveRoutes](../jmouse-spring/jmouse-parallax-live/src/main/java/org/jmouse/parallax/live/socket/ParallaxLiveRoutes.java).

Route constants are expressions resolved by their consumer; `${…}` text is a placeholder, not a literal URL. Shared `ManagementEndpoints.ROOT` may itself be configured by the host.

| Constant | Exact source expression |
|---|---|
| [ParallaxLiveRoutes](../jmouse-spring/jmouse-parallax-live/src/main/java/org/jmouse/parallax/live/socket/ParallaxLiveRoutes.java).SEGMENT | `"/parallax/live"` |
| [ParallaxLiveRoutes](../jmouse-spring/jmouse-parallax-live/src/main/java/org/jmouse/parallax/live/socket/ParallaxLiveRoutes.java).PATH | `"${jmouse.parallax.live.path:" + ManagementEndpoints.ROOT + SEGMENT + "}"` |

## HTTP and protocol surface

This module exposes a servlet-container WebSocket, not an MVC controller. The socket uses `ParallaxLiveRoutes.PATH`; see [LiveCircuitSocket](../jmouse-spring/jmouse-parallax-live/src/main/java/org/jmouse/parallax/live/socket/LiveCircuitSocket.java) for handshake, message and close callbacks.

## Lifecycle and adoption gotchas

The handshake is the access boundary: there is no MVC authorization advice in front of the socket. Browser credentials ride the bearer. subprotocol because the WebSocket constructor cannot set an Authorization header. Handshake configuration is copied per session, preventing principal state from being shared across sockets. Connection closure must dispose its run; closing the Spring service shuts down shared workers.

## Source and related reading

- [Build and dependencies](../jmouse-spring/jmouse-parallax-live/pom.xml).
- [Underlying library manual](JMOUSE_PARALLAX.md).
- [Spring integration family](JMOUSE_SPRING.md).

## Source reference

Direct dependencies below come from this module's [POM](../jmouse-spring/jmouse-parallax-live/pom.xml); test-only dependencies are omitted. Optional dependencies still require the adopter to supply the feature at runtime.

| Dependency | Declared scope | Optional |
|---|---|---|
| `org.jmouse:jmouse-parallax-spring-boot` | compile | no |
| `org.jmouse:jmouse-core` | compile | no |
| `org.springframework.boot:spring-boot` | compile | no |
| `org.springframework.boot:spring-boot-autoconfigure` | compile | yes |
| `org.springframework:spring-context` | compile | no |
| `org.springframework:spring-tx` | compile | no |
| `tools.jackson.core:jackson-databind` | compile | no |
| `jakarta.annotation:jakarta.annotation-api` | compile | no |
| `jakarta.servlet:jakarta.servlet-api` | provided | no |
| `org.apache.tomcat.embed:tomcat-embed-websocket` | provided | no |
| `org.slf4j:slf4j-api` | compile | no |

### Public type map by package

Top-level public types in production source, excluding smoke/example drivers. Links point to the implementation; roles summarize source JavaDoc or the declared contract. Nested builders, records and enum members belong to their enclosing type.

#### `org.jmouse.parallax.live.socket`

| Type | Responsibility / contract |
|---|---|
| [LiveCircuits](../jmouse-spring/jmouse-parallax-live/src/main/java/org/jmouse/parallax/live/socket/LiveCircuits.java) | What a running circuit needs from the stores: the object it runs, checked as the caller may read and run it, the circuit compiled with the catalogue's components, and the controls its live parts offer. The stepping itself is the library's (LiveCircuitSession); each socket drives one (LiveRun) on the shared clock here. |
| [ParallaxLiveConfiguration](../jmouse-spring/jmouse-parallax-live/src/main/java/org/jmouse/parallax/live/socket/ParallaxLiveConfiguration.java) | 🔌 The live-circuit socket. Imported by a product, never auto-configured: |
| [ParallaxLiveRoutes](../jmouse-spring/jmouse-parallax-live/src/main/java/org/jmouse/parallax/live/socket/ParallaxLiveRoutes.java) | 🛣️ Where the live socket answers: jmouse.parallax.live.path, by default /jmouse/parallax/live — beside the REST API at /jmouse/parallax/api, under the same shared root. |

### Verification boundary

This documentation was checked against the current source tree. No Java build, database migration, external API call or runtime scenario is claimed by this documentation change.
