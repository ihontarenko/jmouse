package org.jmouse.core.reflection;

import org.jmouse.util.helper.Arrays;

import java.lang.reflect.*;
import java.util.*;
import java.util.function.*;

/**
 * A utility class for inspecting, resolving, and navigating Java {@link Type} objects.
 * <p>
 * This class simplifies the process of working with Java's reflection API for type handling, enabling
 * efficient resolution and navigation of types such as:
 * <ul>
 *   <li>{@link Class} - raw types</li>
 *   <li>{@link ParameterizedType} - generics</li>
 *   <li>{@link TypeVariable} - generic parameters</li>
 *   <li>{@link WildcardType} - wildcard bounds</li>
 *   <li>{@link GenericArrayType} - generic arrays</li>
 * </ul>
 * <p>
 */
public class JavaType {

    /**
     * A cache for storing resolved {@link JavaType} instances to avoid redundant resolution.
     */
    private static final Map<Type, JavaType> CACHE = new HashMap<>();

    /**
     * A constant representing a "no type" scenario.
     */
    public static final JavaType NONE_TYPE = new JavaType(NoneType.INSTANCE);

    /**
     * A constant representing an empty array of {@link JavaType}.
     */
    private static final JavaType[] EMPTY_TYPE_ARRAY = {NONE_TYPE};

    /**
     * The parent {@link JavaType} in the type hierarchy.
     */
    private final JavaType parent;

    /**
     * The underlying {@link Type} represented by this {@link JavaType}.
     */
    private final Type type;

    /**
     * The resolved raw class for this {@link JavaType}.
     */
    private Class<?> rawType;

    /**
     * Lazily resolved {@link JavaType} representing the superclass.
     */
    private volatile JavaType superType;

    /**
     * Lazily resolved array of {@link JavaType} instances representing interfaces.
     */
    private volatile JavaType[] interfaces;

    /**
     * An array of {@link JavaType} representing generic type arguments.
     */
    private JavaType[] generics;

    /**
     * The cached hash code for this {@link JavaType}.
     */
    private int hashCode;

    /**
     * Constructs a new {@link JavaType} with a given type and optional parent context.
     *
     * @param type   the {@link Type} to represent
     * @param parent the parent {@link JavaType} in the hierarchy, or null if none
     */
    private JavaType(Type type, JavaType parent) {
        this.type = type;
        this.parent = parent;

        this.hashCode = calculateHashCode();

        resolveClass();
    }

    /**
     * Constructs a new {@link JavaType} with a given type.
     *
     * @param type the {@link Type} to represent
     */
    private JavaType(Type type) {
        this(type, null);
    }

    /**
     * Creates a {@link JavaType} instance from a {@link TypeReference}.
     *
     * @param type the {@link TypeReference} representing the desired type
     * @return a {@link JavaType} instance corresponding to the given type reference
     */
    public static JavaType forTypeReference(TypeReference<?> type) {
        return forType(type.getType());
    }

    /**
     * Returns a {@link JavaType} instance for the given {@link Class}.
     *
     * @param klass the class to wrap
     * @return a {@link JavaType} instance
     */
    public static JavaType forClass(Class<?> klass) {
        return forType(klass);
    }

    /**
     * Returns a {@link JavaType} instance for a parameterized class with specified generic arguments.
     *
     * @param klass    the raw class type
     * @param generics the generic type arguments
     * @return a {@link JavaType} instance representing the parameterized class
     * @throws IllegalArgumentException if the number of provided generic arguments does not match the expected number
     */
    public static JavaType forParametrizedClass(Class<?> klass, Class<?>... generics) {
        Type[] variables = klass.getTypeParameters();

        if (Arrays.notEmpty(generics) && variables.length != generics.length) {
            throw new IllegalArgumentException(
                    "Mismatched parameters types. Expected %d but got %d".formatted(variables.length, generics.length));
        }

        return forType(new SyntheticType(klass, generics));
    }

    /**
     * Returns a {@link JavaType} instance for the given {@link Type}.
     *
     * @param type the type to wrap
     * @return a {@link JavaType} instance
     */
    public static JavaType forType(Type type) {
        return forType(type, null);
    }

