package org.jmouse.beans.events;

import org.jmouse.beans.BeanContext;
import org.jmouse.core.events.DeduplicatingPublishPolicy;
import org.jmouse.core.events.EventName;
import org.jmouse.core.trace.TraceContext;

/**
 * Dedup key strategy for BeanContext events.
 *
 * Design goals:
 * - suppress obvious duplicates within the same correlation flow
 * - preserve distinct events for different beans / processors / initializer methods
 * - do not require modifications in DefaultBeanContext
 */
public final class BeanEventDeduplicateKeyStrategy implements DeduplicatingPublishPolicy.DeduplicateKeyStrategy {

    @Override
    public String keyOf(EventName name, TraceContext trace, Object source) {
        if (!(name instanceof BeanEventName beanEvent)) {
            return null;
        }

        String correlationId = trace.correlationId();
        String contextId     = extractContextId(source);

        // If correlation id is missing, don't deduplicate.
        if (correlationId == null || correlationId.isBlank()) {
            return null;
        }

        // Event-specific keying.
        // Note: you can tune these rules later without touching BeanContext.
        return switch (beanEvent) {

            // ===== Lookup =====
            case BEAN_LOOKUP_STARTED -> keyLookupStarted(correlationId, contextId, source);
            case BEAN_LOOKUP_RESOLVED -> keyLookupResolved(correlationId, contextId, source);
            case BEAN_LOOKUP_NOT_FOUND -> keyLookupNotFound(correlationId, contextId, source);
            case BEAN_LOOKUP_PRIMARY_SELECTED -> keyLookupPrimary(correlationId, contextId, source);
            case BEAN_LOOKUP_AMBIGUOUS -> keyLookupAmbiguous(correlationId, contextId, source);

            // ===== Creation =====
            case BEAN_CREATION_STARTED -> keyCreateStarted(correlationId, contextId, source);
            case BEAN_CREATION_COMPLETED -> keyCreateCompleted(correlationId, contextId, source);

            // ===== Initialization =====
            case BEAN_INITIALIZATION_BEFORE_PROCESSING -> keyInitBefore(correlationId, contextId, source);
            case BEAN_INITIALIZATION_AFTER_PROCESSING -> keyInitAfter(correlationId, contextId, source);
            case BEAN_INITIALIZER_INVOKED -> keyInitializerInvoked(correlationId, contextId, source);
            case BEAN_INITIALIZATION_COMPLETED -> keyInitCompleted(correlationId, contextId, source);
            case BEAN_INITIALIZATION_FAILED -> keyInitFailed(correlationId, contextId, source);

            // ===== Context lifecycle =====
            case CONTEXT_REFRESH_STARTED,
                 CONTEXT_REFRESH_COMPLETED,
                 CONTEXT_REFRESH_FAILED,
                 CONTEXT_INTERNAL_ERROR,
                 BEAN_DEFINITION_REGISTERED -> keySimpleOncePerContext(correlationId, contextId, beanEvent);

            default -> null;
        };
    }

    private String extractContextId(Object source) {
        if (source instanceof BeanContext beanContext) {
            return beanContext.getContextId();
        }
        return "unknown";
    }

    // ---------------- Lookup keys ----------------

    private String keyLookupStarted(String correlationId, String contextId, Object source) {
        if (source instanceof BeanContext beanContext) {
            // We want to suppress repeated LOOKUP_STARTED for the SAME semantic lookup target.
            // Payload is not passed here (source only), so we rely on trace/span uniqueness.
            // However we can do a more robust approach: dedup "started" only once per span depth:
            // key by (corr, spanId, eventName) so re-emit in same span is suppressed, but nested spans still emit.
            //
            // You asked: suppress inner duplicates when getBean(Class) -> getBean(String).
            // That requires a stronger rule: once per correlation for LOOKUP_STARTED of the "lookup flow".
            //
            // Since we don't have payload, we choose a correlation-level suppression:
            return correlationId + "|" + contextId + "|LOOKUP|STARTED";
        }
        return correlationId + "|" + contextId + "|LOOKUP|STARTED";
    }

    private String keyLookupResolved(String correlationId, String contextId, Object source) {
        return correlationId + "|" + contextId + "|LOOKUP|RESOLVED";
    }

    private String keyLookupNotFound(String correlationId, String contextId, Object source) {
        return correlationId + "|" + contextId + "|LOOKUP|NOT_FOUND";
    }

    private String keyLookupPrimary(String correlationId, String contextId, Object source) {
        return correlationId + "|" + contextId + "|LOOKUP|PRIMARY_SELECTED";
    }

    private String keyLookupAmbiguous(String correlationId, String contextId, Object source) {
        return correlationId + "|" + contextId + "|LOOKUP|AMBIGUOUS";
    }

    // ---------------- Creation keys ----------------

    private String keyCreateStarted(String correlationId, String contextId, Object source) {
        // Creation is bean-specific; we don't have payload here either,
        // so we cannot key by beanName unless we change policy signature.
        // We therefore do NOT dedup creation by default to avoid hiding important details.
        return null;
    }

    private String keyCreateCompleted(String correlationId, String contextId, Object source) {
        return null;
    }

    // ---------------- Initialization keys ----------------

    private String keyInitBefore(String correlationId, String contextId, Object source) {
        return null;
    }

    private String keyInitAfter(String correlationId, String contextId, Object source) {
        return null;
    }

    private String keyInitializerInvoked(String correlationId, String contextId, Object source) {
        return null;
    }

    private String keyInitCompleted(String correlationId, String contextId, Object source) {
        return null;
    }

    private String keyInitFailed(String correlationId, String contextId, Object source) {
        return null;
    }

    private String keySimpleOncePerContext(String correlationId, String contextId, BeanEventName event) {
        return correlationId + "|" + contextId + "|CTX|" + event.name();
    }
}
