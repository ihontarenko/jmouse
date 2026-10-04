package org.jmouse.ai.embeddings;

import java.util.Collection;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.jmouse.ai.provider.ProviderSettings;

/** Open adapter registry. Declared model capability alone never substitutes for an installed implementation. */
public final class EmbeddingModels {
    private final Map<String, EmbeddingModel> models;
    public EmbeddingModels(Collection<EmbeddingModel> models) {
        this.models = models.stream().collect(Collectors.toUnmodifiableMap(EmbeddingModel::code, Function.identity()));
    }
    public EmbeddingModel require(String code, ProviderSettings settings) {
        EmbeddingModel model = models.get(code);
        if (model == null || !model.supports(settings.providerName())) {
            throw new EmbeddingException(EmbeddingException.Reason.CONFIGURATION, null);
        }
        return model;
    }
    public java.util.Set<String> codes() {
        return models.keySet();
    }
}

