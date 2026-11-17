package org.jmouse.j2js.teavm.classlib.java.lang.reflect;

import org.jmouse.j2js.teavm.supplement.Supplement;
import org.jmouse.j2js.teavm.supplement.SupplementKind;
import org.jmouse.j2js.teavm.supplement.Supplements;
import org.jmouse.j2js.teavm.supplement.jdk.ClassSupplements;

import java.lang.annotation.Annotation;
import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

/**
 * {@code java.lang.reflect.RecordComponent} in the browser: a component of a record — its field, by name and type, and the
 * accessor of the same name, as the record declares them; its annotations are its field's. It also gives {@code Class} a
 * record's components.
 *
 * @author Ivan Hontarenko (Mr. Jerry Mouse)
 * @author ihontarenko@gmail.com
 */
@Supplements(Class.class)
public final class TRecordComponent implements AnnotatedElement {

    private final Class<?> record;
    private final String   name;
    private final Class<?> type;

    public TRecordComponent(Class<?> record, String name, Class<?> type) {
        this.record = record;
        this.name = name;
        this.type = type;
    }

    /** A record's components — its instance fields, in the order declared —; null for a class that is no record. */
    @Supplement(SupplementKind.INSTANCE)
    public static TRecordComponent[] getRecordComponents(Class<?> type) {
        if (!ClassSupplements.isRecord(type)) {
            return null;
        }
        List<TRecordComponent> components = new ArrayList<>();
        for (Field field : type.getDeclaredFields()) {
            if (!Modifier.isStatic(field.getModifiers())) {
                components.add(new TRecordComponent(type, field.getName(), field.getType()));
            }
        }
        return components.toArray(new TRecordComponent[0]);
    }

    public Class<?> getDeclaringRecord() {
        return record;
    }

    public String getName() {
        return name;
    }

    public Class<?> getType() {
        return type;
    }

    /** Its generic type — {@code List<Point>}, not {@code List} —, as its field keeps it where the field is kept for reflection. */
    public Type getGenericType() {
        return field().getGenericType();
    }

    /** No class file is read in a page, so the signature as written there is not had; its generic type is. */
    public String getGenericSignature() {
        return null;
    }

    /** The accessor of the component's name; it takes no parameter. */
    public Method getAccessor() {
        try {
            return record.getMethod(name);
        } catch (NoSuchMethodException exception) {
            throw new IllegalStateException("The record " + record.getName() + " has no accessor " + name + "()", exception);
        }
    }

    /** Its field: where a component's annotations are kept in a class TeaVM compiled. */
    private Field field() {
        try {
            return record.getDeclaredField(name);
        } catch (NoSuchFieldException exception) {
            throw new IllegalStateException("The record " + record.getName() + " has no field " + name, exception);
        }
    }

    @Override
    public <T extends Annotation> T getAnnotation(Class<T> annotationType) {
        return field().getAnnotation(annotationType);
    }

    @Override
    public Annotation[] getAnnotations() {
        return field().getAnnotations();
    }

    @Override
    public Annotation[] getDeclaredAnnotations() {
        return field().getDeclaredAnnotations();
    }

    @Override
    public String toString() {
        return type.getTypeName() + " " + name;
    }
}
