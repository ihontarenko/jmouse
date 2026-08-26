package org.jmouse.core.access.descriptor.structured;

import org.jmouse.core.access.descriptor.structured.bean.JavaBeanDescriptor;
import org.jmouse.core.access.descriptor.structured.bean.JavaBeanIntrospector;
import org.jmouse.core.access.descriptor.structured.record.ValueObjectDescriptor;
import org.jmouse.core.access.descriptor.structured.record.ValueObjectIntrospector;

/**
 * Where a type's description is looked up, and where it is built exactly once. 🗃️
 *
 * <h3>⚠️ Why a {@link ClassValue} and not a map</h3>
 * <p>This is asked for <em>every object wrapped</em> by the accessor layer - the source and the target
 * of every mapped object, plus every nested value - so it is one of the hottest lookups in the mapping
 * engine. It used to be a {@link java.util.concurrent.ConcurrentHashMap} consulted through
 * {@code computeIfAbsent}, which allocates the capturing lambda on every call whether or not the entry
 * is missing, and answers a hit more slowly than a plain read. Profiling the nested path put it at
 * <b>5,6%</b> of the engine's time, all of it re-answering a question settled by the first object.</p>
 *
 * <p>A {@code ClassValue} keeps its entry on the class itself, so a hit is a read rather than a hashed
 * lookup, and it is what the JDK uses for exactly this shape of cache.</p>
 *
 * <p>⚠️ It also fixes a leak that a map could not: a {@code Map<Class<?>, …>} holds every class it has
 * ever described strongly, forever, so a container that loads and discards classloaders keeps all of
 * them alive. A {@code ClassValue} entry dies with its class.</p>
 *
 * <p>Beans and value objects are kept apart rather than sharing one entry per class. Sharing meant the
 * first caller decided which description a class got, and a record asked for as a bean afterwards was
 * handed the wrong one - order-dependent, and never what anybody wanted.</p>
 */
@SuppressWarnings("unchecked")
public final class DescriptorResolver {

    private static final ClassValue<JavaBeanDescriptor<?>> BEANS = new ClassValue<>() {
        @Override
        protected JavaBeanDescriptor<?> computeValue(Class<?> type) {
            return new JavaBeanIntrospector<>(type).introspect().toDescriptor();
        }
    };

    private static final ClassValue<ValueObjectDescriptor<?>> VALUE_OBJECTS = new ClassValue<>() {
        @Override
        @SuppressWarnings({"unchecked", "rawtypes"})
        protected ValueObjectDescriptor<?> computeValue(Class<?> type) {
            return new ValueObjectIntrospector<>((Class<? extends Record>) type).introspect().toDescriptor();
        }
    };

    private DescriptorResolver() {
    }

    /**
     * Describe a type the way the type itself asks to be described.
     *
     * @param type type to describe
     * @param <T> described type
     * @return a value-object description for a record, a bean description otherwise
     */
    public static <T> ObjectDescriptor<T> describe(Class<T> type) {
        return type.isRecord()
                ? (ObjectDescriptor<T>) ofRecordType((Class<? extends Record>) type)
                : ofBeanType(type);
    }

    /**
     * Describe a type as a JavaBean - its readable and writable properties.
     *
     * @param type type to describe
     * @param <T> described type
     * @return bean description, built on the first request for this class
     */
    public static <T> JavaBeanDescriptor<T> ofBeanType(Class<T> type) {
        return (JavaBeanDescriptor<T>) BEANS.get(type);
    }

    /**
     * Describe a record as a value object - its components.
     *
     * @param type record type to describe
     * @param <R> described record type
     * @return value-object description, built on the first request for this class
     */
    public static <R extends Record> ValueObjectDescriptor<R> ofRecordType(Class<R> type) {
        return (ValueObjectDescriptor<R>) VALUE_OBJECTS.get(type);
    }

    // ⚠️ There was an ofMap(Map) here, memoizing a descriptor under map.hashCode(). Two unrelated maps
    // that hash alike shared a descriptor, and a map's hash moves as its contents do, so an entry was
    // kept for every state a map ever passed through. Nothing called it. Describe a map through
    // MapIntrospector directly - a map's description belongs to the instance, and is not cacheable by
    // anything a map can be keyed on.

}
