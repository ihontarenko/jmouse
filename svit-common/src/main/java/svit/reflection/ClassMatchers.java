package svit.reflection;

import svit.matcher.Matcher;
import svit.matcher.TextMatchers;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.stream.Stream;

/**
 * Utility class for creating matchers that work with {@link Class} objects.
 *
 * <p>These matchers allow for checking various properties of classes, such as
 * modifiers, annotations, field names, method names, and implemented interfaces.</p>
 *
 * @see Class
 * @see Matcher
 */
@SuppressWarnings({"unused"})
public class ClassMatchers {

    public static final String[] JDK_PACKAGES = {"java.", "jdk.", "sun."};

    /**
     * Creates a matcher that checks if a class has the specified modifier.
     *
     * @param modifier the modifier to check (e.g., {@link Modifier#PUBLIC})
     * @return a matcher that checks for the given modifier
     * @example
     * <pre>{@code
     * Matcher<Class<?>> publicMatcher = ClassMatchers.withModifier(Modifier.PUBLIC);
     * }</pre>
     */
    public static Matcher<Class<?>> withModifier(int modifier) {
        return new ModifierMatcher(modifier);
    }

    /**
     * Creates a matcher that checks if a class is private.
     *
     * @return a matcher that checks if the class is private
     * @see Modifier#PRIVATE
     * @example
     * <pre>{@code
     * Matcher<Class<?>> privateMatcher = ClassMatchers.isPrivate();
     * }</pre>
     */
    public static Matcher<Class<?>> isPrivate() {
        return withModifier(Modifier.PRIVATE);
    }

    /**
     * Creates a matcher that checks if a class is protected.
     *
     * @return a matcher that checks if the class is protected
     * @see Modifier#PROTECTED
     * @example
     * <pre>{@code
     * Matcher<Class<?>> protectedMatcher = ClassMatchers.isProtected();
     * }</pre>
     */
    public static Matcher<Class<?>> isProtected() {
        return withModifier(Modifier.PROTECTED);
    }

    /**
     * Creates a matcher that checks if a class is public.
     *
     * @return a matcher that checks if the class is public
     * @see Modifier#PUBLIC
     * @example
     * <pre>{@code
     * Matcher<Class<?>> publicMatcher = ClassMatchers.isPublic();
     * }</pre>
     */
    public static Matcher<Class<?>> isPublic() {
        return withModifier(Modifier.PUBLIC);
    }

    /**
     * Creates a matcher that checks if a class is interface.
     *
     * @return a matcher that checks if the class is interface.
     * @see Modifier#INTERFACE
     * @example
     * <pre>{@code
     * Matcher<Class<?>> ifaceMatcher = ClassMatchers.isInterface();
     * }</pre>
     */
    public static Matcher<Class<?>> isInterface() {
        return withModifier(Modifier.INTERFACE);
    }

    /**
     * Creates a matcher that checks if a class is abstract.
     *
     * @return a matcher that checks if the class is abstract
     * @see Modifier#ABSTRACT
     * @example
     * <pre>{@code
     * Matcher<Class<?>> abstractMatcher = ClassMatchers.isAbstract();
     * }</pre>
     */
    public static Matcher<Class<?>> isAbstract() {
        return withModifier(Modifier.ABSTRACT);
    }

    /**
     * Creates a matcher that checks if a class is annotation.
     *
     * @return a matcher that checks if the class is annotation
     * @see Class#isAnnotation()
     * @example
     * <pre>{@code
     * Matcher<Class<?>> annotationMatcher = ClassMatchers.isAnnotation();
     * }</pre>
     */
    public static Matcher<Class<?>> isAnnotation() {
        return Class::isAnnotation;
    }

    /**
     * Creates a matcher that checks if a class is enum.
     *
     * @return a matcher that checks if the class is enum
     * @see Class#isEnum()
     * @example
     * <pre>{@code
     * Matcher<Class<?>> enumMatcher = ClassMatchers.isEnum();
     * }</pre>
     */
    public static Matcher<Class<?>> isEnum() {
        return Class::isEnum;
    }

    /**
     * Creates a matcher that checks if a class implements the specified interface.
     *
     * @param interfaceClass the interface class to check for
     * @return a matcher that checks if the class implements the specified interface
     * @example
     * <pre>{@code
     * Matcher<Class<?>> interfaceMatcher = ClassMatchers.implementsInterface(List.class);
     * }</pre>
     */
    public static Matcher<Class<?>> implementsInterface(Class<?> interfaceClass) {
        return new ImplementsInterfaceMatcher(interfaceClass);
    }

    /**
     * Creates a matcher that checks if a class is annotated with the specified annotation.
     *
     * @param annotation the annotation to check for
     * @return a matcher that checks if the class is annotated with the specified annotation
     * @example
     * <pre>{@code
     * Matcher<Class<?>> annotatedMatcher = ClassMatchers.isAnnotatedWith(Deprecated.class);
     * }</pre>
     */
    public static Matcher<Class<?>> isAnnotatedWith(Class<? extends Annotation> annotation) {
        return new AnnotatedClassMatcher(annotation);
    }

