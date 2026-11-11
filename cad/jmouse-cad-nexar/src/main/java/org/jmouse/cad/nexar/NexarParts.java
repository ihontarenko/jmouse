package org.jmouse.cad.nexar;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.jmouse.cad.document.part.PartCandidate;
import org.jmouse.cad.document.part.PartSearchProvider;
import org.jmouse.cad.document.part.ProviderState;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 🔎 Nexar's supply search — Octopart's part data — as a place to find real parts by number or words.
 *
 * <h2>⚠️ What Nexar's terms allow, and so what this does</h2>
 *
 * <p>Nexar's API licence (nexar.com/api/legal) lets an application show what it returns, with a followable link to the
 * part's Octopart page; it forbids caching or storing the data for more than twenty-four hours and self-hosting its
 * datasheets and images. So this provider searches live and keeps nothing: no result is cached, no candidate is
 * {@linkplain PartCandidate#fetchable() fetchable}, the datasheet is a link to where Nexar points, and every candidate
 * carries its Octopart page for the interface to show. Only the access token is kept, until it expires.</p>
 */
public final class NexarParts implements PartSearchProvider {

    /** The supply search, asking only what a candidate shows. */
    private static final String SEARCH = """
            query PartSearch($q: String!, $limit: Int!) {
              supSearch(q: $q, limit: $limit) {
                results { part { id mpn shortDescription manufacturer { name } octopartUrl bestDatasheet { url } } }
              }
            }""";

    /** The token grant Nexar issues an application's client, for its supply data. */
    private static final Map<String, String> GRANT = Map.of("grant_type", "client_credentials", "scope", "supply.domain");

    /** A token is asked for again this long before Nexar says it expires. */
    private static final long EXPIRY_MARGIN_SECONDS = 60;

    private static final String JSON = "application/json";
    private static final String FORM = "application/x-www-form-urlencoded";

    private final NexarSettings settings;
    private final ObjectMapper  mapper = new ObjectMapper();
    private final HttpClient    client;
    private final Clock         clock;
    private String          token;
    private Instant         tokenExpires = Instant.MIN;
    private volatile String failure;

    public NexarParts(NexarSettings settings, Clock clock) {
        this.settings = settings;
        this.clock = clock;
        this.client = HttpClient.newBuilder().connectTimeout(settings.timeout()).build();
    }

    @Override
    public String id() {
        return "nexar";
    }

    @Override
    public String name() {
        return "Nexar · Octopart";
    }

    @Override
    public ProviderState state() {
        if (!settings.configured()) {
            return ProviderState.notConfigured("A Nexar application's client id and secret are not set, so suppliers' parts "
                    + "are not searched. Results are shown, never kept: Nexar's terms.");
        }
        if (failure != null) {
            return ProviderState.failed(failure);
        }
        return ProviderState.ready("Live supplier data from Octopart; shown, not kept.");
    }

    @Override
    public List<PartCandidate> search(String query, int limit) {
        if (!settings.configured() || query == null || query.isBlank()) {
            return List.of();
        }
        try {
            Map<String, Object> variables = new LinkedHashMap<>();
            variables.put("q", query.strip());
            variables.put("limit", Math.min(limit, settings.limit()));
            JsonNode answer = post(settings.apiUrl(), JSON, mapper.writeValueAsString(Map.of("query", SEARCH,
                    "variables", variables)), "Bearer " + token());
            if (answer.path("errors").isArray() && !answer.path("errors").isEmpty()) {
                throw new IllegalStateException(answer.path("errors").get(0).path("message").asText());
            }
            List<PartCandidate> candidates = new ArrayList<>();
            for (JsonNode result : answer.path("data").path("supSearch").path("results")) {
                candidates.add(candidateOf(result.path("part")));
            }
            failure = null;
            return candidates;
        } catch (IOException | IllegalStateException refused) {
            failure = "Nexar did not answer: " + refused.getMessage();
            throw new IllegalStateException(failure, refused);
        } catch (InterruptedException interrupted) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("The search was interrupted.", interrupted);
        }
    }

    private PartCandidate candidateOf(JsonNode part) {
        String partNumber = textOrNull(part.path("mpn"));
        return new PartCandidate(id(), textOrNull(part.path("id")), PartCandidate.Kind.PART, partNumber, partNumber,
                textOrNull(part.path("manufacturer").path("name")), textOrNull(part.path("shortDescription")),
                textOrNull(part.path("bestDatasheet").path("url")), octopartLink(textOrNull(part.path("octopartUrl"))), null, null, false);
    }

    /** Octopart's page for the part: Nexar's terms ask that what it answers be shown with it. */
    private static PartCandidate.Link octopartLink(String address) {
        return address == null ? null : new PartCandidate.Link("Octopart", address);
    }

    /** The application's token, asked for again shortly before it expires. */
    private synchronized String token() throws IOException, InterruptedException {
        Instant now = clock.instant();
        if (token != null && now.isBefore(tokenExpires)) {
            return token;
        }
        Map<String, String> fields = new LinkedHashMap<>(GRANT);
        fields.put("client_id", settings.clientId());
        fields.put("client_secret", settings.clientSecret());
        JsonNode answer = post(settings.tokenUrl(), FORM, form(fields), null);
        String issued = textOrNull(answer.path("access_token"));
        if (issued == null) {
            throw new IllegalStateException("Nexar issued no token: check the client id and secret.");
        }
        token = issued;
        tokenExpires = now.plusSeconds(Math.max(0, answer.path("expires_in").asLong(0) - EXPIRY_MARGIN_SECONDS));
        return token;
    }

    /** Fields as an HTML form sends them: each name and value encoded, in pairs. */
    private static String form(Map<String, String> fields) {
        return fields.entrySet().stream()
                .map(field -> URLEncoder.encode(field.getKey(), StandardCharsets.UTF_8) + "="
                        + URLEncoder.encode(field.getValue(), StandardCharsets.UTF_8))
                .collect(Collectors.joining("&"));
    }

    private JsonNode post(String url, String contentType, String body, String authorization)
            throws IOException, InterruptedException {
        HttpRequest.Builder request = HttpRequest.newBuilder(URI.create(url)).timeout(settings.timeout())
                .header("Content-Type", contentType).header("Accept", JSON)
                .POST(HttpRequest.BodyPublishers.ofString(body, StandardCharsets.UTF_8));
        if (authorization != null) {
            request.header("Authorization", authorization);
        }
        HttpResponse<String> response = client.send(request.build(), HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() >= 400) {
            throw new IllegalStateException("HTTP " + response.statusCode() + " from " + URI.create(url).getHost());
        }
        try {
            return mapper.readTree(response.body());
        } catch (JsonProcessingException unreadable) {
            throw new IllegalStateException("Nexar's answer is not JSON: " + unreadable.getOriginalMessage(), unreadable);
        }
    }

    private static String textOrNull(JsonNode node) {
        return node == null || node.isMissingNode() || node.isNull() || node.asText().isBlank() ? null : node.asText();
    }
}
