package org.jmouse.core.reflection;

import org.jmouse.util.Arrays;

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
 */
public class InferredType implements TypeClassifier {

    /**
     * 🗄️ Cache key for storing {@link InferredType} instances with their resolution context.
     *
     * <p>This record serves as a composite key for the internal {@code InferredType} cache,
     * pairing the underlying {@link Type} with its {@link InferredType parent} context.
     * This ensures that the same {@link Type} can be resolved differently
     * depending on the context in which it appears, avoiding incorrect
     * type resolution caused by global, context-less caching.</p>
     *
     * <h4>Usage</h4>
     * <pre>{@code
     * Map<TypeCache, InferredType> cache = new HashMap<>();
     * cache.put(TypeCache.of(type, parent), InferredType);
     * }</pre>
     *
     * @param type   the underlying {@link Type} to cache
     * @param parent the parent {@link InferredType} that defines the resolution context
     *
     * @see InferredType
     * @see Type
     */
    record TypeCache(Type type, InferredType parent) {

        /**
         * Factory method for creating a new {@link TypeCache} instance.
         *
         * @param type   the {@link Type} to cache
         * @param parent the resolution context for this type
         * @return a new {@link TypeCache} instance
         */
        static TypeCache of(Type type, InferredType parent) {
            return new TypeCache(type, parent);
        }
    }

    /**
     * A cache for storing resolved {@link InferredType} instances to avoid redundant resolution.
     */
    private static final Map<TypeCache, InferredType> CACHE = new HashMap<>();

    /**
     * A constant representing a "no type" scenario.
     */
    public static final InferredType NONE_TYPE = new InferredType(NoneType.INSTANCE);

    /**
     * A constant representing an empty array of {@link InferredType}.
     */
    private static final InferredType[] EMPTY_TYPE_ARRAY = new InferredType[0];

    /**
     * The parent {@link InferredType} in the type hierarchy.
     */
    private final InferredType parent;

    /**
     * The underlying {@link Type} represented by this {@link InferredType}.
     */
    private final Type type;

    /**
     * The resolved raw class for this {@link InferredType}.
     */
    private Class<?> rawType;

    /**
     * Lazily resolved {@link InferredType} representing the superclass.
     */
    private volatile InferredType superType;

    /**
     * Lazily resolved array of {@link InferredType} instances representing interfaces.
     */
    private volatile InferredType[] interfaces;

    /**
     * An array of {@link InferredType} representing generic type arguments.
     */
    private InferredType[] generics;

    /**
     * The cached hash code for this {@link InferredType}.
     */
    private int hashCode;

    /**
     * Constructs a new {@link InferredType} with a given type and optional parent context.
     *
     * @param type   the {@link Type} to represent
     * @param parent the parent {@link InferredType} in the hierarchy, or null if none
     */
    private InferredType(Type type, InferredType parent) {
        this.type = type;
        this.parent = parent;

        this.hashCode = calculateHashCode();

        resolveClass();
    }

    /**
     * Constructs a new {@link InferredType} with a given type.
     *
     * @param type the {@link Type} to represent
     */
    private InferredType(Type type) {
        this(type, null);
    }

    /**
     * Creates a {@link InferredType} instance from a {@link TypeReference}.
     *
     * @param type the {@link TypeReference} representing the desired type
     * @return a {@link InferredType} instance corresponding to the given type reference
     */
    public static InferredType forTypeReference(TypeReference<?> type) {
        return forType(type.getType());
    }

    /**
     * Returns a {@link InferredType} instance for the given {@link Class}.
     *
     * @param klass the class to wrap
     * @return a {@link InferredType} instance
     */
    public static InferredType forClass(Class<?> klass) {
        return forType(klass);
    }

    /**
     * Returns a {@link InferredType} instance for a parameterized class with specified generic arguments.
     *
     * @param klass    the raw class type
     * @param generics the generic type arguments
     * @return a {@link InferredType} instance representing the parameterized class
     * @throws IllegalArgumentException if the number of provided generic arguments does not match the expected number
     */
    public static InferredType forParametrizedClass(Class<?> klass, Class<?>... generics) {
        TypeVariable<?>[] variables = klass.getTypeParameters();

        if (Arrays.notEmpty(generics) && variables.length != generics.length) {
            throw new IllegalArgumentException(
                    "Mismatched parameters types. Expected %d but got %d".formatted(variables.length, generics.length));
        }

        return forType(new SyntheticType(klass, generics));
    }

