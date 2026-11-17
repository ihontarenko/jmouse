package org.jmouse.j2js.teavm.supplement.jdk;

import org.jmouse.j2js.teavm.supplement.Supplement;
import org.jmouse.j2js.teavm.supplement.SupplementKind;
import org.jmouse.j2js.teavm.supplement.Supplements;

import java.lang.reflect.Type;

/**
 * 🧩 What TeaVM's {@code java.lang.Class} lacks. TeaVM keeps no generic signature, so a class's generic supertypes are its raw
 * ones — the answer the JDK gives for a class declared without type arguments. A record's components are
 * {@code RecordComponent}'s to give.
 *
 * @author Ivan Hontarenko (Mr. Jerry Mouse)
 * @author ihontarenko@gmail.com
 */
@Supplements(Class.class)
public final class ClassSupplements {

    private ClassSupplements() {
    }

    /** The package of a class — of an array's element class; {@code java.lang} for a primitive — as the JDK names it. */
    @Supplement(SupplementKind.INSTANCE)
    public static String getPackageName(Class<?> type) {
        Class<?> named = type;
        while (named.isArray()) {
            named = named.getComponentType();
        }
        if (named.isPrimitive()) {
            return "java.lang";
        }
        String name = named.getName();
        int    dot  = name.lastIndexOf('.');
        return dot < 0 ? "" : name.substring(0, dot);
    }

    @Supplement(SupplementKind.INSTANCE)
    public static Class<?> componentType(Class<?> type) {
        return type.getComponentType();
    }

    @Supplement(SupplementKind.INSTANCE)
    public static Type getGenericSuperclass(Class<?> type) {
        return type.getSuperclass();
    }

    @Supplement(SupplementKind.INSTANCE)
    public static Type[] getGenericInterfaces(Class<?> type) {
        Class<?>[] interfaces = type.getInterfaces();
        Type[]     generic    = new Type[interfaces.length];
        System.arraycopy(interfaces, 0, generic, 0, interfaces.length);
        return generic;
    }

    /** Whether the class is a record: only a record extends {@code java.lang.Record}, for the compiler lets nothing else. */
    @Supplement(SupplementKind.INSTANCE)
    public static boolean isRecord(Class<?> type) {
        return type.getSuperclass() == Record.class;
    }
}
