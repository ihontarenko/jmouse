package svit.scanner;

import java.net.URL;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Set;

public class ResourcesClassScanner extends AbstractScanner<Class<?>> {

    @Override
    public Set<Class<?>> scan(String name, ClassLoader loader) {
        Set<Class<?>> classes = new HashSet<>();

        try {
            String           path      = name.replace('.', '/');
            Enumeration<URL> resources = loader.getResources(path);

            while (resources.hasMoreElements()) {
                URL resource = resources.nextElement();
                for (Scanner<Class<?>> scanner : scanners) {
                    if (scanner.supports(resource.getProtocol())) {
                        classes.addAll(scanner.scan(resource, name, loader));
                    }
                }
            }
        } catch (Throwable e) {
            throw new ClassScannerException(e);
        }

        return classes;
    }

    @Override
    public boolean supports(Object object) {
        return true;
    }

}
