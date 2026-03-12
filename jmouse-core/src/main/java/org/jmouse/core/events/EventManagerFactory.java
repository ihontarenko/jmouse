package org.jmouse.core.events;

import org.jmouse.core.annotation.AnnotationBootstrapper;
import org.jmouse.core.annotation.AnnotationDiscovery;
import org.jmouse.core.annotation.AnnotationProcessingContext;
import org.jmouse.core.mapping.Mappers;
import org.jmouse.core.reflection.TypeMatchers;
import org.jmouse.core.events.annotation.Listener;
import org.jmouse.core.reflection.ClassFinder;

import java.util.List;
import java.util.function.Consumer;

import static org.jmouse.core.reflection.Reflections.findFirstConstructor;
import static org.jmouse.core.reflection.Reflections.instantiate;

/**
 * Factory class for creating and configuring {@link EventManager} instances.
 *
 * <p>This class provides methods to create default or customized {@link EventManager} objects.
 * It can scan for annotated classes, validate their compatibility, and automatically register event listeners.</p>
 *
 * Usage Examples:
 *
 * <pre>{@code
 * // Create a default EventManager
 * EventManager defaultManager = EventManagerFactory.createDefault();
 *
 * // Create an EventManager with listeners annotated by a custom annotation
 * EventManager customManager = EventManagerFactory.create(MyCustomAnnotation.class);
 *
 * // Create and customize an EventManager using a configurator
 * EventManager configuredManager = EventManagerFactory.create(eventManager -> {
 *     eventManager.subscribe("event1", new MyEventListener());
 * });
 * }</pre>
 */
public class EventManagerFactory {

    /**
     * Creates a default {@link EventManager} instance with no pre-configured listeners.
     *
     * @return a new instance of {@link EventManager}.
     */
    public static EventManager createDefault() {
        return new EventManager();
    }

    /**
     * Creates an {@link EventManager} instance and automatically registers event listeners
     * for classes annotated with the specified annotation type.
     *
     * <p>Annotated classes must implement the {@link EventListener} interface. If a class does not
     * implement the required interface, an {@link ObserverException} is thrown.</p>
     *
     * @return a configured {@link EventManager} with registered listeners.
     * @throws ObserverException if an annotated class does not implement {@link EventListener}.
     */
    public static EventManager create(Class<?>... baseClasses) {
        AnnotationBootstrapper bootstrapper = AnnotationBootstrapper.defaults(AnnotationDiscovery.defaults());
        return create(eventManager -> bootstrapper.bootstrap(
                new AnnotationProcessingContext.Default(),
                List.of(new ListenerTypeAnnotationProcessor(eventManager)),
                baseClasses
        ));
    }

    /**
     * Creates an {@link EventManager} instance and applies a custom configurator function
     * to register event listeners or modify its configuration.
     *
     * @param configurator a {@link Consumer} function to configure the {@link EventManager}.
     * @return a configured {@link EventManager} instance.
     */
    public static EventManager create(Consumer<EventManager> configurator) {
        EventManager eventManager = createDefault();

        configurator.accept(eventManager);

        return eventManager;
    }

}
