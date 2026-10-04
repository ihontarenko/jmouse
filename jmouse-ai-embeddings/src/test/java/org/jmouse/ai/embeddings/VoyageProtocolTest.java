package org.jmouse.ai.embeddings;

import java.util.List;
import java.util.Map;
import java.util.Set;
import org.jmouse.ai.model.AiCapability;
import org.jmouse.ai.provider.ProviderCatalog;
import org.jmouse.ai.provider.ProviderSettings;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/** Voyage has different request options and usage fields from OpenAI; no external quota is used. */
class VoyageProtocolTest {
    private final ProviderSettings settings = new ProviderSettings("voyage", "voyage-4-lite", "fixture-secret",
            "https://api.voyageai.com/v1/embeddings", 100, Set.of(AiCapability.EMBEDDINGS), "fixture", 1);
    private final CallLimits limits = new CallLimits(2, 100, 200, 2000, 4096);

    @Test
    void explicitRetrievalRoleOrderedVectorsAndVoyageUsage() {
        assertEquals(Set.of(AiCapability.EMBEDDINGS), ProviderCatalog.capabilities("voyage"));
        assertTrue(ProviderCatalog.modelFor("voyage", () -> settings).isEmpty());
        for (var role : EmbeddingModel.InputType.values()) {
            EmbeddingTransport transport = (uri, provider, rawBody, bounds) -> {
                var body = (Map<?, ?>) rawBody;
                assertEquals(false, body.get("truncation"));
                assertEquals("float", body.get("output_dtype"));
                assertFalse(body.containsKey("encoding_format"));
                assertFalse(body.containsKey("output_dimension"));
                if (role == EmbeddingModel.InputType.UNSPECIFIED) {
                    assertFalse(body.containsKey("input_type"));
                } else {
                    assertEquals(role.name().toLowerCase(java.util.Locale.ROOT), body.get("input_type"));
                }
                return Map.of("data", List.of(Map.of("index", 1, "embedding", List.of(3, 4)),
                        Map.of("index", 0, "embedding", List.of(1, 2))), "usage", Map.of("total_tokens", 17, "prompt_tokens", 999));
            };
            var response = new HttpEmbeddingModel(new VoyageEmbeddingProtocol(), transport)
                    .embed(settings, new EmbeddingModel.Request(List.of("JDBC", "Україна"), 2, role), limits);
            assertEquals(17L, response.inputTokens());
            assertEquals(List.of(List.of(1.0, 2.0),List.of(3.0, 4.0)), response.vectors());
        }
    }

    @Test
    void conflictingModelAndMalformedUsageAreRefusedAndAbsentUsageStaysUnknown() {
        var request = new EmbeddingModel.Request(List.of("JDBC"), 2);
        var data = List.of(Map.of("index", 0, "embedding", List.of(1, 2)));
        for (var reply : List.of(Map.of("model", "wrong", "data", data),
                Map.of("data", data, "usage", Map.of("total_tokens", -1)),
                Map.of("data", data, "usage", Map.of("total_tokens", 1.5)))) {
            var model = new HttpEmbeddingModel(new VoyageEmbeddingProtocol(), (endpoint, provider, body, bounds) -> reply);
            assertEquals(EmbeddingException.Reason.INVALID_RESPONSE,
                    assertThrows(EmbeddingException.class, () -> model.embed(settings, request, limits)).reason());
        }
        var model = new HttpEmbeddingModel(new VoyageEmbeddingProtocol(), (endpoint, provider, body, bounds) -> Map.of("data", data));
        assertNull(model.embed(settings, request, limits).inputTokens());
        assertFalse(new HttpEmbeddingModel(new OpenAiEmbeddingProtocol(), (endpoint, provider, body, bounds) -> null)
                .supportsInputType(EmbeddingModel.InputType.DOCUMENT));
    }
}
