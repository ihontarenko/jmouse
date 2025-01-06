package svit.scanner;

import java.util.HashSet;
import java.util.Set;

import static java.util.stream.Collectors.toSet;

public class ClassScanner extends AbstractScanner<Class<?>> {

    private static final ClassScanner CLASS_SCANNER;

    static {
        CLASS_SCANNER = new ClassScanner();
        CLASS_SCANNER.addScanner(new ResourcesClassScanner() {{
            addScanner(new JarClassScanner());
            addScanner(new LocalClassScanner());
        }});
    }

    public static ClassScanner getDefaultScanner() {
        return CLASS_SCANNER;
    }

    @Override
    public Set<Class<?>> scan(String name, ClassLoader loader) {
        Set<Class<?>> classes = new HashSet<>();

        for (Scanner<Class<?>> scanner : scanners) {
            classes.addAll(scanner.scan(name, loader));
        }

        return classes.stream().filter(this::filter).collect(toSet());
    }

}
