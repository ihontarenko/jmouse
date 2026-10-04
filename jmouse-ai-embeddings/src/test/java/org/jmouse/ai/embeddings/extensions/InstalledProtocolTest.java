package org.jmouse.ai.embeddings.extensions;

import org.jmouse.ai.embeddings.AbstractEmbeddingProtocol;
import org.jmouse.ai.embeddings.CallLimits;
import org.jmouse.ai.embeddings.EmbeddingModel;
import org.jmouse.ai.embeddings.EmbeddingModels;
import org.jmouse.ai.embeddings.HttpEmbeddingModel;
import org.jmouse.ai.model.AiCapability;
import org.jmouse.ai.provider.ProviderSettings;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;

/** A different package exercises only the exported extension surface. */
class InstalledProtocolTest {

    @Test
    void externalAdapterReusesIndexedDecodingWithoutAccessingInternalClasses() {
        var protocol = new ExternalProtocol();
        var model = new HttpEmbeddingModel(protocol, (endpoint, settings, body, bounds) -> Map.of(
                "data", List.of(Map.of("index", 0, "embedding", List.of(1, 2))),
                "usage", Map.of("billed_tokens", 4)));
        var registry = new EmbeddingModels(List.of(model));
        var settings = new ProviderSettings("external-fixture", "fixture-model", "fixture-secret",
                "https://fixture.invalid/embed", 100, Set.of(AiCapability.EMBEDDINGS), "fixture", 1);
        var response = registry.require(protocol.code(), settings).embed(settings,
                new EmbeddingModel.Request(List.of("JDBC"), 2), new CallLimits(1, 100, 100, 2000, 4096));

        assertEquals(4L, response.inputTokens());
        assertEquals(List.of(List.of(1.0, 2.0)), response.vectors());
    }

    private static final class ExternalProtocol extends AbstractEmbeddingProtocol {

        private ExternalProtocol() {
            super("EXTERNAL_FIXTURE", Set.of("external-fixture"), Set.of("external-fixture"));
        }

        @Override
        public Object encode(ProviderSettings settings, EmbeddingModel.Request request) {
            return Map.of("texts", request.inputs(), "model", settings.model());
        }

        @Override
        public EmbeddingModel.Response decode(Object response, ProviderSettings settings, EmbeddingModel.Request request) {
            return indexedResponse(response, settings, request, false, "billed_tokens");
        }
    }
}
