package svit.scanner.filter.type;

import java.lang.reflect.Constructor;
import java.util.Arrays;
import java.util.List;

public class ConstructorClassFilter extends AbstractTypeFilter {

    private final Class<?>[] types;

    public ConstructorClassFilter(List<Class<?>> types, boolean invert) {
        super(invert);
        this.types = types.toArray(Class<?>[]::new);
    }

    public ConstructorClassFilter(Class<?>... types) {
        this(Arrays.asList(types), false);
    }

    @Override
    public boolean accept(Class<?> clazz) {
        return matches(clazz) != invert();
    }

    private boolean matches(Class<?> clazz) {
        Constructor<?>[] constructors = clazz.getConstructors();

        for (Constructor<?> constructor : constructors) {
            if (Arrays.equals(constructor.getParameterTypes(), this.types)) {
                return true;
            }
        }

        return false;
    }

}
