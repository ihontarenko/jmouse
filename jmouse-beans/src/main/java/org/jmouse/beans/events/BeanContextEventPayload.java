package org.jmouse.beans.events;

import org.jmouse.beans.BeanContext;
import org.jmouse.beans.definition.BeanDefinition;

import static org.jmouse.beans.events.BeanContextEventPayload.*;

public sealed interface BeanContextEventPayload permits
        ContextPayload,
        DefinitionPayload,
        LookupPayload,
        CreatePayload,
        InitPayload,
        ErrorPayload {

    record ContextPayload(BeanContext context) implements BeanContextEventPayload { }

    record DefinitionPayload(
            BeanContext context,
            BeanDefinition definition
    ) implements BeanContextEventPayload { }

    record LookupPayload(
            BeanContext context,
            String beanName,
            Class<?> requiredType
    ) implements BeanContextEventPayload { }

    record CreatePayload(
            BeanContext context,
            BeanDefinition definition,
            Object instance
    ) implements BeanContextEventPayload { }

    record InitPayload(
            BeanContext context,
            BeanDefinition definition,
            Object instance,
            String step
    ) implements BeanContextEventPayload { }

    record ErrorPayload(
            BeanContext context,
            BeanContextEventName stage,
            String beanName,
            BeanDefinition definition,
            Throwable error
    ) implements BeanContextEventPayload { }

}