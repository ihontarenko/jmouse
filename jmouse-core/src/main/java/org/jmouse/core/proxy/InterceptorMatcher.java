package org.jmouse.core.proxy;

import org.jmouse.core.matcher.Matcher;
import org.jmouse.core.reflection.ClassMatchers;

import java.lang.annotation.Annotation;

import static org.jmouse.core.reflection.ClassMatchers.hasMethodAnnotatedWith;
import static org.jmouse.core.reflection.ClassMatchers.isAnnotatedWith;

/**
 * 🎯 Specialization of {@link Matcher} for class-level interception rules.
 *
 * <p>Determines whether a given target class should be associated with
 * a {@link MethodInterceptor}.</p>
 *
 * <h3>Factory methods</h3>
 * <ul>
 *   <li>✅ {@link #any()} — matches all classes.</li>
 *   <li>🚫 {@link #none()} — matches no classes.</li>
 *   <li>🎯 {@link #forClasses(Class[])} — matches only the specified classes.</li>
 * </ul>
 *
 * <pre>{@code
 * InterceptorMatcher any = InterceptorMatcher.any();
 * InterceptorMatcher none = InterceptorMatcher.none();
 * InterceptorMatcher onlyServices =
 *         InterceptorMatcher.forClasses(UserService.class, OrderService.class);
 *
 * any.matches(String.class);           // true
 * none.matches(String.class);          // false
 * onlyServices.matches(UserService.class); // true
 * onlyServices.matches(String.class);      // false
 * }</pre>
 */
public interface InterceptorMatcher extends Matcher<Class<?>> {

    /**
     * ✅ Match all classes (always returns {@code true}).
     *
     * @return matcher that accepts any class
     */
    static InterceptorMatcher any() {
        return Matcher.<Class<?>>constant(true)::matches;
    }

    /**
     * 🚫 Match no classes (always returns {@code false}).
     *
     * @return matcher that rejects all classes
     */
    static InterceptorMatcher none() {
        return Matcher.<Class<?>>constant(false)::matches;
    }

    /**
     * 🎯 Match only the specified classes.
     *
     * <p>Equivalent to chaining {@link ClassMatchers#isSame(Class)} with logical OR
     * for each provided class.</p>
     *
     * @param classes array of classes to match
     * @return matcher that accepts only the given classes
     */
    static InterceptorMatcher forClasses(Class<?>... classes) {
        Matcher<Class<?>> matcher = none();

        for (Class<?> clazz : classes) {
            matcher = matcher.or(ClassMatchers.isSupertype(clazz));
        }

        return matcher::matches;
    }

    @SafeVarargs
    static InterceptorMatcher forAnnotations(Class<? extends Annotation>... annotations) {
        Matcher<Class<?>> matcher = none();

        for (Class<? extends Annotation> annotation : annotations) {
            matcher = matcher.or(isAnnotatedWith(annotation));
            matcher = matcher.or(hasMethodAnnotatedWith(annotation));
        }

        return matcher::matches;
    }

}
