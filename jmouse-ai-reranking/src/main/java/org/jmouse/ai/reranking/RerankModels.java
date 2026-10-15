package org.jmouse.ai.reranking;

import org.jmouse.ai.provider.ProviderSettings;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/** Immutable open registry; installed implementations and declared provider capabilities remain separate facts. */
public final class RerankModels {

    private final Map<String, RerankModel> models;

    public RerankModels(Collection<RerankModel> models) {
        Objects.requireNonNull(models, "Installed rerank models are required.");
        var indexed = new LinkedHashMap<String, RerankModel>();
        for (RerankModel model : models) {
            Objects.requireNonNull(model, "An installed model cannot be null.");
            String code = model.code();
            if (code == null || code.isBlank() || indexed.putIfAbsent(code, model) != null) {
                throw new IllegalArgumentException("Installed rerank model codes must be nonblank and unique.");
            }
        }
        this.models = Map.copyOf(indexed);
    }

    public RerankModel require(String code, ProviderSettings settings) {
        Objects.requireNonNull(settings, "Provider settings are required.");
        RerankModel model = code == null ? null : models.get(code);
        if (model == null || !model.supports(settings.providerName())) {
            throw new RerankException(RerankException.Reason.CONFIGURATION, null);
        }
        return model;
    }

    public Set<String> codes() {
        return models.keySet();
    }
}
