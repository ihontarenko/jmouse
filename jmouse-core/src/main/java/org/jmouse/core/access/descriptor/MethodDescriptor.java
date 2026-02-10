package org.jmouse.core.access.descriptor;

import org.jmouse.core.access.descriptor.internal.MethodData;
import org.jmouse.core.matcher.Matcher;
import org.jmouse.core.reflection.MethodMatchers;
import org.jmouse.core.reflection.Reflections;

import java.lang.reflect.Executable;
import java.lang.reflect.Method;

import static org.jmouse.core.reflection.MethodMatchers.*;

/**
 * 🔎 Descriptor for {@link Method} metadata.
 *
 * <p>Wraps reflective {@link Method} with additional type
 * information, matcher utilities, and property access helpers.</p>
 *
 * <h3>Features</h3>
 * <ul>
 *   <li>📚 Access to return type via {@link #getReturnType()}.</li>
 *   <li>✍️ Detects setter methods via {@link #isSetter()}.</li>
 *   <li>👁️ Detects getter methods via {@link #isGetter()} or {@link #isGetter(String)}.</li>
 *   <li>🏷️ Extracts property name for bean-style accessors via {@link #getPropertyName()}.</li>
 *   <li>🧩 Converts back to {@link MethodIntrospector} with {@link #toIntrospector()}.</li>
 * </ul>
 */
public class MethodDescriptor extends ExecutableDescriptor<Method, MethodData, MethodIntrospector> {

    private static final Matcher<Executable> SETTER = MethodMatchers.setter();
    private static final Matcher<Executable> GETTER = MethodMatchers.getter();

    /**
     * 🏗️ Construct a new descriptor for a given method.
     */
    protected MethodDescriptor(MethodIntrospector introspector, MethodData container) {
        super(introspector, container);
    }

    /**
     * 🎯 Return type descriptor of the underlying method.
     */
    public ClassTypeDescriptor getReturnType() {
        return container.getReturnType();
    }

    /**
     * ✍️ Check if method is a setter.
     */
    public boolean isSetter() {
        return SETTER.matches(unwrap());
    }

    /**
     * 👁️ Check if method is a getter (supports {@code getXxx} / {@code isXxx}).
     */
    public boolean isGetter() {
        return GETTER.matches(unwrap());
    }

    /**
     * 👁️ Check if method name starts with a given getter prefix.
     *
     * @param prefix e.g. {@code "is"} or {@code "get"}
     */
    public boolean isGetter(String prefix) {
        return nameStarts(prefix).matches(unwrap());
    }

    /**
     * 🏷️ Derive property name from bean-style getter/setter.
     *
     * @return de-capitalized property name (e.g. {@code "getUserName"} → {@code "userName"})
     */
    public String getPropertyName() {
        String prefix = isGetter() && isGetter(GETTER_IS_PREFIX) ? GETTER_IS_PREFIX : GETTER_GET_PREFIX;
        return Reflections.getPropertyName(unwrap(), prefix);
    }

    /**
     * 🔄 Return the owning introspector.
     */
    @Override
    public MethodIntrospector toIntrospector() {
        return introspector;
    }

    /**
     * 📝 Human-readable string with method signature + return type.
     */
    @Override
    public String toString() {
        return Reflections.getMethodName(unwrap()) + " : " + getReturnType().getName();
    }
}
