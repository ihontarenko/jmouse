package org.jmouse.parallax.store.part;

import org.jmouse.parallax.part.PartCandidate;
import org.jmouse.parallax.part.PartSearchProvider;
import org.jmouse.parallax.part.ProviderState;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

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

/**
 * 🔎 Nexar's supply search — Octopart's part data — as a place to find real parts by number or words.
 *
 * <h2>⚠️ What Nexar's terms allow, and so what this does</h2>
 *
 * <p>Nexar's API licence (nexar.com/api/legal) lets an application show what it returns, with a followable link to the
 * part's Octopart page; it forbids caching or storing the data for more than twenty-four hours and self-hosting its
 * datasheets and images. So this provider searches live and keeps nothing: no result is cached, no candidate is
 * {@linkplain PartCandidate#fetchable() fetchable}, the datasheet is a link to where Nexar points, and every candidate
 * carries its {@code octopartUrl} for the interface to show. Only the access token is kept, until it expires.</p>
 *
 * <p>Every part a supply query returns counts against the Nexar application's monthly allowance, which is why the
 * number asked for is {@code jmouse.parallax.parts.nexar.limit}, small by default.</p>
 */
public final class NexarPartSearch implements PartSearchProvider {

    private static final String SEARCH = """
            query PartSearch($q: String!, $limit: Int!) {
              supSearch(q: $q, limit: $limit) {
                results { part { id mpn shortDescription manufacturer { name } octopartUrl bestDatasheet { url } } }
              }
            }""";
    /** A token is asked for again this long before Nexar says it expires. */
    private static final long EXPIRY_MARGIN_SECONDS = 60;

    private final PartSearchProperties.Nexar properties;
    private final ObjectMapper mapper;
    private final HttpClient client;
    private final Clock clock;
    private String token;
    private Instant tokenExpires = Instant.MIN;
    private volatile String failure;

    public NexarPartSearch(PartSearchProperties.Nexar properties, ObjectMapper mapper, Clock clock) {
        this.properties = properties;
        this.mapper = mapper;
        this.clock = clock;
        this.client = HttpClient.newBuilder().connectTimeout(properties.timeout()).build();
    }

    @Override
    public String code() {
        return "nexar";
    }

    @Override
    public String name() {
        return "Nexar · Octopart";
    }

    @Override
    public ProviderState state() {
        if (!properties.configured()) {
            return ProviderState.notConfigured("Set jmouse.parallax.parts.nexar.client-id and client-secret — a Nexar "
                    + "application's — to search suppliers' parts. Results are shown, never kept: Nexar's terms.");
        }
        if (failure != null) {
            return ProviderState.failed(failure);
        }
        return ProviderState.ready("Live supplier data from Octopart; shown, not kept.");
    }

    @Override
    public List<PartCandidate> search(String query, int limit) {
        if (!properties.configured() || query == null || query.isBlank()) {
            return List.of();
        }
        try {
            Map<String, Object> variables = new LinkedHashMap<>();
            variables.put("q", query.strip());
            variables.put("limit", Math.min(limit, properties.limit()));
            JsonNode answer = post(properties.apiUrl(), "application/json",
                    mapper.writeValueAsString(Map.of("query", SEARCH, "variables", variables)), "Bearer " + token());
            if (answer.path("errors").isArray() && !answer.path("errors").isEmpty()) {
                throw new IllegalStateException(answer.path("errors").get(0).path("message").asString());
            }
            List<PartCandidate> candidates = new ArrayList<>();
            for (JsonNode result : answer.path("data").path("supSearch").path("results")) {
                candidates.add(candidateOf(result.path("part")));
            }
            failure = null;
            return candidates;
        } catch (IOException | IllegalStateException | JacksonException refused) {
            failure = "Nexar did not answer: " + refused.getMessage();
            throw new IllegalStateException(failure, refused);
        } catch (InterruptedException interrupted) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("The search was interrupted.", interrupted);
        }
    }

    private PartCandidate candidateOf(JsonNode part) {
        String datasheet = textOrNull(part.path("bestDatasheet").path("url"));
        String partNumber = textOrNull(part.path("mpn"));
        return new PartCandidate(code(), textOrNull(part.path("id")), PartCandidate.Kind.PART, partNumber, partNumber,
                textOrNull(part.path("manufacturer").path("name")), textOrNull(part.path("shortDescription")), datasheet,
                textOrNull(part.path("octopartUrl")), null, null, false);
    }

    private synchronized String token() throws IOException, InterruptedException {
        Instant now = clock.instant();
        if (token != null && now.isBefore(tokenExpires)) {
            return token;
        }
        String form = "grant_type=client_credentials&scope=supply.domain"
                + "&client_id=" + URLEncoder.encode(properties.clientId(), StandardCharsets.UTF_8)
                + "&client_secret=" + URLEncoder.encode(properties.clientSecret(), StandardCharsets.UTF_8);
        JsonNode answer = post(properties.tokenUrl(), "application/x-www-form-urlencoded", form, null);
        String issued = textOrNull(answer.path("access_token"));
        if (issued == null) {
            throw new IllegalStateException("Nexar issued no token: check the client id and secret.");
        }
        token = issued;
        tokenExpires = now.plusSeconds(Math.max(0, answer.path("expires_in").asLong(0) - EXPIRY_MARGIN_SECONDS));
        return token;
    }

    private JsonNode post(String url, String contentType, String body, String authorization)
            throws IOException, InterruptedException {
        HttpRequest.Builder request = HttpRequest.newBuilder(URI.create(url)).timeout(properties.timeout())
                .header("Content-Type", contentType).header("Accept", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(body, StandardCharsets.UTF_8));
        if (authorization != null) {
            request.header("Authorization", authorization);
        }
        HttpResponse<String> response = client.send(request.build(), HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() >= 400) {
            throw new IllegalStateException("HTTP " + response.statusCode() + " from " + URI.create(url).getHost());
        }
        return mapper.readTree(response.body());
    }

    private static String textOrNull(JsonNode node) {
        return node == null || node.isMissingNode() || node.isNull() || node.asString().isBlank() ? null : node.asString();
    }
}
