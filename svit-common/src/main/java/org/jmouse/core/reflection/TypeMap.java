package org.jmouse.core.reflection;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Map;
import java.util.Set;

/**
 * A utility class that provides mappings for Java primitive types, their wrapper types,
 * and default values for primitive types.
 *
 * <p>This class includes:
 * <ul>
 *   <li>A map of default values for primitive types.</li>
 *   <li>Mappings between primitive types and their corresponding wrapper classes.</li>
 *   <li>Mappings from wrapper classes back to primitive types.</li>
 *   <li>Sets of numeric wrapper and primitive types for quick reference.</li>
 * </ul>
 *
 * <p>These utilities are useful when creating instances, initializing variables of
 * primitive types, or performing type conversions between primitives and wrappers.
 */
public class TypeMap {

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

    /**
     * A set containing wrapper classes for numeric types.
     * This set includes common numeric wrappers and also {@link BigDecimal} and {@link BigInteger}.
     */
    public static final Set<Class<? extends Number>> NUMBER_WRAPPERS = Set.of(
            Integer.class,
            Long.class,
            Double.class,
            Float.class,
            BigDecimal.class,
            BigInteger.class,
            Short.class,
            Byte.class
    );

    /**
     * A set containing primitive numeric types.
     * This set includes common numeric primitives such as int, long, double, float, and short.
     */
    public static final Set<Class<? extends Number>> NUMBER_PRIMITIVES = Set.of(
            int.class,
            long.class,
            double.class,
            float.class,
            short.class,
            byte.class
    );

}
