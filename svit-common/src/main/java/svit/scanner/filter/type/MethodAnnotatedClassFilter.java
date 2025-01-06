package svit.scanner.filter.type;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;

public class MethodAnnotatedClassFilter extends AbstractTypeFilter {

    private final Class<? extends Annotation> annotation;
    private final int                         modifiers;

    public MethodAnnotatedClassFilter(Class<? extends Annotation> annotation, int modifiers, boolean invert) {
        super(invert);
        this.annotation = annotation;
        this.modifiers = modifiers;
    }

    public MethodAnnotatedClassFilter(Class<? extends Annotation> annotation, int modifiers) {
        this(annotation, modifiers, false);
    }

    public MethodAnnotatedClassFilter(Class<? extends Annotation> annotation) {
        this(annotation, Modifier.PUBLIC);
    }

    @Override
    public boolean accept(Class<?> clazz) {
        boolean result = false;

        for (Method method : clazz.getMethods()) {
            if (method.isAnnotationPresent(annotation) && (method.getModifiers() & modifiers) != 0) {
                result = true;
                break;
            }
        }

        return result != invert();
    }

}