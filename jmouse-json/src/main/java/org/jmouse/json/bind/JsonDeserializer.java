package org.jmouse.json.bind;

/**
 * 🧾 How a type is read from JSON: from the value JSON gives — a map, a list, a number, a string, a truth value — a part of it
 * read into its own type by {@link JsonDeserialization#read}, so that part is read by its deserializer, or bound, in turn.
 *
 * @param <T> the type it reads
 *
 * @author Ivan Hontarenko (Mr. Jerry Mouse)
 * @author ihontarenko@gmail.com
 */
@FunctionalInterface
public interface JsonDeserializer<T> {

    T deserialize(Object json, JsonDeserialization deserialization);
}
