package svit.io;

import java.util.Collection;

/**
 * Interface for managing and accessing multiple {@link ResourceLoader} instances.
 * Provides methods for registering, retrieving, and removing loaders.
 */
public interface ResourceLoaderRegistry {

    /**
     * Replaces all loaders with the specified loader.
     *
     * @param resourceLoader the new primary loader
     */
    void setResourceLoader(ResourceLoader resourceLoader);

    /**
     * Returns the primary loader.
     *
     * @return the primary {@link ResourceLoader}
     */
    ResourceLoader getResourceLoader();

    /**
     * Retrieves a loader based on the specified protocol.
     *
     * @param protocol the protocol (e.g., "file", "http") to match
     * @return the matching {@link ResourceLoader}, or {@code null} if none found
     */
    ResourceLoader getResourceLoader(String protocol);

    /**
     * Retrieves a loader based on the specified protocol.
     *
     * @param location the protocol will extract from (e.g., "file", "http") to match
     * @return the matching {@link ResourceLoader}, or {@code null} if none found
     */
    ResourceLoader getRequiredResourceLoader(String location);

    /**
     * Returns all registered loaders.
     *
     * @return a collection of {@link ResourceLoader} instances
     */
    Collection<ResourceLoader> getResourceLoaders();

    /**
     * Registers a new loader.
     *
     * @param resourceLoader the loader to add
     */
    void addResourceLoader(ResourceLoader resourceLoader);

    /**
     * Removes a loader associated with the specified protocol.
     *
     * @param protocol the protocol of the loader to remove (e.g., "file", "http")
     */
    void removeResourceLoader(String protocol);

    /**
     * Removes all registered loaders.
     */
    void clearResourceLoaders();

}
