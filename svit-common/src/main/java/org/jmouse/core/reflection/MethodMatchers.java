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

    /**
     * Creates a matcher that checks if a method or constructor has the specified modifier.
     *
     * @param modifier the modifier to check (e.g., {@link Modifier#PUBLIC})
     * @return a matcher that checks for the given modifier
     * @example
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
     * @example
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
     * @example
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
     * @example
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
     * @example
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
     * @example
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
     * @example
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
     * @example
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
     * @example
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
     * @example
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
     * @example
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
     * @example
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
     * @example
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
     * @example
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
     * @example
     * <pre>{@code
     * Matcher<Executable> startsWithMatcher = MethodMatchers.nameStarts("get");
     * }</pre>
     */
    public static Matcher<Executable> nameStarts(String prefix) {
        return withName(TextMatchers.startsWith(prefix));
    }

    /**
     * Creates a matcher that checks if a method or constructor name ends with the specified suffix.
     *
     * @param suffix the suffix to check for
     * @return a matcher that checks if the method or constructor name ends with the given suffix
     * @example
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
     * @example
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
     * @example
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
     * @example
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
     * @example
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
     * @example
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