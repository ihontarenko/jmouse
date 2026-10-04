package org.jmouse.ai.embeddings;

import org.jmouse.ai.provider.ProviderSettings;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/** Immutable open registry; installed implementations and declared provider capabilities remain separate facts. */
public final class EmbeddingModels {

    private final Map<String, EmbeddingModel> models;

    public EmbeddingModels(Collection<EmbeddingModel> models) {
        Objects.requireNonNull(models, "Installed embedding models are required.");
        var indexed = new LinkedHashMap<String, EmbeddingModel>();
        for (EmbeddingModel model : models) {
            Objects.requireNonNull(model, "An installed model cannot be null.");
            String code = model.code();
            if (code == null || code.isBlank() || indexed.putIfAbsent(code, model) != null) {
                throw new IllegalArgumentException("Installed embedding model codes must be nonblank and unique.");
            }
        }
        this.models = Map.copyOf(indexed);
    }

    public EmbeddingModel require(String code, ProviderSettings settings) {
        Objects.requireNonNull(settings, "Provider settings are required.");
        EmbeddingModel model = code == null ? null : models.get(code);
        if (model == null || !model.supports(settings.providerName())) {
            throw new EmbeddingException(EmbeddingException.Reason.CONFIGURATION, null);
        }
        return model;
    }

    public Set<String> codes() {
        return models.keySet();
    }
}
