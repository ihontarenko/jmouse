package org.jmouse.core.reflection;

import org.jmouse.core.matcher.Matcher;
import org.jmouse.core.matcher.TextMatchers;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;

/**
 * Utility class for creating matchers that work with {@link Field} objects.
 *
 * <p>These matchers allow for checking various properties of fields such as
 * modifiers, annotations, names, and types.</p>
 *
 * @see Field
 * @see Matcher
 */
@SuppressWarnings({"unused"})
public class FieldMatchers {

    /**
     * Creates a matcher that checks if a field has the specified modifier.
     *
     * @param modifier the modifier to check (e.g., {@link Modifier#PUBLIC})
     * @return a matcher that checks for the given modifier
     * @example
     * <pre>{@code
     * Matcher<Field> publicMatcher = FieldMatchers.withModifier(Modifier.PUBLIC);
     * }</pre>
     */
    public static Matcher<Field> withModifier(int modifier) {
        return new FieldModifierMatcher(modifier);
    }

    /**
     * Creates a matcher that checks if a field is private.
     *
     * @return a matcher that checks if a field is private
     * @see Modifier#PRIVATE
     * @example
     * <pre>{@code
     * Matcher<Field> privateMatcher = FieldMatchers.isPrivate();
     * }</pre>
     */
    public static Matcher<Field> isPrivate() {
        return withModifier(Modifier.PRIVATE);
    }

    /**
     * Creates a matcher that checks if a field is protected.
     *
     * @return a matcher that checks if a field is protected
     * @see Modifier#PROTECTED
     * @example
     * <pre>{@code
     * Matcher<Field> protectedMatcher = FieldMatchers.isProtected();
     * }</pre>
     */
    public static Matcher<Field> isProtected() {
        return withModifier(Modifier.PROTECTED);
    }

    /**
     * Creates a matcher that checks if a field is public.
     *
     * @return a matcher that checks if a field is public
     * @see Modifier#PUBLIC
     * @example
     * <pre>{@code
     * Matcher<Field> publicMatcher = FieldMatchers.isPublic();
     * }</pre>
     */
    public static Matcher<Field> isPublic() {
        return withModifier(Modifier.PUBLIC);
    }

    /**
     * Creates a matcher that checks if a field is annotated with the specified annotation.
     *
     * @param annotation the annotation to check
     * @return a matcher that checks if a field has the specified annotation
     * @example
     * <pre>{@code
     * Matcher<Field> annotatedMatcher = FieldMatchers.isAnnotatedWith(Deprecated.class);
     * }</pre>
     */
    public static Matcher<Field> isAnnotatedWith(Class<? extends Annotation> annotation) {
        return new FieldAnnotatedMatcher(annotation);
    }

    /**
     * Creates a matcher that checks if a field has the specified type.
     *
     * @param type the type of the field to check
     * @return a matcher that checks if a field has the specified type
     * @example
     * <pre>{@code
     * Matcher<Field> typeMatcher = FieldMatchers.hasType(String.class);
     * }</pre>
     */
    public static Matcher<Field> hasType(Class<?> type) {
        return new FieldTypeMatcher(type);
    }

    /**
     * Creates a matcher that checks if a field name starts with the specified prefix.
     *
     * @param prefix the prefix to check for
     * @return a matcher that checks if the field name starts with the given prefix
     * @example
     * <pre>{@code
     * Matcher<Field> startsWithMatcher = FieldMatchers.nameStarts("user");
     * }</pre>
     */
    public static Matcher<Field> nameStarts(String prefix) {
        return withName(TextMatchers.startsWith(prefix));
    }

    /**
     * Creates a matcher that checks if a field name ends with the specified suffix.
     *
     * @param suffix the suffix to check for
     * @return a matcher that checks if the field name ends with the given suffix
     * @example
     * <pre>{@code
     * Matcher<Field> endsWithMatcher = FieldMatchers.nameEnds("Id");
     * }</pre>
     */
    public static Matcher<Field> nameEnds(String suffix) {
        return withName(TextMatchers.endsWith(suffix));
    }

    /**
     * Creates a matcher that checks if a field name contains the specified substring.
     *
     * @param substring the substring to check for
     * @return a matcher that checks if the field name contains the given substring
     * @example
     * <pre>{@code
     * Matcher<Field> containsMatcher = FieldMatchers.nameContains("name");
     * }</pre>
     */
    public static Matcher<Field> nameContains(String substring) {
        return withName(TextMatchers.contains(substring));
    }

    /**
     * Creates a matcher that checks if a field name is equal to the specified string.
     *
     * @param actual the name to check
     * @return a matcher that checks if the field name is equal to the given name
     * @example
     * <pre>{@code
     * Matcher<Field> equalsMatcher = FieldMatchers.nameEquals("userName");
     * }</pre>
     */
    public static Matcher<Field> nameEquals(String actual) {
        return withName(TextMatchers.same(actual));
    }

    /**
     * Creates a matcher that checks the field name using the provided text matcher.
     *
     * @param textMatcher the matcher to use for checking the field name
     * @return a matcher that checks the field name
     * @example
     * <pre>{@code
     * Matcher<Field> customNameMatcher = FieldMatchers.withName(TextMatchers.startsWith("user"));
     * }</pre>
     */
    public static Matcher<Field> withName(Matcher<? super String> textMatcher) {
        return new FieldNameWithMatcher(textMatcher);
    }

    // Internal matcher implementations

    private record FieldNameWithMatcher(Matcher<? super String> textMatcher) implements Matcher<Field> {
        @Override
        public boolean matches(Field item) {
            return textMatcher.matches(item.getName());
        }
    }

    private record FieldModifierMatcher(int modifier) implements Matcher<Field> {
        @Override
        public boolean matches(Field field) {
            return (field.getModifiers() & modifier) != 0;
        }
    }

    private record FieldAnnotatedMatcher(Class<? extends Annotation> annotation) implements Matcher<Field> {
        @Override
        public boolean matches(Field field) {
            return field.isAnnotationPresent(annotation);
        }
    }

    private record FieldTypeMatcher(Class<?> type) implements Matcher<Field> {
        @Override
        public boolean matches(Field field) {
            return field.getType().equals(type);
        }
    }

}
