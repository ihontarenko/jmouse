package svit.reflection;

import java.io.Serializable;
import java.lang.reflect.*;
import java.util.StringJoiner;

/**
 * A utility class that encapsulates reflection-based type information. It can
 * resolve a {@link Type} into its corresponding {@link Class} and provides
 * methods to inspect super types, interfaces, and generic type parameters.
 * <p>
 * The internal mechanics rely on {@link #resolveClass()} to determine the
 * concrete class, if possible. For parameterized types, it can extract
 * generic type arguments via {@link #getGenerics()}. If the type is not
 * parameterized or cannot be resolved (e.g., wildcards, generic arrays), it may
 * throw an exception or return partially populated data.
 * <p>
 * Common use cases include:
 * <ul>
 *   <li>Analyzing parent/child type relationships at runtime</li>
 *   <li>Determining generic parameters for dependency injection frameworks</li>
 *   <li>Inspecting classes for reflective or dynamic proxy logic</li>
 * </ul>
 */
public class ReflectType {

    /**
     * A sentinel instance representing a "no type" scenario (e.g., for empty or
     * unresolvable types).
     */
    private static final ReflectType NONE_TYPE = new ReflectType(EmptyType.INSTANCE);

    /**
     * A constant for an empty array of {@code ReflectiveType}.
     */
    private static final ReflectType[] EMPTY_TYPE_ARRAY = {};

    /**
     * The underlying {@link Type} this instance represents. Could be a {@link Class},
     * {@link ParameterizedType}, or {@link EmptyType}.
     */
    private final Type type;

    /**
     * The cached concrete {@link Class} if this {@link Type} can be resolved; otherwise
     * {@code null} until it is lazily resolved by {@link #resolveClass()}.
     */
    private Class<?> klass;

    /**
     * A lazily initialized {@link ReflectType} for the super type of {@link #klass}.
     */
    private volatile ReflectType superType;

    /**
     * A lazily initialized array of {@link ReflectType} representing the interfaces
     * implemented by {@link #klass}.
     */
    private volatile ReflectType[] interfaces;

    /**
     * A lazily initialized array of {@link ReflectType} representing the generic type
     * parameters of {@link #type} if it is a {@link ParameterizedType}.
     */
    private ReflectType[] generics;

    /**
     * Constructs a {@code ReflectiveType} for the given class. If {@code klass} is {@code null},
     * it defaults to {@link Object}.
     *
     * @param klass the class to represent, or {@code null} to default to {@link Object}
     */
    public ReflectType(Class<?> klass) {
        this.klass = (klass == null) ? Object.class : klass;
        this.type = this.klass;
    }

    /**
     * Constructs a {@code ReflectiveType} for the given type. If the type is {@link EmptyType#INSTANCE},
     * it represents a no-type scenario.
     *
     * @param type the type to represent
     */
    public ReflectType(Type type) {
        this.type = type;
        resolveClass();
    }

    /**
     * Returns the resolved {@link Class} for this type, or resolves it if it has not yet been done.
     *
     * @return the corresponding class, or {@code null} if it cannot be resolved
     */
    public Class<?> getClassType() {
        return klass;
    }

    /**
     * Returns the raw {@link Type} that this {@code ReflectiveType} represents, which may be a
     * {@link Class}, {@link ParameterizedType}, or {@link EmptyType}.
     *
     * @return the underlying type
     */
    public Type getType() {
        return type;
    }

    /**
     * Resolves and returns the concrete class for this type, if possible. If the type is
     * {@link EmptyType#INSTANCE}, it returns {@code null}. If the type is already resolved,
     * no action is taken.
     *
     * @return the resolved class, or {@code null} if the type is empty
     */
    public Class<?> resolveClass() {
        if (type == EmptyType.INSTANCE) {
            return null;
        }

        if (klass == null) {
            if (type instanceof Class<?>) {
                this.klass = (Class<?>) type;
            } else if (type instanceof GenericArrayType arrayType) {
                // Generic arrays are currently not fully supported
                throw new UnsupportedOperationException("Generic array types are not supported");
            } else {
                this.klass = resolveType().resolveClass();
            }
        }

        return klass;
    }

    /**
     * Resolves the raw type if this is a {@link ParameterizedType}, otherwise returns
     * {@link #NONE_TYPE}.
     *
     * @return a {@link ReflectType} for the raw type if this is parameterized,
     *         or {@code NONE_TYPE} otherwise
     */
    public ReflectType resolveType() {
        ReflectType reflectiveType = NONE_TYPE;

        if (type instanceof ParameterizedType parameterizedType) {
            reflectiveType = forType(parameterizedType.getRawType());
        }

        return reflectiveType;
    }

    /**
     * Returns the {@link ReflectType} representing the super type of this class. If the class
     * has not been resolved yet, it is lazily resolved by calling {@link #resolveClass()}.
     *
     * @return the super type, or a {@code ReflectiveType} representing {@code Object} if none exists
     */
    public ReflectType getSuperType() {
        ReflectType localSuperType = this.superType;

        if (localSuperType == null) {
            Class<?> klass = resolveClass();
            if (klass != null) {
                this.superType = forType(klass.getGenericSuperclass());
            }
        }

        return this.superType;
    }

    /**
     * Returns an array of {@link ReflectType} representing all the interfaces
     * implemented by this class. If the class is not resolvable, or there are no
     * interfaces, an empty array is returned.
     *
     * @return an array of interface types, or an empty array if none
     */
    public ReflectType[] getInterfaces() {
        Class<?> localClass = getClassType();

        if (localClass == null) {
            return EMPTY_TYPE_ARRAY;
        }

        if (this.interfaces == null) {
            Type[]        ifaces = localClass.getGenericInterfaces();
            ReflectType[] result = new ReflectType[ifaces.length];

            for (int i = 0; i < ifaces.length; i++) {
                result[i] = forType(ifaces[i]);
            }

            this.interfaces = result;
        }

        return this.interfaces;
    }

