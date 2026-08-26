package org.jmouse.core;

import org.jmouse.core.reflection.Reflections;

import java.lang.invoke.CallSite;
import java.lang.invoke.LambdaMetafactory;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.reflect.Constructor;
import java.lang.reflect.Executable;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.function.Supplier;

import static org.jmouse.core.reflection.Reflections.getMethodName;

/**
 * Turns a {@link Method} into a {@link Getter} or a {@link Setter} that calls it directly. ⚡
 *
 * <p>The obvious implementation holds the {@link Method} and calls {@link Method#invoke} on it, and
 * that is what this used to be. {@code invoke} is a reflective call every single time: it re-checks
 * access, boxes the arguments into an {@code Object[]}, and hands control to a
 * {@code DirectMethodHandleAccessor} the JIT cannot see through. Profiling the flat bean path put
 * <b>34%</b> of the mapping engine's time inside that one frame - one call per property read plus one
 * per property write, on every object mapped.</p>
 *
 * <p>{@link LambdaMetafactory} spins a small class <em>once</em>, at the moment the accessor is built,
 * whose body is a plain call to the target method. From then on a read is an interface call the JIT
 * inlines like any other lambda, and the reflection layer is not on the path at all.</p>
 *
 * <h3>When the JVM refuses</h3>
 * <p>Spinning a call site needs access the runtime may not grant - a method in a module that opens
 * nothing to us, most obviously. Every such refusal falls back to {@link Method#invoke}, so a type
 * that could be mapped before is still mapped; it is only slower. Nothing here throws because a call
 * site could not be built.</p>
 *
 * <h3>Why the results are cached</h3>
 * <p>⚠️ Each compiled accessor is a class the JVM has to define, so building one per call would grow
 * metaspace without bound - and {@link Getter#ofMethod} is public API that a caller may well reach for
 * inside a loop. Entries hang off a {@link ClassValue}, so they are collected together with the class
 * that declared the method rather than pinning it alive.</p>
 *
 * @see Getter#ofMethod(Method)
 * @see Setter#ofMethod(Method)
 */
public final class MethodAccessorFactory {

    private static final MethodHandles.Lookup LOOKUP = MethodHandles.lookup();

    private static final ClassValue<Map<Method, Getter<?, ?>>> GETTERS = new ClassValue<>() {
        @Override
        protected Map<Method, Getter<?, ?>> computeValue(Class<?> owner) {
            return new ConcurrentHashMap<>();
        }
    };

    private static final ClassValue<Map<Method, Setter<?, ?>>> SETTERS = new ClassValue<>() {
        @Override
        protected Map<Method, Setter<?, ?>> computeValue(Class<?> owner) {
            return new ConcurrentHashMap<>();
        }
    };

    /**
     * ⚠️ Stands in for "this type has no no-argument constructor", because {@link ClassValue} has no
     * way to say "computed, and the answer is nothing" other than a value. Caching the absence matters
     * as much as caching the presence: without it, every mapped object of a record or of a type built
     * through a factory pays a failed reflective lookup.
     */
    private static final Supplier<?> ABSENT = () -> null;

    private static final ClassValue<Supplier<?>> CONSTRUCTORS = new ClassValue<>() {
        @Override
        protected Supplier<?> computeValue(Class<?> type) {
            Supplier<?> built = buildConstructor(type);

            return built == null ? ABSENT : built;
        }
    };

    private MethodAccessorFactory() {
    }

    /**
     * A supplier that calls {@code type}'s no-argument constructor.
     *
     * <p>The same trick as {@link #getter} and {@link #setter}, applied to construction:
     * {@link LambdaMetafactory} spins the call site <em>once</em> and every instance afterwards costs a
     * direct {@code new}, rather than a reflective {@code Constructor.newInstance} per object.</p>
     *
     * <p>⚠️ <strong>A type with no accessible no-argument constructor answers {@code null}, and that is
     * not an error here.</strong> Whether it is one belongs to the caller — the mapping engine reports
     * it as a failure to instantiate, a binder may reasonably have another answer, and a factory that
     * decided on its own would be making somebody else's decision. The absence is cached like any
     * other answer.</p>
     *
     * @param type the type to construct
     * @param <T> the type to construct
     * @return a supplier of fresh instances, or {@code null} when there is nothing to construct with
     */
    @SuppressWarnings("unchecked")
    public static <T> Supplier<T> constructor(Class<T> type) {
        Supplier<?> supplier = CONSTRUCTORS.get(type);

        return supplier == ABSENT ? null : (Supplier<T>) supplier;
    }