    /**
     * Returns a {@link InferredType} instance for the given {@link Type}.
     *
     * @param type the type to wrap
     * @return a {@link InferredType} instance
     */
    public static InferredType forType(Type type) {
        return forType(type, null);
    }

    /**
     * Returns a {@link InferredType} instance for the given {@link Type} and parent context.
     *
     * @param type   the type to wrap
     * @param parent the parent {@link InferredType} in the hierarchy
     * @return a {@link InferredType} instance
     */
    public static InferredType forType(Type type, InferredType parent) {
        TypeCache    typeCache = TypeCache.of(type, parent);
        InferredType instance  = CACHE.get(typeCache);

        if (instance == null) {
            instance = new InferredType(type, parent);
            CACHE.put(typeCache, instance);
        }

        return instance;
    }

    /**
     * Creates a {@link InferredType} for an instance of an structured.
     *
     * @param object the structured instance
     * @return a {@link InferredType} instance
     */
    public static InferredType forInstance(Object object) {
        return (object instanceof Class<?> klass)
                ? forType(klass)
                : forType(object.getClass());
    }

    /**
     * Creates a {@link InferredType} for the generic type of a {@link Field}.
     *
     * @param field the field whose type is to be resolved
     * @return a {@link InferredType} instance for the field type
     *
     * @see Field#getGenericType()
     */
    public static InferredType forField(Field field) {
        return forType(field.getGenericType());
    }

    public static InferredType forField(Class<?> type, String name) {
        try {
            return forField(type.getDeclaredField(name));
        } catch (NoSuchFieldException e) {
            throw new IllegalArgumentException(e);
        }
    }

    /**
     * Creates a {@link InferredType} for the return type of a {@link Method}.
     *
     * @param method the method whose return type is to be resolved
     * @return a {@link InferredType} instance for the return type
     */
    public static InferredType forMethodReturnType(Method method) {
        return forType(method.getGenericReturnType());
    }

    /**
     * Creates a {@link InferredType} for a specific exception type declared by a {@link Method}.
     * This executable retrieves the exception type at the specified index in the executable's
     * declared exceptions and constructs a {@link InferredType} representation of it.
     * If the specified index is out of bounds, an {@link IllegalArgumentException} is thrown.
     *
     *
     * @param executable the executable whose exception type is to be resolved
     * @param index  the index of the exception type in the declared exceptions
     * @return a {@link InferredType} instance representing the exception type
     * @throws IllegalArgumentException if the specified index is out of bounds
     */
    public static InferredType forExceptionType(Executable executable, int index) {
        Type[] parameters = executable.getGenericExceptionTypes();

        if (parameters.length > index) {
            return forType(parameters[index]);
        }

        throw new IllegalArgumentException(
                "Executable '%s' has no exception type with '%d' index".formatted(Reflections.getMethodName(executable), index));
    }

    /**
     * Creates a {@link InferredType} for a specific parameter of a given {@link Method}.
     *
     * @param executable the executable whose parameter type is to be resolved
     * @param index  the index of the parameter (zero-based)
     * @return a {@link InferredType} instance representing the parameter type at the given index
     * @throws IllegalArgumentException if the executable does not have a parameter at the specified index
     */
    public static InferredType forParameter(Executable executable, int index) {
        Type[] parameters = executable.getGenericParameterTypes();

        if (parameters.length > index) {
            return forType(parameters[index]);
        }

        throw new IllegalArgumentException(
                "Executable '%s' has no parameter '%d' index".formatted(Reflections.getMethodName(executable), index));
    }

    /**
     * Creates a {@link InferredType} for the given method or constructor parameter.
     * <p>
     * This method resolves the generic parameter type using {@link Parameter#getParameterizedType()}.
     * </p>
     *
     * @param parameter the parameter whose type is to be resolved
     * @return a {@link InferredType} instance representing the parameter's type
     */
    public static InferredType forParameter(Parameter parameter) {
        return forType(parameter.getParameterizedType());
    }

    /**
     * Retrieves the raw {@link Class} for this {@link InferredType}.
     *
     * @return the raw {@link Class}, or null if the type cannot be resolved
     */
    public <T> Class<T> getRawType() {
        return (Class<T>) rawType;
    }

