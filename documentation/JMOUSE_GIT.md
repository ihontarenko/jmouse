# jmouse-git and jmouse-git-hosting

Two modules, split on one line: **Git** and **a hosting**. `jmouse-git` clones and updates one branch of a
remote through JGit and knows nothing about who hosts it. `jmouse-git-hosting` speaks the GitHub and GitLab
REST APIs behind one interface and hands `jmouse-git` a ready remote. Neither holds persistence, scheduling,
retries, caching or product policy — those are the adopter's.

Coordinates: `org.jmouse:jmouse-git:1.0.1` (JGit `6.10.0.202406032230-r`, pinned in `jmouse-dependencies` as
`jgit.version`) and `org.jmouse:jmouse-git-hosting:1.0.1` (depends on `jmouse-git` and Jackson). No Spring, no
jmouse-core. Tracked as `JMF-346`.

## Why two modules

A product that only clones public repositories (Raccoon's GIT source) should not carry an HTTP API client for two
hostings; a product that only lists a user's repositories should not carry JGit. The hosting module depends on
`jmouse-git` for exactly two types — `GitCredentials` and `GitRemote` — so a token is one value from inspection
to clone, and `cloneRemote(...)` returns something `GitClient` takes as it is. One hosting module rather than one
per host: GitHub and GitLab share the transport, the refusal taxonomy and the rate-limit reading, and differ only
in endpoints and JSON shapes.

## jmouse-git — package `org.jmouse.git`

- `GitClient` — the contract. `resolve(remote, branch, limits, cancellation)` → `RemoteHead` (ls-remote, no
  clone); `clone(remote, branch, directory, limits, cancellation)` → `Checkout`; `update(...)` → `Checkout` (fetch +
  hard reset + clean; clones into an absent or empty directory); `inspect(directory)` → `Checkout` (no network).
  Each network operation also has an overload without `cancellation`, which passes `GitCancellation.NONE`.
- `GitCancellation` — functional `boolean cancelled()`, polled between phases and every time JGit asks whether to go
  on (per object received, per file written), so it must be cheap. A signal that throws counts as cancelled, which
  lets Raccoon's throwing checkpoint be wrapped as `() -> { try { checkpoint.run(); return false; } catch
  (RuntimeException stop) { return true; } }`.
- `JGitClient` — the implementation. `new JGitClient()` admits https only; `new JGitClient(policy)` otherwise.
  Stateless, safe to share.
- `GitRemote(address, credentials)` — `GitRemote.anonymous(uri)` for public repositories.
- `GitCredentials` — sealed: `Anonymous` and `AccessToken(username, token)`. The username is the hosting's
  convention (`x-access-token` on GitHub, `oauth2` on GitLab). `toString` hides the token. Sealed so ssh can be
  added later as a third kind and every switch is told by the compiler — **ssh is not built**.
- `BranchSelection` — sealed: `defaultBranch()` (whatever the remote's HEAD names) or `named("main")`, validated
  to a name git accepts and nothing that reads as an option or climbs a path.
- `GitAddressPolicy(schemes, publicHostsOnly)` — `HTTPS` (default), `PUBLIC_HTTPS`, `permitting(GitScheme...)`.
- `GitScheme` — `HTTPS`, `HTTP`, `FILE`.
- `GitLimits(timeout, maximumBytes, depth)` — `DEFAULT` is 120 s, 512 MiB, depth 1 (Raccoon's values);
  `withTimeout`, `withMaximumBytes`, `withDepth`.
- `RemoteHead(branch, commit)`, `Checkout(directory, branch, commit, bytes)`, `CommitId(value)` (40 or 64 hex).
- `GitException` — typed `Reason`: `ADDRESS_NOT_ALLOWED`, `NOT_FOUND`, `UNAUTHORISED`, `BRANCH_NOT_FOUND`,
  `EMPTY_REPOSITORY`, `TOO_LARGE`, `TIMED_OUT`, `CANCELLED`, `DIRECTORY_NOT_EMPTY`, `NOT_A_CHECKOUT`, `TRANSPORT`; `retryable()`
  for `TIMED_OUT` and `TRANSPORT`. The message is a fixed sentence per reason; no JGit cause is attached, because
  JGit's messages may quote the server.

### Safety rules (lifted from Raccoon's `GitSnapshotter` and tightened)

| Rule | How |
|---|---|
| Shallow | `fetch --depth` to `GitLimits.depth`, 1 by default; no tags |
| No hooks | JGit runs no hooks for fetch, reset or clean; `core.hooksPath` is set to `/dev/null` anyway |
| No submodules | fetch recursion `NO`; `submodule.recurse` and `fetch.recurseSubmodules` false |
| No symlinks | `core.symlinks=false` — a symbolic link in the tree is written as a plain file |
| No redirects | `http.followRedirects=false` on every repository, so a token is never carried to another host |
| Timeout | JGit's network timeout (inactivity) plus a whole-operation deadline enforced by the progress monitor |
| Size ceiling | the progress monitor measures the directory (at most every 250 ms) as objects arrive and files are written, and cancels past the ceiling; the finished checkout is measured again. `.git` counts |
| https only | default policy; a token is refused over anything but https whatever the policy admits |
| Public hosts | opt-in `PUBLIC_HTTPS`: every resolved address must be public (loopback, RFC 1918, link-local, CGNAT, reserved, ULA refused; `file://` refused) |
| Address hygiene | user information, query and fragment are always refused; credentials only ever travel through the seam |
| No machine configuration | user, XDG, system and JGit configuration files are hidden from every operation, so `url.insteadOf`, `http.extraHeader`, `http.sslVerify` and the rest cannot redirect or weaken a transfer (see *Configuration isolation*) |
| Anonymous means anonymous | an anonymous call hands JGit a provider that answers nothing, so a JVM-wide default provider is never offered |