    /**
     * Returns a {@link JavaType} instance for the given {@link Type} and parent context.
     *
     * @param type   the type to wrap
     * @param parent the parent {@link JavaType} in the hierarchy
     * @return a {@link JavaType} instance
     */
    public static JavaType forType(Type type, JavaType parent) {
        JavaType instance = CACHE.get(type);

        if (instance == null) {
            instance = new JavaType(type, parent);
            CACHE.put(type, instance);
        }

        return instance;
    }

    /**
     * Creates a {@link JavaType} for an instance of an bean.
     *
     * @param object the bean instance
     * @return a {@link JavaType} instance
     */
    public static JavaType forInstance(Object object) {
        return (object instanceof Class<?> klass)
                ? forType(klass)
                : forType(object.getClass());
    }

    /**
     * Creates a {@link JavaType} for the generic type of a {@link Field}.
     *
     * @param field the field whose type is to be resolved
     * @return a {@link JavaType} instance for the field type
     * <p>
     * @see Field#getGenericType()
     */
    public static JavaType forField(Field field) {
        return forType(field.getGenericType());
    }

    /**
     * Creates a {@link JavaType} for the return type of a {@link Method}.
     *
     * @param method the method whose return type is to be resolved
     * @return a {@link JavaType} instance for the return type
     */
    public static JavaType forMethodReturnType(Method method) {
        return forType(method.getGenericReturnType());
    }

    /**
     * Creates a {@link JavaType} for a specific parameter of a given {@link Method}.
     *
     * @param method the method whose parameter type is to be resolved
     * @param index  the index of the parameter (zero-based)
     * @return a {@link JavaType} instance representing the parameter type at the given index
     * @throws IllegalArgumentException if the method does not have a parameter at the specified index
     */
    public static JavaType forMethodParameter(Method method, int index) {
        Type[] parameters = method.getGenericParameterTypes();

        if (parameters.length > index) {
            return forType(parameters[index]);
        }

        throw new IllegalArgumentException(
                "Method '%s' has no parameter '%d' index".formatted(Reflections.getMethodName(method), index));
    }

    /**
     * Retrieves the raw {@link Class} for this {@link JavaType}.
     *
     * @return the raw {@link Class}, or null if the type cannot be resolved
     */
    public <T> Class<T> getRawType() {
        return (Class<T>) rawType;
    }

    /**
     * Retrieves the underlying {@link Type} represented by this {@link JavaType}.
     *
     * @return the {@link Type} instance
     */
    public Type getType() {
        return type;
    }

    /**
     * Resolves and returns the raw {@link Class} for this {@link JavaType}.
     *
     * @return the resolved {@link Class}, or null if the type is {@link NoneType}
     * <p>
     * This method supports resolving raw types, arrays, and generic components by analyzing the underlying {@link Type}.
     * If the type is a {@link GenericArrayType}, its component type is recursively resolved.
     * @see GenericArrayType
     * @see JavaType#getComponentType()
     */
    public Class<?> resolveClass() {
        if (type == NoneType.INSTANCE) {
            return null;
        }

        if (rawType == null) {
            if (isRawType()) {
                this.rawType = (Class<?>) type;
            } else if (isArray()) {
                this.rawType = getComponentType().resolveClass();
            } else {
                this.rawType = resolveType().resolveClass();
            }
        }

        return rawType;
    }

    /**
     * Resolves the type hierarchy and returns the {@link JavaType}.
     *
     * @return the resolved {@link JavaType}, or {@link JavaType#NONE_TYPE} if unresolved
     * <p>
     * This method supports resolving various type categories, including:
     * <ul>
     *   <li>{@link ParameterizedType} - Resolves to the raw type.</li>
     *   <li>{@link TypeVariable} - Attempts to resolve within the current context or bounds.</li>
     *   <li>{@link WildcardType} - Resolves upper or lower bounds.</li>
     * </ul>
     * <p>
     * @see Type
     * @see JavaType#NONE_TYPE
     */
    public JavaType resolveType() {
        return switch (this.type) {
            case ParameterizedType parameterizedType
                    -> forType(parameterizedType.getRawType());
            case TypeVariable<?> typeVariable -> {
                JavaType variable = NONE_TYPE;

                if (parent != null) {
                    variable = parent.resolveVariable(typeVariable);
                } else if (Arrays.notEmpty(typeVariable.getBounds())) {
                    variable = forType(typeVariable.getBounds()[0], this);
                }

                yield variable;
            }
            case WildcardType wildcardType -> {
                // extends - upper bounds
                // super - lower bounds
                JavaType wildcard = NONE_TYPE;

                if (Arrays.notEmpty(wildcardType.getUpperBounds())) {
                    wildcard = forType(wildcardType.getUpperBounds()[0], parent);
                } else if (Arrays.notEmpty(wildcardType.getLowerBounds())) {
                    wildcard = forType(wildcardType.getLowerBounds()[0], parent);
                }

                yield wildcard;
            }
            default -> NONE_TYPE;
        };
    }

