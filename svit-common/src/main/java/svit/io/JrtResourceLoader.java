package svit.io;

import java.lang.module.ModuleFinder;
import java.lang.module.ModuleReader;
import java.lang.module.ModuleReference;
import java.lang.module.ResolvedModule;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.util.*;

/**
 * A resource scanner for locating and loading resources from the JRT (Java Runtime) file system.
 * <p>
 * This implementation scans all modules in the boot layer for resources matching the specified location.
 * It uses {@link ModuleReader} to list and retrieve resources, converting them into {@link Resource} objects.
 * </p>
 *
 * @see Resource
 * @see URLResource
 */
public class JrtResourceLoader implements ResourceLoader {

    /**
     * A set of all Java module references available in the system.
     */
    private static final Set<ModuleReference> JAVA_MODULE_NAMES = ModuleFinder.ofSystem().findAll();

    /**
     * Loads resources from the JRT file system that match the specified location.
     *
     * @param location the location or module name to scan for resources; if null, all modules will be scanned
     * @return a collection of {@link Resource} objects representing the matched resources
     * <p>
     * This method scans all modules in the boot layer, iterates through their entries, and converts
     * them into {@link URLResource} objects depending on the resource type.
     * </p>
     * @see ModuleLayer
     * @see ModuleReader
     */
    @Override
    public Collection<Resource> loadResources(String location) {
        Collection<Resource> resources = new ArrayList<>();
        Set<ResolvedModule>  modules   = ModuleLayer.boot().configuration().modules();

        // Iterate over all modules in the boot layer
        for (ResolvedModule module : modules) {

            // If no specific location is provided or matches the module name, scan it
            if (location == null || location.equals(module.name())) {
                resources.addAll(scanJavaModule(module));
            }
        }

        return resources;
    }

    @Override
    public List<String> supportedProtocols() {
        return List.of();
    }

    /**
     * Scans a specific module for resources and converts them to {@link Resource} objects.
     *
     * @param module the module to scan
     * @return a collection of {@link Resource} objects found in the module
     */
    private Collection<Resource> scanJavaModule(ResolvedModule module) {
        Collection<Resource> resources = new ArrayList<>();

        try (ModuleReader reader = module.reference().open()) {
            // List all resources in the module
            List<String> names = reader.list().toList();
            for (String name : names) {
                // Attempt to find the resource URI
                reader.find(name).map(this::toURL).map(URLResource::new).ifPresent(resources::add);
            }
        } catch (Exception ignore) {}

        return resources;
    }

    /**
     * Converts a {@link URI} to a {@link URL}.
     *
     * @param uri the {@link URI} to convert
     * @return the corresponding {@link URL}
     * @throws ResourceException if the conversion fails due to a malformed URL
     * <p>
     * This method wraps {@link URI#toURL()} and provides a custom exception message
     * to indicate the failure. It ensures that any issues with malformed URIs are
     * encapsulated in a {@link ResourceException}.
     * </p>
     *
     * @see URI#toURL()
     * @see ResourceException
     */
    private URL toURL(URI uri) {
        try {
            return uri.toURL();
        } catch (MalformedURLException e) {
            throw new ResourceException("Failed to convert URI to URL: %s".formatted(uri), e);
        }
    }

}
