package org.jmouse.core.io;

import org.jmouse.core.matcher.Matcher;
import org.jmouse.util.Files;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import static org.jmouse.core.io.Resource.FILE_PROTOCOL;
import static org.jmouse.core.io.Resource.LOCAL_PROTOCOL;

/**
 * A composite {@link ResourceLoader} that aggregates multiple loaders and delegates resource loading
 * based on the protocol.
 * <p>
 * This implementation allows dynamic registration and removal of resource loaders. If no specific loader
 * supports a protocol, the primary {@link FileSystemResourceLoader} is used as the fallback.
 * </p>
 */
public class CompositeResourceLoader implements PatternMatcherResourceLoader, ResourceLoaderRegistry {

    private final List<ResourceLoader> loaders = new ArrayList<>();
    private final ResourceLoader       primary = new FileSystemResourceLoader();

    public CompositeResourceLoader() {
        addResourceLoader(new FileSystemResourceLoader());
        addResourceLoader(new JrtResourceLoader());
        addResourceLoader(new JarURLResourceLoader());
        addResourceLoader(new NetworkURLResourceLoader());
        addResourceLoader(new ClasspathResourceLoader());
    }

    /**
     * Replaces all existing loaders with the specified loader.
     *
     * @param resourceLoader the new primary loader
     */
    @Override
    public void setResourceLoader(ResourceLoader resourceLoader) {
        this.loaders.clear();
        this.loaders.add(resourceLoader);
    }

    /**
     * Returns the primary loader or the fallback loader if no other loaders are registered.
     *
     * @return the primary {@link ResourceLoader}
     */
    @Override
    public ResourceLoader getResourceLoader() {
        return this.loaders.isEmpty() ? primary : this.loaders.getFirst();
    }

    /**
     * Returns a loader that supports the specified protocol.
     *
     * @param protocol the protocol to match (e.g., "file", "http")
     * @return a {@link ResourceLoader} that supports the protocol, or the fallback loader if none matches
     */
    @Override
    public ResourceLoader getResourceLoader(String protocol) {
        return loaders.stream()
                .filter(loader -> loader.supports(protocol))
                .findFirst().orElse(primary);
    }

    /**
     * Retrieves a loader based on the specified protocol.
     *
     * @param location the protocol will extract from (e.g., "file", "http") to match
     * @return the matching {@link ResourceLoader}, or {@code null} if none found
     */
    @Override
    public ResourceLoader getRequiredResourceLoader(String location) {
        String protocol = Files.extractProtocol(location, FILE_PROTOCOL);

        if (!supports(protocol)) {
            throw new ResourceLoaderException(
                    "Unsupported protocol: '%s'. Consider to use: %s.".formatted(protocol, supportedProtocols()));
        }

        return getResourceLoader(protocol);
    }

    /**
     * Removes loaders that support the specified protocol.
     *
     * @param protocol the protocol to match
     */
    @Override
    public void removeResourceLoader(String protocol) {
        loaders.removeIf(loader -> loader.supports(protocol));
    }

    /**
     * Clears all registered loaders.
     */
    @Override
    public void clearResourceLoaders() {
        this.loaders.clear();
    }

    /**
     * Returns all registered loaders.
     */
    @Override
    public Collection<ResourceLoader> getResourceLoaders() {
        return this.loaders;
    }

    /**
     * Registers a new loader.
     */
    @Override
    public void addResourceLoader(ResourceLoader resourceLoader) {
        this.loaders.add(resourceLoader);
    }

    /**
     * Loads a resource from the specified location.
     */
    @Override
    public Resource getResource(String location) {
        return getRequiredResourceLoader(location).getResource(location);
    }

    /**
     * Loads resources from the specified location that match the provided matcher.
     *
     * @param location the resource location
     * @param matcher  the matcher to filter resources
     * @return a collection of {@link Resource} objects
     * @throws UnsupportedOperationException if the operation is not supported
     */
    @Override
    public Collection<Resource> loadResources(String location, Matcher<String> matcher) {
        return getRequiredResourceLoader(location).loadResources(location, matcher);
    }

    /**
     * Returns a list of all supported protocols across registered loaders.
     */
    @Override
    public List<String> supportedProtocols() {
        return loaders.stream().flatMap(loader -> loader.supportedProtocols().stream()).toList();
    }

}