    /**
     * Resolves a type variable within the current context.
     *
     * @param variable the {@link TypeVariable} to resolve
     * @return the resolved {@link JavaType}, or {@link JavaType#NONE_TYPE} if unresolved
     * <p>
     * This method evaluates the type parameters of the raw type and attempts to match the given {@link TypeVariable}.
     * If the variable cannot be resolved in the current context, it defaults to {@link JavaType#NONE_TYPE}.
     * @see TypeVariable
     */
    public JavaType resolveVariable(TypeVariable<?> variable) {
        JavaType type         = null;
        String   expectedName = variable.getName();

        if (isParameterizedType()) {
            int counter = 0;
            for (TypeVariable<? extends Class<?>> typeVariable : getRawType().getTypeParameters()) {
                String actualName = typeVariable.getName();
                if (expectedName.equals(actualName)) {
                    type = getGeneric(counter);
                    break;
                }
                counter++;
            }
        }

        return Objects.requireNonNullElse(type, NONE_TYPE);
    }

    /**
     * Retrieves the {@link JavaType} representing the component type of an array.
     *
     * @return the component {@link JavaType}, or {@link JavaType#NONE_TYPE} if not an array
     * <p>
     * This method supports resolving both generic arrays (via {@link GenericArrayType}) and raw arrays.
     * @see GenericArrayType
     */
    public JavaType getComponentType() {
        JavaType type = NONE_TYPE;

        if (this.type instanceof GenericArrayType arrayType) {
            type = forType(arrayType.getGenericComponentType());
        } else if (this.type instanceof Class<?> classType) {
            type = forType(classType.componentType());
        }

        return type;
    }

    /**
     * Checks if this {@link JavaType} has been resolved.
     *
     * @return true if resolved, false otherwise
     * @see JavaType#isUnresolved()
     */
    public boolean isResolved() {
        return getRawType() != null;
    }

    /**
     * Checks if this {@link JavaType} is unresolved.
     *
     * @return true if unresolved, false otherwise
     */
    public boolean isUnresolved() {
        return !isResolved();
    }

    /**
     * Checks if this {@link JavaType} represents an array.
     *
     * @return true if it represents an array, false otherwise
     * <p>
     * This method handles both raw arrays (e.g., {@code int[]}) and generic array types
     * (e.g., {@code T[]}, where {@code T} is a type variable).
     * @see JavaType#getComponentType()
     * @see JavaType#isGenericArrayType()
     */
    public boolean isArray() {
        return (type instanceof Class<?> clazz && clazz.isArray()) || isGenericArrayType();
    }

    /**
     * Checks if this {@link JavaType} represents a parameterized type.
     *
     * @return true if it is a parameterized type, false otherwise
     * <p>
     * A parameterized type is a type that includes type arguments (e.g., {@code List<String>}).
     * @see ParameterizedType
     */
    public boolean isParameterizedType() {
        return this.type instanceof ParameterizedType;
    }

    /**
     * Checks if this {@link JavaType} represents a wildcard type.
     *
     * @return true if it is a wildcard type, false otherwise
     * <p>
     * Wildcard types are used to represent type parameters with bounds (e.g., {@code ? extends Number} or {@code ? super Integer}).
     * @see WildcardType
     */
    public boolean isWildcardType() {
        return this.type instanceof WildcardType;
    }

    /**
     * Checks if this {@link JavaType} represents a generic array type.
     *
     * @return true if it is a generic array type, false otherwise
     * <p>
     * A generic array type is an array with a generic component type (e.g., {@code T[]}, where {@code T} is a type variable).
     * @see GenericArrayType
     */
    public boolean isGenericArrayType() {
        return this.type instanceof GenericArrayType;
    }

