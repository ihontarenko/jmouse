package org.jmouse.ai.embeddings;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.jmouse.ai.model.AiCapability;
import org.jmouse.ai.provider.ProviderSettings;

/** Bounded wire strategies. Dimensions are validated; silent input truncation is disabled where supported. */
public final class ProtocolEmbeddingModel implements EmbeddingModel {
    public enum Protocol { OPENAI, OLLAMA, TEI, VOYAGE }
    private final Protocol protocol;
    private final EmbeddingTransport transport;
    public ProtocolEmbeddingModel(Protocol protocol, EmbeddingTransport transport) {
        this.protocol = java.util.Objects.requireNonNull(protocol);
        this.transport = java.util.Objects.requireNonNull(transport);
    }
    @Override
    public String code() {
        return protocol.name();
    }
    @Override
    public boolean supports(String providerName) {
        return switch (protocol) {
            case OPENAI -> "openai".equals(providerName) || "ollama".equals(providerName);
            case OLLAMA -> "ollama".equals(providerName);
            case TEI -> "tei".equals(providerName);
            case VOYAGE -> "voyage".equals(providerName);
        };
    }
    @Override
    public boolean supportsInputType(InputType inputType) {
        return inputType != null && (protocol == Protocol.VOYAGE || inputType == InputType.UNSPECIFIED);
    }
    @Override
    public Response embed(ProviderSettings settings, Request request, CallLimits limits) {
        settings.requireCapability(AiCapability.EMBEDDINGS);
        if (!supportsInputType(request.inputType()) || !supports(settings.providerName()) || settings.model() == null || settings.model().isBlank()
                || (("openai".equals(settings.providerName()) || "voyage".equals(settings.providerName())) && !settings.hasApiKey())) {
            throw new EmbeddingException(EmbeddingException.Reason.CONFIGURATION, null);
        }
        limits.validate(request.inputs());
        var body = new LinkedHashMap<String, Object>();
        if (protocol == Protocol.TEI) {
            body.put("inputs", request.inputs());
            body.put("truncate", false);
        } else {
            body.put("model", settings.model());
            body.put("input", request.inputs());
            if (protocol == Protocol.OPENAI) {
                body.put("encoding_format", "float");
            } else if (protocol == Protocol.VOYAGE) {
                body.put("truncation", false);
                body.put("output_dtype", "float");
                if (request.inputType() != InputType.UNSPECIFIED) {
                    body.put("input_type", request.inputType().name().toLowerCase(java.util.Locale.ROOT));
                }
            } else {
                body.put("truncate", false);
            }
        }
        Object answer = transport.post(EmbeddingTransport.requireEndpoint(settings.apiUrl()), settings, body, limits);
        try {
            Long usage = null;
            List<?> raw;
            if (protocol == Protocol.TEI) {
                raw = (List<?>) answer;
            } else {
                var object = (Map<?, ?>) answer;
                if (protocol == Protocol.OLLAMA) {
                    if (!settings.model().equals(object.get("model"))) {
                        throw new IllegalArgumentException();
                    }
                    raw = (List<?>) object.get("embeddings");
                    if (object.get("prompt_eval_count") != null) {
                        usage = whole(object.get("prompt_eval_count"));
                    }
                } else {
                    // Voyage responses may omit model; reject a conflicting model when reported.
                    if ((protocol != Protocol.VOYAGE || object.get("model") != null)
                            && !settings.model().equals(object.get("model"))) {
                        throw new IllegalArgumentException();
                    }
                    var ordered = new ArrayList<Object>(java.util.Collections.nCopies(request.inputs().size(), null));
                    for (Object item : (List<?>) object.get("data")) {
                        var row = (Map<?, ?>) item;
                        int index = Math.toIntExact(whole(row.get("index")));
                        if (index < 0 || index >= ordered.size() || ordered.get(index) != null) {
                            throw new IllegalArgumentException();
                        }
                        ordered.set(index, row.get("embedding"));
                    }
                    raw = ordered;
                    String usageField = protocol == Protocol.VOYAGE ? "total_tokens" : "prompt_tokens";
                    if (object.get("usage") instanceof Map<?, ?> counts && counts.get(usageField) != null) {
                        usage = whole(counts.get(usageField));
                    }
                }
            }
            if (raw.size() != request.inputs().size()) {
                throw new IllegalArgumentException();
            }
            var vectors = new ArrayList<List<Double>>();
            for (Object row : raw) {
                var values = (List<?>) row;
                if (values.size() != request.dimensions()) {
                    throw new IllegalArgumentException();
                }
                var vector = new ArrayList<Double>();
                for (Object value : values) {
                    double number = ((Number) value).doubleValue();
                    if (!Double.isFinite(number)) {
                        throw new IllegalArgumentException();
                    }
                    vector.add(number);
                }
                vectors.add(vector);
            }
            return new Response(vectors, usage);
        } catch (RuntimeException failure) {
            throw new EmbeddingException(EmbeddingException.Reason.INVALID_RESPONSE, null);
        }
    }
    static long whole(Object value) {
        long number = new java.math.BigDecimal(value.toString()).longValueExact();
        if (number < 0) {
            throw new IllegalArgumentException();
        }
        return number;
    }
}

