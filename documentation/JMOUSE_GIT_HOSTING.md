# jmouse-git-hosting — bounded repository metadata from GitHub and GitLab

> **Agent orientation file.** Code wins over this copy.
> Module: `jmouse-git-hosting` · Package: `org.jmouse.git.hosting` · Java 21.

## What it is

One read contract over GitHub/GitLab repository metadata, repository listings and token inspection.
It uses bounded JDK HTTP rather than vendor SDKs. Actual Git checkout belongs to
[jmouse-git](JMOUSE_GIT.md); this module neither clones a repository nor maintains a local worktree.
The existing Git manual also covers the relationship between both artifacts.

## The map

| Type | Responsibility |
|---|---|
| [GitHosting](../jmouse-git-hosting/src/main/java/org/jmouse/git/hosting/GitHosting.java) | Common hosting operations for one configured host |
| [GitHubHosting](../jmouse-git-hosting/src/main/java/org/jmouse/git/hosting/GitHubHosting.java) / [GitLabHosting](../jmouse-git-hosting/src/main/java/org/jmouse/git/hosting/GitLabHosting.java) | REST wire mapping and host-specific repository/token semantics |
| [RepositoryReference](../jmouse-git-hosting/src/main/java/org/jmouse/git/hosting/RepositoryReference.java) / [RepositoryDescription](../jmouse-git-hosting/src/main/java/org/jmouse/git/hosting/RepositoryDescription.java) | Repository identity and metadata such as visibility/default branch |
| [PageRequest](../jmouse-git-hosting/src/main/java/org/jmouse/git/hosting/PageRequest.java) / [RepositoryPage](../jmouse-git-hosting/src/main/java/org/jmouse/git/hosting/RepositoryPage.java) | Explicit listing pagination |
| [TokenInspection](../jmouse-git-hosting/src/main/java/org/jmouse/git/hosting/TokenInspection.java) / [Visibility](../jmouse-git-hosting/src/main/java/org/jmouse/git/hosting/Visibility.java) | Shared representation of accepted credentials and repository visibility |
| [HostingReply](../jmouse-git-hosting/src/main/java/org/jmouse/git/hosting/HostingReply.java) / [RateLimit](../jmouse-git-hosting/src/main/java/org/jmouse/git/hosting/RateLimit.java) | Answer together with the rate information actually reported by the host |
| [HostingTransport](../jmouse-git-hosting/src/main/java/org/jmouse/git/hosting/HostingTransport.java) / [HostingHttpResponse](../jmouse-git-hosting/src/main/java/org/jmouse/git/hosting/HostingHttpResponse.java) | Injectable HTTP GET boundary preserving status and response headers |
| [JdkHostingTransport](../jmouse-git-hosting/src/main/java/org/jmouse/git/hosting/JdkHostingTransport.java) / [HostingLimits](../jmouse-git-hosting/src/main/java/org/jmouse/git/hosting/HostingLimits.java) | JDK exchange with explicit time/response-byte ceilings |
| [HostingException](../jmouse-git-hosting/src/main/java/org/jmouse/git/hosting/HostingException.java) | Typed safe refusal without token, header or remote response-body leakage |

## Adoption flow

Choose `GitHubHosting` or `GitLabHosting` for the installation, supply its declared endpoint/transport
collaborators, and call through `GitHosting`. A host instance represents one host; Enterprise/self-hosted
addresses are explicit, not guessed from repository names. GitLab repository ownership includes its
full namespace, so nested groups must not be reduced to the last path segment.

`parseRepositoryAddress` and `recognises` validate/parse an HTTPS repository address without network I/O;
`accessToken` creates the hosting-specific clone credentials. `describe`, `inspectToken` and `repositories`
make the bounded metadata calls. `cloneRemote` combines a repository reference and credentials into
the `GitRemote` consumed by the checkout module. A rejected token is `TokenInspection.Rejected`, an
answer rather than a transport exception. `HostingLimits.DEFAULT` is 10 seconds and 8 MiB per response.

Keep pagination explicit through `PageRequest` and the returned `RepositoryPage`. Preserve the
`HostingReply` rate-limit information for the adopter's pacing policy. Missing rate headers remain null;
they do not mean unlimited requests. GitHub scopes from response headers do not necessarily fully
describe fine-grained token privileges.

## HTTP and failures

`HostingTransport` returns statuses as responses so adapters can still inspect rate-limit headers on
a refusal. `JdkHostingTransport` applies a whole-response deadline and streams through a byte cap
before decoding JSON; a refused response is not a reason to log its body. Inject a fake/loopback
transport when checking translation without using an external account.

Treat refusal reason, safe status and reported retry/pacing information as distinct from a successful
empty page. This module supplies no automatic retry scheduler, token vault or product credentials UI.
Use [vault](JMOUSE_VAULT.md) or another adopter-owned secret source, and make the hosting dependency
optional when the product only needs direct Git transport.

## Source reference

Direct dependencies below come from this module's [POM](../jmouse-git-hosting/pom.xml); test-only dependencies are omitted. Optional dependencies still require the adopter to supply the feature at runtime.

