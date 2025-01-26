package svit.io;

import svit.matcher.Matcher;
import svit.util.Files;
import svit.util.JavaIO;
import svit.util.Strings;

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
public class JrtResourceLoader extends AbstractResourceLoader {

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
    public Collection<Resource> loadResources(String location, Matcher<String> matcher) {
        Collection<Resource> resources = new ArrayList<>();
        Set<ResolvedModule>  modules   = ModuleLayer.boot().configuration().modules();

        ensureSupportedProtocol(location);

        String javaModule = Strings.prefix(Files.removeProtocol(location), Files.SLASH, false);

        // Iterate over all modules in the boot layer
        for (ResolvedModule module : modules) {

            // If no specific location is provided or matches the module name, scan it
            if (location == null || javaModule.contains(module.name())) {
                resources.addAll(scanJavaModule(module));
            }
        }

        return resources;
    }

    @Override
    public List<String> supportedProtocols() {
        return List.of(Resource.JRT_PROTOCOL);
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
                reader.find(name).map(JavaIO::toURL).map(JrtURLResource::new).ifPresent(resources::add);
            }
        } catch (Exception ignore) {}

        return resources;
    }

}