A clone is built as `init` → configure → `ls-remote` → `fetch --depth` → hard reset onto the fetched commit, not
with `CloneCommand`, so the configuration above is in place before the first byte arrives and clone and update
share one path. `resolve()` lists through an in-memory repository.

### Cancellation

- **Clone**: honoured throughout. A cancelled clone fails with `CANCELLED` and removes everything it wrote, exactly
  like any other failure; the caller's directory itself is kept.
- **Update**: honoured until the fetch completes, then deliberately ignored while refs move, the working tree is
  reset and untracked files are cleaned. A cancelled update therefore leaves the checkout **consistent at the commit
  it held** (fetched objects may remain in `.git`); no fresh clone is needed. Any other failure during the rewrite —
  the deadline or the size ceiling — still leaves it unspecified, and the caller clones afresh.
- **Resolve**: checked before and after the listing; the listing itself cannot be interrupted and ends within the
  timeout.

### Configuration isolation

JGit reads machine configuration through one JVM-wide `SystemReader`, and a repository on disk takes it as its base
when opened — there is no per-repository switch. `IsolatedConfiguration` installs, once, a `SystemReader.Delegate`
wrapping whatever reader is in place: it changes nothing for a thread outside a jmouse-git call and answers empty
user, system and JGit configuration for a thread inside one. If something installs another reader later, the next
call wraps that one. ⚠️ That install **is** a JVM-wide side effect, visible to anyone who inspects
`SystemReader.getInstance()`; it is the only way short of forking JGit. JGit takes proxies from the JVM's
`ProxySelector`, never from git configuration, so isolation does not change proxying.

## jmouse-git-hosting — package `org.jmouse.git.hosting`

- `GitHosting` — the contract. `host()`; `parseRepositoryAddress(address)` → `RepositoryReference` and
  `recognises(address)` (no network); `accessToken(token)` → `GitCredentials.AccessToken` with the hosting's
  username; `describe(repository, credentials)` → `HostingReply<RepositoryDescription>`;
  `inspectToken(token)` → `HostingReply<TokenInspection>`; `repositories(token, page)` →
  `HostingReply<RepositoryPage>`; `cloneRemote(repository, credentials)` → `GitRemote` (no network).
- `GitHubHosting(transport, limits)` for github.com, or `(webAddress, apiAddress, transport, limits)` for a GitHub
  Enterprise server. REST v3, `X-GitHub-Api-Version: 2022-11-28`.
- `GitLabHosting(baseAddress, transport, limits)` — any instance, base path allowed (`https://host/gitlab`); API is
  `{base}/api/v4`. `GitLabHosting.GITLAB_COM` for gitlab.com.
