package org.jmouse.core.reflection.annotation;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 🧠 Caches and provides access to annotation attribute methods.
 * <p>
 * This class extracts non-void, no-arg methods from a given annotation type
 * and treats them as its "attributes" (in the style of Spring and JSR-330).
 * Useful for synthesizing annotation metadata efficiently.
 * </p>
 *
 * <pre>{@code
 * AnnotationAttributes attributes = AnnotationAttributes.forAnnotationType(Controller.class);
 * Method method = attributes.getAttribute("value");
 * }</pre>
 *
 * @author Ivan Hontarenko (Mr. Jerry Mouse)
 * @author ihontarenko@gmail.com
 */
public class AnnotationAttributes {

    static final Map<Class<? extends Annotation>, AnnotationAttributes> CACHE = new ConcurrentHashMap<>();

    private final Class<? extends Annotation> type;
    private final Method[]                    attributes;

    /**
     * Constructs metadata holder for annotation type and its attribute methods.
     *
     * @param type       annotation class
     * @param attributes array of attribute methods
     */
    public AnnotationAttributes(Class<? extends Annotation> type, Method[] attributes) {
        this.type = type;
        this.attributes = attributes;
    }

    /**
     * 💾 Gets cached {@link AnnotationAttributes} for the given annotation type, computing it if necessary.
     *
     * @param type annotation class
     * @return attribute metadata holder
     */
    public static AnnotationAttributes forAnnotationType(Class<? extends Annotation> type) {
        return CACHE.computeIfAbsent(type, AnnotationAttributes::compute);
    }

    /**
     * 🔍 Computes attribute methods for the given annotation.
     *
     * @param type annotation class
     * @return constructed {@link AnnotationAttributes}
     */
    private static AnnotationAttributes compute(Class<? extends Annotation> type) {
        Method[]     methods    = type.getDeclaredMethods();
        List<Method> attributes = new ArrayList<>();

        for (Method method : methods) {
            if (isAnnotationAttribute(method)) {
                attributes.add(method);
            }
        }

        attributes.sort(Comparator.comparing(Method::getName));

        return new AnnotationAttributes(type, attributes.toArray(Method[]::new));
    }

    /**
     * ✅ Checks if a method qualifies as an annotation attribute:
     * no parameters and non-void return type.
     *
     * @param attribute method to check
     * @return true if it's a valid attribute
     */
    public static boolean isAnnotationAttribute(Method attribute) {
        return attribute.getParameterCount() == 0 && attribute.getReturnType() != void.class;
    }

    /**
     * 📥 Gets attribute method by name (case-insensitive).
     *
     * @param name attribute name
     * @return corresponding {@link Method}
     */
    public Method getAttribute(String name) {
        return attributes[indexOf(name)];
    }

    /**
     * 📥 Gets attribute method by index.
     *
     * @param index position in internal array
     * @return attribute method
     */
    public Method getAttribute(int index) {
        return attributes[index];
    }

    /**
     * 📥 Gets all attributes method
     */
    public Method[] getAttributes() {
        return attributes;
    }

    /**
     * 🔎 Finds index of attribute method by name.
     *
     * @param name attribute name
     * @return index of attribute or -1 if not found
     */
    public int indexOf(String name) {
        int index = -1;

        for (Method attribute : attributes) {
            index++;
            if (attribute.getName().equalsIgnoreCase(name)) {
                return index;
            }
        }

        return -1;
    }

    /**
     * 🔎 Finds index of attribute method by method reference.
     *
     * @param attribute method reference
     * @return index in internal array
     */
    public int indexOf(Method attribute) {
        return indexOf(attribute.getName());
    }
}
