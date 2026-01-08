package org.jmouse.beans.events;

import org.jmouse.beans.BeanContext;
import org.jmouse.core.events.DeduplicatingPublishPolicy;
import org.jmouse.core.events.EventName;
import org.jmouse.core.trace.TraceContext;

/**
 * Dedup key strategy for BeanContext events.
 * <p>
 * Goals:
 * - suppress duplicates within the same correlation flow
 * - preserve distinct events for different beans / processors / initializer methods
 * - no changes in DefaultBeanContext
 */
public final class BeanEventDeduplicateKeyStrategy implements DeduplicatingPublishPolicy.DeduplicateKeyStrategy {

    private final BeanEventPayloadExtractor extractor = new BeanEventPayloadExtractor();

    @Override
    public String keyOf(EventName name, TraceContext trace, Object source, Object payload) {
        if (!(name instanceof BeanEventName beanEventName)) {
            return null;
        }
        if (trace == null || isBlank(trace.correlationId())) {
            return null;
        }

        String correlationId = trace.correlationId();
        String contextId     = contextId(source);

        return switch (beanEventName) {
            case BEAN_LOOKUP_STARTED -> lookupKey(correlationId, contextId, "STARTED", payload);
            case BEAN_LOOKUP_RESOLVED -> lookupKey(correlationId, contextId, "RESOLVED", payload);
            case BEAN_LOOKUP_NOT_FOUND -> lookupKey(correlationId, contextId, "NOT_FOUND", payload);
            case BEAN_LOOKUP_PRIMARY_SELECTED -> lookupKey(correlationId, contextId, "PRIMARY_SELECTED", payload);
            case BEAN_LOOKUP_AMBIGUOUS ->
                    DeduplicateKey.of(correlationId, contextId, "LOOKUP", "AMBIGUOUS", extractor.requiredTypeName(payload));
            case BEAN_CREATION_STARTED ->
                    beanKey(correlationId, contextId, "CREATE", "STARTED", extractor.beanName(payload));
            case BEAN_CREATION_COMPLETED ->
                    beanKey(correlationId, contextId, "CREATE", "COMPLETED", extractor.beanName(payload));
            case BEAN_INITIALIZATION_BEFORE_PROCESSING ->
                    initProcessorKey(correlationId, contextId, "BEFORE_PROCESSING", payload);
            case BEAN_INITIALIZATION_AFTER_PROCESSING ->
                    initProcessorKey(correlationId, contextId, "AFTER_PROCESSING", payload);
            case BEAN_INITIALIZER_INVOKED -> initInitializerKey(correlationId, contextId, payload);
            case BEAN_INITIALIZATION_COMPLETED ->
                    beanKey(correlationId, contextId, "INITIALIZATION", "COMPLETED", extractor.beanName(payload));
            case BEAN_INITIALIZATION_FAILED -> failedKey(correlationId, contextId, payload);
            case CONTEXT_REFRESH_STARTED,
                 CONTEXT_REFRESH_COMPLETED,
                 CONTEXT_REFRESH_FAILED,
                 CONTEXT_INTERNAL_ERROR,
                 BEAN_DEFINITION_REGISTERED ->
                    DeduplicateKey.of(correlationId, contextId, "CONTEXT", beanEventName.name());

            default -> null;
        };
    }

    private String lookupKey(String correlationId, String contextId, String phase, Object payload) {
        String beanName         = extractor.beanName(payload);
        String requiredTypeName = extractor.requiredTypeName(payload);
        return DeduplicateKey.of(correlationId, contextId, "LOOKUP", phase, beanName, requiredTypeName);
    }

    private String initProcessorKey(String correlationId, String contextId, String phase, Object payload) {
        String beanName  = extractor.beanName(payload);
        String stepName = extractor.stepName(payload);

        if (isBlank(beanName) || isBlank(stepName)) {
            return null;
        }

        return DeduplicateKey.of(correlationId, contextId, "INITIALIZATION", phase, beanName, stepName);
    }

    private String initInitializerKey(String correlationId, String contextId, Object payload) {
        String beanName    = extractor.beanName(payload);
        String stepName = extractor.stepName(payload);

        if (isBlank(beanName) || isBlank(stepName)) {
            return null;
        }

        return DeduplicateKey.of(correlationId, contextId, "INITIALIZATION", "INITIALIZER_INVOKED", beanName, stepName);
    }

    private String failedKey(String correlationId, String contextId, Object payload) {
        String beanName = extractor.beanName(payload);

        if (isBlank(beanName)) {
            return null;
        }

        String errorType = extractor.errorType(payload);
        return DeduplicateKey.of(correlationId, contextId, "INITIALIZATION", "FAILED", beanName, errorType);
    }

    private String beanKey(String correlationId, String contextId, String category, String phase, String beanName) {
        if (isBlank(beanName)) {
            return null;
        }
        return DeduplicateKey.of(correlationId, contextId, category, phase, beanName);
    }

    private String contextId(Object source) {
        if (source instanceof BeanContext beanContext && !isBlank(beanContext.getContextId())) {
            return beanContext.getContextId();
        }
        return "unknown";
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
