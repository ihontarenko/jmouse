package org.jmouse.git.hosting;

import org.jmouse.git.GitCredentials;
import org.jmouse.git.GitRemote;
import org.junit.jupiter.api.Test;

import java.net.URI;
import java.time.Instant;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/** GitLab against canned REST v4 responses through a fake transport, on a self-hosted instance under a path. */
class GitLabHostingTest {

    private static final String SECRET = "glpat-xXxXxXxXxXxXxXxXxXxX";

    private final FakeHostingTransport transport = new FakeHostingTransport();
    private final GitLabHosting        hosting   = new GitLabHosting(
            URI.create("https://gitlab.example.com/gitlab/"), transport, HostingLimits.DEFAULT);
    private final GitCredentials.AccessToken token = hosting.accessToken(SECRET);

    @Test
    void parsesNestedNamespacesUnderTheBasePath() {
        RepositoryReference expected = new RepositoryReference("gitlab.example.com", "diaspora/platform", "diaspora-project-site");
        for (String address : new String[]{
                "https://gitlab.example.com/gitlab/diaspora/platform/diaspora-project-site",
                "https://gitlab.example.com/gitlab/diaspora/platform/diaspora-project-site.git",
                "https://gitlab.example.com/gitlab/diaspora/platform/diaspora-project-site/-/tree/main/docs",
                "https://GITLAB.example.com/gitlab/diaspora/platform/diaspora-project-site/"}) {
            assertEquals(expected, hosting.parseRepositoryAddress(address), address);
        }
        assertEquals("diaspora/platform/diaspora-project-site", expected.path());
        for (String address : new String[]{
                "https://gitlab.example.com/diaspora/platform/diaspora-project-site",
                "https://gitlab.example.com/gitlab/diaspora",
                "https://gitlab.example.com/gitlab/diaspora/-/tree/main",
                "https://other.example.com/gitlab/diaspora/site",
                "http://gitlab.example.com/gitlab/diaspora/site"}) {
            assertFalse(hosting.recognises(address), address);
        }
    }

    @Test
    void describesAProjectByItsEncodedPath() {
        transport.answer("/gitlab/api/v4/projects/diaspora%2Fplatform%2Fdiaspora-project-site", "gitlab/project.json",
                Map.of("RateLimit-Limit", "2000", "RateLimit-Remaining", "1999", "RateLimit-Reset", "1767225600"));

        HostingReply<RepositoryDescription> reply = hosting.describe(
                new RepositoryReference("gitlab.example.com", "diaspora/platform", "diaspora-project-site"), token);

        assertEquals(Visibility.INTERNAL, reply.value().visibility());
        assertTrue(reply.value().isPrivate(), "internal still needs credentials");
        assertEquals("main", reply.value().defaultBranch());
        assertEquals(2000L, reply.rateLimit().limit());
        assertEquals(1999L, reply.rateLimit().remaining());
        assertEquals("Bearer " + SECRET, transport.lastRequest().headers().get("Authorization"));
        assertEquals("/gitlab/api/v4/projects/diaspora%2Fplatform%2Fdiaspora-project-site",
                transport.lastRequest().endpoint().getRawPath());
    }

    @Test
    void aProjectReadableAnonymouslyIsPublicEvenWithoutAVisibilityField() {
        GitLabHosting gitlabCom = new GitLabHosting(GitLabHosting.GITLAB_COM, transport, HostingLimits.DEFAULT);
        transport.answer("/api/v4/projects/gitlab-org%2Fgitlab", "gitlab/project-anonymous.json", Map.of());

        RepositoryDescription description = gitlabCom.describe(
                gitlabCom.parseRepositoryAddress("https://gitlab.com/gitlab-org/gitlab"), GitCredentials.anonymous()).value();

        assertEquals(Visibility.PUBLIC, description.visibility());
        assertEquals("master", description.defaultBranch());
        assertFalse(transport.lastRequest().headers().containsKey("Authorization"));
    }

    @Test
    void anAuthenticatedAnswerWithoutVisibilityIsRefused() {
        transport.answer("/gitlab/api/v4/projects/gitlab-org%2Fgitlab", "gitlab/project-anonymous.json", Map.of());

        HostingException failure = assertThrows(HostingException.class, () -> hosting.describe(
                new RepositoryReference("gitlab.example.com", "gitlab-org", "gitlab"), token));

        assertEquals(HostingException.Reason.INVALID_RESPONSE, failure.reason());
    }

