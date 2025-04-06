package org.jmouse.el.core;

import java.util.List;
import java.util.Set;

/**
 * 🎛️ A generic container for managing various types of extensions or objects.
 * This interface provides methods for registering, removing, retrieving, and checking
 * objects using their associated keys.
 *
 * <p>Common use cases:</p>
 * <ul>
 *     <li>Storing functions, operators, parsers, or any other extensions</li>
 *     <li>Quick retrieval of objects by their unique keys</li>
 *     <li>Ensuring uniqueness and easy lookup for stored objects</li>
 * </ul>
 *
 * @param <K> 🔑 the type of the key used to identify objects
 * @param <E> 🧩 the type of the objects/extensions being managed
 *
 * @author Ivan Hontarenko (Mr. Jerry Mouse)
 * @author ihontarenko@gmail.com
 */
public interface ObjectContainer<K, E> {

    /**
     * ➕ Registers a new object in the container.
     *
     * @param extension 🛠️ the object to register
     */
    void register(E extension);

    /**
     * ❌ Removes an object from the container.
     *
     * @param extension 🗑️ the object to remove
     */
    void remove(E extension);

    /**
     * 🔍 Retrieves an object by its key.
     *
     * @param key 🔑 the key associated with the object
     * @return 🧩 the object corresponding to the key, or {@code null} if not found
     */
    E get(K key);

    /**
     * 🏷️ Retrieves the key associated with a given object.
     *
     * @param extension 🧩 the object to retrieve the key for
     * @return 🔑 the key associated with the object
     */
    K keyFor(E extension);

    /**
     * 📜 Returns a list of all stored objects in the container.
     *
     * @return 📦 a list of registered objects
     */
    List<E> values();

    /**
     * 🔑 Returns a set of all keys currently registered in the container.
     *
     * @return 🗂️ a set of all keys
     */
    Set<K> keys();

    /**
     * ✅ Checks whether the given object is present in the container.
     *
     * @param extension 🧩 the object to check
     * @return 🔄 {@code true} if the object is present, otherwise {@code false}
     */
    default boolean contains(E extension) {
        return get(keyFor(extension)) != null;
    }
}