    /**
     * Build the supplier for one type: a compiled call site, or reflection when that is refused.
     *
     * @param type the type to construct
     * @return a supplier, or {@code null} when the type declares no no-argument constructor
     */
    private static Supplier<?> buildConstructor(Class<?> type) {
        Constructor<?> constructor;

        try {
            constructor = type.getDeclaredConstructor();
        } catch (NoSuchMethodException | SecurityException absent) {
            return null;
        }

        openUp(constructor);

        Supplier<?> compiled = compileConstructor(constructor);

        return compiled == null ? reflectiveConstructor(constructor) : compiled;
    }

    /**
     * Compile {@code constructor} into a {@link Supplier}, or answer {@code null} when it cannot be.
     *
     * <p>⚠️ This does not go through {@link #compile}: that unreflects a <em>method</em>, and a
     * constructor needs {@link MethodHandles.Lookup#unreflectConstructor}. Everything else about the
     * shape is the same, which is why the two sit beside each other rather than one being bent to
     * accept the other.</p>
     *
     * @param constructor the no-argument constructor to call
     * @return the generated supplier, or {@code null} when the runtime refused
     */
    private static Supplier<?> compileConstructor(Constructor<?> constructor) {
        MethodHandles.Lookup lookup = lookupFor(constructor);

        if (lookup == null) {
            return null;
        }

        try {
            MethodHandle handle   = lookup.unreflectConstructor(constructor);
            CallSite     callSite = LambdaMetafactory.metafactory(
                    lookup, "get", MethodType.methodType(Supplier.class),
                    MethodType.methodType(Object.class), handle,
                    MethodType.methodType(constructor.getDeclaringClass())
            );

            return (Supplier<?>) callSite.getTarget().invoke();
        } catch (Throwable refused) {
            return null;
        }
    }

    /**
     * Construct reflectively, for a call site the runtime would not spin.
     *
     * <p>Never throwing <em>because of how</em> it would construct is the rule the getters and setters
     * already follow: a type that cannot be compiled still works, more slowly.</p>
     *
     * @param constructor the no-argument constructor to call
     * @return a supplier that calls it
     */
    private static Supplier<?> reflectiveConstructor(Constructor<?> constructor) {
        return () -> {
            try {
                return constructor.newInstance();
            } catch (ReflectiveOperationException | RuntimeException thrown) {
                throw new IllegalStateException(
                        "could not construct '%s'".formatted(constructor.getDeclaringClass().getName()),
                        thrown);
            }
        };
    }

    /**
     * A getter that calls {@code method} on the instance it is given.
     *
     * @param method no-argument method to read through
     * @param <T> type declaring the method
     * @param <V> value the method returns
     * @return getter for that method, compiled where the runtime allows it
     */
    @SuppressWarnings("unchecked")
    public static <T, V> Getter<T, V> getter(Method method) {
        return (Getter<T, V>) GETTERS.get(method.getDeclaringClass())
                .computeIfAbsent(method, MethodAccessorFactory::buildGetter);
    }

    /**
     * A setter that calls {@code method} on the instance it is given.
     *
     * @param method single-argument method to write through
     * @param <T> type declaring the method
     * @param <V> value the method accepts
     * @return setter for that method, compiled where the runtime allows it
     */
    @SuppressWarnings("unchecked")
    public static <T, V> Setter<T, V> setter(Method method) {
        return (Setter<T, V>) SETTERS.get(method.getDeclaringClass())
                .computeIfAbsent(method, MethodAccessorFactory::buildSetter);
    }