    @Test
    void inspectsAnAccessTokenWithItsScopesAccountAndExpiry() {
        transport.answer("/gitlab/api/v4/personal_access_tokens/self", "gitlab/personal-access-token.json", Map.of());
        transport.answer("/gitlab/api/v4/user", "gitlab/user.json", Map.of("RateLimit-Remaining", "1998"));

        HostingReply<TokenInspection> reply = hosting.inspectToken(token);

        TokenInspection.Accepted accepted = assertInstanceOf(TokenInspection.Accepted.class, reply.value());
        assertEquals("john_smith", accepted.account());
        assertEquals(Set.of("read_api", "read_repository"), accepted.scopes());
        assertTrue(accepted.scopesReported());
        assertEquals(Instant.parse("2026-12-31T00:00:00Z"), accepted.expiresAt());
        assertEquals(1998L, reply.rateLimit().remaining());
    }

    @Test
    void anOauthTokenIsAcceptedWithScopesUnreported() {
        transport.refuse("/gitlab/api/v4/personal_access_tokens/self", 404, Map.of());
        transport.answer("/gitlab/api/v4/user", "gitlab/user.json", Map.of());

        TokenInspection.Accepted accepted = assertInstanceOf(TokenInspection.Accepted.class, hosting.inspectToken(token).value());

        assertEquals("john_smith", accepted.account());
        assertFalse(accepted.scopesReported());
        assertTrue(accepted.scopes().isEmpty());
    }

    @Test
    void aTokenThatMayNotReadItsAccountIsStillAccepted() {
        transport.answer("/gitlab/api/v4/personal_access_tokens/self", "gitlab/personal-access-token.json", Map.of());
        transport.refuse("/gitlab/api/v4/user", 403, Map.of());

        TokenInspection.Accepted accepted = assertInstanceOf(TokenInspection.Accepted.class, hosting.inspectToken(token).value());

        assertTrue(accepted.accountName().isEmpty());
        assertTrue(accepted.hasScope("read_repository"));
    }

    @Test
    void aRevokedTokenIsRejected() {
        transport.refuse("/gitlab/api/v4/personal_access_tokens/self", 401, Map.of());

        assertInstanceOf(TokenInspection.Rejected.class, hosting.inspectToken(token).value());
        assertEquals(1, transport.requests().size(), "a rejected token is not asked about its account");
    }

    @Test
    void listsMemberProjectsPageByPage() {
        transport.answer("/gitlab/api/v4/projects", "gitlab/projects.json", Map.of("x-next-page", "3", "x-page", "2"));

        RepositoryPage page = hosting.repositories(token, new PageRequest(2, 20)).value();

        assertEquals(2, page.repositories().size());
        RepositoryDescription empty = page.repositories().get(1);
        assertEquals(new RepositoryReference("gitlab.example.com", "john_smith", "empty"), empty.repository());
        assertNull(empty.defaultBranch(), "an empty project has no default branch");
        assertEquals(Visibility.PUBLIC, empty.visibility());
        assertEquals(new PageRequest(3, 20), page.nextPage().orElseThrow());
        assertEquals("membership=true&per_page=20&page=2&order_by=id&sort=asc",
                transport.lastRequest().endpoint().getRawQuery());
    }

    @Test
    void theLastPageHasAnEmptyNextPageHeader() {
        transport.answer("/gitlab/api/v4/projects", "gitlab/projects.json", Map.of("x-next-page", ""));

        assertFalse(hosting.repositories(token, PageRequest.first(20)).value().hasNextPage());
    }

    @Test
    void aThrottledRequestCarriesRetryAfter() {
        transport.refuse("/gitlab/api/v4/projects", 429, Map.of("Retry-After", "30", "RateLimit-Remaining", "0"));

        HostingException failure = assertThrows(HostingException.class, () -> hosting.repositories(token, PageRequest.first(20)));

        assertEquals(HostingException.Reason.RATE_LIMITED, failure.reason());
        assertEquals(30, failure.rateLimit().retryAfter().toSeconds());
        assertFalse(failure.getMessage().contains(SECRET));
    }

    @Test
    void cloneRemoteKeepsTheBasePathAndUsesTheOauthUsername() {
        GitRemote remote = hosting.cloneRemote(
                new RepositoryReference("gitlab.example.com", "diaspora/platform", "diaspora-project-site"), token);

        assertEquals(URI.create("https://gitlab.example.com/gitlab/diaspora/platform/diaspora-project-site.git"), remote.address());
        assertEquals("oauth2", assertInstanceOf(GitCredentials.AccessToken.class, remote.credentials()).username());
        assertFalse(remote.toString().contains(SECRET));
    }
}
