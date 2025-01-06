package svit.scanner.filter.type;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;

public class FieldAnnotatedClassFilter extends AbstractTypeFilter {

    private final Class<? extends Annotation> annotation;
    private final int                         modifiers;

    public FieldAnnotatedClassFilter(Class<? extends Annotation> annotation, int modifiers, boolean invert) {
        super(invert);
        this.annotation = annotation;
        this.modifiers = modifiers;
    }

    public FieldAnnotatedClassFilter(Class<? extends Annotation> annotation, int modifiers) {
        this(annotation, modifiers, false);
    }

    public FieldAnnotatedClassFilter(Class<? extends Annotation> annotation) {
        this(annotation, Modifier.PUBLIC, false);
    }

    @Override
    public boolean accept(Class<?> clazz) {
        boolean result = false;

        for (Field field : clazz.getFields()) {
            if (field.isAnnotationPresent(annotation) && (field.getModifiers() & modifiers) != 0) {
                result = true;
                break;
            }
        }

        return result;
    }

}