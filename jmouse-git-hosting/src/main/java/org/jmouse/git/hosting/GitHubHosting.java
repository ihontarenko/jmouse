package org.jmouse.git.hosting;

import org.jmouse.git.GitCredentials;
import org.jmouse.git.GitRemote;

import java.net.URI;
import java.time.Instant;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * GitHub over its REST API (v3, version {@code 2022-11-28}): github.com by default, or a GitHub Enterprise server
 * given its web and API addresses.
 *
 * <p>Scopes come from the {@code X-OAuth-Scopes} header, which GitHub sends for classic tokens and OAuth tokens
 * only; a fine-grained token's permissions are not reported, and {@link TokenInspection.Accepted#scopesReported()}
 * says so. GitHub answers 404 rather than 403 for a private repository the caller may not see, so
 * {@link HostingException.Reason#NOT_FOUND} covers both. A renamed repository answers with a redirect, which is
 * refused as {@link HostingException.Reason#PROVIDER_REFUSED} rather than followed.
 */
public final class GitHubHosting implements GitHosting {

    /** github.com's web address. */
    public static final URI GITHUB_WEB_ADDRESS = URI.create("https://github.com");

    /** github.com's API address. */
    public static final URI GITHUB_API_ADDRESS = URI.create("https://api.github.com");

    /** The username GitHub documents beside a token in an https clone; any nonblank name is accepted. */
    public static final String TOKEN_USERNAME = "x-access-token";

    private static final DateTimeFormatter EXPIRATION_WITH_ZONE_NAME   = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss z");
    private static final DateTimeFormatter EXPIRATION_WITH_ZONE_OFFSET = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss Z");

    private final URI        webAddress;
    private final HostingApi api;

    /** github.com. */
    public GitHubHosting(HostingTransport transport, HostingLimits limits) {
        this(GITHUB_WEB_ADDRESS, GITHUB_API_ADDRESS, transport, limits);
    }

    /**
     * A GitHub Enterprise server, or github.com named explicitly.
     *
     * @param webAddress the https address repositories live under, such as {@code https://github.example.com}
     * @param apiAddress the https REST API address, such as {@code https://github.example.com/api/v3}
     */
    public GitHubHosting(URI webAddress, URI apiAddress, HostingTransport transport, HostingLimits limits) {
        this.webAddress = HostingAddresses.requireBase(webAddress);
        this.api = new HostingApi(apiAddress, Map.of(
                "Accept", "application/vnd.github+json",
                "X-GitHub-Api-Version", "2022-11-28"), transport, limits);
    }

    @Override
    public String host() {
        return HostingAddresses.hostOf(webAddress);
    }

    @Override
    public RepositoryReference parseRepositoryAddress(String address) {
        List<String> segments = HostingAddresses.segmentsUnder(webAddress, address);
        if (segments.size() < 2) {
            throw HostingAddresses.notRecognised();
        }
        String owner = segments.get(0);
        String name  = HostingAddresses.withoutGitSuffix(segments.get(1));
        if (!RepositoryReference.isSegment(owner) || !RepositoryReference.isSegment(name)) {
            throw HostingAddresses.notRecognised();
        }
        return new RepositoryReference(host(), owner, name);
    }

    @Override
    public GitCredentials.AccessToken accessToken(String token) {
        return GitCredentials.accessToken(TOKEN_USERNAME, token);
    }

    @Override
    public HostingReply<RepositoryDescription> describe(RepositoryReference repository, GitCredentials credentials) {
        requireOwnRepository(repository);
        HostingHttpResponse response = api.require("/repos/" + repository.owner() + "/" + repository.name(), credentials);
        return new HostingReply<>(description(response.body()), RateLimit.of(response.headers()));
    }

    @Override
    public HostingReply<TokenInspection> inspectToken(GitCredentials.AccessToken token) {
        HostingHttpResponse response = api.get("/user", token);
        RateLimit rateLimit = RateLimit.of(response.headers());
        if (response.status() == 401) {
            return new HostingReply<>(new TokenInspection.Rejected(), rateLimit);
        }
        Optional<HostingException> refusal = HostingApi.refusal(response);
        if (refusal.isPresent()) {
            throw refusal.get();
        }
        String account = HostingJson.text(HostingJson.object(response.body()), "login");
        Optional<String> scopesHeader = response.headers().firstValue("x-oauth-scopes");
        Set<String> scopes = new LinkedHashSet<>();
        scopesHeader.ifPresent(header -> {
            for (String scope : header.split(",")) {
                if (!scope.isBlank()) {
                    scopes.add(scope.strip());
                }
            }
        });
        Instant expiresAt = response.headers().firstValue("github-authentication-token-expiration")
                .map(GitHubHosting::expiration)
                .orElse(null);
        return new HostingReply<>(new TokenInspection.Accepted(account, scopes, scopesHeader.isPresent(), expiresAt), rateLimit);
    }

    @Override
    public HostingReply<RepositoryPage> repositories(GitCredentials.AccessToken token, PageRequest page) {
        if (page == null) {
            throw new IllegalArgumentException("A listing needs a page request.");
        }
        HostingHttpResponse response = api.require("/user/repos?per_page=" + page.size() + "&page=" + page.page()
                + "&sort=full_name&direction=asc", token);
        List<RepositoryDescription> repositories = new ArrayList<>();
        for (Object repository : HostingJson.array(response.body())) {
            repositories.add(description(repository));
        }
        RepositoryPage listing = new RepositoryPage(repositories, page, HostingApi.hasNextPage(response.headers()));
        return new HostingReply<>(listing, RateLimit.of(response.headers()));
    }

    @Override
    public GitRemote cloneRemote(RepositoryReference repository, GitCredentials credentials) {
        requireOwnRepository(repository);
        if (credentials == null) {
            throw new IllegalArgumentException("A clone remote needs credentials; use GitCredentials.anonymous() for none.");
        }
        return new GitRemote(URI.create(webAddress + "/" + repository.path() + ".git"), credentials);
    }

    private RepositoryDescription description(Object value) {
        Map<?, ?> repository = HostingJson.object(value);
        Map<?, ?> owner      = HostingJson.object(repository.get("owner"));
        String    login      = HostingJson.text(owner, "login");
        String    name       = HostingJson.text(repository, "name");
        if (!RepositoryReference.isSegment(login) || !RepositoryReference.isSegment(name)) {
            throw HostingJson.invalidResponse();
        }
        Visibility visibility = Visibility.of(HostingJson.optionalText(repository, "visibility"))
                .orElseGet(() -> privateFlag(repository) ? Visibility.PRIVATE : Visibility.PUBLIC);
        return new RepositoryDescription(new RepositoryReference(host(), login, name), visibility,
                HostingJson.optionalText(repository, "default_branch"));
    }

    private static boolean privateFlag(Map<?, ?> repository) {
        Boolean flag = HostingJson.optionalBoolean(repository, "private");
        if (flag == null) {
            throw HostingJson.invalidResponse();
        }
        return flag;
    }

    private void requireOwnRepository(RepositoryReference repository) {
        if (repository == null || !repository.host().equals(host()) || repository.owner().contains("/")) {
            throw HostingAddresses.notRecognised();
        }
    }

    /** GitHub's expiration header, such as {@code 2026-11-30 00:00:00 UTC}; an unreadable value means "not reported". */
    private static Instant expiration(String header) {
        for (DateTimeFormatter format : List.of(EXPIRATION_WITH_ZONE_NAME, EXPIRATION_WITH_ZONE_OFFSET)) {
            try {
                return ZonedDateTime.parse(header.strip(), format).toInstant();
            } catch (DateTimeParseException unreadable) {
                // Try the next documented shape.
            }
        }
        return null;
    }
}
