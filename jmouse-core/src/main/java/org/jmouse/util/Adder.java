package org.jmouse.util;

import java.util.List;

public interface Adder<T, V> {

    void add(T instance, V value);

    static <V> Adder<List<V>, V> ofList() {
        return List::add;
    }

}
