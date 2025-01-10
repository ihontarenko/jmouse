package svit.invocable;

import svit.util.Strings;

import java.lang.reflect.Field;

import static svit.util.Strings.underscored;

public class ReflectionFieldDescriptor implements FieldDescriptor {

    private final Class<?> nativeClass;
    private final Field field;

    public ReflectionFieldDescriptor(Field field, Class<?> nativeClass) {
        this.field = field;
        this.nativeClass = nativeClass;
    }

    @Override
    public ClassTypeDescriptor getObjectDescriptor() {
        return new ReflectionClassTypeDescriptor(nativeClass);
    }

    @Override
    public Field getNativeField() {
        return field;
    }

    @Override
    public ClassTypeDescriptor getFieldTypeDescriptor() {
        return new ReflectionClassTypeDescriptor(getFieldType());
    }

    @Override
    public Class<?> getFieldType() {
        return field.getType();
    }

    @Override
    public String getName() {
        return field.getName();
    }

    @Override
    public String toString() {
        return "%s: [%s]".formatted(Strings.underscored(getClass().getSimpleName()).toUpperCase(), getNativeField());
    }

}
