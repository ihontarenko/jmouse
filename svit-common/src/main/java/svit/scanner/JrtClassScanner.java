package svit.scanner;

import java.lang.module.ModuleReader;
import java.lang.module.ModuleReference;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class JrtClassScanner extends AbstractScanner<Class<?>> {

    private final String module;

    public JrtClassScanner(String module) {
        this.module = module;
    }

    @Override
    public Set<Class<?>> scan(String packageName, ClassLoader loader) {
        Set<Class<?>>   classes   = new HashSet<>();
        ModuleReference reference = ModuleLayer.boot().configuration().findModule(module)
                .orElseThrow().reference();

        try {
            ModuleReader reader = reference.open();
            List<String> names  = reader.list().toList();

            for (String name : names) {
                if (name.endsWith(CLASS_FILE_SUFFIX)) {
                    try {
                        String className = getClassName(name);
                        if (className.startsWith(packageName)) {
                            classes.add(loader.loadClass(className));
                        }
                    } catch (Error ignore) {
                        // skip undefined classes
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
        return object != null;
    }

    private String getClassName(String name) {
        return name.substring(0, name.length() - CLASS_FILE_SUFFIX.length())
                .replace("/", ".");
    }

}
