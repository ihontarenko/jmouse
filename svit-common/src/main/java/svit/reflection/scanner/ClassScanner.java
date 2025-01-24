package svit.reflection.scanner;

import svit.matcher.Matcher;

import java.util.Set;

public interface ClassScanner {

    ClassLoader DEFAULT_CLASS_LOADER = ClassScanner.class.getClassLoader();

    Set<Class<?>> scan(Matcher<Class<?>> matcher, ClassLoader loader, Class<?>... baseClasses);

    default Set<Class<?>> scan(Matcher<Class<?>> matcher, Class<?>... baseClasses) {
        return scan(matcher, DEFAULT_CLASS_LOADER, baseClasses);
    }

    default Set<Class<?>> scan(ClassLoader loader, Class<?>... baseClasses) {
        return scan(Matcher.constant(true), loader, baseClasses);
    }

    default Set<Class<?>> scan(Class<?>... baseClasses) {
        return scan(DEFAULT_CLASS_LOADER, baseClasses);
    }

}