    /**
     * Checks if this {@link JavaType} represents a raw type.
     *
     * @return true if it is a raw type, false otherwise
     * <p>
     * A raw type is a class or interface without type arguments (e.g., {@code List} instead of {@code List<String>}).
     * @see Class
     */
    public boolean isRawType() {
        return this.type instanceof Class<?>;
    }

    /**
     * Checks if this {@link JavaType} represents a type variable.
     *
     * @return true if it is a type variable, false otherwise
     * <p>
     * A type variable is a generic type parameter declared in a generic class, interface, or method
     * (e.g., {@code T} in {@code class UserClass<T>}).
     * @see TypeVariable
     */
    public boolean isTypeVariable() {
        return this.type instanceof TypeVariable<?>;
    }


    /**
     * Resolves the superclass of the raw type.
     *
     * @return the {@link JavaType} representing the superclass, or {@link JavaType#NONE_TYPE} if none exists
     * <p>
     * This method handles the resolution of generic superclasses if they exist and caches the result for efficiency.
     * @see Class#getGenericSuperclass()
     */
    public JavaType getSuperType() {
        JavaType superType = this.superType;

        if (superType == null) {
            superType = NONE_TYPE;

            Class<?> rawType = getRawType();

            if (rawType != null && !rawType.isInterface()) {
                Type genericSuperclass = rawType.getGenericSuperclass();
                if (genericSuperclass != null) {
                    superType = forType(genericSuperclass, this);
                }
            }

            this.superType = superType;
        }

        return this.superType;
    }

    /**
     * Retrieves the interfaces implemented by the raw type as an array of {@link JavaType}.
     *
     * @return an array of {@link JavaType} representing the interfaces
     * <p>
     * This method resolves generic interfaces implemented by the raw type and lazily caches the result.
     * @see Class#getGenericInterfaces()
     */
    public JavaType[] getInterfaces() {
        Class<?> rawType = getRawType();

        if (rawType == null) {
            return EMPTY_TYPE_ARRAY;
        }

        if (this.interfaces == null) {
            Type[]     interfaces = rawType.getGenericInterfaces();
            JavaType[] types      = new JavaType[interfaces.length];

            for (int i = 0; i < interfaces.length; i++) {
                types[i] = forType(interfaces[i], this);
            }

            this.interfaces = types;
        }

        return this.interfaces;
    }

    /**
     * Retrieves the type parameters associated with this {@link JavaType}.
     *
     * @return an array of {@link Type} representing the type parameters, or an empty array if none exist
     * <p>
     * This method handles type parameters for various {@link Type} categories:
     * <ul>
     *   <li>For {@link ParameterizedType}, it returns the actual type arguments.</li>
     *   <li>For {@link WildcardType}, it returns the upper or lower bounds.</li>
     *   <li>For raw types, it returns the type parameters declared on the class or interface.</li>
     * </ul>
     * <p>
     *
     * @see ParameterizedType#getActualTypeArguments()
     * @see WildcardType#getUpperBounds()
     * @see WildcardType#getLowerBounds()
     * @see Class#getTypeParameters()
     */
    public Type[] getTypeParameters() {
        Type[] types = {};

        if (isParameterizedType()) {
            types = ((ParameterizedType) type).getActualTypeArguments();
        } else if (isWildcardType()) {
            WildcardType wildcardType = (WildcardType) type;
            if (Arrays.notEmpty(wildcardType.getUpperBounds())) {
                types = wildcardType.getUpperBounds();
            } else if (Arrays.notEmpty(wildcardType.getLowerBounds())) {
                types = wildcardType.getLowerBounds();
            }
        } else if (isRawType()) {
            types = getRawType().getTypeParameters();
        }

        return types;
    }

