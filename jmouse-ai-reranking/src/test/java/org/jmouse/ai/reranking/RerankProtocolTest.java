package org.jmouse.ai.reranking;

import org.jmouse.ai.model.AiCapability;
import org.jmouse.ai.provider.ProviderException;
import org.jmouse.ai.provider.ProviderSettings;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

/** Wire mapping for each protocol, the shared result contract, and unknown usage; no network. */
class RerankProtocolTest {

    private static final List<String> DOCUMENTS = List.of("first", "second", "third");
    private final RerankLimits limits = new RerankLimits(10, 100, 100, 1000, 2000, 4096);

    private static ProviderSettings settings(String provider, String apiKey, Set<AiCapability> capabilities) {
        return new ProviderSettings(provider, "rerank-fixture", apiKey, "https://reranker.example/v1/rerank", 100,
                capabilities, "fixture", 1);
    }

    private static ProviderSettings settings(String provider, String apiKey) {
        return settings(provider, apiKey, Set.of(AiCapability.RERANK));
    }

    @Test
    void voyageSendsItsFieldsAndReadsDataAndTotalTokens() {
        var sent = new AtomicReference<Map<?, ?>>();
        var sentHeaders = new AtomicReference<Map<String, String>>();
        RerankTransport transport = (endpoint, body, headers, bounds) -> {
            sent.set((Map<?, ?>) body);
            sentHeaders.set(headers);
            return Map.of("object", "list", "model", "rerank-fixture", "usage", Map.of("total_tokens", 42),
                    "data", List.of(Map.of("index", 2, "relevance_score", 0.9), Map.of("index", 0, "relevance_score", 0.4)));
        };
        var model = new HttpRerankModel(new VoyageRerankProtocol(), transport);
        var response = model.rerank(settings("voyage", "fixture-secret"), new RerankModel.Request("query", DOCUMENTS, 2), limits);

        assertEquals("query", sent.get().get("query"));
        assertEquals(DOCUMENTS, sent.get().get("documents"));
        assertEquals("rerank-fixture", sent.get().get("model"));
        assertEquals(2, sent.get().get("top_k"));
        assertEquals(false, sent.get().get("return_documents"));
        assertEquals(false, sent.get().get("truncation"));
        assertEquals("Bearer fixture-secret", sentHeaders.get().get("Authorization"));
        assertEquals(List.of(new RerankModel.Ranked(2, 0.9), new RerankModel.Ranked(0, 0.4)), response.ranked());
        assertEquals(42L, response.inputTokens());
    }

    @Test
    void voyageWithoutTopResultsSendsNoTopK() {
        var sent = new AtomicReference<Map<?, ?>>();
        RerankTransport transport = (endpoint, body, headers, bounds) -> {
            sent.set((Map<?, ?>) body);
            return Map.of("data", List.of(Map.of("index", 0, "relevance_score", 0.1), Map.of("index", 1, "relevance_score", 0.2),
                    Map.of("index", 2, "relevance_score", 0.3)));
        };
        var response = new HttpRerankModel(new VoyageRerankProtocol(), transport)
                .rerank(settings("voyage", "fixture-secret"), new RerankModel.Request("query", DOCUMENTS), limits);
        assertFalse(sent.get().containsKey("top_k"));
        assertEquals(List.of(2, 1, 0), response.ranked().stream().map(RerankModel.Ranked::index).toList(),
                "results come back best first whatever order the provider used");
        assertNull(response.inputTokens(), "absent usage stays unknown, never zero");
    }

    @Test
    void cohereShapeReadsJinaUsageThenCohereTokensNeverSearchUnits() {
        var sent = new AtomicReference<Map<?, ?>>();
        List<Object> rows = List.of(Map.of("index", 1, "relevance_score", 0.8));
        Object jina = Map.of("model", "rerank-fixture", "usage", Map.of("total_tokens", 7), "results", rows);
        Object cohere = Map.of("id", "x", "results", rows, "meta", Map.of("billed_units", Map.of("search_units", 1),
                "tokens", Map.of("input_tokens", 11, "output_tokens", 0)));
        Object billedOnly = Map.of("results", rows, "meta", Map.of("billed_units", Map.of("search_units", 1)));
        var request = new RerankModel.Request("query", DOCUMENTS, 1);
        var expected = List.of(new RerankModel.Ranked(1, 0.8));

        for (var answer : List.of(Map.entry(jina, 7L), Map.entry(cohere, 11L))) {
            RerankTransport transport = (endpoint, body, headers, bounds) -> {
                sent.set((Map<?, ?>) body);
                return answer.getKey();
            };
            var response = new HttpRerankModel(new CohereRerankProtocol(), transport).rerank(settings("cohere", "key"), request, limits);
            assertEquals(expected, response.ranked());
            assertEquals(answer.getValue(), response.inputTokens());
        }
        assertEquals(Set.of("model", "query", "documents", "top_n"), sent.get().keySet());
        assertEquals(1, sent.get().get("top_n"));

        var billed = new HttpRerankModel(new CohereRerankProtocol(), (endpoint, body, headers, bounds) -> billedOnly)
                .rerank(settings("cohere", "key"), request, limits);
        assertNull(billed.inputTokens(), "search units are not tokens");
    }

