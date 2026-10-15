# jmouse-ai-mcp-authorization

> Siblings: [`JMOUSE_AI_MODEL.md`](JMOUSE_AI_MODEL.md) (the vocabulary). The document shapes and the
> route constants live in `jmouse-ai-mcp`, which this module and every adopter both depend on.

The client-authorization half of the Model Context Protocol: Dynamic Client Registration, `authorize`,
the token endpoint, refresh, revocation, and the consent screen a person actually clicks. Written once
after Identity, Tessera and WiQ had each written it — 258, 366 and 381 lines that agreed on everything
that mattered.

**Without this module a protocol endpoint can only be reached by a bearer token pasted into a client's
configuration**, because there is nothing to answer a registration request with. A client that gets a
404 there reports `Dynamic Client Registration rejected`, which is the single most common symptom of
the module simply not being installed.

It holds no persistence of its own beyond `jmouse-ai-jpa`'s rows, no scheduling and no product policy.
It knows nothing about who a credential is issued *to*: that reaches it as an opaque reference.

---

## What a product owes it — three beans and a key

The two autoconfigurations are deliberately separate. `McpCredentialAutoConfiguration` *provides* the
issuer; `McpAuthorizationConfiguration` is `@ConditionalOnBean(CredentialIssuer.class)` and configures
the flow around it. Together they would make a condition depend on its own output.

| | |
|---|---|
| `CredentialOwners` | `find(ownerReference)` → `CredentialOwner(subject, displayName, email)`. The `subject` becomes the token's `sub`. |
| `ApprovingSubject` | `current()` → who is approving, and the `Choice` list they may issue against. |
| `JwtEncoder` **named `mcpTokenEncoder`** | ⚠️ **By name, never by type.** A product may hold several encoders, and the one that signs a protocol credential must not be whichever happened to be the only candidate that day. |
| a signing key | The product's own secret, 32 bytes minimum for HS256. Not a property of this module. |

`AgentDirectory` and `AgentConnections` must also be beans — they come from `jmouse-ai-jpa`, and
`McpCredentialAutoConfiguration` is `afterName = "org.jmouse.ai.spring.AiJpaAutoConfiguration"` for
exactly that reason. Without the ordering the condition evaluated first, found no directory, and
**contributed nothing silently** — surfacing later as a missing `ProtocolCredentials` bean, pointing at
the controller that wanted it rather than at the condition that declined.

Every bean is `@ConditionalOnMissingBean`, so a product that genuinely differs replaces one without
opting out of the rest. Innoventa contributes its own `ProtocolTokenMinter` and still gets the
enrolment, the refusals, the rotation and the revocation.

## The routes it serves, and who may reach them

Prefixes are placeholders in the mappings, resolved before any bean exists:
`jmouse.mcp.authorization.protocol-prefix` (default `/api/agents/authorization`) and `consent-prefix`
(defaults to the protocol one). `properties.routes()` returns them assembled.

| Route | Reached by | Must be |
|---|---|---|
| `POST …/register` | a client with nothing yet | **public** |
| `POST …/token` | a client proving PKCE, no secret | **public** |
| `GET …/authorize` | **a browser** | ⚠️ **public** — see below |
| `GET …/consent` | a browser | **public** (grants nothing) |
| `POST …/review` · `POST …/approve` | the consent page, carrying the person's token | **authenticated** |

⚠️ **`/authorize` MUST BE PUBLIC, and closing it is the easy mistake.** It looks like the endpoint that
identifies somebody, and it is not: it vets the query parameters and sends the browser on to the consent
screen. A browser arrives with **no `Authorization` header** — it cannot, the token is in web storage
where only script reaches it. Behind `authenticated()` the whole flow dies on a 401 in a tab, and the
client reports an authentication failure that has nothing to do with authentication.

## ⚠️ The discovery documents are the PRODUCT's to serve

This module serves none of them. `jmouse-ai-mcp` owns only their *shape*:
`AuthorizationDocuments.protectedResource(...)` and `.authorizationServer(...)`, plus the paths in
`AuthorizationRoutes` — `/.well-known/oauth-protected-resource`,
`/.well-known/oauth-authorization-server`, `WELL_KNOWN_PATTERN`, `ANY_RESOURCE_SUFFIX`, `SCOPE`.

They sit at the **site root**, not under any prefix, which is why their location is a deployment fact
and not this module's business. A product writes a small `@RestController` for both.

⚠️ **Declare BOTH the bare path and the `/**`-suffixed one.** A product that is an OAuth2 resource
server gets the bare `oauth-protected-resource` served by Spring Security, so Kiwi declares only the
suffixed form and redirects. A product that is **not** a resource server — one with its own bearer
filter — has nothing else answering, and must declare both or registration never starts.

## ⚠️ `WWW-Authenticate` on the 401 is where the whole chain begins

A client meets a 401 from the protocol endpoint, reads `resource_metadata` off that header, fetches the
two documents, and only then knows where to register. **No header, no flow** — and the failure looks
like a broken server rather than a missing line.

```
WWW-Authenticate: Bearer resource_metadata="https://host/.well-known/oauth-protected-resource"
```

⚠️ Spring Security's `authenticationEntryPoint` is not always where that 401 comes from. A product with
a filter of its own that writes `sendError(401)` directly — Raccoon's `AiHttpBoundaryFilter` does, when
no caller is present — **bypasses the entry point entirely**, so the header has to be set there as well.
Setting it in one place and testing through the other is a long afternoon.

## ⚠️ Two addresses, and in development they are two servers

| | |
|---|---|
| `resource-url` | where a **client** reaches the API; what the discovery documents publish |
| `browser-url` | where a **person** reaches the interface — locally a dev server on another port |