    /**
     * Retrieves the generic type arguments of this {@link JavaType}.
     *
     * @return an array of {@link JavaType} representing the generics
     * <p>
     * This method resolves the generic type arguments based on the {@link Type} and the current context.
     * @see ParameterizedType#getActualTypeArguments()
     */
    public JavaType[] getGenerics() {
        JavaType[] generics = this.generics;

        if (Arrays.empty(generics)) {
            generics = EMPTY_TYPE_ARRAY;

            if (isParameterizedType() || isRawType()) {
                Type[]     types = getTypeParameters();
                JavaType[] array = new JavaType[types.length];

                for (int i = 0; i < types.length; i++) {
                    JavaType type = forType(types[i], parent);

                    // if type is TypeVariable or WildcardType, but it don't resolve
                    if ((type.isTypeVariable() || type.isWildcardType()) && type.isResolved()) {
                        type = type.resolveType();
                    }

                    array[i] = type;
                }

                generics = array;
            }

            this.generics = generics;
        }

        return this.generics;
    }

    /**
     * Retrieves a specific {@link JavaType} from the sequence of generics.
     *
     * @param sequence the sequence of indexes representing the desired generic type
     * @return the {@link JavaType} at the specified sequence, or {@link JavaType#NONE_TYPE} if not found
     * <p>
     * This method navigates through the generic type arguments to retrieve a nested generic type based on the provided sequence.
     * <ul>
     *   <li>If the sequence is empty or null, it returns the first generic type or {@link JavaType#NONE_TYPE} if none exist.</li>
     *   <li>If the sequence points to an invalid index, it returns {@link JavaType#NONE_TYPE}.</li>
     * </ul>
     * <p>
     *
     * @see JavaType#getGenerics()
     * @see JavaType#NONE_TYPE
     */
    public JavaType getGeneric(int... sequence) {
        JavaType[] types = getGenerics();
        JavaType   generic;

        if (sequence == null || sequence.length == 0) {
            generic = (types.length == 0) ? NONE_TYPE : types[0];
        } else {
            generic = this;
            for (int index : sequence) {
                JavaType[] generics = generic.getGenerics();

                if (index < 0 || index >= generics.length) {
                    return NONE_TYPE;
                }

                generic = generics[index];
            }
        }

        return generic;
    }

    /**
     * Retrieves the first generic type from the list of generics.
     *
     * @return the first {@link JavaType} in the generics list, or {@link JavaType#NONE_TYPE} if no generics are present
     * <p>
     * This method is a shorthand for accessing the first generic type using {@link #getGeneric(int...)}.
     *
     * @see JavaType#getGeneric(int...)
     */
    public JavaType getFirst() {
        return getGeneric(0);
    }

    /**
     * Retrieves the last generic type from the list of generics.
     *
     * @return the last {@link JavaType} in the generics list, or {@link JavaType#NONE_TYPE} if no generics are present
     * <p>
     * This method is a shorthand for accessing the last generic type using {@link #getGeneric(int...)}.
     *
     * @see JavaType#getGeneric(int...)
     * @see JavaType#getGenerics()
     */
    public JavaType getLast() {
        return getGeneric(getGenerics().length - 1);
    }

    /**
     * Navigates the type hierarchy to locate {@link Map}-related classes or interfaces.
     *
     * @return the matching {@link JavaType} for {@link Map}, or {@link JavaType#NONE_TYPE} if not found
     * @see JavaType#locate(Class)
     */
    public JavaType toMap() {
        return locate(Map.class);
    }

    /**
     * Navigates the type hierarchy to locate {@link List}-related classes or interfaces.
     *
     * @return the matching {@link JavaType} for {@link List}, or {@link JavaType#NONE_TYPE} if not found
     * @see JavaType#locate(Class)
     */
    public JavaType toList() {
        return locate(List.class);
    }

    /**
     * Navigates the type hierarchy to locate {@link Set}-related classes or interfaces.
     *
     * @return the matching {@link JavaType} for {@link Set}, or {@link JavaType#NONE_TYPE} if not found
     * @see JavaType#locate(Class)
     */
    public JavaType toSet() {
        return locate(Set.class);
    }

    /**
     * Navigates the type hierarchy to locate {@link Collection}-related classes or interfaces.
     *
     * @return the matching {@link JavaType} for {@link Collection}, or {@link JavaType#NONE_TYPE} if not found
     * @see JavaType#locate(Class)
     */
    public JavaType toCollection() {
        return locate(Collection.class);
    }