- `RepositoryReference(host, owner, name)` — on GitLab the owner is the full namespace (`group/sub`).
- `RepositoryDescription(repository, visibility, defaultBranch)` — `isPrivate()`; `defaultBranch` is null when the
  hosting reports none (an empty GitLab project).
- `Visibility` — `PUBLIC`, `INTERNAL`, `PRIVATE`.
- `TokenInspection` — sealed: `Accepted(account, scopes, scopesReported, expiresAt)` or `Rejected()`. A 401 is an
  answer (`Rejected`), not an exception.
- `PageRequest(page, size)` (size 1…100), `RepositoryPage(repositories, request, hasNextPage)` with `nextPage()`.
- `HostingReply<T>(value, rateLimit)` and `RateLimit(limit, remaining, resetsAt, retryAfter)` — read from
  `x-ratelimit-*` (GitHub), `ratelimit-*` (GitLab) and `retry-after`; every field null when not sent.
- `HostingTransport` / `JdkHostingTransport` — bounded JSON GET: whole-response deadline with cancellation, success
  body capped while streaming (before parsing), a refusal's body discarded unread, redirects never followed,
  reusable client the adopter closes. Every status comes back as a `HostingHttpResponse` so the rate-limit headers
  of a refusal can be read.
- `HostingLimits(timeoutMilliseconds, maximumResponseBytes)` — `DEFAULT` 10 s, 8 MiB.
- `HostingException` — typed `Reason`: `ADDRESS_NOT_RECOGNISED`, `CONFIGURATION`, `UNAUTHORISED`, `FORBIDDEN`,
  `NOT_FOUND`, `RATE_LIMITED`, `TIMEOUT`, `TRANSPORT`, `PROVIDER_REFUSED`, `INVALID_RESPONSE`, `RESPONSE_LIMIT`;
  safe status, `rateLimit()`, `retryable()`. A rate-limited call is **not** `retryable()` — the caller waits for
  `resetsAt` or `retryAfter`. 403 is `RATE_LIMITED` only when the headers say so (window exhausted or
  `retry-after`), otherwise `FORBIDDEN`.

| Question | GitHub | GitLab |
|---|---|---|
| Describe | `GET /repos/{owner}/{name}` — `owner.login`, `name`, `visibility` (else `private`), `default_branch` | `GET /projects/{url-encoded path}` — `namespace.full_path`, `path`, `visibility`, `default_branch`; no `visibility` on an anonymous answer means public |
| Token | `GET /user` — `login`; scopes from `X-OAuth-Scopes` (classic and OAuth tokens only); expiry from `github-authentication-token-expiration` | `GET /personal_access_tokens/self` — `scopes`, `expires_at` (404/403 → scopes unreported, e.g. an OAuth token); then `GET /user` — `username` (403 → account unknown) |
| List | `GET /user/repos?per_page&page&sort=full_name&direction=asc`; next from `Link rel="next"` | `GET /projects?membership=true&per_page&page&order_by=id&sort=asc`; next from `x-next-page` or `Link` |
| Clone address | `https://github.com/{owner}/{name}.git`, username `x-access-token` | `{base}/{namespace}/{name}.git`, username `oauth2` |

- GitHub answers 404 for a private repository the caller may not see: `NOT_FOUND` covers both.
- A renamed GitHub repository answers 301; it is refused as `PROVIDER_REFUSED`, never followed.
- Base addresses must be https; a token is never sent to a plain-http hosting.

## Wiring

```java
GitClient git = new JGitClient(GitAddressPolicy.PUBLIC_HTTPS);
try (JdkHostingTransport transport = new JdkHostingTransport(Duration.ofSeconds(5))) {
    GitHosting github = new GitHubHosting(transport, HostingLimits.DEFAULT);
    RepositoryReference repository = github.parseRepositoryAddress("https://github.com/owner/name");
    GitCredentials.AccessToken token = github.accessToken(secret);
    RepositoryDescription description = github.describe(repository, token).value();
    Checkout checkout = git.clone(github.cloneRemote(repository, token),
            BranchSelection.defaultBranch(), directory, GitLimits.DEFAULT);
}
```

## Not in these modules