| Dependency | Declared scope | Optional |
|---|---|---|
| `org.jmouse:jmouse-git` | compile | no |
| `com.fasterxml.jackson.core:jackson-databind` | compile | no |

### Public type map by package

Top-level public types in production source, excluding smoke/example drivers. Links point to the implementation; roles summarize source JavaDoc or the declared contract. Nested builders, records and enum members belong to their enclosing type.

#### `org.jmouse.git.hosting`

| Type | Responsibility / contract |
|---|---|
| [GitHosting](../jmouse-git-hosting/src/main/java/org/jmouse/git/hosting/GitHosting.java) | One Git hosting — GitHub or a GitLab instance — and what it can say about its repositories and tokens. |
| [GitHubHosting](../jmouse-git-hosting/src/main/java/org/jmouse/git/hosting/GitHubHosting.java) | GitHub over its REST API (v3, version 2022-11-28): github.com by default, or a GitHub Enterprise server given its web and API addresses. |
| [GitLabHosting](../jmouse-git-hosting/src/main/java/org/jmouse/git/hosting/GitLabHosting.java) | A GitLab instance over its REST API (v4): gitlab.com or a self-hosted instance given its base address, which may carry a path (https://example.com/gitlab). |
| [HostingException](../jmouse-git-hosting/src/main/java/org/jmouse/git/hosting/HostingException.java) | A typed refusal from a hosting call. It never carries a token, a request header or anything the hosting sent in a response body; only the reason, the HTTP status when there was one, and the rate-limit headers. |
| [HostingHttpResponse](../jmouse-git-hosting/src/main/java/org/jmouse/git/hosting/HostingHttpResponse.java) | What a HostingTransport brings back from one GET. |
| [HostingLimits](../jmouse-git-hosting/src/main/java/org/jmouse/git/hosting/HostingLimits.java) | Explicit per-request ceilings supplied by the adopter's policy. |
| [HostingReply](../jmouse-git-hosting/src/main/java/org/jmouse/git/hosting/HostingReply.java) | An answer from a hosting, with the rate-limit state reported alongside it so a caller can pace itself. |
| [HostingTransport](../jmouse-git-hosting/src/main/java/org/jmouse/git/hosting/HostingTransport.java) | Injectable bounded JSON GET. An implementation returns every status as a response rather than throwing, so the hosting can read rate-limit headers off a refusal; it throws HostingException only when there is no response to return — TIMEOUT, TRANSPORT, RESPONSE_LIMIT, INVALID_RESPONSE or CONFIGURATION. |
| [JdkHostingTransport](../jmouse-git-hosting/src/main/java/org/jmouse/git/hosting/JdkHostingTransport.java) | HostingTransport over the JDK HttpClient. The whole response is timed and a success body is capped in bytes while it streams, before any JSON is parsed; an expired call is cancelled. A refusal's body is discarded unread. Redirects are never followed, so credentials cannot be carried elsewhere. The client is reusable; the adopter closes it. |
| [PageRequest](../jmouse-git-hosting/src/main/java/org/jmouse/git/hosting/PageRequest.java) | One page of a listing. |
| [RateLimit](../jmouse-git-hosting/src/main/java/org/jmouse/git/hosting/RateLimit.java) | The rate-limit state a hosting reported with a response. Every field is null when the hosting did not send it; nothing is guessed. GitHub sends x-ratelimit-*, GitLab sends ratelimit-*, and both may send retry-after when they refuse. |
| [RepositoryDescription](../jmouse-git-hosting/src/main/java/org/jmouse/git/hosting/RepositoryDescription.java) | What a hosting says about one repository. |
| [RepositoryPage](../jmouse-git-hosting/src/main/java/org/jmouse/git/hosting/RepositoryPage.java) | One page of the repositories a token can see. |
| [RepositoryReference](../jmouse-git-hosting/src/main/java/org/jmouse/git/hosting/RepositoryReference.java) | Which repository an address names on a hosting. |
| [TokenInspection](../jmouse-git-hosting/src/main/java/org/jmouse/git/hosting/TokenInspection.java) | What a hosting says about an access token: accepted, with what it may do, or rejected. |
| [Visibility](../jmouse-git-hosting/src/main/java/org/jmouse/git/hosting/Visibility.java) | Who may read a repository, in the vocabulary both hostings share. |

### Verification boundary

This documentation was checked against the current source tree. No Java build, database migration, external API call or runtime scenario is claimed by this documentation change.

Existing checks/examples to consult when changing this API:

- [FakeHostingTransport.java](../jmouse-git-hosting/src/test/java/org/jmouse/git/hosting/FakeHostingTransport.java).
- [GitHubHostingTest.java](../jmouse-git-hosting/src/test/java/org/jmouse/git/hosting/GitHubHostingTest.java).
- [GitLabHostingTest.java](../jmouse-git-hosting/src/test/java/org/jmouse/git/hosting/GitLabHostingTest.java).
- [JdkHostingTransportTest.java](../jmouse-git-hosting/src/test/java/org/jmouse/git/hosting/JdkHostingTransportTest.java).
