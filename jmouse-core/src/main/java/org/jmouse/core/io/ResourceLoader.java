package org.jmouse.core.io;

import org.jmouse.core.matcher.Matcher;

import java.util.Collection;
import java.util.List;

import static org.jmouse.core.reflection.Reflections.getShortName;

/**
 * Interface for loading resources from various locations and protocols.
 * <p>
 * Provides default implementations for common methods and allows subclasses to override behavior
 * for specific protocols or resource types.
 * </p>
 */
public interface ResourceLoader {

    String UNSUPPORTED_EXCEPTION = "Unsupported for '%s' implementation.";

    /**
     * Loads all resources from the specified location.
     *
     * @param location the resource location
     * @return a collection of {@link Resource} objects
     * @throws UnsupportedOperationException if the operation is not supported
     */
    default Collection<Resource> loadResources(String location) {
        return loadResources(location, Matcher.constant(true));
    }

    /**
     * Loads resources from the specified location that match the provided matcher.
     *
     * @param location the resource location
     * @param matcher  the matcher to filter resources
     * @return a collection of {@link Resource} objects
     * @throws UnsupportedOperationException if the operation is not supported
     */
    default Collection<Resource> loadResources(String location, Matcher<String> matcher) {
        throw new UnsupportedOperationException(UNSUPPORTED_EXCEPTION.formatted(getShortName(this)));
    }

    /**
     * Loads a single resource from the specified location.
     *
     * @param location the resource location
     * @return the loaded {@link Resource}
     * @throws UnsupportedOperationException if the operation is not supported
     */
    default Resource getResource(String location) {
        throw new UnsupportedOperationException(UNSUPPORTED_EXCEPTION.formatted(getShortName(this)));
    }

    /**
     * Returns the {@link ClassLoader} used for loading resources.
     *
     * @return the {@link ClassLoader}
     */
    default ClassLoader getClassLoader() {
        return Thread.currentThread().getContextClassLoader();
    }

    /**
     * Checks if this loader supports the specified protocol.
     *
     * @param protocol the protocol to check (e.g., "file", "classpath")
     * @return {@code true} if the protocol is supported, {@code false} otherwise
     */
    default boolean supports(String protocol) {
        return supportedProtocols().contains(protocol);
    }

    /**
     * Returns the list of protocols supported by this loader.
     *
     * @return a list of supported protocols
     */
    List<String> supportedProtocols();
}