    /**
     * Creates a matcher that checks if a class has a field matching the specified field matcher.
     *
     * @param fieldMatcher the field matcher to use
     * @return a matcher that checks if the class has a field that matches the field matcher
     * @example
     * <pre>{@code
     * Matcher<Class<?>> fieldMatcher = ClassMatchers.hasField(FieldMatchers.isPublic());
     * }</pre>
     */
    public static Matcher<Class<?>> hasField(Matcher<? super Field> fieldMatcher) {
        return new HasFieldMatcher(fieldMatcher);
    }

    /**
     * Creates a matcher that checks if a class has a method matching the specified method matcher.
     *
     * @param methodMatcher the method matcher to use
     * @return a matcher that checks if the class has a method that matches the method matcher
     * @example
     * <pre>{@code
     * Matcher<Class<?>> methodMatcher = ClassMatchers.hasMethod(MethodMatchers.isPublic());
     * }</pre>
     */
    public static Matcher<Class<?>> hasMethod(Matcher<? super Method> methodMatcher) {
        return new HasMethodMatcher(methodMatcher);
    }

    /**
     * Creates a matcher that checks if a class has a field with a name matching the specified text matcher.
     *
     * @param textMatcher the text matcher to use for field name matching
     * @return a matcher that checks if the class has a field with a matching name
     * @example
     * <pre>{@code
     * Matcher<Class<?>> fieldNameMatcher = ClassMatchers.hasFieldName(TextMatchers.contains("name"));
     * }</pre>
     */
    public static Matcher<Class<?>> hasFieldName(Matcher<? super String> textMatcher) {
        return hasField(FieldMatchers.withName(textMatcher));
    }

    /**
     * Creates a matcher that checks if a class has a method with a name matching the specified text matcher.
     *
     * @param textMatcher the text matcher to use for method name matching
     * @return a matcher that checks if the class has a method with a matching name
     * @example
     * <pre>{@code
     * Matcher<Class<?>> methodNameMatcher = ClassMatchers.hasMethodName(TextMatchers.startsWith("get"));
     * }</pre>
     */
    public static Matcher<Class<?>> hasMethodName(Matcher<? super String> textMatcher) {
        return hasMethod(MethodMatchers.withName(textMatcher));
    }

    /**
     * Creates a matcher that checks if a class has a method annotated with the specified annotation.
     *
     * @param annotation the annotation to check for on methods
     * @return a matcher that checks if the class has a method annotated with the specified annotation
     * @example
     * <pre>{@code
     * Matcher<Class<?>> methodAnnotatedMatcher = ClassMatchers.hasMethodAnnotatedWith(Deprecated.class);
     * }</pre>
     */
    public static Matcher<Class<?>> hasMethodAnnotatedWith(Class<? extends Annotation> annotation) {
        return new HasMethodMatcher(MethodMatchers.isAnnotatedWith(annotation));
    }

    /**
     * Creates a matcher that checks if a class has a field annotated with the specified annotation.
     *
     * @param annotation the annotation to check for on fields
     * @return a matcher that checks if the class has a field annotated with the specified annotation
     * @example
     * <pre>{@code
     * Matcher<Class<?>> fieldAnnotatedMatcher = ClassMatchers.hasFieldAnnotatedWith(Deprecated.class);
     * }</pre>
     */
    public static Matcher<Class<?>> hasFieldAnnotatedWith(Class<? extends Annotation> annotation) {
        return new HasFieldMatcher(FieldMatchers.isAnnotatedWith(annotation));
    }

    /**
     * Creates a matcher that checks if a class name starts with the specified prefix.
     *
     * @param prefix the prefix to check for
     * @return a matcher that checks if the class name starts with the given prefix
     * @example
     * <pre>{@code
     * Matcher<Class<?>> nameStartsMatcher = ClassMatchers.nameStarts("User");
     * }</pre>
     */
    public static Matcher<Class<?>> nameStarts(String prefix) {
        return withName(TextMatchers.startsWith(prefix));
    }

    /**
     * Creates a matcher that checks if a class name ends with the specified suffix.
     *
     * @param suffix the suffix to check for
     * @return a matcher that checks if the class name ends with the given suffix
     * @example
     * <pre>{@code
     * Matcher<Class<?>> nameEndsMatcher = ClassMatchers.nameEnds("Service");
     * }</pre>
     */
    public static Matcher<Class<?>> nameEnds(String suffix) {
        return withName(TextMatchers.endsWith(suffix));
    }

    /**
     * Creates a matcher that checks if a class name contains the specified substring.
     *
     * @param substring the substring to check for
     * @return a matcher that checks if the class name contains the given substring
     * @example
     * <pre>{@code
     * Matcher<Class<?>> nameContainsMatcher = ClassMatchers.nameContains("Controller");
     * }</pre>
     */
    public static Matcher<Class<?>> nameContains(String substring) {
        return withName(TextMatchers.contains(substring));
    }