    /**
     * Navigates the type hierarchy to locate {@link Function}-related classes or interfaces.
     *
     * @return the matching {@link JavaType} for {@link Function}, or {@link JavaType#NONE_TYPE} if not found
     * @see JavaType#locate(Class)
     */
    public JavaType toFunction() {
        return locate(Function.class);
    }

    /**
     * Navigates the type hierarchy to locate {@link BiFunction}-related classes or interfaces.
     *
     * @return the matching {@link JavaType} for {@link BiFunction}, or {@link JavaType#NONE_TYPE} if not found
     * @see JavaType#locate(Class)
     */
    public JavaType toBiFunction() {
        return locate(BiFunction.class);
    }

    /**
     * Navigates the type hierarchy to locate {@link BiPredicate}-related classes or interfaces.
     *
     * @return the matching {@link JavaType} for {@link BiPredicate}, or {@link JavaType#NONE_TYPE} if not found
     * @see JavaType#locate(Class)
     */
    public JavaType toBiPredicate() {
        return locate(BiPredicate.class);
    }

    /**
     * Navigates the type hierarchy to locate {@link Predicate}-related classes or interfaces.
     *
     * @return the matching {@link JavaType} for {@link Predicate}, or {@link JavaType#NONE_TYPE} if not found
     * @see JavaType#locate(Class)
     */
    public JavaType toPredicate() {
        return locate(Predicate.class);
    }

    /**
     * Navigates the type hierarchy to locate {@link Supplier}-related classes or interfaces.
     *
     * @return the matching {@link JavaType} for {@link Supplier}, or {@link JavaType#NONE_TYPE} if not found
     * @see JavaType#locate(Class)
     */
    public JavaType toSupplier() {
        return locate(Supplier.class);
    }

    /**
     * Navigates the type hierarchy to locate {@link Consumer}-related classes or interfaces.
     *
     * @return the matching {@link JavaType} for {@link Consumer}, or {@link JavaType#NONE_TYPE} if not found
     * @see JavaType#locate(Class)
     */
    public JavaType toConsumer() {
        return locate(Consumer.class);
    }


    /**
     * Navigates the type hierarchy to locate a specific {@link Class}.
     *
     * @param classType the class to navigate to
     * @return the matching {@link JavaType}, or {@link JavaType#NONE_TYPE} if not found
     * <p>
     * This method traverses the type hierarchy in the following order:
     * <ul>
     *   <li>If the current type matches the given class type, it is returned.</li>
     *   <li>Otherwise, it recursively searches the implemented interfaces.</li>
     *   <li>If not found in interfaces, it navigates to the superclass and continues the search.</li>
     * </ul>
     *
     * @see JavaType#getInterfaces()
     * @see JavaType#getSuperType()
     */
    public JavaType locate(Class<?> classType) {
        JavaType targetType = NONE_TYPE;

        if (this != NONE_TYPE) {
            // If we haven't resolved the class or if we've found a direct match
            if (this.rawType == null || this.rawType == classType) {
                targetType = this;
            } else {
                // Search interfaces
                for (JavaType iface : getInterfaces()) {
                    targetType = iface.locate(classType);
                    if (targetType != NONE_TYPE) {
                        break;
                    }
                }
                // Search super type if not found in interfaces
                if (targetType == NONE_TYPE) {
                    targetType = getSuperType().locate(classType);
                }
            }
        }

        return targetType;
    }

    /**
     * Converts the {@link JavaType} to a detailed representation of its hierarchy with indentation.
     *
     * @param indent the initial indentation level
     * @return a string representation of the type hierarchy with proper indentation
     * <p>
     * This method builds a visual representation of the type's hierarchy, including its superclass and interfaces.
     * Indentation is used to represent the depth of the hierarchy, making the structure easy to read.
     * <p>
     *
     * @see JavaType#getSuperType()
     * @see JavaType#getInterfaces()
     */
    public String toHierarchyString(int indent) {
        StringBuilder builder = new StringBuilder();
        builder.append(getName());

        JavaType superType = getSuperType();
        if (superType != NONE_TYPE) {
            builder.append("\n").append("\t".repeat(indent)).append(" extends ")
                    .append(superType.toHierarchyString(indent + 1));
        }

        for (JavaType iface : getInterfaces()) {
            builder.append("\n").append("\t".repeat(indent)).append(" implements ")
                    .append(iface.toHierarchyString(indent + 1));
        }

        return builder.toString();
    }

