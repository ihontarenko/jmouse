package org.jmouse.core.events;

import org.jmouse.core.annotation.AnnotationProcessingContext;
import org.jmouse.core.annotation.support.AbstractTypeAnnotationProcessor;
import org.jmouse.core.events.annotation.Listener;
import org.jmouse.core.reflection.TypeMatchers;

import static org.jmouse.core.reflection.Reflections.findFirstConstructor;
import static org.jmouse.core.reflection.Reflections.instantiate;

public class ListenerTypeAnnotationProcessor extends AbstractTypeAnnotationProcessor<Listener> {

    private final EventManager eventManager;

    protected ListenerTypeAnnotationProcessor(EventManager eventManager) {
        super(Listener.class);
        this.eventManager = eventManager;
    }

    @Override
    public void process(Class<?> annotatedClass, Listener annotation, AnnotationProcessingContext context) {
        if (TypeMatchers.isSupertype(EventListener.class).matches(annotatedClass)) {
            EventListener<?> eventListener = (EventListener<?>) instantiate(findFirstConstructor(annotatedClass));
            for (String event : annotation.events()) {
                eventManager.subscribe(EventName.of(event, EventCategory.UNCATEGORIZED), eventListener);
            }
        } else {
            throw new ObserverException(
                    "Annotated event-listener '%s' is required to implement the interface '%s'"
                            .formatted(annotatedClass, EventListener.class));
        }
    }

}