    @Test
    void teiSendsTextsReadsBareArrayAndCutsTopResultsItself() {
        var sent = new AtomicReference<Map<?, ?>>();
        var sentHeaders = new AtomicReference<Map<String, String>>();
        RerankTransport transport = (endpoint, body, headers, bounds) -> {
            sent.set((Map<?, ?>) body);
            sentHeaders.set(headers);
            return List.of(Map.of("index", 0, "score", 0.2), Map.of("index", 1, "score", 0.95), Map.of("index", 2, "score", 0.5));
        };
        var response = new HttpRerankModel(new TeiRerankProtocol(), transport)
                .rerank(settings("tei", null), new RerankModel.Request("query", DOCUMENTS, 2), limits);
        assertEquals(DOCUMENTS, sent.get().get("texts"));
        assertEquals(Set.of("query", "texts", "raw_scores", "return_text", "truncate"), sent.get().keySet());
        assertEquals(false, sent.get().get("truncate"));
        assertTrue(sentHeaders.get().isEmpty(), "a local TEI needs no credential");
        assertEquals(List.of(new RerankModel.Ranked(1, 0.95), new RerankModel.Ranked(2, 0.5)), response.ranked());
        assertNull(response.inputTokens());
    }

    @Test
    void malformedResultsAreRefusedWithoutTheirContents() {
        var request = new RerankModel.Request("query", DOCUMENTS, 2);
        List<Object> bad = List.of(
                Map.of("data", List.of(Map.of("index", 0, "relevance_score", 0.1))),
                Map.of("data", List.of(Map.of("index", 0, "relevance_score", 0.1), Map.of("index", 0, "relevance_score", 0.2))),
                Map.of("data", List.of(Map.of("index", 0, "relevance_score", 0.1), Map.of("index", 9, "relevance_score", 0.2))),
                Map.of("data", List.of(Map.of("index", 0, "relevance_score", "high"), Map.of("index", 1, "relevance_score", 0.2))),
                Map.of("data", List.of(Map.of("index", -1, "relevance_score", 0.1), Map.of("index", 1, "relevance_score", 0.2))),
                Map.of("data", "not a list"),
                List.of("not an object"),
                Map.of("data", List.of(Map.of("index", 0, "relevance_score", 0.1), Map.of("index", 1, "relevance_score", 0.2)),
                        "usage", Map.of("total_tokens", -3)));
        for (Object response : bad) {
            var model = new HttpRerankModel(new VoyageRerankProtocol(), (endpoint, body, headers, bounds) -> response);
            var failure = assertThrows(RerankException.class, () -> model.rerank(settings("voyage", "key"), request, limits));
            assertEquals(RerankException.Reason.INVALID_RESPONSE, failure.reason());
            assertFalse(failure.getMessage().contains("high"));
        }
    }

    @Test
    void configurationIsCheckedBeforeAnyCall() {
        RerankTransport unreachable = (endpoint, body, headers, bounds) -> fail("no call may be made");
        var voyage = new HttpRerankModel(new VoyageRerankProtocol(), unreachable);
        var request = new RerankModel.Request("query", DOCUMENTS);

        assertEquals(RerankException.Reason.CONFIGURATION,
                assertThrows(RerankException.class, () -> voyage.rerank(settings("voyage", null), request, limits)).reason(),
                "Voyage needs a key");
        assertEquals(RerankException.Reason.CONFIGURATION,
                assertThrows(RerankException.class, () -> voyage.rerank(settings("tei", null), request, limits)).reason(),
                "a protocol refuses a provider it does not speak");
        assertThrows(ProviderException.class,
                () -> voyage.rerank(settings("voyage", "key", Set.of(AiCapability.EMBEDDINGS)), request, limits),
                "the configuration must declare the RERANK capability");
        var tooMany = new RerankLimits(2, 100, 100, 1000, 2000, 4096);
        assertThrows(IllegalArgumentException.class, () -> voyage.rerank(settings("voyage", "key"), request, tooMany));
        var badEndpoint = new ProviderSettings("voyage", "m", "key", "ftp://reranker.example", 100, Set.of(AiCapability.RERANK), "fixture", 1);
        assertEquals(RerankException.Reason.CONFIGURATION,
                assertThrows(RerankException.class, () -> voyage.rerank(badEndpoint, request, limits)).reason());
    }

    @Test
    void requestAndRegistryRules() {
        assertThrows(IllegalArgumentException.class, () -> new RerankModel.Request("", DOCUMENTS));
        assertThrows(IllegalArgumentException.class, () -> new RerankModel.Request("query", List.of()));
        assertThrows(IllegalArgumentException.class, () -> new RerankModel.Request("query", DOCUMENTS, 4));
        assertThrows(IllegalArgumentException.class, () -> new RerankModel.Request("query", DOCUMENTS, 0));
        assertThrows(IllegalArgumentException.class, () -> new RerankModel.Response(List.of(), -1L));

        RerankTransport unused = (endpoint, body, headers, bounds) -> fail("unused");
        var registry = new RerankModels(List.of(new HttpRerankModel(new VoyageRerankProtocol(), unused),
                new HttpRerankModel(new CohereRerankProtocol(), unused), new HttpRerankModel(new TeiRerankProtocol(), unused)));
        assertEquals(Set.of("VOYAGE", "COHERE", "TEI"), registry.codes());
        assertEquals("COHERE", registry.require("COHERE", settings("jina", "key")).code());
        assertThrows(RerankException.class, () -> registry.require("COHERE", settings("voyage", "key")));
        assertThrows(IllegalArgumentException.class, () -> new RerankModels(List.of(
                new HttpRerankModel(new TeiRerankProtocol(), unused), new HttpRerankModel(new TeiRerankProtocol(), unused))));
    }
}
