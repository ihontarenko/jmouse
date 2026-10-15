package org.jmouse.git.hosting;

import org.jmouse.git.GitCredentials;
import org.jmouse.git.GitRemote;
import org.junit.jupiter.api.Test;

import java.net.URI;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/** GitHub against canned REST v3 responses through a fake transport. */
class GitHubHostingTest {

    private static final String SECRET = "ghp_16C7e42F292c6912E7710c838347Ae178B4a";

    private final FakeHostingTransport transport = new FakeHostingTransport();
    private final GitHubHosting        hosting   = new GitHubHosting(transport, HostingLimits.DEFAULT);
    private final GitCredentials.AccessToken token = hosting.accessToken(SECRET);

    @Test
    void parsesTheRepositoryAnAddressNames() {
        RepositoryReference expected = new RepositoryReference("github.com", "octocat", "Hello-World");
        for (String address : new String[]{
                "https://github.com/octocat/Hello-World",
                "https://github.com/octocat/Hello-World.git",
                "https://github.com/octocat/Hello-World/",
                "https://GitHub.com/octocat/Hello-World/tree/main/docs",
                "  https://github.com/octocat/Hello-World/blob/main/README.md  "}) {
            assertEquals(expected, hosting.parseRepositoryAddress(address), address);
        }
        for (String address : new String[]{
                "http://github.com/octocat/Hello-World",
                "https://gitlab.com/octocat/Hello-World",
                "https://user:secret@github.com/octocat/Hello-World",
                "https://github.com/octocat",
                "git@github.com:octocat/Hello-World.git",
                "https://github.com/octocat/..",
                "not an address"}) {
            assertFalse(hosting.recognises(address), address);
        }
    }

    @Test
    void describesAPrivateRepositoryAndItsDefaultBranch() {
        transport.answer("/repos/octocat/Hello-World", "github/repository.json",
                Map.of("x-ratelimit-limit", "5000", "x-ratelimit-remaining", "4999", "x-ratelimit-reset", "1767225600"));

        HostingReply<RepositoryDescription> reply = hosting.describe(
                hosting.parseRepositoryAddress("https://github.com/octocat/Hello-World"), token);

        assertTrue(reply.value().isPrivate());
        assertEquals(Visibility.PRIVATE, reply.value().visibility());
        assertEquals("master", reply.value().defaultBranch());
        assertEquals(5000L, reply.rateLimit().limit());
        assertEquals(4999L, reply.rateLimit().remaining());
        assertEquals(Instant.ofEpochSecond(1767225600), reply.rateLimit().resetsAt());
        FakeHostingTransport.Request request = transport.lastRequest();
        assertEquals(URI.create("https://api.github.com/repos/octocat/Hello-World"), request.endpoint());
        assertEquals("Bearer " + SECRET, request.headers().get("Authorization"));
        assertEquals("application/vnd.github+json", request.headers().get("Accept"));
        assertEquals("2022-11-28", request.headers().get("X-GitHub-Api-Version"));
    }

    @Test
    void anAnonymousDescriptionSendsNoAuthorisation() {
        transport.answer("/repos/octocat/Hello-World", "github/repository.json", Map.of());

        hosting.describe(new RepositoryReference("github.com", "octocat", "Hello-World"), GitCredentials.anonymous());

        assertFalse(transport.lastRequest().headers().containsKey("Authorization"));
    }

    @Test
    void inspectsAClassicTokenWithItsScopesAndExpiry() {
        transport.answer("/user", "github/user.json", Map.of(
                "X-OAuth-Scopes", "repo, read:org",
                "github-authentication-token-expiration", "2026-11-30 00:00:00 UTC"));

        TokenInspection inspection = hosting.inspectToken(token).value();

        TokenInspection.Accepted accepted = assertInstanceOf(TokenInspection.Accepted.class, inspection);
        assertEquals("octocat", accepted.account());
        assertEquals(Set.of("repo", "read:org"), accepted.scopes());
        assertTrue(accepted.scopesReported());
        assertTrue(accepted.hasScope("repo"));
        assertEquals(Instant.parse("2026-11-30T00:00:00Z"), accepted.expiresAt());
    }

    @Test
    void aFineGrainedTokenReportsNoScopes() {
        transport.answer("/user", "github/user.json", Map.of());

        TokenInspection.Accepted accepted = assertInstanceOf(TokenInspection.Accepted.class, hosting.inspectToken(token).value());

        assertFalse(accepted.scopesReported());
        assertTrue(accepted.scopes().isEmpty());
        assertNull(accepted.expiresAt());
    }

    @Test
    void aRejectedTokenIsAnAnswerNotAFailure() {
        transport.refuse("/user", 401, Map.of("x-ratelimit-remaining", "59"));

        HostingReply<TokenInspection> reply = hosting.inspectToken(token);

        assertFalse(reply.value().valid());
        assertInstanceOf(TokenInspection.Rejected.class, reply.value());
        assertEquals(59L, reply.rateLimit().remaining());
    }

    @Test
    void listsRepositoriesPageByPage() {
        transport.answer("/user/repos", "github/user-repositories.json", Map.of(
                "Link", "<https://api.github.com/user/repos?per_page=2&page=2>; rel=\"next\", "
                        + "<https://api.github.com/user/repos?per_page=2&page=5>; rel=\"last\""));

        RepositoryPage page = hosting.repositories(token, PageRequest.first(2)).value();

        assertEquals(2, page.repositories().size());
        assertEquals(new RepositoryReference("github.com", "octo-org", "Spoon-Knife"), page.repositories().get(1).repository());
        assertEquals(Visibility.PRIVATE, page.repositories().get(1).visibility(), "falls back to the private flag");
        assertEquals(Visibility.PUBLIC, page.repositories().get(0).visibility());
        assertEquals(new PageRequest(2, 2), page.nextPage().orElseThrow());
        assertEquals("per_page=2&page=1&sort=full_name&direction=asc", transport.lastRequest().endpoint().getRawQuery());
    }

