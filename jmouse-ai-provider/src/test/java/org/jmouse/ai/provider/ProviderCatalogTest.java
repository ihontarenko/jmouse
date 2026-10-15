package org.jmouse.ai.provider;

import java.util.Map;
import java.util.Set;
import org.jmouse.ai.model.AiCapability;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/** Reranking is a capability like any other: the same providers, a different endpoint per capability. */
class ProviderCatalogTest {

    @Test
    void oneProviderAnswersEachCapabilityAtItsOwnAddress() {
        assertEquals(Set.of(AiCapability.EMBEDDINGS, AiCapability.RERANK), ProviderCatalog.capabilities("voyage"));
        assertEquals("https://api.voyageai.com/v1/embeddings",
                ProviderCatalog.defaultAddress("voyage", AiCapability.EMBEDDINGS).orElseThrow());
        assertEquals("https://api.voyageai.com/v1/rerank",
                ProviderCatalog.defaultAddress("voyage", AiCapability.RERANK).orElseThrow());
        assertTrue(ProviderCatalog.defaultAddress("voyage", AiCapability.CHAT).isEmpty());
    }

    @Test
    void rerankOnlyProvidersAreShippedButBuildNoChatModel() {
        assertEquals(Set.of(AiCapability.RERANK), ProviderCatalog.capabilities("cohere"));
        assertEquals(Set.of(AiCapability.RERANK), ProviderCatalog.capabilities("jina"));
        assertTrue(ProviderCatalog.requiresKey("jina"));
        assertTrue(ProviderCatalog.modelFor("cohere", () -> null).isEmpty());
    }

    @Test
    void aLocalServerHasNothingToGuess() {
        assertTrue(ProviderCatalog.capabilities("tei").contains(AiCapability.RERANK));
        assertTrue(ProviderCatalog.defaultAddress("tei", AiCapability.RERANK).isEmpty());
        assertFalse(ProviderCatalog.requiresKey("tei"));
    }

    @Test
    void anAddressForACapabilityTheProviderLacksIsRefused() {
        assertThrows(IllegalArgumentException.class, () -> new ProviderCatalog.Shipped("example", null, true, "note",
                Set.of(AiCapability.EMBEDDINGS), Map.of(AiCapability.RERANK, "https://example.com/rerank")));
    }

    @Test
    void chatProvidersKeepTheirSingleAddress() {
        assertEquals("http://localhost:11434/v1/chat/completions",
                ProviderCatalog.defaultAddress("ollama", AiCapability.CHAT).orElseThrow());
    }
}
