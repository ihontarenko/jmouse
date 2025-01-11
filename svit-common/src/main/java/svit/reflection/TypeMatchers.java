package svit.reflection;

import svit.matcher.Matcher;

import java.util.List;

import static svit.reflection.JavaTypes.PRIMITIVES;
import static svit.reflection.JavaTypes.WRAPPERS;

/**
 * A utility class that provides matchers for comparing types. These matchers allow for checking
 * if two types are similar (i.e., primitive and its wrapper) or if one type is a subtype of another.
 */
@SuppressWarnings({"unused"})
public class TypeMatchers {

    /**
     * Returns a matcher that checks if the given type is similar to the expected type.
     * This matcher considers a primitive type and its corresponding wrapper type as similar.
     *
     * @param expectedType the expected type
     * @return a matcher that returns true if the given type is similar to the expected type
     * @example
     * <pre>{@code
     * Matcher<Class<?>> matcher = TypeMatchers.isSimilar(int.class);
     * boolean result = matcher.matches(Integer.class); // returns true
     * }</pre>
     * @see MethodMatchers#hasSoftParameterTypes(Class[])
     */
    public static Matcher<Class<?>> isSimilar(Class<?> expectedType) {
        return new SimilarTypeMatcher(expectedType);
    }

    /**
     * Returns a matcher that checks if the given type is the same to the expected type.
     *
     * @param expectedType the expected type
     * @return a matcher that returns true if the given type is the same to the expected type
     */
    public static Matcher<Class<?>> isSame(Class<?> expectedType) {
        return expectedType::equals;
    }

    /**
     * Returns a matcher that checks if the given type is a subtype of the expected type.
     * This matcher uses {@linkplain Class#isAssignableFrom} to determine if one type is a subtype of another.
     *
     * @param expectedType the expected type
     * @return a matcher that returns true if the given type is a subtype of the expected type
     * @example
     * <pre>{@code
     * Matcher<Class<?>> matcher = TypeMatchers.isSubtype(Number.class);
     * boolean result = matcher.matches(Integer.class); // returns true
     * }</pre>
     */
    public static Matcher<Class<?>> isSubtype(Class<?> expectedType) {
        return new SubtypeTypeMatcher(expectedType);
    }

    /**
     * Returns a matcher that checks if the given type is a supertype of the expected type.
     * This matcher uses {@linkplain Class#isAssignableFrom} to determine if one type is a supertype of another.
     *
     * @param expectedType the expected type
     * @return a matcher that returns true if the given type is a supertype of the expected type
     *
     * <P>Example Usage:</P>
     * <pre>{@code
     * Matcher<Class<?>> matcher = TypeMatchers.isSupertype(Integer.class);
     * boolean result = matcher.matches(Number.class); // returns true
     * }</pre>
     */
    public static Matcher<Class<?>> isSupertype(Class<?> expectedType) {
        return new SupertypeTypeMatcher(expectedType);
    }

    /**
     * Matcher that checks if a list of types is compatible with the specified types, allowing for autoboxing.
     *
     * @param expectedTypes the expected types.
     * @return a matcher that returns {@code true} if the given types are compatible with the expected types.
     *
     * <p>Example Usage:</p>
     * <pre>{@code
     * Matcher<Class<?>[]> matcher = TypeMatchers.isSoftTypes(Integer.class, String.class);
     * boolean result = matcher.matches(new Class<?>[]{Number.class, String.class}); // returns true
     * }</pre>
     */
    public static Matcher<Class<?>[]> isSoftTypes(Class<?>... expectedTypes) {
        return new SoftTypesMatcher(expectedTypes);
    }

    /**
     * Creates a matcher that checks if a class implements a specific generic interface with the provided type parameters.
     *
     * @param iface             the generic interface to check.
     * @param parameterizedTypes the expected type parameters of the generic interface.
     * @return a matcher that returns {@code true} if the class implements the interface with the specified type parameters.
     *
     * <p>Example Usage:</p>
     * <pre>{@code
     * Matcher<Class<?>> matcher = TypeMatchers.implementsGenericInterface(List.class, String.class);
     * boolean result = matcher.matches(ArrayList.class); // returns true
     * }</pre>
     */
    public static Matcher<Class<?>> implementsGenericInterface(Class<?> iface, Class<?>... parameterizedTypes) {
        return clazz -> {
            List<Class<?>> actualTypes = Reflections.getInterfacesParameterizedTypes(clazz, iface);
            return isSoftTypes(parameterizedTypes).matches(actualTypes.toArray(Class[]::new));
        };
    }

    /**
     * Creates a matcher that checks if a class extends a specific generic superclass with the provided type parameters.
     *
     * @param parameterizedTypes the expected type parameters of the generic superclass.
     * @return a matcher that returns {@code true} if the class extends the superclass with the specified type parameters.
     *
     * <p>Example Usage:</p>
     * <pre>{@code
     * class GenericBase<T> {}
     * class SpecificClass extends GenericBase<String> {}
     *
     * Matcher<Class<?>> matcher = TypeMatchers.extendsGenericSuperclass(String.class);
     * boolean result = matcher.matches(SpecificClass.class); // returns true
     * }</pre>
     */
    public static Matcher<Class<?>> extendsGenericSuperclass(Class<?>... parameterizedTypes) {
        return clazz -> {
            List<Class<?>> actualTypes = Reflections.getSuperclassParameterizedTypes(clazz);
            return isSoftTypes(parameterizedTypes).matches(actualTypes.toArray(Class[]::new));
        };
    }


    /**
     * A matcher that checks if two types are similar, meaning a primitive type and its wrapper class
     * are considered equivalent.
     */
    private record SimilarTypeMatcher(Class<?> expectedType) implements Matcher<Class<?>> {
        @Override
        public boolean matches(Class<?> actualType) {
            // If the types are not exactly the same, check for primitive/wrapper equivalence
            if (!expectedType.equals(actualType)) {
                Class<?> wrapper = expectedType.isPrimitive() ? WRAPPERS.get(expectedType) : PRIMITIVES.get(expectedType);
                return wrapper != null && wrapper.equals(actualType);
            }
            return true;
        }
    }

    /**
     * A matcher that checks if the actual type is a subtype of the expected type.
     * This uses {@linkplain Class#isAssignableFrom} to determine if the types are related.
     */
    private record SubtypeTypeMatcher(Class<?> expectedType) implements Matcher<Class<?>> {
        @Override
        public boolean matches(Class<?> actualType) {
            return actualType.isAssignableFrom(expectedType);
        }
    }

    /**
     * A matcher that checks if the actual type is a super-type of the expected type.
     * This uses {@linkplain Class#isAssignableFrom} to determine if the types are related.
     */
    private record SupertypeTypeMatcher(Class<?> expectedType) implements Matcher<Class<?>> {
        @Override
        public boolean matches(Class<?> actualType) {
            return expectedType.isAssignableFrom(actualType);
        }
    }

    /**
     * Matcher that checks if a list of types that are compatible with the specified types, allowing for autoboxing.
     */
    public record SoftTypesMatcher(Class<?>[] expectedTypes) implements Matcher<Class<?>[]> {

        @Override
        public boolean matches(Class<?>... actualTypes) {
            if (actualTypes.length != expectedTypes.length) {
                return false;
            }

            for (int i = 0; i < actualTypes.length; i++) {
                Matcher<Class<?>> matcher = isSubtype(expectedTypes[i]).or(isSimilar(expectedTypes[i]));
                if (!matcher.matches(actualTypes[i])) {
                    return false;
                }
            }

            return true;
        }

    }
}