    /**
     * Returns an array of {@link ReflectType} representing the generic type parameters
     * of this {@link #type} if it is a {@link ParameterizedType}. Otherwise, an empty array
     * is returned.
     *
     * @return an array of generic parameter types, or an empty array if none
     */
    public ReflectType[] getGenerics() {
        ReflectType[] localGenerics = this.generics;

        if (localGenerics == null || localGenerics.length == 0) {
            if (this.type instanceof ParameterizedType parameterizedType) {
                Type[]        actualTypes = parameterizedType.getActualTypeArguments();
                ReflectType[] result      = new ReflectType[actualTypes.length];

                for (int i = 0; i < actualTypes.length; i++) {
                    result[i] = forType(actualTypes[i]);
                }

                this.generics = result;
            } else {
                this.generics = EMPTY_TYPE_ARRAY;
            }
        }

        return this.generics;
    }

    /**
     * Returns a nested generic type by following the specified sequence of indexes.
     * For example, calling {@code getGeneric(0, 1)} first gets the 0th generic type,
     * then from that type, the 1st generic type, and so on. If no matching generic
     * is found at any step, returns a sentinel {@link #NONE_TYPE}.
     *
     * @param sequence an array of indexes specifying which generic to traverse
     * @return the matching generic type, or {@code NONE_TYPE} if out of bounds
     */
    public ReflectType getGeneric(int... sequence) {
        ReflectType[] types = getGenerics();
        ReflectType   generic;

        if (sequence == null || sequence.length == 0) {
            generic = (types.length == 0) ? NONE_TYPE : types[0];
        } else {
            generic = this;
            for (int index : sequence) {
                ReflectType[] generics = generic.getGenerics();

                if (index < 0 || index >= generics.length) {
                    return NONE_TYPE;
                }

                generic = generics[index];
            }
        }

        return generic;
    }

    /**
     * Attempts to navigate from this {@code ReflectType} to the specified {@code classType}
     * by searching the current class, its interfaces, and its super types. If it finds
     * a match, returns the corresponding {@code ReflectType}; otherwise returns {@link #NONE_TYPE}.
     *
     * @param classType the target class to navigate toward
     * @return a {@code ReflectType} representing the path to {@code classType}, or {@code NONE_TYPE} if not found
     */
    public ReflectType navigate(Class<?> classType) {
        ReflectType targetType = NONE_TYPE;

        if (this != NONE_TYPE) {
            // If we haven't resolved the class or if we've found a direct match
            if (this.klass == null || this.klass == classType) {
                targetType = this;
            } else {
                // Search interfaces
                for (ReflectType iface : getInterfaces()) {
                    targetType = iface.navigate(classType);
                    if (targetType != NONE_TYPE) {
                        break;
                    }
                }
                // Search super type if not found in interfaces
                if (targetType == NONE_TYPE) {
                    targetType = getSuperType().navigate(classType);
                }
            }
        }

        return targetType;
    }

    /**
     * Creates a new {@code ReflectiveType} for the given {@link Class}.
     *
     * @param klass the class to represent
     * @return a {@code ReflectiveType} wrapping the given class
     */
    public static ReflectType forClass(Class<?> klass) {
        return new ReflectType(klass);
    }

    /**
     * Creates a new {@code ReflectiveType} for the given {@link Type}.
     *
     * @param type the type to represent
     * @return a {@code ReflectiveType} wrapping the given type
     */
    public static ReflectType forType(Type type) {
        return new ReflectType(type);
    }

    /**
     * Creates a new {@code ReflectiveType} for the class or type of the given object.
     * If the object is itself a {@code Class}, it uses that; otherwise, it uses the
     * object's runtime class.
     *
     * @param object the object whose type to represent
     * @return a {@code ReflectiveType} for the object's class or type
     */
    public static ReflectType forInstance(Object object) {
        return (object instanceof Class<?> klass)
                ? forType(klass)
                : forType(object.getClass());
    }

    /**
     * Creates a new {@code ReflectiveType} representing the generic type of a field.
     *
     * @param field the field whose type is to be represented
     * @return a {@code ReflectiveType} for the field's generic type
     */
    public static ReflectType forField(Field field) {
        return new ReflectType(field.getGenericType());
    }

    /**
     * Creates a new {@code ReflectiveType} representing the return type of a method.
     *
     * @param method the method whose return type is to be represented
     * @return a {@code ReflectiveType} for the method's generic return type
     */
    public static ReflectType forMethodReturnType(Method method) {
        return new ReflectType(method.getGenericReturnType());
    }

    /**
     * Returns a string representation of the object.
     */
    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();

        if (klass != null) {
            builder.append(klass.getSimpleName());

            ReflectType[] generics = getGenerics();

            if (generics.length > 0) {
                StringJoiner joiner = new StringJoiner(", ", "<", ">");

                for (ReflectType generic : generics) {
                    joiner.add(generic.toString());
                }

                builder.append(joiner);
            }
        } else {
            builder.append('?');
        }

        return builder.toString();
    }

    /**
     * A singleton class representing a "no type" scenario. This is used internally
     * when no actual type information is available.
     */
    static class EmptyType implements Type, Serializable {
        /**
         * The single instance of this empty type.
         */
        static final Type INSTANCE = new EmptyType();

        /**
         * Ensures that deserialization always returns the single static instance.
         *
         * @return the singleton {@link #INSTANCE}
         */
        Object readResolve() {
            return INSTANCE;
        }
    }

}
