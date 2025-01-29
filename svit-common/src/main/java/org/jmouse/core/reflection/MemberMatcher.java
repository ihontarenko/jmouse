package org.jmouse.core.reflection;

import org.jmouse.core.matcher.Matcher;

import java.lang.reflect.Member;

/**
 * A utility class for creating {@link Matcher} instances that operate on {@link Member} objects.
 */
public class MemberMatcher {

    /**
     * Creates a {@link Matcher} that checks if a {@link Member} is declared by a specific class.
     *
     * @param type the class to check against the declaring class of the {@link Member}
     * @return a {@link Matcher} that evaluates whether the declaring class of a {@link Member} is the specified class
     */
    public static Matcher<Member> isDeclaredClass(Class<?> type) {
        return new IsDeclaredClassMatcher(type);
    }

    /**
     * An implementation of {@link Matcher} that evaluates whether the declaring class of a {@link Member} matches a specific class.
     */
    private record IsDeclaredClassMatcher(Class<?> type) implements Matcher<Member> {

        /**
         * Checks if the declaring class of the provided {@link Member} matches the specified class.
         *
         * @param member the {@link Member} to evaluate
         * @return {@code true} if the declaring class of the {@link Member} matches the specified class, {@code false} otherwise
         */
        @Override
        public boolean matches(Member member) {
            return TypeMatchers.isSame(type).matches(member.getDeclaringClass());
        }
    }

}
