package svit.scanner;

import java.net.URL;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Stream;

public class JarClassScanner extends AbstractScanner<Class<?>> {

    @Override
    public boolean supports(Object protocol) {
        return protocol.equals("jar");
    }

    @Override
    public Set<Class<?>> scan(URL resource, String name, ClassLoader loader) {
        Set<Class<?>> classes = new HashSet<>();

        try {
            if (supports(resource.getProtocol())) {
                FileSystem   fileSystem = FileSystems.newFileSystem(resource.toURI(), Collections.emptyMap());
                Path         path       = fileSystem.getPath(name.replace('.', '/'));
                Stream<Path> walk       = Files.walk(path, Integer.MAX_VALUE);

                for (String className : walk.map(Path::toString).map(this::getClassFile).toList()) {
                    if (className.endsWith(CLASS_FILE_SUFFIX)) {
                        try {
                            classes.add(loader.loadClass(getClassName(className)));
                        } catch (NoClassDefFoundError ignore) {}
                    }
                }
            }

        } catch (Throwable ignore) {}

        return classes;
    }

    private String getClassFile(String path) {
        return path.replace('/', '.');
    }

    private String getClassName(String className) {
        return className.substring(0, className.length() - CLASS_FILE_SUFFIX.length());
    }

}
