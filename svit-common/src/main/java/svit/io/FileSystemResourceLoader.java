package svit.io;

import svit.matcher.Matcher;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.stream.Stream;

import static svit.util.Files.removeProtocol;

public class FileSystemResourceLoader extends AbstractResourceLoader {

    @Override
    public Collection<Resource> loadResources(String location, Matcher<Resource> matcher) {
        Collection<Resource> resources = new ArrayList<>();

        ensureSupportedProtocol(location);

        location = removeProtocol(location);

        try (Stream<Path> stream = Files.walk(Path.of(location))) {
            stream.filter(Files::isRegularFile)     // Only process regular files
                    .map(this::createResource)      // Convert Path to FileSystemResource
                    .filter(matcher::matches)       // Apply the matcher
                    .forEach(resources::add);       // Add matching resources to the collection
        } catch (IOException exception) {
            throw new ResourceLoaderException("Failed to load resources from '%s'".formatted(location), exception);
        }

        return resources;
    }

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

    @Override
    public List<String> supportedProtocols() {
        return List.of(Resource.FILE_SYSTEM);
    }

    public Resource createResource(Path path) {
        return new FileSystemResource(path);
    }

}