    /**
     * Build the getter for one method: a compiled call site, or reflection when that is refused.
     *
     * @param method method to read through
     * @return getter for that method
     */
    private static Getter<?, ?> buildGetter(Method method) {
        openUp(method);

        Function<Object, Object> call       = compileGetter(method);
        Class<?>                 returnType = method.getReturnType();

        if (call == null) {
            return reflectiveGetter(method, returnType);
        }

        return (Object instance) -> {
            try {
                return call.apply(instance);
            } catch (Throwable thrown) {
                return failedRead(method, returnType, thrown);
            }
        };
    }

    /**
     * Build the setter for one method: a compiled call site, or reflection when that is refused.
     *
     * @param method method to write through
     * @return setter for that method
     */
    private static Setter<?, ?> buildSetter(Method method) {
        openUp(method);

        BiConsumer<Object, Object> call = compileSetter(method);

        if (call == null) {
            return reflectiveSetter(method);
        }

        return (Object instance, Object value) -> {
            try {
                call.accept(instance, value);
            } catch (Throwable thrown) {
                throw failedWrite(method, thrown);
            }
        };
    }

    /**
     * The getter of last resort, going through {@link Method#invoke}.
     *
     * <p>{@code setAccessible} is done above, once, rather than on every call - it was previously the
     * first statement of every read.</p>
     *
     * @param method method to read through
     * @param returnType what it returns, for the primitive recovery below
     * @return reflective getter
     */
    private static Getter<Object, Object> reflectiveGetter(Method method, Class<?> returnType) {
        return (Object instance) -> {
            try {
                return method.invoke(instance);
            } catch (InvocationTargetException invocationFailure) {
                return failedRead(method, returnType, invocationFailure.getTargetException());
            } catch (Throwable thrown) {
                throw new Getter.GetterCallException(
                        "Failed to call getter '%s'".formatted(getMethodName(method)), thrown);
            }
        };
    }

    /**
     * The setter of last resort, going through {@link Method#invoke}.
     *
     * @param method method to write through
     * @return reflective setter
     */
    private static Setter<Object, Object> reflectiveSetter(Method method) {
        return (Object instance, Object value) -> {
            try {
                method.invoke(instance, value);
            } catch (Throwable thrown) {
                throw failedWrite(method, thrown);
            }
        };
    }

    /**
     * Settle a getter whose body threw.
     *
     * <p>⚠️ A primitive-returning getter answers with the type's default instead of failing, which is
     * behaviour this class inherited and deliberately keeps: a bean whose accessor throws on an unset
     * field is mappable, and a {@code long} property has no null to report. Everything else is a real
     * failure and is reported as one.</p>
     *
     * <p>The distinction used to be drawn on {@link InvocationTargetException}, because that is the
     * only shape {@code invoke} produces. A compiled call site throws the body's own throwable
     * untouched, so the return type is what the decision turns on now - the same outcome, reached
     * without a wrapper that no longer exists.</p>
     *
     * @param method the getter that failed
     * @param returnType what it returns
     * @param thrown what its body threw
     * @return the primitive default, when there is one
     * @throws Getter.GetterCallException for every reference-returning getter
     */
    private static Object failedRead(Method method, Class<?> returnType, Throwable thrown) {
        if (returnType.isPrimitive()) {
            return Reflections.PRIMITIVES_DEFAULT_TYPE_VALUES.get(returnType);
        }

        throw new Getter.GetterCallException(
                "Failed to call getter '%s'".formatted(getMethodName(method)), thrown);
    }

    /**
     * Report a setter whose body threw.
     *
     * @param method the setter that failed
     * @param thrown what its body threw
     * @return the failure to raise, so the caller reads as a throw
     */
    private static Setter.SetterCallException failedWrite(Method method, Throwable thrown) {
        return new Setter.SetterCallException(
                "Failed to call setter '%s'".formatted(getMethodName(method)), thrown);
    }

