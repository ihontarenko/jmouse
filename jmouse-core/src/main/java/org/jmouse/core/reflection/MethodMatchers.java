package org.jmouse.core.reflection;

import org.jmouse.core.matcher.Matcher;
import org.jmouse.core.matcher.TextMatchers;

import java.lang.annotation.Annotation;
import java.lang.reflect.Constructor;
import java.lang.reflect.Executable;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.List;

/**
 * Utility class for creating matchers that work with {@link Method} and {@link Constructor} objects.
 *
 * <p>These matchers allow for checking various properties of methods and constructors,
 * such as modifiers, annotations, parameter types, return types, and names.</p>
 *
 * @see Method
 * @see Constructor
 * @see Matcher
 */
@SuppressWarnings({"unused"})
public class MethodMatchers {

    public static final String GETTER_GET_PREFIX = "get";
    public static final String GETTER_IS_PREFIX  = "is";
    public static final String GETTER_TO_PREFIX  = "to";
    public static final String SETTER_PREFIX     = "set";

    /**
     * Creates a matcher that checks if a method or constructor has the specified modifier.
     *
     * @param modifier the modifier to check (e.g., {@link Modifier#PUBLIC})
     * @return a matcher that checks for the given modifier
     * <p>Example
     * <pre>{@code
     * Matcher<Executable> publicMatcher = MethodMatchers.withModifier(Modifier.PUBLIC);
     * }</pre>
     */
    public static Matcher<Executable> withModifier(int modifier) {
        return new ModifierMatcher(modifier);
    }

    /**
     * Creates a matcher that checks if a method or constructor is private.
     *
     * @return a matcher that checks if the method or constructor is private
     * @see Modifier#PRIVATE
     * <p>Example
     * <pre>{@code
     * Matcher<Executable> privateMatcher = MethodMatchers.isPrivate();
     * }</pre>
     */
    public static Matcher<Executable> isPrivate() {
        return withModifier(Modifier.PRIVATE);
    }

    /**
     * Creates a matcher that checks if a method or constructor is protected.
     *
     * @return a matcher that checks if the method or constructor is protected
     * @see Modifier#PROTECTED
     * <p>Example
     * <pre>{@code
     * Matcher<Executable> protectedMatcher = MethodMatchers.isProtected();
     * }</pre>
     */
    public static Matcher<Executable> isProtected() {
        return withModifier(Modifier.PROTECTED);
    }

    /**
     * Creates a matcher that checks if a method or constructor is public.
     *
     * @return a matcher that checks if the method or constructor is public
     * @see Modifier#PUBLIC
     * <p>Example
     * <pre>{@code
     * Matcher<Executable> publicMatcher = MethodMatchers.isPublic();
     * }</pre>
     */
    public static Matcher<Executable> isPublic() {
        return withModifier(Modifier.PUBLIC);
    }

    /**
     * Creates a matcher that checks if a method or constructor is static.
     *
     * @return a matcher that checks if the method or constructor is static
     * @see Modifier#STATIC
     * <p>Example
     * <pre>{@code
     * Matcher<Executable> staticMatcher = MethodMatchers.isStatic();
     * }</pre>
     */
    public static Matcher<Executable> isStatic() {
        return withModifier(Modifier.STATIC);
    }

    /**
     * Creates a matcher that checks if a method is abstract.
     *
     * @return a matcher that checks if the method is abstract
     * @see Modifier#ABSTRACT
     * <p>Example
     * <pre>{@code
     * Matcher<Executable> staticMatcher = MethodMatchers.isAbstract();
     * }</pre>
     */
    public static Matcher<Executable> isAbstract() {
        return withModifier(Modifier.ABSTRACT);
    }

    /**
     * Creates a matcher that checks if a method or constructor is final.
     *
     * @return a matcher that checks if the method or constructor is final
     * @see Modifier#FINAL
     * <p>Example
     * <pre>{@code
     * Matcher<Executable> finalMatcher = MethodMatchers.isFinal();
     * }</pre>
     */
    public static Matcher<Executable> isFinal() {
        return withModifier(Modifier.FINAL);
    }

    /**
     * Creates a matcher that checks if a method is a default interface method.
     *
     * @return a matcher that checks if the method is a default interface method
     * @see Method#isDefault()
     * <p>Example
     * <pre>{@code
     * Matcher<Method> defaultMethodMatcher = MethodMatchers.isDefault();
     * }</pre>
     */
    public static Matcher<Method> isDefault() {
        return Method::isDefault;
    }

    /**
     * Creates a matcher that checks if a method or constructor is annotated with the specified annotation.
     *
     * @param annotation the annotation to check for
     * @return a matcher that checks if the method or constructor is annotated with the specified annotation
     * <p>Example
     * <pre>{@code
     * Matcher<Executable> annotatedMatcher = MethodMatchers.isAnnotatedWith(Deprecated.class);
     * }</pre>
     */
    public static Matcher<Executable> isAnnotatedWith(Class<? extends Annotation> annotation) {
        return new AnnotatedMatcher(annotation);
    }

