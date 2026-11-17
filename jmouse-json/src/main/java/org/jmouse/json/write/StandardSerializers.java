package org.jmouse.json.write;

import org.jmouse.core.access.descriptor.Describer;
import org.jmouse.core.access.descriptor.structured.ObjectDescriptor;
import org.jmouse.core.access.descriptor.structured.PropertyDescriptor;

import java.lang.reflect.Array;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Map;
import java.util.Optional;

/**
 * 🧾 How each kind of value is written unless a mapper says otherwise: truth values, numbers, text, characters and enum
 * constants as JSON's scalars; a map an object, an iterable or an array an array, an {@link Optional} its value or null; a
 * record or a bean an object of its readable properties, as the core describes it; and a value of the JDK no serializer reaches
 * — a date, a URI, a UUID — the string of its text.
 *
 * @author Ivan Hontarenko (Mr. Jerry Mouse)
 * @author ihontarenko@gmail.com
 */
public final class StandardSerializers {

    /** The packages of the JDK: a class of them no serializer reaches is written as its text, never read as a bean. */
    private static final String[] JDK_PACKAGES = {"java.", "javax.", "jdk.", "sun."};

    private StandardSerializers() {
    }

    /** A registry of every standard serializer. */
    public static JsonSerializers registered() {
        JsonSerializers serializers = new JsonSerializers(StandardSerializers::array, StandardSerializers::object);

        serializers.register(Boolean.class, (value, output) -> output.truth(value));
        serializers.register(Number.class, StandardSerializers::number);
        serializers.register(CharSequence.class, (value, output) -> output.text(value.toString()));
        serializers.register(Character.class, (value, output) -> output.text(value.toString()));
        serializers.register(Enum.class, (value, output) -> output.text(value.name()));
        serializers.register(Map.class, StandardSerializers::map);
        serializers.register(Iterable.class, (value, output) -> output.array(elements -> value.forEach(elements::element)));
        serializers.register(Optional.class, (value, output) -> output.value(value.orElse(null)));

        return serializers;
    }

    private static void number(Number value, JsonOutput output) {
        switch (value) {
            case Double number -> output.number(number);
            case Float number -> output.number(number.doubleValue());
            case BigDecimal number -> output.number(number);
            case BigInteger number -> output.number(number);
            default -> output.number(value.longValue());
        }
    }

    private static void map(Map<?, ?> value, JsonOutput output) {
        output.object(members -> value.forEach((name, member) -> members.member(String.valueOf(name), member)));
    }

    private static void array(Object value, JsonOutput output) {
        output.array(elements -> {
            for (int index = 0; index < Array.getLength(value); index++) {
                elements.element(Array.get(value, index));
            }
        });
    }

    /** A record or a bean as an object of its readable properties; a value of the JDK as its text. */
    private static void object(Object value, JsonOutput output) {
        if (isOfTheJdk(value.getClass())) {
            output.text(value.toString());
            return;
        }

        @SuppressWarnings("unchecked")
        ObjectDescriptor<Object> descriptor = (ObjectDescriptor<Object>) Describer.forObjectDescriptor(value.getClass());

        output.object(members -> {
            for (PropertyDescriptor<Object> property : descriptor.getProperties().values()) {
                if (property.isReadable()) {
                    members.member(property.getName(), property.getGetter().get(value));
                }
            }
        });
    }

    private static boolean isOfTheJdk(Class<?> type) {
        String name = type.getName();
        for (String prefix : JDK_PACKAGES) {
            if (name.startsWith(prefix)) {
                return true;
            }
        }
        return false;
    }
}
