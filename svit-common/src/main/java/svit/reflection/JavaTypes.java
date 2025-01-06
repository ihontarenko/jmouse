package svit.reflection;

import java.util.Map;

/**
 * A utility class that provides mappings for Java primitive types, their wrapper types,
 * and default values for primitive types.
 */
public class JavaTypes {

    /**
     * A map that provides the default values for Java primitive types.
     * This map is useful when creating instances or initializing variables of primitive types.
     *
     * <ul>
     * <li>boolean: {@code false}</li>
     * <li>byte: {@code 0}</li>
     * <li>short: {@code 0}</li>
     * <li>int: {@code 0}</li>
     * <li>long: {@code 0L}</li>
     * <li>float: {@code 0F}</li>
     * <li>double: {@code 0D}</li>
     * <li>char: {@code '\0'}</li>
     * </ul>
     *
     * @example
     * <pre>{@code
     * // Example: Getting default value for an int
     * Object defaultValue = JavaTypes.PRIMITIVES_DEFAULT_TYPE_VALUES.get(int.class);
     * System.out.println(defaultValue); // prints 0
     * }</pre>
     */
    public static final Map<Class<?>, Object> PRIMITIVES_DEFAULT_TYPE_VALUES = Map.of(
            boolean.class, false,
            byte.class, (byte) 0,
            short.class, (short) 0,
            int.class, 0,
            long.class, 0L,
            float.class, 0F,
            double.class, 0D,
            char.class, '\0'
    );

    /**
     * A map that provides the corresponding wrapper types for Java primitive types.
     * This is useful when you need to convert a primitive type to its wrapper class.
     *
     * <ul>
     * <li>boolean.class: {@link Boolean}</li>
     * <li>byte.class: {@link Byte}</li>
     * <li>short.class: {@link Short}</li>
     * <li>int.class: {@link Integer}</li>
     * <li>long.class: {@link Long}</li>
     * <li>float.class: {@link Float}</li>
     * <li>double.class: {@link Double}</li>
     * <li>char.class: {@link Character}</li>
     * </ul>
     *
     * @example
     * <pre>{@code
     * // Example: Getting the wrapper class for a primitive type
     * Class<?> wrapper = JavaTypes.WRAPPERS.get(int.class);
     * System.out.println(wrapper.getSimpleName()); // prints "Integer"
     * }</pre>
     */
    public static final Map<Class<?>, Class<?>> WRAPPERS = Map.of(
            boolean.class, Boolean.class,
            byte.class, Byte.class,
            short.class, Short.class,
            int.class, Integer.class,
            long.class, Long.class,
            float.class, Float.class,
            double.class, Double.class,
            char.class, Character.class
    );

    /**
     * A map that provides the corresponding primitive types for Java wrapper classes.
     * This is useful when you need to convert a wrapper class to its primitive type.
     *
     * <ul>
     * <li>{@link Boolean}: boolean.class</li>
     * <li>{@link Byte}: byte.class</li>
     * <li>{@link Short}: short.class</li>
     * <li>{@link Integer}: int.class</li>
     * <li>{@link Long}: long.class</li>
     * <li>{@link Float}: float.class</li>
     * <li>{@link Double}: double.class</li>
     * <li>{@link Character}: char.class</li>
     * </ul>
     *
     * @example
     * <pre>{@code
     * // Example: Getting the primitive type for a wrapper class
     * Class<?> primitive = JavaTypes.PRIMITIVES.get(Integer.class);
     * System.out.println(primitive.getSimpleName()); // prints "int"
     * }</pre>
     */
    public static final Map<Class<?>, Class<?>> PRIMITIVES = Map.of(
            Boolean.class, boolean.class,
            Byte.class, byte.class,
            Short.class, short.class,
            Integer.class, int.class,
            Long.class, long.class,
            Float.class, float.class,
            Double.class, double.class,
            Character.class, char.class
    );
}
