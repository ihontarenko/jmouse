package org.jmouse.git.hosting;

import org.jmouse.git.GitCredentials;
import org.jmouse.git.GitRemote;

import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * A GitLab instance over its REST API (v4): gitlab.com or a self-hosted instance given its base address, which may
 * carry a path ({@code https://example.com/gitlab}).
 *
 * <p>A repository's owner is its full namespace, subgroups included. Scopes come from
 * {@code GET /personal_access_tokens/self}, which answers for personal, project and group access tokens; an OAuth
 * token is not reported there, and {@link TokenInspection.Accepted#scopesReported()} says so. A token without
 * {@code read_user} may not read its own account, which is reported as an unknown account rather than a refusal.
 */
public final class GitLabHosting implements GitHosting {

    /** gitlab.com's base address. */
    public static final URI GITLAB_COM = URI.create("https://gitlab.com");

    /** The username GitLab documents beside an OAuth token in an https clone; it also accepts it for access tokens. */
    public static final String TOKEN_USERNAME = "oauth2";

    private final URI        baseAddress;
    private final HostingApi api;

    /**
     * @param baseAddress the instance's https address, such as {@code https://gitlab.example.com}; the API is
     *                    {@code {baseAddress}/api/v4}
     */
    public GitLabHosting(URI baseAddress, HostingTransport transport, HostingLimits limits) {
        this.baseAddress = HostingAddresses.requireBase(baseAddress);
        this.api = new HostingApi(URI.create(this.baseAddress + "/api/v4"), Map.of("Accept", "application/json"),
                transport, limits);
    }

    @Override
    public String host() {
        return HostingAddresses.hostOf(baseAddress);
    }

    /**
     * {@inheritDoc}
     *
     * <p>Everything up to GitLab's {@code /-/} separator is the project's path: the last segment is its name and the
     * rest its namespace. An address of an older form without that separator ({@code /group/project/tree/main}) is
     * read as a deeper namespace, since nothing in the address tells the two apart.
     */
    @Override
    public RepositoryReference parseRepositoryAddress(String address) {
        List<String> segments = HostingAddresses.segmentsUnder(baseAddress, address);
        int separator = segments.indexOf("-");
        if (separator >= 0) {
            segments = segments.subList(0, separator);
        }
        if (segments.size() < 2) {
            throw HostingAddresses.notRecognised();
        }
        List<String> namespace = segments.subList(0, segments.size() - 1);
        String       name      = HostingAddresses.withoutGitSuffix(segments.getLast());
        if (!namespace.stream().allMatch(RepositoryReference::isSegment) || !RepositoryReference.isSegment(name)) {
            throw HostingAddresses.notRecognised();
        }
        return new RepositoryReference(host(), String.join("/", namespace), name);
    }

    @Override
    public GitCredentials.AccessToken accessToken(String token) {
        return GitCredentials.accessToken(TOKEN_USERNAME, token);
    }

    @Override
    public HostingReply<RepositoryDescription> describe(RepositoryReference repository, GitCredentials credentials) {
        requireOwnRepository(repository);
        HostingHttpResponse response = api.require("/projects/" + URLEncoder.encode(repository.path(), StandardCharsets.UTF_8),
                credentials);
        boolean anonymous = credentials instanceof GitCredentials.Anonymous;
        return new HostingReply<>(description(response.body(), anonymous), RateLimit.of(response.headers()));
    }

    @Override
    public HostingReply<TokenInspection> inspectToken(GitCredentials.AccessToken token) {
        HostingHttpResponse tokenResponse = api.get("/personal_access_tokens/self", token);
        if (tokenResponse.status() == 401) {
            return new HostingReply<>(new TokenInspection.Rejected(), RateLimit.of(tokenResponse.headers()));
        }
        Map<?, ?> tokenDetails = toleratingUnavailable(tokenResponse);

        HostingHttpResponse userResponse = api.get("/user", token);
        RateLimit rateLimit = RateLimit.of(userResponse.headers());
        if (userResponse.status() == 401) {
            return new HostingReply<>(new TokenInspection.Rejected(), rateLimit);
        }
        Map<?, ?> user = toleratingUnavailable(userResponse);

        String account = user == null ? null : HostingJson.text(user, "username");
        if (tokenDetails == null) {
            return new HostingReply<>(new TokenInspection.Accepted(account, null, false, null), rateLimit);
        }
        List<String> scopes = HostingJson.optionalTexts(tokenDetails, "scopes");
        Instant expiresAt = expiration(HostingJson.optionalText(tokenDetails, "expires_at"));
        return new HostingReply<>(new TokenInspection.Accepted(account, new LinkedHashSet<>(scopes), true, expiresAt), rateLimit);
    }

    @Override
    public HostingReply<RepositoryPage> repositories(GitCredentials.AccessToken token, PageRequest page) {
        if (page == null) {
            throw new IllegalArgumentException("A listing needs a page request.");
        }
        HostingHttpResponse response = api.require("/projects?membership=true&per_page=" + page.size() + "&page=" + page.page()
                + "&order_by=id&sort=asc", token);
        List<RepositoryDescription> repositories = new ArrayList<>();
        for (Object project : HostingJson.array(response.body())) {
            repositories.add(description(project, false));
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
        return new GitRemote(URI.create(baseAddress + "/" + repository.path() + ".git"), credentials);
    }

    /**
     * A project, from the full representation GitLab gives a signed-in caller. Anonymously GitLab may leave out
     * {@code visibility}; a project readable with no credentials at all is public by definition.
     */
    private RepositoryDescription description(Object value, boolean anonymous) {
        Map<?, ?> project   = HostingJson.object(value);
        Map<?, ?> namespace = HostingJson.object(project.get("namespace"));
        String    fullPath  = HostingJson.text(namespace, "full_path");
        String    name      = HostingJson.text(project, "path");
        String    reported  = HostingJson.optionalText(project, "visibility");
        Visibility visibility;
        if (reported == null && anonymous) {
            visibility = Visibility.PUBLIC;
        } else {
            visibility = Visibility.of(reported).orElseThrow(HostingJson::invalidResponse);
        }
        try {
            return new RepositoryDescription(new RepositoryReference(host(), fullPath, name), visibility,
                    HostingJson.optionalText(project, "default_branch"));
        } catch (IllegalArgumentException malformed) {
            throw HostingJson.invalidResponse();
        }
    }

    /** The body of a success; {@code null} when the endpoint is missing (404) or the token may not use it (403). */
    private static Map<?, ?> toleratingUnavailable(HostingHttpResponse response) {
        Optional<HostingException> refusal = HostingApi.refusal(response);
        if (refusal.isEmpty()) {
            return HostingJson.object(response.body());
        }
        HostingException.Reason reason = refusal.get().reason();
        if (reason == HostingException.Reason.NOT_FOUND || reason == HostingException.Reason.FORBIDDEN) {
            return null;
        }
        throw refusal.get();
    }

    private void requireOwnRepository(RepositoryReference repository) {
        if (repository == null || !repository.host().equals(host())) {
            throw HostingAddresses.notRecognised();
        }
    }

    /** GitLab's expiry date, such as {@code 2026-12-31}: the token stops working at the start of that day, UTC. */
    private static Instant expiration(String date) {
        if (date == null) {
            return null;
        }
        try {
            return LocalDate.parse(date).atStartOfDay(ZoneOffset.UTC).toInstant();
        } catch (DateTimeParseException unreadable) {
            return null;
        }
    }
}