`/authorize` redirects to `browser-url + consentRoute`. Building that from the API address lands a
browser on a backend that serves no application and finds nobody signed in; building the discovery
documents from the browser address publishes an origin the protocol is not served on. **Neither failure
is visible from the other side.**

⚠️ **The consent screen must therefore be reachable through whatever serves the interface** — in
development, the dev server's proxy. Two consequences:

- the proxy needs a rule for the protocol prefix (and for `/.well-known`);
- the prefix **may not live under a path the product guards**. Raccoon's first attempt put it under
  `/jmouse-ai`, which its own boundary filter closes behind `system:read`/`system:write` — and
  `/register` and `/token` are reached by a client that holds nothing.

The screen reads the product's own session out of web storage, which is what these say:

```yaml
consent:
  token-storage-key:   oidc.user:http://localhost:9090:kiwi-web   # or a product's own key
  token-storage-field: access_token                                # empty when the value IS the token
  sign-in-route:       /
  template:            consent-permit
```

⚠️ Changing the key in the interface changes it here, and **the only symptom of a mismatch is a consent
screen saying nobody is signed in.** There are ~23 templates beside `consent-frame`, all extending it,
so only the markup differs — and **a name nothing matches fails at STARTUP**, which is the right place.

## ⚠️ `allow-nested-redirect-paths` exists for Codex

Codex registers a fresh nonce in the path it listens on — `http://127.0.0.1:<port>/callback/<nonce>` —
so there is no spelling of it to allow ahead of time, and exact matching refuses it at registration.
Switching this on allows a client's own segments *under* an already-allowed path, held to unreserved
characters, on a loopback address it already proved. Host, explicit port, absent query and fragment, and
PKCE are all untouched.

## ⚠️ `ApprovingSubject.newSubject(...)` versus `Choice.of(...)` — not a style choice

This is the one that costs an evening, because it fails **after** the person has approved, at the token
exchange, with a sentence that sounds like the code expired:

```
{"error":"invalid_grant","error_description":"The agent this code was approved for no longer exists."}
```

`StandardCredentialIssuer.approvedAgent` branches on the prefix:

| the choice's reference | what happens |
|---|---|
| `new-subject:<ownerReference>` | `AgentEnrolment.agentFor(...)` — **an agent is enrolled** against that owner |
| anything else | `agents.find(reference)` — the reference must be **an existing agent's id** |

So a product that has no agent rows of its own — one where the assistant is meant to act as the
approving person — must offer `ApprovingSubject.newSubject(subjectId, name, detail)`. Offering
`Choice.of(subjectId, …)` reads as "an agent whose id is that subject", there is none, and everything up
to and including the browser approval succeeds first.

Kiwi does not hit this: an agent there is already a member row, so it has a real agent id to name.

## Verifying the credential

The token carries `ProtocolCredentials.CREDENTIAL_CLAIM` (the connection), `AGENT_CLAIM` and
`SCOPE_CLAIM`. A signature alone is not enough: a connection revoked from the interface, an agent
switched off or an owner disabled all leave the token intact.

```java
String connection = token.getClaimAsString(ProtocolCredentials.CREDENTIAL_CLAIM);
String agent      = token.getClaimAsString(ProtocolCredentials.AGENT_CLAIM);
return credentials.admit(agent, connection)      // ⚠️ Optional of the REFUSAL REASON, not of a subject
        .map(reason -> failure(reason))
        .orElseGet(() -> { credentials.noteUsage(agent, connection); return success(); });
```

⚠️ `admit` returning a present `Optional` means **refused** — it answers with the reason. Reading it as
"the subject" inverts the check and admits everything.

A resource-server product plugs a `JwtDecoder` with that validator into Spring Security and is done. A
product with its own bearer filter — Raccoon — **decodes the token itself**, as a third branch beside
its bootstrap token and its API credentials.

⚠️ **Watch for a length cap on the `Authorization` header.** Raccoon's filter bounded it at 263
characters — ample for an opaque API token, far too short for a JWT carrying claims — and a header over
the bound was *ignored*, so every protocol call answered 401 with nothing anywhere saying why.

## Other things worth knowing

- ⚠️ **`ProtocolCredentials` was `AgentCredentials`** until the module's own architecture test began
  refusing the words `account`, `user`, `member` and `agent` in this package's type names: one product
  issues to a sub-account with its own permissions and another to the approving person, so a type that
  knew the difference would be wrong for one of them. A product pinned to `1.0.0-b20260802` still
  imports the old name — **Kiwi and Tessera do; Raccoon on `1.0.1` does not** — so their integrations
  are not copy-paste for one another.
- `ClientNameRegistry` defaults to the **JPA** one when `AiClientRegistration` and an
  `EntityManagerFactory` are present. It used to be in-memory unconditionally, and that was losing
  names: the client's name is copied onto the connection row at approval and in one product *becomes the
  agent's name*, so a restart between registration and approval baked "An unnamed client" into a durable
  record, permanently — and two clients that both landed there became one agent.
- An unknown `client_id` at `/authorize` is refused with `invalid_client`, and the description is
  **written for a person**: the client prints it verbatim in a dead-ended tab. The refusal is also the
  mechanism — it makes the client discard its cached registration, so the next attempt registers afresh
  and succeeds. One failed round trip is the design.
- `signing-secret` unset means **one generated per run**: every credential dies on restart and two
  instances refuse each other's. Right on a laptop, wrong everywhere else.
- `audience` is stated, never defaulted: an empty one mints a credential no decoder can be made to
  refuse, so startup fails instead.

⚠️ **The code is the record and this file is a copy.** When one disagrees with the code, the code wins —
and this file is corrected in the same change.
