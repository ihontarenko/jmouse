package org.jmouse.core.events.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 🎧 Declares a class as an event listener.
 * <p>
 * {@code @Listener} marks a type as eligible for automatic registration
 * in an {@link org.jmouse.core.events.EventManager}.
 * <p>
 * When used together with framework initializers (e.g. bean-context bridges),
 * listener instances are subscribed to the specified event names.
 *
 * <h3>Usage</h3>
 * <pre>{@code
 * @Listener(events = {"bean.created", "bean.lookup.start"})
 * public class MyListener implements EventListener<MyPayload> {
 *     ...
 * }
 * }</pre>
 *
 * <h3>Notes</h3>
 * <ul>
 *   <li>If no event names are specified, the listener is ignored.</li>
 *   <li>Event names must match those published by the {@code EventManager}.</li>
 * </ul>
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface Listener {

    /**
     * Names of events this listener should be subscribed to.
     *
     * @return array of event names
     */
    String[] events() default {};
}
