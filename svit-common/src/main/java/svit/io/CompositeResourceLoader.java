package svit.io;

import svit.util.Files;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import static svit.io.Resource.LOCAL_PROTOCOL;

/**
 * A composite {@link ResourceLoader} that aggregates multiple loaders and delegates resource loading
 * based on the protocol.
 * <p>
 * This implementation allows dynamic registration and removal of resource loaders. If no specific loader
 * supports a protocol, the primary {@link FileSystemResourceLoader} is used as the fallback.
 * </p>
 */
public class CompositeResourceLoader implements ResourceLoader, ResourceLoaderRegistry {

    private final List<ResourceLoader> loaders = new ArrayList<>();
    private final ResourceLoader       primary = new FileSystemResourceLoader();

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
        String protocol = Files.extractProtocol(location, LOCAL_PROTOCOL);

        location = location.substring(protocol.length() + 1);

        return getResourceLoader(protocol).getResource(location);
    }

    /**
     * Returns a list of all supported protocols across registered loaders.
     */
    @Override
    public List<String> supportedProtocols() {
        return loaders.stream().flatMap(loader -> loader.supportedProtocols().stream()).toList();
    }

}