    /**
     * Generates a string representation of this {@link JavaType}.
     *
     * @return a string representation of this type
     * <p>
     * The representation varies based on the state of the type:
     * <ul>
     *   <li>If the type is resolved, it returns the name of the type (including generics, if applicable).</li>
     *   <li>If the type is an unresolved type variable, it prefixes the type name with {@code !}.</li>
     *   <li>If the type is unknown, it returns {@code ?}.</li>
     * </ul>
     * @see JavaType#getName()
     */
    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();

        if (isResolved()) {
            // build name for regular type
            builder.append(getName());
        } else if (isTypeVariable()) {
            // unresolved type variable
            builder.append('!');
            builder.append(this.type.getTypeName());
        } else {
            // none or unknown type
            builder.append('?');
        }

        return builder.toString();
    }

    /**
     * Retrieves the simple name of the type, including generics if present.
     */
    public String getName() {
        StringBuilder builder = new StringBuilder();

        if (isArray()) {
            builder.append(getComponentType().toString());
            builder.append("[]");
        } else {
            if (isResolved()) {
                builder.append(Reflections.getShortName(getRawType()));
            }

            JavaType[] generics = getGenerics();

            if (generics.length > 0) {
                StringJoiner joiner = new StringJoiner(", ", "<", ">");

                for (JavaType generic : generics) {
                    if (generic == this) {
                        joiner.add(Reflections.getShortName(getRawType()));
                    } else {
                        joiner.add(generic.toString());
                    }
                }

                builder.append(joiner);
            }

        }

        return builder.toString();
    }

    /**
     * Retrieves the cached hash code for this {@link JavaType}.
     *
     * @return the hash code
     */
    public int getHashCode() {
        return hashCode;
    }

    /**
     * Sets the hash code for this {@link JavaType}.
     *
     * @param hashCode the hash code to set
     */
    public void setHashCode(int hashCode) {
        this.hashCode = hashCode;
    }

    /**
     * Calculates the hash code for this {@link JavaType}.
     *
     * @return the calculated hash code
     * <p>
     * This method uses the underlying {@link Type} to compute the hash code.
     * @see Objects#hashCode(Object)
     */
    public int calculateHashCode() {
        return Objects.hashCode(this.type);
    }

    @Override
    public boolean equals(Object object) {
        boolean equals = false;

        if (object != null && getClass() == object.getClass()) {
            JavaType that = (JavaType) object;
            equals = Objects.equals(type, that.type);
        }

        return equals;
    }

    @Override
    public int hashCode() {
        return hashCode == 0 ? calculateHashCode() : hashCode;
    }

    /**
     * Retrieves the current size of the cache.
     *
     * @see JavaType#CACHE
     */
    public static int getCacheSize() {
        return CACHE.size();
    }

    /**
     * A singleton class representing the absence of a type.
     * <p>
     * This class is used internally to signify "no type" scenarios, such as unresolved or unknown types.
     * It implements the {@link Type} interface but provides no additional functionality.
     * </p>
     *
     * @see Type
     */
    static class NoneType implements Type {
        /**
         * A singleton instance of {@link NoneType}.
         */
        static final Type INSTANCE = new NoneType();
    }

    static class SyntheticType implements ParameterizedType {

        private final Class<?> rawType;
        private final Type[]   typeArguments;

        SyntheticType(Class<?> rawType, Type[] typeArguments) {
            this.rawType = rawType;
            this.typeArguments = typeArguments;
        }

        @Override
        public String getTypeName() {
            StringBuilder builder = new StringBuilder();

            builder.append("Synthetic:");
            builder.append(Reflections.getShortName(rawType));

            if (typeArguments.length > 0) {
                StringJoiner joiner = new StringJoiner(", ", "<", ">");

                for (Type type : typeArguments) {
                    joiner.add(type.getTypeName());
                }

                builder.append(joiner);
            }

            return builder.toString();
        }

        @Override
        public Type[] getActualTypeArguments() {
            return typeArguments;
        }

        @Override
        public Type getRawType() {
            return rawType;
        }

        @Override
        public Type getOwnerType() {
            return null;
        }

    }

}
