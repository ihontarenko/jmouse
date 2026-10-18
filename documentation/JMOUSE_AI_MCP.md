# jmouse-ai-mcp — publish guarded tools and adopt remote tools

> **Agent orientation file.** Source code wins when this description differs.
> Module: `jmouse-ai-mcp` · Package: `org.jmouse.ai.mcp` · Java 21.

## What it is

The Model Context Protocol adapter around the [tool catalogue](JMOUSE_AI.md), plus a client bridge that presents
another server's tools as local feature definitions. The authorization package owns protocol primitives;
the Spring companion owns the account-facing HTTP flow documented in
[JMOUSE_AI_MCP_AUTHORIZATION.md](JMOUSE_AI_MCP_AUTHORIZATION.md).

## The map

| Type | Responsibility |
|---|---|
| [McpToolServer](../jmouse-ai-mcp/src/main/java/org/jmouse/ai/mcp/McpToolServer.java) | Convert published tools to SDK specifications; delegate every call to `ToolDispatcher` |
| [McpEndpointAgreement](../jmouse-ai-mcp/src/main/java/org/jmouse/ai/mcp/McpEndpointAgreement.java) | Refuse different served and credential-confined endpoint paths at startup |
| [RemoteToolServer](../jmouse-ai-mcp/src/main/java/org/jmouse/ai/mcp/client/RemoteToolServer.java) | Injectable remote tool listing and invocation contract |
| [McpSyncRemoteToolServer](../jmouse-ai-mcp/src/main/java/org/jmouse/ai/mcp/client/McpSyncRemoteToolServer.java) | Actual SDK-backed synchronous remote connection |
| [McpToolClient](../jmouse-ai-mcp/src/main/java/org/jmouse/ai/mcp/client/McpToolClient.java) / [RemoteTools](../jmouse-ai-mcp/src/main/java/org/jmouse/ai/mcp/client/RemoteTools.java) | Contribute remote actions to a local catalogue and close the connection |
| [RemoteToolSettings](../jmouse-ai-mcp/src/main/java/org/jmouse/ai/mcp/client/RemoteToolSettings.java) | Local namespace, fallback permission and per-remote-tool permission overrides |
| [RemoteTool](../jmouse-ai-mcp/src/main/java/org/jmouse/ai/mcp/client/RemoteTool.java) / [RemoteCallResult](../jmouse-ai-mcp/src/main/java/org/jmouse/ai/mcp/client/RemoteCallResult.java) | Remote metadata and structured success/refusal result |
| [RemoteToolException](../jmouse-ai-mcp/src/main/java/org/jmouse/ai/mcp/client/RemoteToolException.java) | A failed exchange, distinct from a remote application refusal |
| [AuthorizationRoutes](../jmouse-ai-mcp/src/main/java/org/jmouse/ai/mcp/authorization/AuthorizationRoutes.java) / [AuthorizationDocuments](../jmouse-ai-mcp/src/main/java/org/jmouse/ai/mcp/authorization/AuthorizationDocuments.java) | Shared route derivation and discovery document representation |
| [ProofKeyPolicy](../jmouse-ai-mcp/src/main/java/org/jmouse/ai/mcp/authorization/ProofKeyPolicy.java) / [LoopbackRedirectPolicy](../jmouse-ai-mcp/src/main/java/org/jmouse/ai/mcp/authorization/LoopbackRedirectPolicy.java) | PKCE S256 verification and permitted callback addresses |
| [AuthorizationCodeStore](../jmouse-ai-mcp/src/main/java/org/jmouse/ai/mcp/authorization/AuthorizationCodeStore.java) | Account-side issue/redeem boundary without embedding product identity here |

## Serving

Construct `McpToolServer(dispatcher, serverName, serverVersion)` and attach an SDK transport with `serving(...)`.
There are overloads for streamable and general server transport providers. `specifications()` returns the SDK
tool registrations when the adopter assembles a server itself. The protocol surface carries descriptions and
dispatch results, not a second handler registry that can bypass authorization and guards.

HTTP authentication, routing and deployment are the adopter's or the Spring starter's responsibility.
Call `McpEndpointAgreement.require(servedAt, credentialsConfinedTo)` wherever these are separately configured.
A credential valid at one path does not silently become valid at the other. Discovery routes required before
authorization must remain reachable as explained in the authorization manual.

## Connecting another server

Create a `RemoteToolServer`, supply `RemoteToolSettings.of(serverName, namespace, permission)`, then contribute
`McpToolClient` as a `ToolDefinition` to the ordinary catalogue. Use `costing(remoteToolName, permission)` when
different remote tools require different local privileges. The local product decides those privileges; a remote
description cannot assign itself access. Remote origin/trace attributes retain the server/tool association.

`McpToolClient.orAbsent(...)` is the optional-connection path: it can produce an empty contribution when the
server is unavailable at setup. Its result is still `RemoteTools`, with a uniform closing contract. Close owned
connections on shutdown. Tool metadata collected for a catalogue is not a live remote discovery feed.

