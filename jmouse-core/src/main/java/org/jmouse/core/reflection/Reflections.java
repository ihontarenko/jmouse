package org.jmouse.core.reflection;

import java.beans.BeanInfo;
import java.beans.IntrospectionException;
import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.lang.annotation.Annotation;
import java.lang.module.ModuleFinder;
import java.lang.module.ModuleReference;
import java.lang.reflect.*;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

import static org.jmouse.core.reflection.MethodMatchers.hasParameterTypes;
import static org.jmouse.core.reflection.MethodMatchers.isAbstract;
import static org.jmouse.util.helper.Strings.uncapitalize;

/**
 * A utility class providing various reflection-based methods for working with classes, fields, methods, and constructors.
 *
 * <p>Offers methods to retrieve, set, and manipulate class members, including annotated fields and methods, methods by signature,
 * parameterized types, and so on. This class also helps to instantiate objects dynamically using reflection.
 */
abstract public class Reflections {

    public static final  String                PROXY_CLASS_NAME_SEPARATOR     = "$$";
    public static final  char                  PACKAGE_SEPARATOR              = '.';
    /**
     * A set of all Java module references
     */
    public static final Set<ModuleReference> JAVA_MODULES = ModuleFinder.ofSystem().findAll();
    /**
     * A set of all Java module names.
     */
    public static final Set<String> JAVA_MODULE_NAMES = JAVA_MODULES.stream().map(r -> r.descriptor().name())
            .collect(Collectors.toSet());
    private static final Map<Class<?>, Object> PRIMITIVES_DEFAULT_TYPE_VALUES = Map.of(
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
     * Extracts all static methods declared in the specified class.
     *
     * @param clazz the class to analyze
     * @return a list containing all static methods of the class
     *
     * <p><b>Example usage:</b></p>
     * <pre>{@code
     * List<Method> staticMethods = Reflections.extractStaticMethods(Math.class);
     * staticMethods.forEach(method -> System.out.println(method.getName()));
     * }</pre>
     */
    public static List<Method> extractStaticMethods(Class<?> clazz) {
        Method[]     methods       = clazz.getMethods();
        List<Method> staticMethods = new ArrayList<>(methods.length);

        for (Method method : methods) {
            if (Modifier.isStatic(method.getModifiers())) {
                staticMethods.add(method);
            }
        }

        return staticMethods;
    }

    /**
     * Creates a Class instance for the given class name, suppressing the {@link ClassNotFoundException}.
     *
     * @param className the fully qualified name of the desired class
     * @return the corresponding Class structured
     * @throws ReflectionException if the class cannot be found
     *
     *                             <p><b>Example usage:</b></p>
     *                             <pre>{@code
     *                             Class<?> stringClass = Reflections.getClassFor("java.lang.String");
     *                             System.out.println(stringClass.getName()); // java.lang.String
     *                             }</pre>
     */
    public static Class<?> getClassFor(String className) {
        try {
            return Class.forName(className);
        } catch (ClassNotFoundException | NoClassDefFoundError e) {
            throw new ReflectionException(e);
        }
    }

    /**
     * Instantiates an structured using the given constructor and arguments.
     *
     * <p>This method will attempt to match null arguments to default primitive values if
     * the constructor parameter is a primitive type.</p>
     *
     * @param <T>         the type of structured to instantiate
     * @param constructor the constructor to use for instantiation
     * @param arguments   the arguments to pass to the constructor
     * @return a new instance of the structured
     * @throws ReflectionException if instantiation fails
     *
     *                             <p><b>Example usage:</b></p>
     *                             <pre>{@code
     *                             Constructor<MyClass> constructor = MyClass.class.getConstructor(String.class);
     *                             MyClass instance = Reflections.instantiate(constructor, "binder argument");
     *                             }</pre>
     */
    public static <T> T instantiate(Constructor<T> constructor, Object... arguments) {
        final int parametersCount = constructor.getParameterCount();
        T         instance;

        try {
            constructor.setAccessible(true);

            if (parametersCount == 0) {
                instance = constructor.newInstance();
            } else {
                Object[]   constructorArguments = new Object[arguments.length];
                Class<?>[] parameterTypes       = constructor.getParameterTypes();

                for (int i = 0; i < constructorArguments.length; i++) {
                    if (arguments[i] == null) {
                        Class<?> parameterType = parameterTypes[i];
                        constructorArguments[i] = parameterType.isPrimitive() ? PRIMITIVES_DEFAULT_TYPE_VALUES.get(
                                parameterType) : null;
                    } else {
                        constructorArguments[i] = arguments[i];
                    }
                }

                instance = constructor.newInstance(constructorArguments);
            }

            return instance;
        } catch (InstantiationException | IllegalAccessException | InvocationTargetException e) {
            throw new ReflectionException("COULD NOT INSTANTIATE OBJECT USING CONSTRUCTOR: " + constructor, e);
        }
    }

    /**
     * Finds the first constructor that matches the provided parameter types.
     *
     * @param clazz the class to search for constructors
     * @param types the parameter types of the desired constructor
     * @return the first matching constructor
     * @throws ReflectionException if no matching constructor is found
     *
     *                             <p><b>Example usage:</b></p>
     *                             <pre>{@code
     *                             Constructor<?> constructor = Reflections.findFirstConstructor(MyClass.class, String.class, int.class);
     *                             Object instance = Reflections.instantiate(constructor, "binder", 42);
     *                             }</pre>
     */
    public static Constructor<?> findFirstConstructor(Class<?> clazz, Class<?>... types) {
        return new ConstructorFinder().findFirst(clazz, hasParameterTypes(types).and(isAbstract().not())).orElseThrow(
                () -> new ReflectionException(
                        "CONSTRUCTOR %s(%s) NOT FOUND".formatted(getShortName(clazz), Arrays.toString(types))));
    }

    /**
     * Finds the first constructor annotated with the specified annotation.
     *
     * @param clazz      the class to search for constructors
     * @param annotation the annotation to look for
     * @return the first annotated constructor
     * @throws ReflectionException if no annotated constructor is found
     *
     *                             <p><b>Example usage:</b></p>
     *                             <pre>{@code
     *                             @CustomAnnotation
     *                             public MyClass(String param) {
     *                                 // ...
     *                             }
     *
     *                             Constructor<?> constructor = Reflections.findFirstAnnotatedConstructor(MyClass.class, CustomAnnotation.class);
     *                             }</pre>
     */
    public static Constructor<?> findFirstAnnotatedConstructor(Class<?> clazz, Class<? extends Annotation> annotation) {
        return new ConstructorFinder().findFirst(clazz, MethodMatchers.isAnnotatedWith(annotation))
                .orElseThrow(() -> new ReflectionException("ANNOTATED CONSTRUCTOR NOT FOUND"));
    }

    /**
     * Finds all fields annotated with the specified annotation.
     *
     * @param clazz      the class to search for fields
     * @param annotation the annotation to look for
     * @return a set of annotated fields
     *
     * <p><b>Example usage:</b></p>
     * <pre>{@code
     * @CustomAnnotation
     * private String name;
     *
     * Set<Field> fields = Reflections.findAllAnnotatedFields(MyClass.class, CustomAnnotation.class);
     * fields.forEach(field -> System.out.println(field.getName()));
     * }</pre>
     */
    public static Set<Field> findAllAnnotatedFields(Class<?> clazz, Class<? extends Annotation> annotation) {
        return new HashSet<>(new FieldFinder().find(clazz, FieldMatchers.isAnnotatedWith(annotation)));
    }

    /**
     * Finds all methods annotated with the specified annotation.
     *
     * @param clazz      the class to search for methods
     * @param annotation the annotation to look for
     * @return a set of annotated methods
     *
     * <p><b>Example usage:</b></p>
     * <pre>{@code
     * @CustomAnnotation
     * public void myAnnotatedMethod() {
     *     // ...
     * }
     *
     * Set<Method> methods = Reflections.findAllAnnotatedMethods(MyClass.class, CustomAnnotation.class);
     * methods.forEach(method -> System.out.println(method.getName()));
     * }</pre>
     */
    public static Set<Method> findAllAnnotatedMethods(Class<?> clazz, Class<? extends Annotation> annotation) {
        return new HashSet<>(new MethodFinder().find(clazz, MethodMatchers.isAnnotatedWith(annotation)));
    }

    /**
     * Finds a field by its name in the specified class.
     *
     * @param targetClass the class to search for the field
     * @param fieldName   the name of the field
     * @return an optional field, or empty if the field is not found
     *
     * <p><b>Example usage:</b></p>
     * <pre>{@code
     * Optional<Field> field = Reflections.getField(MyClass.class, "name");
     * field.ifPresent(f -> System.out.println("Field found: " + f.getName()));
     * }</pre>
     */
    public static Optional<Field> getField(Class<?> targetClass, String fieldName) {
        return new FieldFinder().findFirst(targetClass, FieldMatchers.nameEquals(fieldName));
    }

    /**
     * Finds all fields with the specified modifiers in the given class.
     *
     * @param type      the class to search for fields
     * @param modifiers the modifiers to match (e.g., {@link Modifier#PUBLIC})
     * @return a list of fields with the specified modifiers
     *
     * <p><b>Example usage:</b></p>
     * <pre>{@code
     * Collection<Field> fields = Reflections.getClassFields(MyClass.class, Modifier.PUBLIC);
     * fields.forEach(field -> System.out.println(field.getName()));
     * }</pre>
     */
    public static Collection<Field> getClassFields(Class<?> type, int modifiers) {
        return new FieldFinder().find(type, FieldMatchers.withModifier(modifiers));
    }

    /**
     * Sets the value of a field in an structured by its field name.
     *
     * @param object    the structured whose field is to be set
     * @param fieldName the name of the field
     * @param value     the value to set
     *
     *                  <p><b>Example usage:</b></p>
     *                  <pre>{@code
     *                  MyClass myObj = new MyClass();
     *                  Reflections.setFieldValue(myObj, "name", "John Doe");
     *                  }</pre>
     * @see #getFieldValue(Object, String)
     */
    public static void setFieldValue(Object object, String fieldName, Object value) {
        getField(object.getClass(), fieldName).ifPresent(field -> setFieldValue(object, field, value));
    }

    /**
     * Sets the value of a field in an structured.
     *
     * @param object the structured whose field is to be set
     * @param field  the field to set
     * @param value  the value to set
     *
     *               <p><b>Example usage:</b></p>
     *               <pre>{@code
     *               Field nameField = MyClass.class.getDeclaredField("name");
     *               Reflections.setFieldValue(myObj, nameField, "Jane Doe");
     *               }</pre>
     */
    public static void setFieldValue(Object object, Field field, Object value) {
        try {
            field.setAccessible(true);
            field.set(object, value);
        } catch (IllegalAccessException ignore) {
        }
    }

    /**
     * Gets the value of a field from an structured by its field name.
     *
     * @param object    the structured whose field value is to be retrieved
     * @param fieldName the name of the field
     * @return the value of the field, or null if the field does not exist or cannot be accessed
     *
     * <p><b>Example usage:</b></p>
     * <pre>{@code
     * MyClass myObj = new MyClass();
     * Object value = Reflections.getFieldValue(myObj, "name");
     * System.out.println("Field value: " + value);
     * }</pre>
     * @see #setFieldValue(Object, String, Object)
     */
    public static Object getFieldValue(Object object, String fieldName) {
        return getField(object.getClass(), fieldName).map(field -> getFieldValue(object, field)).orElse(null);
    }

    /**
     * Gets the value of a field from an structured.
     *
     * @param object the structured whose field value is to be retrieved
     * @param field  the field to retrieve the value from
     * @return the value of the field, or null if it cannot be accessed
     *
     * <p><b>Example usage:</b></p>
     * <pre>{@code
     * Field nameField = MyClass.class.getDeclaredField("name");
     * Object value = Reflections.getFieldValue(myObj, nameField);
     * System.out.println("Field value: " + value);
     * }</pre>
     */
    public static Object getFieldValue(Object object, Field field) {
        try {
            field.setAccessible(true);
            return field.get(object);
        } catch (IllegalAccessException ignore) {
            return null;
        }
    }

    /**
     * Invokes a method on the specified structured with the given arguments.
     *
     * @param object    the structured on which to invoke the method
     * @param method    the method to invoke
     * @param arguments the arguments to pass to the method
     * @return the result of the method invocation, or {@code null} if the method has a void return type
     * @throws ReflectionException if an exception occurs during invocation
     *
     *                             <p><b>Example usage:</b></p>
     *                             <pre>{@code
     *                             Method method = MyClass.class.getMethod("greet", String.class);
     *                             Object result = Reflections.invokeMethod(myObj, method, "Hello");
     *                             System.out.println("Method returned: " + result);
     *                             }</pre>
     */
    public static Object invokeMethod(Object object, Method method, Object... arguments) {
        try {
            method.setAccessible(true);
            return method.invoke(object, arguments);
        } catch (Throwable e) {
            Throwable targetException = e;

            if (e instanceof InvocationTargetException invocationTargetException) {
                targetException = invocationTargetException.getTargetException();
            }

            throw new ReflectionException(targetException.getMessage(), targetException);
        }
    }

    /**
     * Invokes a getter method (no-argument method starting with "get") on the specified structured.
     *
     * @param object     the structured on which to invoke the getter
     * @param methodName the name of the field (the actual method will be "get" + capitalized field name)
     * @return the result of the getter method, or {@code null} if the method does not exist or fails to invoke
     *
     * <p><b>Example usage:</b></p>
     * <pre>{@code
     * MyClass myObj = new MyClass();
     * Object fieldValue = Reflections.getMethodValue(myObj, "name"); // calls getName()
     * System.out.println("Getter returned: " + fieldValue);
     * }</pre>
     */
    public static Object getMethodValue(Object object, String methodName) {
        return getGetter(object.getClass(), methodName).map(method -> invokeMethod(object, method)).orElse(null);
    }

    /**
     * Finds a method in the specified class by name and parameter types.
     *
     * @param targetClass the class to search in
     * @param methodName  the name of the method
     * @param types       the parameter types
     * @return an optional containing the method if found, or empty if not found
     *
     * <p><b>Example usage:</b></p>
     * <pre>{@code
     * Optional<Method> method = Reflections.getMethod(MyClass.class, "processData", String.class, int.class);
     * method.ifPresent(m -> System.out.println("Method found: " + m.getName()));
     * }</pre>
     */
    public static Optional<Method> getMethod(Class<?> targetClass, String methodName, Class<?>... types) {
        return new MethodFinder().findFirst(targetClass,
                                            MethodMatchers.nameEquals(methodName).and(hasParameterTypes(types)));
    }

    /**
     * Finds a setter method in the specified class based on a field name.
     * The method searched for will be named "set" + capitalized field name.
     *
     * @param targetClass the class to search in
     * @param methodName  the name of the field (not the full setter name)
     * @return an optional containing the setter method if found, or empty if not found
     *
     * <p><b>Example usage:</b></p>
     * <pre>{@code
     * Optional<Method> setterMethod = Reflections.getSetter(MyClass.class, "name"); // looks for setName(...)
     * setterMethod.ifPresent(m -> Reflections.invokeMethod(myObj, m, "New Value"));
     * }</pre>
     */
    public static Optional<Method> getSetter(Class<?> targetClass, String methodName) {
        return getMethod(targetClass, "set" + methodName.substring(0, 1).toUpperCase() + methodName.substring(1));
    }

    /**
     * Finds a getter method in the specified class based on a field name.
     * The method searched for will be named "get" + capitalized field name.
     *
     * @param targetClass the class to search in
     * @param methodName  the name of the field (not the full getter name)
     * @return an optional containing the getter method if found, or empty if not found
     *
     * <p><b>Example usage:</b></p>
     * <pre>{@code
     * Optional<Method> getterMethod = Reflections.getGetter(MyClass.class, "name"); // looks for getName()
     * getterMethod.ifPresent(m -> {
     *     Object value = Reflections.invokeMethod(myObj, m);
     *     System.out.println("Getter value: " + value);
     * });
     * }</pre>
     */
    public static Optional<Method> getGetter(Class<?> targetClass, String methodName) {
        return getMethod(targetClass, "get" + methodName.substring(0, 1).toUpperCase() + methodName.substring(1));
    }

    /**
     * Finds all public methods in the specified class.
     *
     * @param clazz the class to search for methods
     * @return a list of public methods
     *
     * <p><b>Example usage:</b></p>
     * <pre>{@code
     * List<Method> methods = Reflections.getClassMethods(MyClass.class);
     * methods.forEach(method -> System.out.println(method.getName()));
     * }</pre>
     */
    public static Collection<Method> getClassMethods(Class<?> clazz) {
        return new MethodFinder().find(clazz, MethodMatchers.isPublic());
    }

    /**
     * Checks if the specified class contains a method with the given name.
     *
     * @param targetClass the class to inspect
     * @param methodName  the name of the method
     * @return true if the method exists in the class, false otherwise
     *
     * <p><b>Example usage:</b></p>
     * <pre>{@code
     * boolean exists = Reflections.hasMethod(MyClass.class, "toString");
     * System.out.println("Method 'toString' exists? " + exists);
     * }</pre>
     */
    public static boolean hasMethod(Class<?> targetClass, String methodName) {
        return getMethod(targetClass, methodName).isPresent();
    }

    /**
     * Checks if the specified method is an "equals" method (i.e., named "equals" with a single parameter).
     *
     * @param method the method to check
     * @return true if the method is the standard equals method, false otherwise
     *
     * <p><b>Example usage:</b></p>
     * <pre>{@code
     * Method equalsMethod = MyClass.class.getMethod("equals", Object.class);
     * boolean isEquals = Reflections.isEqualsMethod(equalsMethod);
     * System.out.println("Is 'equals' method? " + isEquals);
     * }</pre>
     */
    public static boolean isEqualsMethod(Method method) {
        return method != null && "equals".equals(method.getName()) && method.getParameterCount() == 1;
    }

    /**
     * Checks if the specified method is a "hashCode" method (i.e., named "hashCode" with no parameters).
     *
     * @param method the method to check
     * @return true if the method is the standard hashCode method, false otherwise
     *
     * <p><b>Example usage:</b></p>
     * <pre>{@code
     * Method hashCodeMethod = MyClass.class.getMethod("hashCode");
     * boolean isHash = Reflections.isHashCodeMethod(hashCodeMethod);
     * System.out.println("Is 'hashCode' method? " + isHash);
     * }</pre>
     */
    public static boolean isHashCodeMethod(Method method) {
        return method != null && "hashCode".equals(method.getName()) && method.getParameterCount() == 0;
    }

    /**
     * Retrieves all interfaces implemented by the given class and its superclasses.
     *
     * @param baseClass the class whose interfaces should be retrieved
     * @return an array of implemented interfaces
     *
     * <p><b>Example usage:</b></p>
     * <pre>{@code
     * Class<?>[] interfaces = Reflections.getClassInterfaces(ArrayList.class);
     * for (Class<?> iface : interfaces) {
     *     System.out.println(iface.getName());
     * }
     * }</pre>
     */
    public static Class<?>[] getClassInterfaces(Class<?> baseClass) {
        Set<Class<?>> interfaces = new HashSet<>();

        while (baseClass != null) {
            interfaces.addAll(Set.of(baseClass.getInterfaces()));
            baseClass = baseClass.getSuperclass();
        }

        return interfaces.toArray(Class[]::new);
    }

    /**
     * Retrieves all parent classes (superclasses) of the given class, stopping before {@code Object}.
     *
     * @param baseClass the class whose parent classes should be retrieved
     * @return an array of parent classes
     *
     * <p><b>Example usage:</b></p>
     * <pre>{@code
     * Class<?>[] superClasses = Reflections.getSuperClasses(HashMap.class);
     * for (Class<?> superClass : superClasses) {
     *     System.out.println(superClass.getName());
     * }
     * }</pre>
     */
    public static Class<?>[] getSuperClasses(Class<?> baseClass) {
        Set<Class<?>> classes = new HashSet<>();

        while (baseClass != null && baseClass != Object.class) {
            classes.add(baseClass);
            baseClass = baseClass.getSuperclass();
        }

        return classes.toArray(Class[]::new);
    }

    /**
     * Converts an array of objects to their corresponding array of classes.
     *
     * @param arguments an array of objects
     * @return an array of classes representing each structured's type
     *
     * <p><b>Example usage:</b></p>
     * <pre>{@code
     * Class<?>[] argTypes = Reflections.getArgumentsTypes("Hello", 123, null);
     * // argTypes = [class java.lang.String, class java.lang.Integer, class java.lang.Object]
     * }</pre>
     */
    public static Class<?>[] getArgumentsTypes(Object... arguments) {
        Class<?>[] classes = new Class<?>[arguments.length];

        for (int i = 0; i < arguments.length; i++) {
            classes[i] = arguments[i] != null ? arguments[i].getClass() : Object.class;
        }

        return classes;
    }

    public static Class<?>[] getParameters(Executable executable) {
        return executable.getParameterTypes();
    }

    public static Class<?>[] getRecordComponentsTypes(Class<?> record) {
        Class<?>[] classes = null;

        if (record.isRecord()) {
            RecordComponent[] components = record.getRecordComponents();
            classes = new Class<?>[components.length];
            for (int i = 0; i < components.length; i++) {
                classes[i] = components[i].getType();
            }
        }

        return classes;
    }

    /**
     * Returns a string representing the executable name along with its declaring class's simple name.
     *
     * @param executable the executable to retrieve the name for
     * @return a string in the format {@code ClassName#methodName}
     */
    public static String getMethodName(Executable executable) {
        return "%s#%s".formatted(executable.getDeclaringClass().getSimpleName(), executable.getName());
    }

    public static String getPropertyName(Method method, String prefix) {
        return uncapitalize(method.getName().substring(prefix.length()));
    }

    /**
     * Returns a string representing the field name along with its declaring class's simple name.
     * <p>
     * This method formats the string as {@code ClassName#fieldName}.
     * </p>
     *
     * @param field the field to retrieve the name for
     * @return a string in the format {@code ClassName#fieldName}
     */
    public static String getFieldName(Field field) {
        return "%s.%s".formatted(field.getDeclaringClass().getSimpleName(), field.getName());
    }

    /**
     * Returns a string representing the method's parameter name.
     *
     * @param parameter the parameter to retrieve the name for
     * @return a string in the format {@code ClassName#methodName@arg0}
     */
    public static String getParameterName(Parameter parameter) {
        return "%s@%s".formatted(getMethodName(parameter.getDeclaringExecutable()), parameter.getName());
    }

    /**
     * Returns the short name of the class for the given structured.
     * If the structured is itself a {@link Class}, it delegates to the {@link #getShortName(Class)} method.
     *
     * @param instance the structured whose class name is to be shortened
     * @return the shortened class name
     */
    public static String getShortName(Object instance) {
        Class<?> classType = instance.getClass();

        if (instance instanceof Class<?> type) {
            classType = type;
        }

        return getShortName(classType);
    }

    /**
     * Returns the short name of the given class.
     * The short name is the substring of the class name after the last {@code '.'} character,
     * unless the class is an array or a primitive type, in which case the full name is returned.
     *
     * @param clazz the class whose name is to be shortened
     * @return the shortened class name
     */
    public static String getShortName(Class<?> clazz) {
        String className = clazz.getName();

        if (!(clazz.isArray() || clazz.isPrimitive())) {
            int lastIndex = className.lastIndexOf(PACKAGE_SEPARATOR);
            if (lastIndex != -1) {
                className = className.substring(lastIndex + 1);
            }
        }

        return className;
    }


    /**
     * Extracts parameterized types from the interfaces implemented by the specified class.
     *
     * <p>Only interfaces that use generic type parameters will be included in the returned map.
     * The map's keys are the raw interface types, and the values are the parameterized types used by those interfaces.</p>
     *
     * @param type the class to analyze
     * @return a map where keys are the raw types of implemented interfaces and values are lists of parameterized types
     * used in those interfaces
     * @throws NullPointerException if {@code type} is null
     *
     *                              <p><b>Example usage:</b></p>
     *                              <pre>{@code
     *                              class MyClass implements Comparable<MyClass> {}
     *                              Map<Class<?>, List<Class<?>>> result = Reflections.getInterfacesParameterizedTypes(MyClass.class);
     *                              System.out.println(result); // Output: {interface java.lang.Comparable=[class MyClass]}
     *                              }</pre>
     */
    public static Map<Class<?>, List<Class<?>>> getInterfacesParameterizedTypes(Class<?> type) {
        Map<Class<?>, List<Class<?>>> parameterizedTypes = new HashMap<>();

        for (Type genericInterface : type.getGenericInterfaces()) {
            if (genericInterface instanceof ParameterizedType parameterizedType) {
                parameterizedTypes.computeIfAbsent((Class<?>) parameterizedType.getRawType(),
                                                   key -> Reflections.getParameterizedTypes(parameterizedType));
            }
        }

        return parameterizedTypes;
    }

    /**
     * Extracts parameterized types from the direct superclass of the specified class.
     *
     * @param type the class to analyze
     * @return a list of classes representing the parameterized types of the superclass
     * @throws NullPointerException if {@code type} is null
     *
     *                              <p><b>Example usage:</b></p>
     *                              <pre>{@code
     *                              class MyClass extends HashMap<String, Integer> {}
     *                              List<Class<?>> result = Reflections.getSuperclassParameterizedTypes(MyClass.class);
     *                              System.out.println(result); // Output: [class java.lang.String, class java.lang.Integer]
     *                              }</pre>
     */
    public static List<Class<?>> getSuperclassParameterizedTypes(Class<?> type) {
        return getParameterizedTypes(type.getGenericSuperclass());
    }

    /**
     * Retrieves the parameterized type of a superclass at the specified index.
     * <p>
     * This method extracts the generic type parameters of a class's superclass
     * and returns the type at the given index. If the index is out of bounds or
     * the superclass does not have parameterized types, {@code null} is returned.
     *
     * @param type  the class whose superclass parameterized type is to be retrieved.
     * @param index the index of the parameterized type to retrieve.
     * @return the parameterized type at the specified index, or {@code null} if not available.
     *
     * <p>Example Usage:</p>
     * <pre>{@code
     * class Base<T> {}
     * class Derived extends Base<String> {}
     *
     * Class<?> parameterizedType = Reflections.getSuperclassParameterizedType(Derived.class, 0);
     * System.out.println(parameterizedType); // Outputs: class java.lang.String
     * }</pre>
     */
    public static Class<?> getSuperclassParameterizedType(Class<?> type, int index) {
        List<Class<?>> classes = getSuperclassParameterizedTypes(type);

        if (index < classes.size()) {
            return classes.get(index);
        }

        return null;
    }

    /**
     * Extracts parameterized types from the provided {@link Type} (which may be a {@link ParameterizedType}).
     *
     * @param type the type to analyze
     * @return a list of classes representing the parameterized types
     * @throws NullPointerException if {@code type} is null
     *
     *                              <p><b>Example usage:</b></p>
     *                              <pre>{@code
     *                              Type genericSuperclass = MyClass.class.getGenericSuperclass();
     *                              List<Class<?>> parameterized = Reflections.getParameterizedTypes(genericSuperclass);
     *                              System.out.println(parameterized);
     *                              }</pre>
     */
    public static List<Class<?>> getParameterizedTypes(Type type) {
        List<Class<?>> parameterizedTypes = new ArrayList<>();

        if (type instanceof ParameterizedType parameterizedType) {
            for (Type actualTypeArgument : parameterizedType.getActualTypeArguments()) {
                parameterizedTypes.add(toClass(actualTypeArgument));
            }
        }

        return parameterizedTypes;
    }

    /**
     * Retrieves a list of parameterized types for a specific interface implemented by the given class.
     *
     * @param type  the class to analyze
     * @param iface the interface whose parameterized types are to be retrieved
     * @return a list of parameterized types if present; otherwise, an empty {@code List}
     * @throws NullPointerException if {@code type} or {@code iface} is null
     *
     *                              <p><b>Example usage:</b></p>
     *                              <pre>{@code
     *                              class MyClass implements Comparable<MyClass> {}
     *                              List<Class<?>> result = Reflections.getInterfacesParameterizedTypes(MyClass.class, Comparable.class);
     *                              System.out.println(result); // Output: [class MyClass]
     *                              }</pre>
     */
    public static List<Class<?>> getInterfacesParameterizedTypes(Class<?> type, Class<?> iface) {
        return getInterfacesParameterizedTypes(type).getOrDefault(iface, Collections.emptyList());
    }

    /**
     * Retrieves a specific parameterized type used in a particular interface implemented by the given class.
     *
     * @param type  the class to analyze
     * @param iface the interface whose parameterized type is to be retrieved
     * @param index the index of the parameterized type to retrieve (zero-based)
     * @return the parameterized type if present; otherwise {@code null}
     * @throws NullPointerException if {@code type} or {@code iface} is null
     *
     *                              <p><b>Example usage:</b></p>
     *                              <pre>{@code
     *                              class MyClass implements Comparable<MyClass> {}
     *                              Class<?> paramType = Reflections.getInterfacesParameterizedType(MyClass.class, Comparable.class, 0);
     *                              System.out.println(paramType); // Output: class MyClass
     *                              }</pre>
     */
    public static Class<?> getInterfacesParameterizedType(Class<?> type, Class<?> iface, int index) {
        List<Class<?>> classes = getInterfacesParameterizedTypes(type, iface);

        if (index < classes.size()) {
            return classes.get(index);
        }

        return null;
    }

    /**
     * Converts a {@link Type} to its corresponding {@link Class} representation.
     * <p>If {@code type} is a generic array type, it is converted to an array class of its component type.</p>
     *
     * @param type the type to convert
     * @return the corresponding class representation
     * @throws NullPointerException if {@code type} is null
     * @throws ClassCastException   if the type cannot be converted to a class
     *
     *                              <p><b>Example usage:</b></p>
     *                              <pre>{@code
     *                              ParameterizedType paramType = (ParameterizedType) SomeClass.class.getGenericSuperclass();
     *                              Class<?> clazz = Reflections.toClass(paramType.getActualTypeArguments()[0]);
     *                              System.out.println(clazz);
     *                              }</pre>
     */
    public static Class<?> toClass(Type type) {
        Class<?> clazz;

        if (type instanceof GenericArrayType arrayType) {
            Class<?> arrayClass = toClass(arrayType.getGenericComponentType());
            clazz = Array.newInstance(arrayClass, 0).getClass();
        } else if (type instanceof ParameterizedType parameterizedType) {
            clazz = (Class<?>) parameterizedType.getRawType();
        } else {
            clazz = (Class<?>) type;
        }

        return clazz;
    }

    /**
     * Gets the nearest non-anonymous class by traversing superclasses if the provided class is anonymous.
     *
     * @param classType the class to check
     * @return the first non-anonymous superclass or the class itself if it is not anonymous
     *
     * <p><b>Example usage:</b></p>
     * <pre>{@code
     * Class<?> actualClass = Reflections.getAnonymousClass(someAnonymousInstance.getClass());
     * System.out.println(actualClass.getName());
     * }</pre>
     */
    public static Class<?> getAnonymousClass(Class<?> classType) {
        Class<?> userClass = classType;

        while (userClass.isAnonymousClass()) {
            userClass = userClass.getSuperclass();
        }

        return userClass;
    }

    /**
     * If the specified class is an array, returns the component type; otherwise returns the class itself.
     *
     * @param clazz the class to check
     * @return the unwrapped class if the provided class is an array; otherwise returns the same class
     *
     * <p><b>Example usage:</b></p>
     * <pre>{@code
     * Class<?> arrayClass = String[].class;
     * Class<?> unwrapped = Reflections.getArrayClass(arrayClass);
     * System.out.println(unwrapped.getName()); // java.lang.String
     * }</pre>
     */
    public static Class<?> getArrayClass(Class<?> clazz) {
        Class<?> userClass = clazz;

        if (clazz.isArray()) {
            userClass = clazz.getComponentType();
        }

        return userClass;
    }

    /**
     * Unwraps the actual class from a proxy class.
     * <p>If the class name contains {@link #PROXY_CLASS_NAME_SEPARATOR}, this method attempts
     * to retrieve its superclass, which should be the actual class.</p>
     *
     * @param classType the class to unwrap
     * @return the unwrapped class, or the original class if it's not a proxy
     *
     * <p><b>Example usage:</b></p>
     * <pre>{@code
     * // Suppose proxyClass is an instance of a proxy for SomeClass
     * Class<?> originalClass = Reflections.getUserClass(proxyClass);
     * System.out.println("Unwrapped class: " + originalClass.getName());
     * }</pre>
     */
    public static Class<?> getUserClass(Class<?> classType) {
        Class<?> userClass = classType;

        if (classType.getName().contains(PROXY_CLASS_NAME_SEPARATOR)) {
            Class<?> superclass = classType.getSuperclass();
            if (superclass != null && superclass != Object.class) {
                userClass = superclass;
            }
        }

        return userClass;
    }

    /**
     * Unwraps the actual class from a proxy class.
     * <p>If the class name contains {@link #PROXY_CLASS_NAME_SEPARATOR}, this method attempts
     * to retrieve its superclass, which should be the actual class.</p>
     *
     * @param instance the structured which class will be unwrapped
     * @return the unwrapped class, or the original class if it's not a proxy
     *
     * <p><b>Example usage:</b></p>
     * <pre>{@code
     * Class<?> originalClass = Reflections.getUserClass(proxyObject);
     * System.out.println("Unwrapped class: " + originalClass.getName());
     * }</pre>
     */
    public static Class<?> getUserClass(Object instance) {
        return getUserClass(instance.getClass());
    }

    /**
     * Populates a map with property descriptors for a given class, using the property names as keys.
     *
     * @param type                the class to introspect.
     * @param propertyDescriptors the map to populate with property descriptors.
     * @throws ReflectionException if an error occurs during introspection.
     */
    public static void readPropertyDescriptors(Class<?> type, Map<String, PropertyDescriptor> propertyDescriptors) {
        for (PropertyDescriptor propertyDescriptor : getPropertyDescriptors(type)) {
            propertyDescriptors.put(propertyDescriptor.getName(), propertyDescriptor);
        }
    }

    /**
     * Retrieves a list of {@link PropertyDescriptor} objects for the specified class.
     *
     * @param type the class to introspect.
     * @return a list of property descriptors for the given class.
     * @throws ReflectionException if an error occurs during introspection.
     */
    public static List<PropertyDescriptor> getPropertyDescriptors(Class<?> type) {
        try {
            BeanInfo beanInfo = Introspector.getBeanInfo(type);
            return List.of(beanInfo.getPropertyDescriptors());
        } catch (IntrospectionException e) {
            throw new ReflectionException(e);
        }
    }

    /**
     * Retrieves the value of a specified annotation attribute from an annotated element.
     *
     * @param element         the annotated element to inspect.
     * @param annotationClass the class of the annotation to retrieve.
     * @param extractor       a function to extract the desired attribute value from the annotation.
     * @param <A>             the type of the annotation.
     * @param <R>             the type of annotation method return type.
     * @return the extracted attribute value, or {@code null} if the annotation is not present.
     */
    public static <A extends Annotation, R> R getAnnotationValue(
            AnnotatedElement element, Class<? extends A> annotationClass, Function<A, R> extractor) {
        R value = null;

        if (element.isAnnotationPresent(annotationClass)) {
            A annotation = element.getAnnotation(annotationClass);
            value = extractor.apply(annotation);
        }

        return value;
    }

    /**
     * Checks if the given class belongs to a JRT (Java Runtime) module.
     *
     * @param clazz the class to check
     * @return {@code true} if the class is part of a JRT module, {@code false} otherwise
     * <p>
     * This method evaluates the module of the given class and checks if it matches any of the known
     * Java modules in {@code JAVA_MODULE_NAMES}.
     */
    public static boolean isJrtResource(Class<?> clazz) {
        return JAVA_MODULE_NAMES.contains(clazz.getModule());
    }

}
