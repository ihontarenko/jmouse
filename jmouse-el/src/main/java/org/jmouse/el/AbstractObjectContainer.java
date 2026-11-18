package org.jmouse.el;

import org.jmouse.core.Sorter;

import java.util.*;

/**
 * 🏗️ Abstract implementation of {@link ObjectContainer}, providing a basic storage mechanism
 * for managing extensions using a key-value structure.
 *
 * @param <K> 🔑 the type of the key used to identify extensions
 * @param <E> 🧩 the type of the extension being managed
 *
 * @author Ivan Hontarenko (Mr. Jerry Mouse)
 * @author ihontarenko@gmail.com
 */
public abstract class AbstractObjectContainer<K, E> implements ObjectContainer<K, E> {

    /**
     * 🗂️ Stores registered extensions mapped by their corresponding keys.
     */
    private final Map<K, E> extensions;

    private List<E> sortedValues;

    /**
     * 🏗️ Constructs an empty extension container.
     */
    protected AbstractObjectContainer() {
        this.extensions = new LinkedHashMap<>();
    }

    /**
     * ➕ Registers a new extension into the container.
     *
     * @param extension 🛠️ the extension to register
     */
    @Override
    public synchronized void register(E extension) {
        Objects.requireNonNull(extension, "extension");
        extensions.put(Objects.requireNonNull(keyFor(extension), "extension key"), extension);
        sortedValues = null;
    }

    /**
     * ❌ Removes an extension from the container if it exists.
     *
     * @param extension 🗑️ the extension to remove
     */
    @Override
    public synchronized void remove(E extension) {
        if (contains(extension)) {
            extensions.remove(keyFor(extension));
            sortedValues = null;
        }
    }

    /**
     * 🔍 Retrieves an extension by its key.
     *
     * @param name 🔑 the key associated with the extension
     * @return 🧩 the extension corresponding to the key, or {@code null} if not found
     */
    @Override
    public synchronized E get(K name) {
        return extensions.get(name);
    }

    /**
     * 📜 Returns a list of all stored objects in the container.
     *
     * @return 📦 a list of registered objects
     */
    @Override
    public synchronized List<E> values() {
        if (sortedValues == null) {
            List<E> ordered = new ArrayList<>(extensions.values());
            ordered.sort(Sorter.PRIORITY_COMPARATOR);
            sortedValues = List.copyOf(ordered);
        }

        return sortedValues;
    }

    /**
     * 🔑 Returns a set of all keys currently registered in the container.
     *
     * @return 🗂️ a set of all keys
     */
    @Override
    public synchronized Set<K> keys() {
        return Collections.unmodifiableSet(new LinkedHashSet<>(extensions.keySet()));
    }

    @Override
    public String toString() {
        return "[%d]".formatted(keys().size());
    }
}
