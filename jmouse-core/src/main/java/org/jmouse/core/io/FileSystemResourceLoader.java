package org.jmouse.core.io;

import org.jmouse.core.matcher.Matcher;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.stream.Stream;

import static org.jmouse.util.helper.Files.*;

/**
 * A {@link ResourceLoader} implementation for loading resources from the file system.
 * <p>
 * This loader supports scanning directories and loading files based on specified matchers and protocols.
 * It provides efficient resource handling using Java NIO.
 * </p>
 */
public class FileSystemResourceLoader extends AbstractResourceLoader {

    /**
     * Loads resources from the file system that match the specified location and matcher.
     *
     * @param location the resource location
     * @param matcher  the matcher to filter resource names
     * @return a collection of {@link Resource} objects that match the criteria
     * @throws ResourceLoaderException if an I/O error occurs during resource loading
     */
    @Override
    public Collection<Resource> loadResources(String location, Matcher<String> matcher) {
        ensureSupportedProtocol(location);
        return loadResources(location, Path.of(removeProtocol(location)), matcher);
    }

    /**
     * Loads resources from the specified path that match the given matcher.
     *
     * @param location the resource location
     * @param path     the file system path to scan
     * @param matcher  the matcher to filter resource names
     * @return a collection of {@link Resource} objects
     * @throws ResourceLoaderException if an I/O error occurs while accessing the file system
     */
    public Collection<Resource> loadResources(String location, Path path, Matcher<String> matcher) {
        Collection<Resource> resources = new ArrayList<>();
        Matcher<Path>        filter    = p -> matcher.matches(normalizePath(p.toString(), SLASH));

        try (Stream<Path> stream = Files.walk(path)) {
            stream.filter(Files::isRegularFile)     // Only process regular files
                    .filter(filter::matches)        // Apply the matcher
                    .map(this::createResource)      // Convert Path to Resource object
                    .forEach(resources::add);       // Add matching resources to the collection
        } catch (IOException exception) {
            throw new ResourceLoaderException("Failed to load resources from '%s'".formatted(location), exception);
        }

        return resources;
    }

    /**
     * Loads a single resource from the specified location.
     *
     * @param location the resource location
     * @return the loaded {@link Resource}
     * @throws ResourceLoaderException if the resource cannot be resolved
     */
    @Override
    public Resource getResource(String location) {
        Resource resource;

        ensureSupportedProtocol(location);

        try {
            resource = createResource(Paths.get(removeProtocol(location)));
        } catch (InvalidPathException exception) {
            throw new ResourceLoaderException("Failed to load filesystem resource", exception);
        }

        return resource;
    }

    /**
     * Returns a list of protocols supported by this loader.
     *
     * @return a list containing "local" as the supported protocol
     */
    @Override
    public List<String> supportedProtocols() {
        return List.of(Resource.LOCAL_PROTOCOL);
    }

    /**
     * Creates a {@link Resource} from a given {@link Path}.
     *
     * @param path the file system path
     * @return a {@link FileSystemResource} representing the file
     */
    public Resource createResource(Path path) {
        return new FileSystemResource(path);
    }

}
