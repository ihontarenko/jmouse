package org.jmouse.j2js.teavm.classlib.java.lang.reflect;

import org.jmouse.j2js.teavm.supplement.Supplement;
import org.jmouse.j2js.teavm.supplement.SupplementKind;
import org.jmouse.j2js.teavm.supplement.Supplements;

import java.lang.annotation.Annotation;
import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Executable;
import java.lang.reflect.Type;

/**
 * {@code java.lang.reflect.Parameter} in the browser: a parameter of a method or a constructor by its position. TeaVM keeps no
 * parameter names, so a parameter is named as the JDK names one whose name the class file does not carry — {@code arg0},
 * {@code arg1} — and says its name is not present. It also gives {@code Executable} — methods and constructors —
 * their parameters.
 *
 * @author Ivan Hontarenko (Mr. Jerry Mouse)
 * @author ihontarenko@gmail.com
 */
@Supplements(Executable.class)
public final class TParameter implements AnnotatedElement {

    private final Executable executable;
    private final int        index;

    public TParameter(Executable executable, int index) {
        this.executable = executable;
        this.index = index;
    }

    /** Every parameter of {@code executable}, in order: {@code Executable.getParameters()}. */
    @Supplement(SupplementKind.INSTANCE)
    public static TParameter[] getParameters(Executable executable) {
        TParameter[] parameters = new TParameter[executable.getParameterCount()];
        for (int index = 0; index < parameters.length; index++) {
            parameters[index] = new TParameter(executable, index);
        }
        return parameters;
    }

    public String getName() {
        return "arg" + index;
    }

    public boolean isNamePresent() {
        return false;
    }

    public Executable getDeclaringExecutable() {
        return executable;
    }

    public int getModifiers() {
        return 0;
    }

    public Class<?> getType() {
        return executable.getParameterTypes()[index];
    }

    /**
     * Its generic type; its type where the executable's generic parameter types do not cover every parameter — TeaVM gives none
     * for an executable it keeps no signature of —, as the JDK's {@code Parameter} answers then.
     */
    public Type getParameterizedType() {
        Type[] generic = executable.getGenericParameterTypes();
        return generic.length == executable.getParameterCount() ? generic[index] : getType();
    }

    /** Its annotations; none where the executable keeps none for it. */
    @Override
    public Annotation[] getAnnotations() {
        Annotation[][] annotations = executable.getParameterAnnotations();
        return index < annotations.length ? annotations[index] : new Annotation[0];
    }

    @Override
    public Annotation[] getDeclaredAnnotations() {
        return getAnnotations();
    }

    @Override
    public <T extends Annotation> T getAnnotation(Class<T> annotationType) {
        for (Annotation annotation : getAnnotations()) {
            if (annotationType.isInstance(annotation)) {
                return annotationType.cast(annotation);
            }
        }
        return null;
    }

    @Override
    public boolean isAnnotationPresent(Class<? extends Annotation> annotationType) {
        return getAnnotation(annotationType) != null;
    }

    public boolean isVarArgs() {
        return executable.isVarArgs() && index == executable.getParameterCount() - 1;
    }

    public boolean isImplicit() {
        return false;
    }

    public boolean isSynthetic() {
        return false;
    }

    @Override
    public boolean equals(Object other) {
        return other instanceof TParameter that && executable.equals(that.executable) && index == that.index;
    }

    @Override
    public int hashCode() {
        return executable.hashCode() ^ index;
    }

    @Override
    public String toString() {
        return getType().getName() + " " + getName();
    }
}