    /**
     * Creates a matcher that checks if a method has the specified return type.
     *
     * @param returnType the return type to check for
     * @return a matcher that checks if the method has the specified return type
     * <p>Example
     * <pre>{@code
     * Matcher<Method> returnTypeMatcher = MethodMatchers.hasReturnType(String.class);
     * }</pre>
     */
    public static Matcher<Method> hasReturnType(Class<?> returnType) {
        return new ReturnTypeMatcher(returnType);
    }

    /**
     * Creates a matcher that checks if a method or constructor has the specified number of parameters.
     *
     * @param count the number of parameters
     * @return a matcher that checks if the method or constructor has the specified number of parameters
     * <p>Example
     * <pre>{@code
     * Matcher<Executable> parameterCountMatcher = MethodMatchers.hasParameterCount(2);
     * }</pre>
     */
    public static Matcher<Executable> hasParameterCount(int count) {
        return new ParameterCountMatcher(count);
    }

    /**
     * Creates a matcher that checks if a method or constructor has the specified parameter types.
     *
     * @param parameterTypes the parameter types to check
     * @return a matcher that checks if the method or constructor has the specified parameter types
     * <p>Example
     * <pre>{@code
     * Matcher<Executable> parameterTypeMatcher = MethodMatchers.hasParameterTypes(String.class, int.class);
     * }</pre>
     */
    public static Matcher<Executable> hasParameterTypes(Class<?>... parameterTypes) {
        return new ParameterTypesMatcher(parameterTypes);
    }

    /**
     * Creates a matcher that checks if a method or constructor has parameter types that are compatible
     * with the specified types, allowing for autoboxing.
     *
     * @param parameterTypes the parameter types to check
     * @return a matcher that checks if the method or constructor has compatible parameter types
     * <p>Example
     * <pre>{@code
     * Matcher<Executable> softParameterTypeMatcher = MethodMatchers.hasSoftParameterTypes(int.class, String.class);
     * }</pre>
     */
    public static Matcher<Executable> hasSoftParameterTypes(Class<?>... parameterTypes) {
        return new SoftParameterTypesMatcher(parameterTypes);
    }

    /**
     * Creates a matcher that checks if a method or constructor throws the specified exception.
     *
     * @param exceptionType the exception type to check for
     * @return a matcher that checks if the method or constructor throws the specified exception
     * <p>Example
     * <pre>{@code
     * Matcher<Executable> exceptionMatcher = MethodMatchers.throwsException(IOException.class);
     * }</pre>
     */
    public static Matcher<Executable> throwsException(Class<? extends Throwable> exceptionType) {
        return (method) -> List.of(method.getExceptionTypes()).contains(exceptionType);
    }

    /**
     * Creates a matcher that checks if a method or constructor name starts with the specified prefix.
     *
     * @param prefix the prefix to check for
     * @return a matcher that checks if the method or constructor name starts with the given prefix
     * <p>Example
     * <pre>{@code
     * Matcher<Executable> startsWithMatcher = MethodMatchers.nameStarts("get");
     * }</pre>
     */
    public static Matcher<Executable> nameStarts(String prefix) {
        return withName(TextMatchers.startsWith(prefix));
    }

    /**
     * Creates a matcher that checks if an {@link Executable} follows the JavaBean "is" getter naming convention.
     * <p>
     * Matches methods that start with {@code "is"}, which is typically used for boolean getters.
     * </p>
     *
     * @return a {@link Matcher} for methods starting with {@code "is"}
     */
    public static Matcher<Executable> prefixIs() {
        return nameStarts(GETTER_IS_PREFIX);
    }

    /**
     * Creates a matcher that checks if an {@link Executable} follows the JavaBean "get" getter naming convention.
     * <p>
     * Matches methods that start with {@code "get"}, which is typically used for standard getters.
     * </p>
     *
     * @return a {@link Matcher} for methods starting with {@code "get"}
     */
    public static Matcher<Executable> prefixGet() {
        return nameStarts(GETTER_GET_PREFIX);
    }

    /**
     * Creates a matcher that checks if an {@link Executable} follows the JavaBean "set" setter naming convention.
     * <p>
     * Matches methods that start with {@code "set"}, which is typically used for standard setters.
     * </p>
     *
     * @return a {@link Matcher} for methods starting with {@code "set"}
     */
    public static Matcher<Executable> prefixSet() {
        return nameStarts(SETTER_PREFIX);
    }

    /**
     * Creates a matcher that checks if an {@link Executable} is a getter method.
     * <p>
     * Matches methods that start with either {@code "get"} or {@code "is"}, following JavaBean conventions.
     * </p>
     *
     * @return a {@link Matcher} for getter methods
     */
    public static Matcher<Executable> getter() {
        return hasParameterCount(0).and(prefixIs().or(prefixGet()));
    }

    /**
     * Creates a matcher that checks if an {@link Executable} is a setter method.
     * <p>
     * Matches methods that start with {@code "set"}, following JavaBean conventions.
     * </p>
     *
     * @return a {@link Matcher} for setter methods
     */
    public static Matcher<Executable> setter() {
        return prefixSet().and(hasParameterCount(1));
    }

