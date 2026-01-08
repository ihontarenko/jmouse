package org.jmouse.beans.events;

import org.jmouse.beans.definition.BeanDefinition;

final class BeanEventPayloadExtractor {

    String beanName(Object payload) {
        if (payload instanceof BeanContextEventPayload.LookupPayload lookupPayload) {
            return lookupPayload.beanName();
        }
        if (payload instanceof BeanContextEventPayload.CreatePayload createPayload) {
            return beanName(createPayload.definition());
        }
        if (payload instanceof BeanContextEventPayload.InitPayload initPayload) {
            return beanName(initPayload.definition());
        }
        if (payload instanceof BeanContextEventPayload.DefinitionPayload definitionPayload) {
            return beanName(definitionPayload.definition());
        }
        if (payload instanceof BeanContextEventPayload.ErrorPayload errorPayload) {
            if (hasText(errorPayload.beanName())) {
                return errorPayload.beanName();
            }
            return beanName(errorPayload.definition());
        }
        return null;
    }

    String requiredTypeName(Object payload) {
        if (payload instanceof BeanContextEventPayload.LookupPayload lookupPayload) {
            return typeName(lookupPayload.requiredType());
        }
        if (payload instanceof BeanContextEventPayload.AmbiguousLookupPayload ambiguousLookupPayload) {
            return typeName(ambiguousLookupPayload.requiredType());
        }
        return null;
    }

    String stepName(Object payload) {
        if (payload instanceof BeanContextEventPayload.InitPayload initPayload) {
            return initPayload.step();
        }
        return null;
    }

    String errorType(Object payload) {
        if (payload instanceof BeanContextEventPayload.ErrorPayload errorPayload) {
            Throwable error = errorPayload.error();
            return error == null ? null : error.getClass().getName();
        }
        return null;
    }

    private String beanName(BeanDefinition definition) {
        return definition == null ? null : definition.getBeanName();
    }

    private String typeName(Class<?> type) {
        return type == null ? null : type.getName();
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}
