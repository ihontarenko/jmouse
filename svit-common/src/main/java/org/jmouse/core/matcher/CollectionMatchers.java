package org.jmouse.core.matcher;

import java.util.Collection;

public class CollectionMatchers {

    public static <T> Matcher<Collection<T>> contains(T element) {
        return new CollectionContainsMatcher<>(element);
    }

    public static <T> Matcher<Collection<T>> isEmpty() {
        return new CollectionEmptyMatcher<>();
    }

    public static <T> Matcher<Collection<T>> size(int size) {
        return new CollectionSizeMatcher<>(size);
    }

    public static <T> Matcher<Collection<T>> anyOf(Matcher<? super T> itemMatcher) {
        return new CollectionAnyOfMatcher<>(itemMatcher);
    }

    public static <T> Matcher<Collection<T>> allOf(Matcher<? super T> itemMatcher) {
        return new CollectionAllOfMatcher<>(itemMatcher);
    }

    private record CollectionAnyOfMatcher<T>(Matcher<? super T> itemMatcher) implements Matcher<Collection<T>> {
        @Override
        public boolean matches(Collection<T> collection) {
            for (T item : collection) {
                if (itemMatcher.matches(item)) {
                    return true;
                }
            }
            return false;
        }
    }

    private record CollectionAllOfMatcher<T>(Matcher<? super T> itemMatcher) implements Matcher<Collection<T>> {
        @Override
        public boolean matches(Collection<T> collection) {
            for (T item : collection) {
                if (!itemMatcher.matches(item)) {
                    return true;
                }
            }
            return true;
        }
    }

    private record CollectionContainsMatcher<T>(T element) implements Matcher<Collection<T>> {
        @Override
        public boolean matches(Collection<T> collection) {
            return collection != null && collection.contains(element);
        }
    }

    private static class CollectionEmptyMatcher<T> implements Matcher<Collection<T>> {
        @Override
        public boolean matches(Collection<T> collection) {
            return collection != null && collection.isEmpty();
        }
    }

    private record CollectionSizeMatcher<T>(int expectedSize) implements Matcher<Collection<T>> {
        @Override
        public boolean matches(Collection<T> collection) {
            return collection != null && collection.size() == expectedSize;
        }
    }

}