    /**
     * Compile {@code method} into a {@link Function}, or answer {@code null} when it cannot be.
     *
     * @param method method to read through
     * @return compiled call site, or {@code null} to fall back to reflection
     */
    @SuppressWarnings("unchecked")
    private static Function<Object, Object> compileGetter(Method method) {
        if (Modifier.isStatic(method.getModifiers()) || method.getParameterCount() != 0
                || method.getReturnType() == void.class) {
            return null;
        }

        return (Function<Object, Object>) compile(
                method,
                Function.class,
                "apply",
                MethodType.methodType(Object.class, Object.class),
                MethodType.methodType(Reflections.boxType(method.getReturnType()), method.getDeclaringClass())
        );
    }

    /**
     * Compile {@code method} into a {@link BiConsumer}, or answer {@code null} when it cannot be.
     *
     * <p>A fluent setter returning {@code this} compiles too: the instantiated type declares
     * {@code void} and the metafactory drops the returned value.</p>
     *
     * @param method method to write through
     * @return compiled call site, or {@code null} to fall back to reflection
     */
    @SuppressWarnings("unchecked")
    private static BiConsumer<Object, Object> compileSetter(Method method) {
        if (Modifier.isStatic(method.getModifiers()) || method.getParameterCount() != 1) {
            return null;
        }

        return (BiConsumer<Object, Object>) compile(
                method,
                BiConsumer.class,
                "accept",
                MethodType.methodType(void.class, Object.class, Object.class),
                MethodType.methodType(
                        void.class, method.getDeclaringClass(),
                        Reflections.boxType(method.getParameterTypes()[0]))
        );
    }

    /**
     * Ask {@link LambdaMetafactory} for an implementation of {@code functionalInterface} that calls
     * {@code method}.
     *
     * @param method method the generated body calls
     * @param functionalInterface interface to implement
     * @param name its single abstract method
     * @param erasedType that method's erased signature
     * @param instantiatedType the signature this particular accessor really has, boxing included
     * @return the generated instance, or {@code null} when the runtime refused
     */
    private static Object compile(
            Method method,
            Class<?> functionalInterface,
            String name,
            MethodType erasedType,
            MethodType instantiatedType
    ) {
        MethodHandles.Lookup lookup = lookupFor(method);

        if (lookup == null) {
            return null;
        }

        try {
            MethodHandle handle   = lookup.unreflect(method);
            CallSite     callSite = LambdaMetafactory.metafactory(
                    lookup, name, MethodType.methodType(functionalInterface),
                    erasedType, handle, instantiatedType
            );

            return callSite.getTarget().invoke();
        } catch (Throwable refused) {
            return null;
        }
    }

    /**
     * The lookup to compile {@code method} through.
     *
     * @param method method about to be compiled
     * @return a usable lookup, or {@code null} when there is none
     */
    private static MethodHandles.Lookup lookupFor(Executable member) {
        Class<?> owner = member.getDeclaringClass();

        try {
            return MethodHandles.privateLookupIn(owner, LOOKUP);
        } catch (IllegalAccessException | SecurityException refused) {
            // ⚠️ Without private access into the owner's package the generated class is defined in
            // THIS one, so it can only name what this package can reach. Anything less than public on
            // both the member and its owner links at the first call rather than here - a failure
            // raised long after the point that could still have chosen reflection instead. Refused
            // now, while there is somewhere to fall back to.
            boolean reachable = Modifier.isPublic(member.getModifiers())
                    && Modifier.isPublic(owner.getModifiers());

            return reachable ? LOOKUP : null;
        }
    }

    /**
     * Clear the access check on a method that needs it, once.
     *
     * <p>Refusal is not a failure here: it means this method will be compiled through a private lookup
     * or not at all, and both of those answer for themselves.</p>
     *
     * @param method method about to be read or written through
     */
    private static void openUp(Executable member) {
        boolean open = Modifier.isPublic(member.getModifiers())
                && Modifier.isPublic(member.getDeclaringClass().getModifiers());

        if (open) {
            return;
        }

        try {
            member.setAccessible(true);
        } catch (RuntimeException refused) {
            // Left as it is; the caller falls back to whatever still works.
        }
    }
}
