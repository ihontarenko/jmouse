package svit.scanner;

import java.io.File;
import java.net.URL;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.Set;

import static svit.matcher.PathMatchers.*;

public class LocalClassScanner extends AbstractScanner<Class<?>> {

    private static final FileScanner FILE_SCANNER = new FileScanner();

    static {
        FILE_SCANNER.setMatcher(exists().and(isFile()).and(hasExtension("class")));
    }

    @Override
    public Set<Class<?>> scan(URL resource, String name, ClassLoader loader) {
        Set<Class<?>> classes = new HashSet<>();

        for (Path path : FILE_SCANNER.scan(resource, name, loader)) {
            try {
                String packagePath = path.toFile().getParent().replaceAll("[\\\\/]+", ".");
                int    lastIndex   = packagePath.lastIndexOf(name);
                String packageName = packagePath.substring(lastIndex);

                classes.add(loader.loadClass(getClassName(packageName, path.toFile())));
            } catch (Throwable ignore) { }
        }

        return classes;
    }

    @Override
    public boolean supports(Object object) {
        return object.equals("file");
    }

    private String getClassName(String name, File file) {
        return name + '.' + file.getName().substring(0, file.getName().length() - CLASS_FILE_SUFFIX.length());
    }

}
