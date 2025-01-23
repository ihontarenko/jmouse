package svit.io;

import svit.util.Files;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import static svit.io.Resource.FILE_SYSTEM;

public class StandardResourceLoader implements ResourceLoader, ResourceLoaderRegistry {

    private final List<ResourceLoader> loaders = new ArrayList<>();
    private final ResourceLoader       primary = new FileSystemResourceLoader();

    @Override
    public void setResourceLoader(ResourceLoader resourceLoader) {
        this.loaders.clear();
        this.loaders.add(resourceLoader);
    }

    @Override
    public ResourceLoader getResourceLoader() {
        return this.loaders.isEmpty() ? primary : this.loaders.getFirst();
    }

    @Override
    public ResourceLoader getResourceLoader(String protocol) {
        ResourceLoader loader = primary;

        for (ResourceLoader resourceLoader : loaders) {
            if (resourceLoader.supports(protocol)) {
                loader = resourceLoader;
                break;
            }
        }

        return loader;
    }

    @Override
    public void removeResourceLoader(String protocol) {
        loaders.removeIf(loader -> loader.supports(protocol));
    }

    @Override
    public void clearResourceLoaders() {
        this.loaders.clear();
    }

    @Override
    public Collection<ResourceLoader> getResourceLoaders() {
        return this.loaders;
    }

    @Override
    public void addResourceLoader(ResourceLoader resourceLoader) {
        this.loaders.add(resourceLoader);
    }

    @Override
    public Resource getResource(String location) {
        String protocol = Files.extractProtocol(location, FILE_SYSTEM);

        location = location.substring(protocol.length() + 1);

        return getResourceLoader(protocol).getResource(location);
    }

    @Override
    public List<String> supportedProtocols() {
        return loaders.stream().flatMap(loader -> loader.supportedProtocols().stream()).toList();
    }

}
