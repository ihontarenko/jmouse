package svit.reflection;

import java.io.Serializable;
import java.lang.reflect.*;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.StringJoiner;

public class ReflectType {

    private static final Map<ReflectType, ReflectType> CACHE = new HashMap<>();

    private static final ReflectType NONE_TYPE = new ReflectType(NoneType.INSTANCE);

    private static final ReflectType[] EMPTY_TYPE_ARRAY = {};

    private final ReflectType parent;

    private final Type type;

    private Class<?> rawType;

    private volatile ReflectType superType;

    private volatile ReflectType[] interfaces;

    private ReflectType[] generics;

    private VariableResolver resolver;

    private int hashCode;

    private ReflectType(Type type, ReflectType parent) {
        this.type = type;
        this.parent = parent;

        if (parent != null) {
            this.resolver = new ParentalVariableResolver(parent);
        }

        this.hashCode = calculateHashCode();

        resolveClass();
    }

    private ReflectType(Type type) {
        this(type, null);
    }

    public static ReflectType forClass(Class<?> klass) {
        return new ReflectType(klass);
    }

    public static ReflectType forType(Type type) {
        return forType(type, null);
    }

    public static ReflectType forType(Type type, ReflectType parent) {
        ReflectType newType    = new ReflectType(type, parent);
        ReflectType cachedType = CACHE.get(newType);

        if (cachedType == null) {
            CACHE.put(newType, newType);
            cachedType = newType;
        }

        return cachedType;
    }

    public static ReflectType forInstance(Object object) {
        return (object instanceof Class<?> klass)
                ? forType(klass)
                : forType(object.getClass());
    }

    public static ReflectType forField(Field field) {
        return forType(field.getGenericType());
    }

    public static ReflectType forMethodReturnType(Method method) {
        return forType(method.getGenericReturnType());
    }

    public Class<?> getRawType() {
        return rawType;
    }

    public Type getType() {
        return type;
    }

    public Class<?> resolveClass() {
        if (type == NoneType.INSTANCE) {
            return null;
        }

        if (rawType == null) {
            if (isRawClass()) {
                this.rawType = (Class<?>) type;
            } else if (isArray()) {
                this.rawType = getComponentType().resolveClass();
            } else {
                this.rawType = resolveType().resolveClass();
            }
        }

        return rawType;
    }

    public ReflectType resolveType() {
        return switch (this.type) {
            case ParameterizedType parameterizedType -> forType(parameterizedType.getRawType());
            case TypeVariable<?> typeVariable -> {
                ReflectType variable = resolver.resolve(typeVariable);;

                if (resolver != null) {
                    variable = resolver.resolve(typeVariable);
                }

                yield variable;
            }
            default -> NONE_TYPE;
        };
    }

    public ReflectType resolveVariable(TypeVariable<?> variable) {
        ReflectType type         = null;
        String      expectedName = variable.getName();

        if (isParametrizedType()) {
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

    public ReflectType getComponentType() {
        ReflectType type = NONE_TYPE;

        if (this.type instanceof GenericArrayType arrayType) {
            type = forType(arrayType.getGenericComponentType());
        } else if (this.type instanceof Class<?> classType) {
            type = forType(classType.componentType());
        }

        return type;
    }

    public boolean isArray() {
        return (type instanceof Class<?> clazz && clazz.isArray()) || isGenericArrayType();
    }

    public boolean isParametrizedType() {
        return this.type instanceof ParameterizedType;
    }

    public boolean isGenericArrayType() {
        return this.type instanceof GenericArrayType;
    }

    public boolean isRawClass() {
        return this.type instanceof Class<?>;
    }

    public boolean isTypeVariable() {
        return this.type instanceof TypeVariable<?>;
    }

    public ReflectType getSuperType() {
        ReflectType superType = this.superType;

        if (superType == null) {
            superType = NONE_TYPE;

            Class<?> klass = resolveClass();

            if (klass != null) {
                Type genericSuperclass = klass.getGenericSuperclass();
                if (genericSuperclass != null) {
                    superType = forType(genericSuperclass, this);
                }
            }

            this.superType = superType;
        }

        return this.superType;
    }

    public ReflectType[] getInterfaces() {
        Class<?> localClass = getRawType();

        if (localClass == null) {
            return EMPTY_TYPE_ARRAY;
        }

        if (this.interfaces == null) {
            Type[]        interfaces = localClass.getGenericInterfaces();
            ReflectType[] types      = new ReflectType[interfaces.length];

            for (int i = 0; i < interfaces.length; i++) {
                types[i] = forType(interfaces[i], this);
            }

            this.interfaces = types;
        }

        return this.interfaces;
    }

    public ReflectType[] getGenerics() {
        boolean empty = generics == null || generics.length == 0;

        if (empty) {
            Type[]        types = isParametrizedType()
                    ? ((ParameterizedType) type).getActualTypeArguments() : getRawType().getTypeParameters();
            ReflectType[] array = new ReflectType[types.length];

            for (int i = 0; i < types.length; i++) {
                ReflectType type = forType(types[i], parent);

                if (type.isParametrizedType() && type.getGeneric(0).isTypeVariable()) {
                    System.out.println(">>>");
                    System.out.println(type);
//                    type = type.resolveType();
                    System.out.println(type.hashCode);
                    System.out.println("<<<");
                }

                array[i] = type;
            }

            this.generics = array;
        }

        return this.generics;
    }

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

    public ReflectType navigate(Class<?> classType) {
        ReflectType targetType = NONE_TYPE;

        if (this != NONE_TYPE) {
            // If we haven't resolved the class or if we've found a direct match
            if (this.rawType == null || this.rawType == classType) {
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

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();

        if (isArray()) {
            builder.append(getComponentType().toString());
            builder.append("[]");
        } else {
            if (rawType != null) {
                builder.append(rawType.getSimpleName());

                ReflectType[] generics = getGenerics();

                if (generics.length > 0) {
                    StringJoiner joiner = new StringJoiner(", ", "<", ">");

                    for (ReflectType generic : generics) {
                        joiner.add(generic.toString());
                    }

                    builder.append(joiner);
                }
            } else if (isTypeVariable()) {
                builder.append('!');
                builder.append(this.type.getTypeName());
            } else {
                builder.append('?');
            }
        }

        return builder.toString();
    }

    public int getHashCode() {
        return hashCode;
    }

    public void setHashCode(int hashCode) {
        this.hashCode = hashCode;
    }

    public int calculateHashCode() {
        return Objects.hashCode(this.type);
    }

    @Override
    public boolean equals(Object object) {
        boolean equals = false;

        if (object != null && getClass() == object.getClass()) {
            ReflectType that = (ReflectType) object;
            equals = Objects.equals(type, that.type);
        }

        return equals;
    }

    @Override
    public int hashCode() {
        return hashCode == 0 ? calculateHashCode() : hashCode;
    }

    interface VariableResolver {
        ReflectType resolve(Type variable);
    }

    static class ParentalVariableResolver implements VariableResolver {

        private final ReflectType root;

        ParentalVariableResolver(ReflectType type) {
            this.root = type;
        }

        public ReflectType uptoRoot() {
            ReflectType current = root;

            while (current.parent != null) {
                current = current.parent;
            }

            return current;
        }

        @Override
        public ReflectType resolve(Type variable) {
            return root.resolveVariable((TypeVariable<?>) variable);
        }

    }

    static class NoneType implements Type {
        static final Type INSTANCE = new NoneType();
    }

}
