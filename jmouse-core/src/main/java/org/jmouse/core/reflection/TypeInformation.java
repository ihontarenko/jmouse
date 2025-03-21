package org.jmouse.core.reflection;

import java.util.Objects;

/**
 * Represents type descriptor and provides utility methods for type analysis.
 * <p>
 * This class wraps a {@link Class} and a corresponding {@link JavaType} to facilitate
 * various type-checking operations commonly used in reflection-based processing.
 * </p>
 */
public class TypeInformation implements ClassTypeInspector {

    private final Class<?> rawType;
    private final JavaType type;

    /**
     * Constructs a {@link TypeInformation} from a raw {@link Class}.
     *
     * @param rawType the class type to be wrapped
     */
    public TypeInformation(Class<?> rawType) {
        this.rawType = Objects.requireNonNullElse(rawType, Object.class);
        this.type = JavaType.forType(this.rawType);
    }

    /**
     * Constructs a {@link TypeInformation} from a {@link JavaType}.
     *
     * @param type the {@link JavaType} instance to be wrapped
     */
    public TypeInformation(JavaType type) {
        this.type = type;
        this.rawType = Objects.requireNonNullElse(type.getRawType(), Object.class);
    }

    /**
     * Creates a {@link TypeInformation} for a given raw {@link Class}.
     *
     * @param rawType the class type to wrap
     * @return a new {@link TypeInformation} instance
     */
    public static TypeInformation forClass(Class<?> rawType) {
        return new TypeInformation(rawType);
    }

    /**
     * Creates a {@link TypeInformation} for a given {@link JavaType}.
     *
     * @param type the {@link JavaType} instance to wrap
     * @return a new {@link TypeInformation} instance
     */
    public static TypeInformation forJavaType(JavaType type) {
        return new TypeInformation(type);
    }

    /**
     * Creates a {@link TypeInformation} for a given instance object.
     */
    public static TypeInformation forInstance(Object instance) {
        return new TypeInformation(instance == null ? JavaType.NONE_TYPE : JavaType.forInstance(instance));
    }

    /**
     * Returns the raw class type.
     *
     * @return the underlying class type
     */
    public Class<?> getRawType() {
        return rawType;
    }

    /**
     * Returns the wrapped {@link JavaType} instance.
     *
     * @return the wrapped Java type
     */
    public JavaType getType() {
        return type;
    }

    /**
     * Returns the class type being inspected.
     *
     * @return the {@link Class} structured representing the inspected type
     */
    @Override
    public Class<?> getClassType() {
        return getRawType();
    }

    /**
     * Checks if this type is assignable from another {@link TypeInformation}.
     *
     * @param typeDescriptor the type descriptor to check
     * @return {@code true} if this type is assignable from the given descriptor
     */
    public boolean is(TypeInformation typeDescriptor) {
        return is(typeDescriptor.getRawType());
    }

    @Override
    public String toString() {
        return "TypeDescriptor: [%s]".formatted(type);
    }
}