    /**
     * Creates a matcher that checks if a method or constructor name ends with the specified suffix.
     *
     * @param suffix the suffix to check for
     * @return a matcher that checks if the method or constructor name ends with the given suffix
     * <p>Example
     * <pre>{@code
     * Matcher<Executable> endsWithMatcher = MethodMatchers.nameEnds("Service");
     * }</pre>
     */
    public static Matcher<Executable> nameEnds(String suffix) {
        return withName(TextMatchers.endsWith(suffix));
    }

    /**
     * Creates a matcher that checks if a method or constructor name contains the specified substring.
     *
     * @param substring the substring to check for
     * @return a matcher that checks if the method or constructor name contains the given substring
     * <p>Example
     * <pre>{@code
     * Matcher<Executable> containsMatcher = MethodMatchers.nameContains("save");
     * }</pre>
     */
    public static Matcher<Executable> nameContains(String substring) {
        return withName(TextMatchers.contains(substring));
    }

    /**
     * Creates a matcher that checks if a method or constructor name is equal to the specified string.
     *
     * @param actual the name to check for
     * @return a matcher that checks if the method or constructor name is equal to the given name
     * <p>Example
     * <pre>{@code
     * Matcher<Executable> equalsMatcher = MethodMatchers.nameEquals("toString");
     * }</pre>
     */
    public static Matcher<Executable> nameEquals(String actual) {
        return withName(TextMatchers.same(actual));
    }

    /**
     * Creates a matcher that checks the method or constructor name using the provided text matcher.
     *
     * @param textMatcher the matcher to use for checking the method or constructor name
     * @return a matcher that checks the method or constructor name
     * <p>Example
     * <pre>{@code
     * Matcher<Executable> customNameMatcher = MethodMatchers.withName(TextMatchers.startsWith("get"));
     * }</pre>
     */
    public static Matcher<Executable> withName(Matcher<? super String> textMatcher) {
        return new MethodNameWithMatcher(textMatcher);
    }

    /**
     * Creates a matcher that checks if a constructor is a default constructor (no arguments).
     *
     * @return a matcher that checks if the constructor is a default constructor
     * <p>Example
     * <pre>{@code
     * Matcher<Constructor<?>> defaultConstructorMatcher = MethodMatchers.isDefaultConstructor();
     * }</pre>
     */
    public static Matcher<Constructor<?>> isDefaultConstructor() {
        return new DefaultConstructorMatcher();
    }

    /**
     * Creates a matcher that checks if a constructor is a copy constructor (one argument of the same type).
     *
     * @return a matcher that checks if the constructor is a copy constructor
     * <p>Example
     * <pre>{@code
     * Matcher<Constructor<?>> copyConstructorMatcher = MethodMatchers.isCopyConstructor();
     * }</pre>
     */
    public static Matcher<Constructor<?>> isCopyConstructor() {
        return new CopyConstructorMatcher();
    }

    private record CopyConstructorMatcher() implements Matcher<Constructor<?>> {
        @Override
        public boolean matches(Constructor<?> constructor) {
            return Matcher.logicalAnd(hasParameterCount(1), hasParameterTypes(constructor.getDeclaringClass())).matches(constructor);
        }
    }

    private record DefaultConstructorMatcher() implements Matcher<Constructor<?>> {
        @Override
        public boolean matches(Constructor<?> item) {
            return hasParameterCount(0).matches(item);
        }
    }

    private record MethodNameWithMatcher(Matcher<? super String> textMatcher) implements Matcher<Executable> {
        @Override
        public boolean matches(Executable item) {
            return textMatcher.matches(item.getName());
        }
    }

    private record ParameterTypesMatcher(Class<?>... expectedTypes) implements Matcher<Executable> {
        @Override
        public boolean matches(Executable method) {
            Class<?>[] actualTypes = method.getParameterTypes();

            if (actualTypes.length != expectedTypes.length) {
                return false;
            }

            for (int i = 0; i < actualTypes.length; i++) {
                if (!actualTypes[i].equals(expectedTypes[i])) {
                    return false;
                }
            }

            return true;
        }
    }

    private record ParameterCountMatcher(int expectedCount) implements Matcher<Executable> {
        @Override
        public boolean matches(Executable method) {
            return method.getParameterCount() == expectedCount;
        }
    }

    private record SoftParameterTypesMatcher(Class<?>... expectedTypes) implements Matcher<Executable> {
        @Override
        public boolean matches(Executable method) {
            return TypeMatchers.isSoftTypes(expectedTypes).matches(method.getParameterTypes());
        }
    }

    private record ModifierMatcher(int modifier) implements Matcher<Executable> {
        @Override
        public boolean matches(Executable method) {
            return (method.getModifiers() & modifier) != 0;
        }
    }

    private record AnnotatedMatcher(Class<? extends Annotation> annotation) implements Matcher<Executable> {
        @Override
        public boolean matches(Executable method) {
            return method.isAnnotationPresent(annotation);
        }
    }

    private record ReturnTypeMatcher(Class<?> returnType) implements Matcher<Method> {
        @Override
        public boolean matches(Method method) {
            return method.getReturnType().equals(returnType);
        }
    }

}