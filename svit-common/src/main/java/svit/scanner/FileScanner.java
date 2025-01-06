package svit.scanner;

import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Set;
import java.util.stream.Stream;

import static java.util.Objects.requireNonNull;
import static java.util.stream.Collectors.toSet;

public class FileScanner extends AbstractScanner<Path> {

    @Override
    public Set<Path> scan(String name, ClassLoader loader) {
        return scan(requireNonNull(loader.getResource(name)), name, loader);
    }

    @Override
    public Set<Path> scan(URL resource, String name, ClassLoader loader) {
        try (Stream<Path> stream = Files.walk(Paths.get(resource.toURI()))) {
            return stream.filter(this::filter).collect(toSet());
        } catch (IOException | URISyntaxException e) {
            throw new RuntimeException(e);
        }
    }

}
