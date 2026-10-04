package org.jmouse.ai.embeddings;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import org.jmouse.ai.model.AiCapability;
import org.jmouse.ai.provider.ProviderSettings;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/** Exercises extension through the public contracts without editing a built-in enum or execution path. */
class EmbeddingExtensionTest {

    private final CallLimits limits = new CallLimits(2, 100, 200, 2000, 4096);
    private final EmbeddingModel.Request request = new EmbeddingModel.Request(List.of("JDBC"), 2);

    @Test
    void independentlyInstalledProtocolUsesSharedExecutionAndValidation() {
        var calls = new AtomicInteger();
        var protocol = new FixtureProtocol();
        EmbeddingTransport transport = (endpoint, settings, body, callLimits) -> {
            calls.incrementAndGet();
            assertEquals(Map.of("texts", List.of("JDBC")), body);
            return Map.of("coordinates", List.of(List.of(1.0, 2.0)), "used", 7L);
        };
        var model = new HttpEmbeddingModel(protocol, transport);
        var registry = new EmbeddingModels(List.of(model));
        var settings = settings("independent-fixture", "fixture-secret");

        var response = registry.require("CUSTOM_FIXTURE", settings).embed(settings, request, limits);
        assertEquals(7L, response.inputTokens());
        assertEquals(1, calls.get());
        assertThrows(UnsupportedOperationException.class, () -> response.vectors().getFirst().add(3.0));
        assertThrows(EmbeddingException.class, () -> model.embed(settings, new EmbeddingModel.Request(request.inputs(), 3), limits));

        int beforeRefusals = calls.get();
        assertThrows(EmbeddingException.class, () -> model.embed(settings("independent-fixture", null), request, limits));
        assertThrows(EmbeddingException.class, () -> model.embed(settings("wrong-provider", "fixture-secret"), request, limits));
        assertThrows(EmbeddingException.class, () -> model.embed(settings,
                new EmbeddingModel.Request(request.inputs(), 2, EmbeddingModel.InputType.QUERY), limits));
        assertEquals(beforeRefusals, calls.get());
        assertThrows(IllegalArgumentException.class, () -> new EmbeddingModels(List.of(model, model)));
    }

    @Test
    void compatibleWireFormatsNeedOnlyABindingAndMalformedResponsesFailSafely() {
        var protocol = new OpenAiEmbeddingProtocol("COMPATIBLE_FIXTURE", Set.of("compatible-fixture"), Set.of("compatible-fixture"));
        var settings = settings("compatible-fixture", "fixture-secret");
        var replies = List.of(
                Map.of("model", "fixture-model", "data", List.of(Map.of("index", 0, "embedding", List.of(1, 2))),
                        "usage", Map.of("prompt_tokens", 3)),
                Map.of("model", "fixture-model", "data", List.of(Map.of("index", "0", "embedding", List.of(1, 2)))),
                Map.of("model", "fixture-model", "data", List.of(Map.of("index", Long.MAX_VALUE, "embedding", List.of(1, 2)))),
                Map.of("model", "fixture-model", "data", List.of(Map.of("index", 0, "embedding", List.of(Double.NaN, 2)))),
                Map.of("model", "fixture-model", "data", List.of(Map.of("index", 0, "embedding", List.of(1, 2))), "usage", "malformed"));

        for (int position = 0; position < replies.size(); position++) {
            Object reply = replies.get(position);
            var model = new HttpEmbeddingModel(protocol, (endpoint, provider, body, bounds) -> reply);
            if (position == 0) {
                assertEquals(3L, new EmbeddingModels(List.of(model)).require(protocol.code(), settings)
                        .embed(settings, request, limits).inputTokens());
            } else {
                var failure = assertThrows(EmbeddingException.class, () -> model.embed(settings, request, limits));
                assertEquals(EmbeddingException.Reason.INVALID_RESPONSE, failure.reason());
                assertFalse(failure.getMessage().contains("fixture-secret"));
            }
        }
    }

    private static ProviderSettings settings(String provider, String credential) {
        return new ProviderSettings(provider, "fixture-model", credential, "https://fixture.invalid/embed", 100,
                Set.of(AiCapability.EMBEDDINGS), "fixture", 1);
    }

    /** Deliberately a different wire format, not an unverified implementation of a real vendor. */
    private static final class FixtureProtocol extends AbstractEmbeddingProtocol {

        private FixtureProtocol() {
            super("CUSTOM_FIXTURE", Set.of("independent-fixture"), Set.of("independent-fixture"));
        }

        @Override
        public Object encode(ProviderSettings settings, EmbeddingModel.Request request) {
            return Map.of("texts", request.inputs());
        }

        @Override
        public EmbeddingModel.Response decode(Object response, ProviderSettings settings, EmbeddingModel.Request request) {
            Map<?, ?> object = EmbeddingResponseReader.object(response);
            List<List<Double>> coordinates = EmbeddingResponseReader.vectors(object.get("coordinates"), request);
            return new EmbeddingModel.Response(coordinates, EmbeddingResponseReader.nonnegativeInteger(object.get("used")));
        }
    }
}