    /**
     * Retrieves the underlying {@link Type} represented by this {@link InferredType}.
     *
     * @return the {@link Type} instance
     */
    public Type getType() {
        return type;
    }

    /**
     * Resolves and returns the raw {@link Class} for this {@link InferredType}.
     *
     * @return the resolved {@link Class}, or null if the type is {@link NoneType}
     * <p>
     * This method supports resolving raw types, arrays, and generic components by analyzing the underlying {@link Type}.
     * If the type is a {@link GenericArrayType}, its component type is recursively resolved.
     * @see GenericArrayType
     * @see InferredType#getComponentType()
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
     * Resolves the type hierarchy and returns the {@link InferredType}.
     *
     * @return the resolved {@link InferredType}, or {@link InferredType#NONE_TYPE} if unresolved
     * <p>
     * This method supports resolving various type categories, including:
     * <ul>
     *   <li>{@link ParameterizedType} - Resolves to the raw type.</li>
     *   <li>{@link TypeVariable} - Attempts to resolve within the current context or bounds.</li>
     *   <li>{@link WildcardType} - Resolves upper or lower bounds.</li>
     * </ul>
     * <p>
     * @see Type
     * @see InferredType#NONE_TYPE
     */
    public InferredType resolveType() {

        Infer.rawClassOf(this.type);

        return switch (this.type) {
            case ParameterizedType parameterizedType
                    -> forType(parameterizedType.getRawType());
            case TypeVariable<?> typeVariable -> {
                InferredType variable = NONE_TYPE;

                if (parent != null) {
                    variable = parent.resolveVariable(typeVariable);
                } else if (Arrays.notEmpty(typeVariable.getBounds())) {
                    variable = forType(typeVariable.getBounds()[0], this);
                }

                yield variable;
            }
            case WildcardType wildcardType -> {
                // extends  - upper bounds
                // super    - lower bounds
                InferredType wildcard = NONE_TYPE;

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
     * @return the resolved {@link InferredType}, or {@link InferredType#NONE_TYPE} if unresolved
     * <p>
     * This method evaluates the type parameters of the raw type and attempts to match the given {@link TypeVariable}.
     * If the variable cannot be resolved in the current context, it defaults to {@link InferredType#NONE_TYPE}.
     * @see TypeVariable
     */
    public InferredType resolveVariable(TypeVariable<?> variable) {
        InferredType type         = null;
        String       expectedName = variable.getName();

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
     * Retrieves the {@link InferredType} representing the component type of an array.
     *
     * @return the component {@link InferredType}, or {@link InferredType#NONE_TYPE} if not an array
     * <p>
     * This method supports resolving both generic arrays (via {@link GenericArrayType}) and raw arrays.
     * @see GenericArrayType
     */
    public InferredType getComponentType() {
        InferredType type = NONE_TYPE;

        if (this.type instanceof GenericArrayType arrayType) {
            type = forType(arrayType.getGenericComponentType());
        } else if (this.type instanceof Class<?> classType) {
            type = forType(classType.componentType());
        }

        return type;
    }

    /**
     * Checks if this {@link InferredType} has been resolved.
     *
     * @return true if resolved, false otherwise
     * @see InferredType#isUnresolved()
     */
    public boolean isResolved() {
        return getRawType() != null;
    }

    /**
     * Checks if this {@link InferredType} is unresolved.
     *
     * @return true if unresolved, false otherwise
     */
    public boolean isUnresolved() {
        return !isResolved();
    }

    /**
     * Returns the class type being inspected.
     *
     * @return the {@link Class} structured representing the inspected type
     */
    @Override
    public Class<?> getClassType() {
        return isResolved() ? getRawType() : Unknown.TYPE;
    }

    /**
     * Checks if this {@link InferredType} represents an array.
     *
     * @return true if it represents an array, false otherwise
     * <p>
     * This method handles both raw arrays (e.g., {@code int[]}) and generic array types
     * (e.g., {@code T[]}, where {@code T} is a type variable).
     * @see InferredType#getComponentType()
     * @see InferredType#isGenericArrayType()
     */
    public boolean isArray() {
        return (type instanceof Class<?> clazz && clazz.isArray()) || isGenericArrayType();
    }

    /**
     * Checks if this {@link InferredType} represents a parameterized type.
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
     * Checks if this {@link InferredType} represents a wildcard type.
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
     * Checks if this {@link InferredType} represents a generic array type.
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
     * Checks if this {@link InferredType} represents a raw type.
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
     * Checks if this {@link InferredType} represents a type variable.
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
     * @return the {@link InferredType} representing the superclass, or {@link InferredType#NONE_TYPE} if none exists
     * <p>
     * This method handles the resolution of generic superclasses if they exist and caches the result for efficiency.
     * @see Class#getGenericSuperclass()
     */
    public InferredType getSuperType() {
        InferredType superType = this.superType;

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
     * Retrieves the interfaces implemented by the raw type as an array of {@link InferredType}.
     *
     * @return an array of {@link InferredType} representing the interfaces
     * <p>
     * This method resolves generic interfaces implemented by the raw type and lazily caches the result.
     * @see Class#getGenericInterfaces()
     */
    public InferredType[] getInterfaces() {
        Class<?> rawType = getRawType();

        if (rawType == null) {
            return EMPTY_TYPE_ARRAY;
        }

        if (this.interfaces == null) {
            Type[]         interfaces = rawType.getGenericInterfaces();
            InferredType[] types      = new InferredType[interfaces.length];

            for (int i = 0; i < interfaces.length; i++) {
                types[i] = forType(interfaces[i], parent);
            }

            this.interfaces = types;
        }

        return this.interfaces;
    }

    /**
     * Retrieves the type parameters associated with this {@link InferredType}.
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
     * Retrieves the generic type arguments of this {@link InferredType}.
     *
     * @return an array of {@link InferredType} representing the generics
     * <p>
     * This method resolves the generic type arguments based on the {@link Type} and the current context.
     * @see ParameterizedType#getActualTypeArguments()
     */
    public InferredType[] getGenerics() {
        InferredType[] generics = this.generics;

        if (Arrays.empty(generics)) {
            generics = EMPTY_TYPE_ARRAY;

            if (isParameterizedType() || isRawType()) {
                Type[]         types = getTypeParameters();
                InferredType[] array = new InferredType[types.length];

                for (int i = 0; i < types.length; i++) {
                    InferredType type = forType(types[i], parent);

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
     * Retrieves a specific {@link InferredType} from the sequence of generics.
     *
     * @param sequence the sequence of indexes representing the desired generic type
     * @return the {@link InferredType} at the specified sequence, or {@link InferredType#NONE_TYPE} if not found
     * <p>
     * This method navigates through the generic type arguments to retrieve a nested generic type based on the provided sequence.
     * <ul>
     *   <li>If the sequence is empty or null, it returns the first generic type or {@link InferredType#NONE_TYPE} if none exist.</li>
     *   <li>If the sequence points to an invalid index, it returns {@link InferredType#NONE_TYPE}.</li>
     * </ul>
     * <p>
     *
     * @see InferredType#getGenerics()
     * @see InferredType#NONE_TYPE
     */
    public InferredType getGeneric(int... sequence) {
        InferredType[] types = getGenerics();
        InferredType   generic;

        if (sequence == null || sequence.length == 0) {
            generic = (types.length == 0) ? NONE_TYPE : types[0];
        } else {
            generic = this;
            for (int index : sequence) {
                InferredType[] generics = generic.getGenerics();

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
     * @return the first {@link InferredType} in the generics list, or {@link InferredType#NONE_TYPE} if no generics are present
     * <p>
     * This method is a shorthand for accessing the first generic type using {@link #getGeneric(int...)}.
     *
     * @see InferredType#getGeneric(int...)
     */
    public InferredType getFirst() {
        return getGeneric(0);
    }

    /**
     * Retrieves the last generic type from the list of generics.
     *
     * @return the last {@link InferredType} in the generics list, or {@link InferredType#NONE_TYPE} if no generics are present
     * <p>
     * This method is a shorthand for accessing the last generic type using {@link #getGeneric(int...)}.
     *
     * @see InferredType#getGeneric(int...)
     * @see InferredType#getGenerics()
     */
    public InferredType getLast() {
        return getGeneric(getGenerics().length - 1);
    }

    /**
     * Navigates the type hierarchy to locate {@link Map}-related classes or interfaces.
     *
     * @return the matching {@link InferredType} for {@link Map}, or {@link InferredType#NONE_TYPE} if not found
     * @see InferredType#locate(Class)
     */
    public InferredType toMap() {
        return locate(Map.class);
    }

    /**
     * Navigates the type hierarchy to locate {@link List}-related classes or interfaces.
     *
     * @return the matching {@link InferredType} for {@link List}, or {@link InferredType#NONE_TYPE} if not found
     * @see InferredType#locate(Class)
     */
    public InferredType toList() {
        return locate(List.class);
    }

    /**
     * Navigates the type hierarchy to locate {@link Set}-related classes or interfaces.
     *
     * @return the matching {@link InferredType} for {@link Set}, or {@link InferredType#NONE_TYPE} if not found
     * @see InferredType#locate(Class)
     */
    public InferredType toSet() {
        return locate(Set.class);
    }

    /**
     * Navigates the type hierarchy to locate {@link Collection}-related classes or interfaces.
     *
     * @return the matching {@link InferredType} for {@link Collection}, or {@link InferredType#NONE_TYPE} if not found
     * @see InferredType#locate(Class)
     */
    public InferredType toCollection() {
        return locate(Collection.class);
    }

    /**
     * Navigates the type hierarchy to locate {@link Function}-related classes or interfaces.
     *
     * @return the matching {@link InferredType} for {@link Function}, or {@link InferredType#NONE_TYPE} if not found
     * @see InferredType#locate(Class)
     */
    public InferredType toFunction() {
        return locate(Function.class);
    }

    /**
     * Navigates the type hierarchy to locate {@link BiFunction}-related classes or interfaces.
     *
     * @return the matching {@link InferredType} for {@link BiFunction}, or {@link InferredType#NONE_TYPE} if not found
     * @see InferredType#locate(Class)
     */
    public InferredType toBiFunction() {
        return locate(BiFunction.class);
    }

    /**
     * Navigates the type hierarchy to locate {@link BiPredicate}-related classes or interfaces.
     *
     * @return the matching {@link InferredType} for {@link BiPredicate}, or {@link InferredType#NONE_TYPE} if not found
     * @see InferredType#locate(Class)
     */
    public InferredType toBiPredicate() {
        return locate(BiPredicate.class);
    }

    /**
     * Navigates the type hierarchy to locate {@link Predicate}-related classes or interfaces.
     *
     * @return the matching {@link InferredType} for {@link Predicate}, or {@link InferredType#NONE_TYPE} if not found
     * @see InferredType#locate(Class)
     */
    public InferredType toPredicate() {
        return locate(Predicate.class);
    }

    /**
     * Navigates the type hierarchy to locate {@link Supplier}-related classes or interfaces.
     *
     * @return the matching {@link InferredType} for {@link Supplier}, or {@link InferredType#NONE_TYPE} if not found
     * @see InferredType#locate(Class)
     */
    public InferredType toSupplier() {
        return locate(Supplier.class);
    }

    /**
     * Navigates the type hierarchy to locate {@link Consumer}-related classes or interfaces.
     *
     * @return the matching {@link InferredType} for {@link Consumer}, or {@link InferredType#NONE_TYPE} if not found
     * @see InferredType#locate(Class)
     */
    public InferredType toConsumer() {
        return locate(Consumer.class);
    }


    /**
     * Navigates the type hierarchy to locate a specific {@link Class}.
     *
     * @param classType the class to navigate to
     * @return the matching {@link InferredType}, or {@link InferredType#NONE_TYPE} if not found
     * <p>
     * This method traverses the type hierarchy in the following order:
     * <ul>
     *   <li>If the current type matches the given class type, it is returned.</li>
     *   <li>Otherwise, it recursively searches the implemented interfaces.</li>
     *   <li>If not found in interfaces, it navigates to the superclass and continues the search.</li>
     * </ul>
     *
     * @see InferredType#getInterfaces()
     * @see InferredType#getSuperType()
     */
    public InferredType locate(Class<?> classType) {
        InferredType targetType = NONE_TYPE;

        if (this != NONE_TYPE) {
            // If we haven't resolved the class or if we've found a direct match
            if (this.rawType == null || this.rawType == classType) {
                targetType = this;
            } else {
                // Search interfaces
                for (InferredType iface : getInterfaces()) {
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
     * Converts the {@link InferredType} to a detailed representation of its hierarchy with indentation.
     *
     * @param indent the initial indentation level
     * @return a string representation of the type hierarchy with proper indentation
     * <p>
     * This method builds a visual representation of the type's hierarchy, including its superclass and interfaces.
     * Indentation is used to represent the depth of the hierarchy, making the structure easy to read.
     * <p>
     *
     * @see InferredType#getSuperType()
     * @see InferredType#getInterfaces()
     */
    public String toHierarchyString(int indent) {
        StringBuilder builder = new StringBuilder();
        builder.append(getName());

        InferredType superType = getSuperType();
        if (superType != NONE_TYPE) {
            builder.append("\n").append("\t".repeat(indent)).append(" extends ")
                    .append(superType.toHierarchyString(indent + 1));
        }

        for (InferredType iface : getInterfaces()) {
            builder.append("\n").append("\t".repeat(indent)).append(" implements ")
                    .append(iface.toHierarchyString(indent + 1));
        }

        return builder.toString();
    }

    /**
     * Generates a string representation of this {@link InferredType}.
     *
     * @return a string representation of this type
     * <p>
     * The representation varies based on the state of the type:
     * <ul>
     *   <li>If the type is resolved, it returns the name of the type (including generics, if applicable).</li>
     *   <li>If the type is an unresolved type variable, it prefixes the type name with {@code !}.</li>
     *   <li>If the type is unknown, it returns {@code ?}.</li>
     * </ul>
     * @see InferredType#getName()
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

            InferredType[] generics = getGenerics();

            if (generics.length > 0) {
                StringJoiner joiner = new StringJoiner(", ", "<", ">");

                for (InferredType generic : generics) {
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
     * Retrieves the cached hash code for this {@link InferredType}.
     *
     * @return the hash code
     */
    public int getHashCode() {
        return hashCode;
    }

    /**
     * Sets the hash code for this {@link InferredType}.
     *
     * @param hashCode the hash code to set
     */
    public void setHashCode(int hashCode) {
        this.hashCode = hashCode;
    }

    /**
     * Calculates the hash code for this {@link InferredType}.
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
            InferredType that = (InferredType) object;
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
     * @see InferredType#CACHE
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

    /**
     * Represents a synthetic {@link ParameterizedType} that can be used to define parameterized types dynamically.
     * <p>
     * This implementation allows the creation of parameterized types without requiring explicit generic declarations
     * in the source code. It is particularly useful for runtime type resolution in reflection-based frameworks.
     * </p>
     *
     * Example usage:
     * <pre>{@code
     * ParameterizedType syntheticType = new SyntheticType(List.class, new Type[]{String.class});
     * System.out.println(syntheticType.getTypeName()); // Outputs: "Synthetic:List<String>"
     * }</pre>
     *
     * @author JMouse - Team
     * @author Mr. Jerry Mouse
     * @author Ivan Hontarenko
     */
    static class SyntheticType implements ParameterizedType {

        private final Class<?> rawType;
        private final Type[]   typeArguments;

        /**
         * Constructs a synthetic parameterized type with the specified raw type and type arguments.
         *
         * @param rawType       the raw type of the parameterized type
         * @param typeArguments the actual type arguments
         */
        SyntheticType(Class<?> rawType, Type[] typeArguments) {
            this.rawType = rawType;
            this.typeArguments = typeArguments;
        }

        /**
         * Returns the type name representation of this synthetic parameterized type.
         * <p>
         * The format follows: {@code Synthetic:RawType<TypeArg1, TypeArg2, ...>}
         * </p>
         *
         * @return the type name of this parameterized type
         */
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

        /**
         * Returns the actual type arguments of this parameterized type.
         *
         * @return an array of {@link Type} representing the actual type arguments
         */
        @Override
        public Type[] getActualTypeArguments() {
            return typeArguments;
        }

        /**
         * Returns the raw type of this parameterized type.
         *
         * @return the {@link Type} representing the raw type
         */
        @Override
        public Type getRawType() {
            return rawType;
        }

        /**
         * Returns the owner type of this parameterized type, which is always {@code null}
         * since synthetic types do not have an owner.
         *
         * @return {@code null} as synthetic types do not have an owner
         */
        @Override
        public Type getOwnerType() {
            return null;
        }
    }

}