Do not collapse `RemoteCallResult` refusal into network failure. A lost exchange cannot establish whether the
remote operation ran, so automatic replay of writes is unsafe. Remote tool declarations still pass local
catalogue validation and dispatch guards; the bridge is not an exemption from the local tool contract.

## Source reference

Direct dependencies below come from this module's [POM](../jmouse-ai-mcp/pom.xml); test-only dependencies are omitted. Optional dependencies still require the adopter to supply the feature at runtime.

| Dependency | Declared scope | Optional |
|---|---|---|
| `org.jmouse:jmouse-ai` | compile | no |
| `io.modelcontextprotocol.sdk:mcp` | compile | no |
| `org.slf4j:slf4j-api` | compile | no |
| `io.micrometer:context-propagation` | compile | no |
| `ch.qos.logback:logback-classic` | compile | yes |

### Public type map by package

Top-level public types in production source, excluding smoke/example drivers. Links point to the implementation; roles summarize source JavaDoc or the declared contract. Nested builders, records and enum members belong to their enclosing type.

#### `org.jmouse.ai.mcp`

| Type | Responsibility / contract |
|---|---|
| [McpEndpointAgreement](../jmouse-ai-mcp/src/main/java/org/jmouse/ai/mcp/McpEndpointAgreement.java) | Two unrelated things have to name the same path, and one startup check that says so. |
| [McpToolServer](../jmouse-ai-mcp/src/main/java/org/jmouse/ai/mcp/McpToolServer.java) | The catalogue, published over the protocol. Decides nothing. |

#### `org.jmouse.ai.mcp.authorization`

| Type | Responsibility / contract |
|---|---|
| [AuthorizationCodeStore](../jmouse-ai-mcp/src/main/java/org/jmouse/ai/mcp/authorization/AuthorizationCodeStore.java) | The seam. ⚠️ Everything on the account side of the flow is behind it. |
| [AuthorizationDocuments](../jmouse-ai-mcp/src/main/java/org/jmouse/ai/mcp/authorization/AuthorizationDocuments.java) | The two documents that make a protocol endpoint connectable. |
| [AuthorizationRoutes](../jmouse-ai-mcp/src/main/java/org/jmouse/ai/mcp/authorization/AuthorizationRoutes.java) | Every route the authorization flow needs, derived from the prefixes a product chose. |
| [AuthorizationVocabulary](../jmouse-ai-mcp/src/main/java/org/jmouse/ai/mcp/authorization/AuthorizationVocabulary.java) | The specification's own words, spelled once for everybody. |
| [LoopbackRedirectPolicy](../jmouse-ai-mcp/src/main/java/org/jmouse/ai/mcp/authorization/LoopbackRedirectPolicy.java) | Where an approved client may be sent back to, and nowhere else. |
| [McpAuthorizationException](../jmouse-ai-mcp/src/main/java/org/jmouse/ai/mcp/authorization/McpAuthorizationException.java) | A client's attempt to authorize itself that will not be allowed to continue. |
| [ProofKeyPolicy](../jmouse-ai-mcp/src/main/java/org/jmouse/ai/mcp/authorization/ProofKeyPolicy.java) | Proof that the client redeeming an authorization code is the one that asked for it — PKCE (RFC 7636). |

#### `org.jmouse.ai.mcp.client`

| Type | Responsibility / contract |
|---|---|
| [McpSyncRemoteToolServer](../jmouse-ai-mcp/src/main/java/org/jmouse/ai/mcp/client/McpSyncRemoteToolServer.java) | A remote server reached over the Model Context Protocol itself. |
| [McpToolClient](../jmouse-ai-mcp/src/main/java/org/jmouse/ai/mcp/client/McpToolClient.java) | A server somebody else runs, contributed to this application's catalogue as ordinary tools. |
| [RemoteCallResult](../jmouse-ai-mcp/src/main/java/org/jmouse/ai/mcp/client/RemoteCallResult.java) | What a server this application connected to answered with. |
| [RemoteTool](../jmouse-ai-mcp/src/main/java/org/jmouse/ai/mcp/client/RemoteTool.java) | One thing a server this application connected to says it can do. |
| [RemoteToolException](../jmouse-ai-mcp/src/main/java/org/jmouse/ai/mcp/client/RemoteToolException.java) | A server this application connected to did not answer. |
| [RemoteTools](../jmouse-ai-mcp/src/main/java/org/jmouse/ai/mcp/client/RemoteTools.java) | What a connected server contributes, and the connection behind it. |
| [RemoteToolServer](../jmouse-ai-mcp/src/main/java/org/jmouse/ai/mcp/client/RemoteToolServer.java) | A server this application has connected to, reduced to the two questions a catalogue needs answered. |
| [RemoteToolSettings](../jmouse-ai-mcp/src/main/java/org/jmouse/ai/mcp/client/RemoteToolSettings.java) | What the product decides about a server it connected to, because the library cannot. |

### Verification boundary

This documentation was checked against the current source tree. No Java build, database migration, external API call or runtime scenario is claimed by this documentation change.

Existing checks/examples to consult when changing this API:

- [McpArchitectureTest.java](../jmouse-ai-mcp/src/test/java/org/jmouse/ai/mcp/McpArchitectureTest.java).
