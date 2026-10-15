package org.jmouse.git.hosting;

import org.jmouse.git.GitCredentials;

import java.net.URI;
import java.net.http.HttpHeaders;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Pattern;

/**
 * The request mechanics GitHub and GitLab share: the base address of their API, the headers every request carries,
 * bearer authentication, and how a status becomes a typed refusal. Composed into each hosting rather than inherited,
 * because the two share mechanics, not vocabulary.
 */
final class HostingApi {

    private static final String  USER_AGENT = "jmouse-git-hosting";
    private static final Pattern LINK_NEXT  = Pattern.compile("<[^>]*>\\s*;\\s*rel=\"?next\"?", Pattern.CASE_INSENSITIVE);

    private final String              apiAddress;
    private final Map<String, String> commonHeaders;
    private final HostingTransport    transport;
    private final HostingLimits       limits;

    HostingApi(URI apiAddress, Map<String, String> commonHeaders, HostingTransport transport, HostingLimits limits) {
        if (transport == null || limits == null) {
            throw new IllegalArgumentException("A hosting needs a transport and limits.");
        }
        this.apiAddress = HostingAddresses.requireBase(apiAddress).toString();
        this.commonHeaders = Map.copyOf(commonHeaders);
        this.transport = transport;
        this.limits = limits;
    }

    /** One GET under the API address; every status comes back, so the caller decides which ones it can answer. */
    HostingHttpResponse get(String pathAndQuery, GitCredentials credentials) {
        Map<String, String> headers = new LinkedHashMap<>(commonHeaders);
        headers.put("User-Agent", USER_AGENT);
        if (credentials instanceof GitCredentials.AccessToken token) {
            headers.put("Authorization", "Bearer " + token.token());
        }
        URI endpoint;
        try {
            endpoint = URI.create(apiAddress + pathAndQuery);
        } catch (IllegalArgumentException failure) {
            throw new HostingException(HostingException.Reason.CONFIGURATION);
        }
        HostingHttpResponse response = transport.get(endpoint, headers, limits);
        if (response == null) {
            throw new HostingException(HostingException.Reason.TRANSPORT);
        }
        return response;
    }

    /** One GET whose answer must be a success. */
    HostingHttpResponse require(String pathAndQuery, GitCredentials credentials) {
        HostingHttpResponse response = get(pathAndQuery, credentials);
        Optional<HostingException> refusal = refusal(response);
        if (refusal.isPresent()) {
            throw refusal.get();
        }
        return response;
    }

    /**
     * The refusal a response stands for, if it is not a success. A 403 counts as throttling only when the hosting
     * says so through its rate-limit headers; otherwise it is a permission refusal.
     */
    static Optional<HostingException> refusal(HostingHttpResponse response) {
        if (response.successful()) {
            if (response.body() == null) {
                return Optional.of(HostingJson.invalidResponse());
            }
            return Optional.empty();
        }
        RateLimit rateLimit = RateLimit.of(response.headers());
        HostingException.Reason reason = switch (response.status()) {
            case 401 -> HostingException.Reason.UNAUTHORISED;
            case 403 -> rateLimit.asksToWait() ? HostingException.Reason.RATE_LIMITED : HostingException.Reason.FORBIDDEN;
            case 404 -> HostingException.Reason.NOT_FOUND;
            case 429 -> HostingException.Reason.RATE_LIMITED;
            default -> HostingException.Reason.PROVIDER_REFUSED;
        };
        return Optional.of(new HostingException(reason, response.status(), rateLimit));
    }

    /** Whether the hosting reported a page after this one: GitHub through {@code Link}, GitLab through {@code x-next-page} too. */
    static boolean hasNextPage(HttpHeaders headers) {
        Optional<String> nextPage = headers.firstValue("x-next-page");
        if (nextPage.isPresent() && !nextPage.get().isBlank()) {
            return true;
        }
        return headers.allValues("link").stream()
                .flatMap(link -> Arrays.stream(link.split(",")))
                .anyMatch(entry -> LINK_NEXT.matcher(entry.strip()).matches());
    }
}