    @Test
    void theLastPageHasNoNext() {
        transport.answer("/user/repos", "github/user-repositories.json", Map.of(
                "Link", "<https://api.github.com/user/repos?per_page=2&page=1>; rel=\"prev\", "
                        + "<https://api.github.com/user/repos?per_page=2&page=1>; rel=\"first\""));

        assertTrue(hosting.repositories(token, new PageRequest(2, 2)).value().nextPage().isEmpty());
    }

    @Test
    void anExhaustedWindowIsRateLimitedWithItsResetNotForbidden() {
        transport.refuse("/repos/octocat/Hello-World", 403, Map.of(
                "x-ratelimit-limit", "60", "x-ratelimit-remaining", "0", "x-ratelimit-reset", "1767225600"));

        HostingException failure = assertThrows(HostingException.class,
                () -> hosting.describe(new RepositoryReference("github.com", "octocat", "Hello-World"), token));

        assertEquals(HostingException.Reason.RATE_LIMITED, failure.reason());
        assertEquals(403, failure.status());
        assertTrue(failure.rateLimit().exhausted());
        assertEquals(Instant.ofEpochSecond(1767225600), failure.rateLimit().resetsAt());
        assertFalse(failure.retryable(), "a throttled call waits for the window; it is not retried blindly");
        assertFalse(failure.getMessage().contains(SECRET));
    }

    @Test
    void aSecondaryLimitAsksToWait() {
        transport.refuse("/repos/octocat/Hello-World", 429, Map.of("retry-after", "60"));

        HostingException failure = assertThrows(HostingException.class,
                () -> hosting.describe(new RepositoryReference("github.com", "octocat", "Hello-World"), token));

        assertEquals(HostingException.Reason.RATE_LIMITED, failure.reason());
        assertEquals(Duration.ofSeconds(60), failure.rateLimit().retryAfter());
    }

    @Test
    void statusesBecomeTypedRefusals() {
        RepositoryReference repository = new RepositoryReference("github.com", "octocat", "Hello-World");
        assertRefusal(404, HostingException.Reason.NOT_FOUND, repository);
        assertRefusal(403, HostingException.Reason.FORBIDDEN, repository);
        assertRefusal(401, HostingException.Reason.UNAUTHORISED, repository);
        assertRefusal(301, HostingException.Reason.PROVIDER_REFUSED, repository);
        HostingException serverError = assertRefusal(502, HostingException.Reason.PROVIDER_REFUSED, repository);
        assertTrue(serverError.retryable());
    }

    @Test
    void cloneRemoteHandsJmouseGitAnHttpsAddressAndTheToken() {
        GitRemote remote = hosting.cloneRemote(new RepositoryReference("github.com", "octocat", "Hello-World"), token);

        assertEquals(URI.create("https://github.com/octocat/Hello-World.git"), remote.address());
        GitCredentials.AccessToken credentials = assertInstanceOf(GitCredentials.AccessToken.class, remote.credentials());
        assertEquals("x-access-token", credentials.username());
        assertFalse(remote.toString().contains(SECRET));
        assertThrows(HostingException.class,
                () -> hosting.cloneRemote(new RepositoryReference("gitlab.com", "octocat", "Hello-World"), token));
    }

    @Test
    void anEnterpriseServerUsesItsOwnAddresses() {
        GitHubHosting enterprise = new GitHubHosting(URI.create("https://github.example.com/"),
                URI.create("https://github.example.com/api/v3"), transport, HostingLimits.DEFAULT);
        transport.answer("/api/v3/repos/team/tool", "github/repository.json", Map.of());

        RepositoryReference repository = enterprise.parseRepositoryAddress("https://github.example.com/team/tool.git");
        enterprise.describe(repository, GitCredentials.anonymous());

        assertEquals("github.example.com", repository.host());
        assertEquals(URI.create("https://github.example.com/api/v3/repos/team/tool"), transport.lastRequest().endpoint());
        assertThrows(HostingException.class, () -> new GitHubHosting(URI.create("http://github.example.com"),
                URI.create("https://github.example.com/api/v3"), transport, HostingLimits.DEFAULT));
    }

    @Test
    void aMalformedBodyIsRefusedWithoutItsContent() {
        transport.answer("/repos/octocat/Hello-World", "github/user.json", Map.of());

        HostingException failure = assertThrows(HostingException.class,
                () -> hosting.describe(new RepositoryReference("github.com", "octocat", "Hello-World"), token));

        assertEquals(HostingException.Reason.INVALID_RESPONSE, failure.reason());
        assertFalse(failure.getMessage().contains("octocat"));
    }

    private HostingException assertRefusal(int status, HostingException.Reason reason, RepositoryReference repository) {
        transport.refuse("/repos/octocat/Hello-World", status, Map.of());
        HostingException failure = assertThrows(HostingException.class, () -> hosting.describe(repository, token));
        assertEquals(reason, failure.reason(), "HTTP " + status);
        assertEquals(status, failure.status());
        return failure;
    }
}