    /**
     * Creates a matcher that checks if a class name is equal to the specified string.
     *
     * @param actual the name to check for
     * @return a matcher that checks if the class name is equal to the given name
     * @example
     * <pre>{@code
     * Matcher<Class<?>> nameEqualsMatcher = ClassMatchers.nameEquals("UserController");
     * }</pre>
     */
    public static Matcher<Class<?>> nameEquals(String actual) {
        return withName(TextMatchers.same(actual));
    }

    /**
     * Creates a matcher that checks the class name using the provided text matcher.
     *
     * @param textMatcher the text matcher to use for checking the class name
     * @return a matcher that checks the class name
     * @example
     * <pre>{@code
     * Matcher<Class<?>> customNameMatcher = ClassMatchers.withName(TextMatchers.startsWith("User"));
     * }</pre>
     */
    public static Matcher<Class<?>> withName(Matcher<? super String> textMatcher) {
        return new ClassNameWithMatcher(textMatcher);
    }

    /**
     * Creates a matcher that checks if a class is similar to the specified type (handling primitive and wrapper types).
     *
     * @param expectedType the expected type to check against
     * @return a matcher that checks if the class is similar to the expected type
     * @example
     * <pre>{@code
     * Matcher<Class<?>> similarMatcher = ClassMatchers.isSimilar(int.class);
     * }</pre>
     */
    public static Matcher<Class<?>> isSimilar(Class<?> expectedType) {
        return TypeMatchers.isSimilar(expectedType);
    }

    /**
     * Creates a matcher that checks if a class is a subtype of the specified type.
     *
     * @param expectedType the expected type to check for
     * @return a matcher that checks if the class is a subtype of the expected type
     * @example
     * <pre>{@code
     * Matcher<Class<?>> subtypeMatcher = ClassMatchers.isSubtype(Number.class);
     * }</pre>
     */
    public static Matcher<Class<?>> isSubtype(Class<?> expectedType) {
        return TypeMatchers.isSubtype(expectedType);
    }

    /**
     * Creates a matcher that checks if a class is a supertype of the specified type.
     *
     * @param expectedType the expected type to check for
     * @return a matcher that checks if the class is a supertype of the expected type
     * @example
     * <pre>{@code
     * Matcher<Class<?>> supertypeMatcher = ClassMatchers.isSupertype(Number.class);
     * }</pre>
     */
    public static Matcher<Class<?>> isSupertype(Class<?> expectedType) {
        return TypeMatchers.isSupertype(expectedType);
    }

    /**
     * Creates a matcher that checks if a class is equals of the specified type.
     *
     * @param expectedType the expected type to check for
     * @return a matcher that checks if the class is equals of the expected type
     * @example
     * <pre>{@code
     * Matcher<Class<?>> sameMatcher = ClassMatchers.isSame(Number.class);
     * }</pre>
     */
    public static Matcher<Class<?>> isSame(Class<?> expectedType) {
        return TypeMatchers.isSame(expectedType);
    }

    /**
     * Creates a matcher that checks if a class name starts with 'java.'
     *
     * @return a matcher that checks if the class name starts with 'java.'
     * @example
     * <pre>{@code
     * Matcher<Class<?>> javaPackageMatcher = ClassMatchers.isJavaPackage();
     * javaPackageMatcher.matches(String.class); // true
     * }</pre>
     */
    public static Matcher<Class<?>> isJavaPackage() {
        return Stream.of(JDK_PACKAGES).map(ClassMatchers::nameStarts)
                .reduce(Matcher.constant(false), Matcher::logicalOr);
    }

    private record ClassNameWithMatcher(Matcher<? super String> textMatcher) implements Matcher<Class<?>> {
        @Override
        public boolean matches(Class<?> item) {
            return textMatcher.matches(item.getName());
        }
    }

    private record ImplementsInterfaceMatcher(Class<?> interfaceClass) implements Matcher<Class<?>> {
        @Override
        public boolean matches(Class<?> clazz) {
            for (Class<?> implementedInterface : Reflections.getClassInterfaces(clazz)) {
                if (implementedInterface.equals(interfaceClass)) {
                    return true;
                }
            }
            return false;
        }
    }

    private record ModifierMatcher(int modifier) implements Matcher<Class<?>> {
        @Override
        public boolean matches(Class<?> clazz) {
            return (clazz.getModifiers() & modifier) != 0;
        }
    }

    private record AnnotatedClassMatcher(Class<? extends Annotation> annotation) implements Matcher<Class<?>> {
        @Override
        public boolean matches(Class<?> clazz) {
            return clazz.isAnnotationPresent(annotation);
        }
    }

    private record HasFieldMatcher(Matcher<? super Field> fieldMatcher) implements Matcher<Class<?>> {
        @Override
        public boolean matches(Class<?> clazz) {
            for (Field field : clazz.getDeclaredFields()) {
                if (fieldMatcher.matches(field)) {
                    return true;
                }
            }
            return false;
        }
    }

    private record HasMethodMatcher(Matcher<? super Method> methodMatcher) implements Matcher<Class<?>> {
        @Override
        public boolean matches(Class<?> clazz) {
            for (Method method : clazz.getDeclaredMethods()) {
                if (methodMatcher.matches(method)) {
                    return true;
                }
            }
            return false;
        }
    }

}
