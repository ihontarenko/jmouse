package org.jmouse.beans;

import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

public interface Beans<T> {

    Collection<T> getBeans();

    default boolean isEmpty() {
        return getBeans().isEmpty();
    }

    default boolean isNotEmpty() {
        return !isEmpty();
    }

    default int size() {
        if (getBeans() == null) {
            return 0;
        }
        return getBeans().size();
    }

    default T get(int index) {
        if (getBeans() == null) {
            return null;
        }

        if (index < 0 || index >= size()) {
            return null;
        }

        Iterator<T> iterator = getBeans().iterator();
        int         i        = 0;
        while (iterator.hasNext()) {
            T bean = iterator.next();
            if (i == index) {
                return bean;
            }
        }

        return null;
    }

    default T getFirst() {
        return get(0);
    }

    default T getLast() {
        return get(size() - 1);
    }

    class ArrayList<T> implements Beans<T> {

        private final List<T> beans;

        public ArrayList(List<T> beans) {
            this.beans = beans;
        }

        @Override
        public Collection<T> getBeans() {
            return beans;
        }

    }

    class HashSet<T> implements Beans<T> {

        private final Set<T> beans;

        public HashSet(Set<T> beans) {
            this.beans = beans;
        }

        @Override
        public Collection<T> getBeans() {
            return beans;
        }

    }

}
