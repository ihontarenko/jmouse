package org.jmouse.mvc;

/**
 * ⚙️ Simple implementation of {@link MappedHandler} that holds a single handler instance.
 *
 * <p>Useful for adapters that do not require extra metadata.</p>
 *
 * <pre>{@code
 * SimpleMappedHandler mapped = new SimpleMappedHandler(myController);
 * Object actualHandler = mapped.handler();
 * }</pre>
 *
 * @author Ivan Hontarenko (Mr. Jerry Mouse)
 * @author ihontarenko@gmail.com
 */
public record SimpleMappedHandler(Object handler) implements MappedHandler { }