- ssh remotes and keys — `GitCredentials` is sealed with room for them; nothing is built.
- Pruning an updated checkout: objects of earlier revisions stay, and the size ceiling counts them.
- DNS rebinding between the public-host check and JGit's own connect is not excluded — an egress proxy is.
- Raccoon still uses its own `GitSnapshotter`; adopting `GitClient` there is a separate change.

## Verification

`mvn -o -q install -pl jmouse-git,jmouse-git-hosting -am` — 61 tests, no network.

- `JGitClientTest` (16) — JGit-built repositories in a temp directory over `file://` under
  `GitAddressPolicy.permitting(FILE)`: default-branch shallow clone (one commit, hooks path, symlinks off, remote
  URL), named branch, deeper depth, missing branch, empty repository, missing repository, size ceiling with the
  directory emptied, non-empty target refused, update to a new tip discarding local edits and untracked files,
  update switching branch, update into an empty directory, update refused for a plain directory and for a checkout
  of another remote, inspect, resolve, the default client refusing `file://` before touching the directory, and a
  token never sent to a local address.
- `CancellationAndIsolationTest` (6) — a clone cancelled mid-transfer of a 120-file commit (more than three polls,
  directory emptied), a clone cancelled before it begins, a throwing signal counting as cancelled, a cancelled
  resolve, a cancelled update leaving the checkout clean at its old commit and a later update succeeding, and the
  machine's `url.insteadOf` not reaching clone, update or resolve — with a paired control showing plain JGit does
  follow the rewrite under the same configuration.
- `GitAddressPolicyTest` (7) — https-only default, token over https only, public-only refusals across private and
  reserved ranges, address classification, credential redaction, branch-name rules, limit bounds.
- `GitHubHostingTest` (14) and `GitLabHostingTest` (12) — canned REST v3 / v4 bodies through a fake transport:
  address parsing (nested GitLab namespaces under a base path, `/-/` pages), describe, anonymous requests, token
  inspection (scopes, unreported scopes, expiry, rejection, account unreadable), paging, rate limiting (exhausted
  403, 429 with `retry-after`), status taxonomy, malformed bodies, clone remotes, enterprise addresses.
- `JdkHostingTransportTest` (6) — the real transport against a loopback server: body and headers within the cap,
  over-cap refusal, refusal with headers and no body, redirect not followed, timeout, a bad header value refused
  without being repeated.

Not covered by a test: a whole-operation timeout of a clone (no local remote is slow enough to trigger it), and a
real https host.

## Local working trees (Atomic A8)

org.jmouse.git.local.LocalGit is a separate capability from bounded remote checkout/update. JGitLocalGit implements:

- status(directory): staged and working-tree states, configured author, HEAD, branch and content versions;
- log(directory, limit): bounded current-HEAD history, real parent identities and branch/remote/tag references;
- diff(directory, source, currentText): HEAD against disk or an optional unsaved buffer, with exclusive zero-based line edits;
- commit(directory, CommitSelection): explicit selected saved working-tree versions, reviewed HEAD/content checks,
  including new and deleted files. Unrelated staged entries remain staged. Initial commits are supported.

LocalGitLimits bounds file bytes, diff lines, history and selected files. Paths are canonical repository-relative paths;
Git metadata, traversal and symbolic-link escapes cannot be selected. Text diffs reject binary/oversized content.
The configured project root must be the working-tree root; a parent repository is never silently adopted.

Local commits are unsigned and do not run repository hooks. The hooksPath override is in-memory for the operation and
is never persisted to the user's Git configuration. No network, checkout, reset or push is performed. JGit handles
its ordinary index/working-tree conversion rules. A failed new-file commit may leave explicitly selected new files staged;
the reported error says so. Reviewed checks and ordinary JGit locks protect normal concurrent-edit/ref-update cases;
this API is not a distributed transaction across arbitrary external editors and Git processes.

Atomic blocks commits of selected documents with unsaved buffers. The UI starts with no files selected, shows the exact
selection and author, supports diff review, and makes the local/unsigned/no-hooks behavior explicit before Commit.

Verification: one temporary-repository smoke covered status, log, HEAD/buffer diff, selected modification/addition/deletion,
unrelated staged content, stale HEAD/content, traversal and initial commit. No real workspace commit was created.
