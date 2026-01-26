package org.jmouse.core;

import java.lang.reflect.Constructor;
import java.util.*;

import static org.jmouse.core.Verify.nonNull;
import static org.jmouse.core.Verify.state;

/**
 * Factory utilities for creating {@link Collection} instances by requested collection type,
 * with sensible defaults for common collection interfaces.
 *
 * <h2>Examples</h2>
 * <pre>{@code
 * // Create by interface type
 * List<String> list = CollectionFactory.createCollection(List.class, 32); // ArrayList(32)
 * Set<Long> set   = CollectionFactory.createCollection(Set.class, 64);   // LinkedHashSet(64+)
 * Deque<Integer> q = CollectionFactory.createCollection(Deque.class, 10); // ArrayDeque(16+)
 *
 * // Create a collection "like" an existing one
 * Collection<Integer> likeSet  = CollectionFactory.createLike(Set.of(1, 2, 3), 8, Collection.class);
 * Collection<Integer> likeList = CollectionFactory.createLike(List.of(1, 2), 0, Collection.class);
 *
 * // EnumSet requires enum type
 * enum Color { RED, GREEN }
 * EnumSet<Color> colors = CollectionFactory.createEnumSet(Color.class);
 * }</pre>
 *
 * <h3>Selection rules</h3>
 * <ul>
 *   <li>If {@code collectionType} is an interface (e.g., {@link List}, {@link Set}, {@link Queue}),
 *       the factory chooses a default implementation.</li>
 *   <li>If {@code collectionType} is a known concrete class (e.g., {@link ArrayList}, {@link HashSet}),
 *       the factory instantiates that concrete type directly.</li>
 *   <li>Otherwise, it attempts reflective instantiation via a no-arg constructor.</li>
 * </ul>
 *
 * <h3>Capacity semantics</h3>
 * <ul>
 *   <li>For {@link ArrayList}, {@code initialCapacity} is used directly.</li>
 *   <li>For {@link HashSet}/{@link LinkedHashSet}/{@link ArrayDeque}, capacity is
 *       {@code max(DEFAULT_CAPACITY, initialCapacity)} to avoid tiny internal tables.</li>
 *   <li>For {@link TreeSet}, {@code initialCapacity} is ignored.</li>
 * </ul>
 *
 * <p><strong>Note:</strong> {@link EnumSet} cannot be created by {@link #createCollection(Class, int)};
 * use {@link #createEnumSet(Class)} instead.</p>
 */
public final class CollectionFactory {

    /**
     * Default minimum capacity used for some collections (e.g., sets/queues) to avoid undersizing.
     */
    public static final int DEFAULT_CAPACITY = 16;

    private CollectionFactory() {}

    /**
     * Create a collection of the given type.
     *
     * <p>If {@code collectionType} is a common collection interface, a default implementation is chosen:
     * {@link List} -&gt; {@link ArrayList}, {@link Set} -&gt; {@link LinkedHashSet},
     * {@link SortedSet}/{@link NavigableSet} -&gt; {@link TreeSet},
     * {@link Queue}/{@link Deque} -&gt; {@link ArrayDeque}.</p>
     *
     * <p>If {@code collectionType} is a known concrete class, that class is instantiated directly
     * using capacity-aware constructors when available.</p>
     *
     * <p>For other {@code Collection} subtypes, this method attempts to instantiate the class via
     * an accessible no-arg constructor (reflectively).</p>
     *
     * <h3>Examples</h3>
     * <pre>{@code
     * List<String> a = CollectionFactory.createCollection(List.class, 10);      // ArrayList(10)
     * Set<String>  b = CollectionFactory.createCollection(Set.class, 10);       // LinkedHashSet(16)
     * SortedSet<Integer> c = CollectionFactory.createCollection(SortedSet.class, 0); // TreeSet
     *
     * // Concrete types are respected
     * HashSet<String> d = CollectionFactory.createCollection(HashSet.class, 4); // HashSet(16)
     * }</pre>
     *
     * @param collectionType the requested collection type (interface or concrete class)
     * @param initialCapacity requested initial capacity (must be {@code >= 0})
     * @param <E> element type
     * @param <C> collection type
     * @return a new collection instance compatible with {@code collectionType}
     * @throws IllegalArgumentException if {@code collectionType} is not a {@link Collection} type
     * @throws IllegalArgumentException if {@code collectionType} cannot be reflectively instantiated
     * @throws IllegalStateException if {@code initialCapacity < 0} or if {@code collectionType} is {@link EnumSet}
     */
    public static <E, C extends Collection<E>> C createCollection(Class<C> collectionType, int initialCapacity) {
        nonNull(collectionType, "collectionType");
        state(initialCapacity >= 0, "argument: initialCapacity");

        if (List.class.isAssignableFrom(collectionType)) {
            return cast(new ArrayList<>(initialCapacity));
        }
        if (Set.class.isAssignableFrom(collectionType)) {
            return cast(new LinkedHashSet<>(Math.max(DEFAULT_CAPACITY, initialCapacity)));
        }
        if (SortedSet.class.isAssignableFrom(collectionType) || NavigableSet.class.isAssignableFrom(collectionType)) {
            return cast(new TreeSet<>());
        }
        if (Queue.class.isAssignableFrom(collectionType) || Deque.class.isAssignableFrom(collectionType)) {
            return cast(new ArrayDeque<>(Math.max(DEFAULT_CAPACITY, initialCapacity)));
        }

        state(!EnumSet.class.isAssignableFrom(collectionType), "use createEnumSet(enumType)");

        if (ArrayList.class.isAssignableFrom(collectionType)) {
            return cast(new ArrayList<>(initialCapacity));
        }
        if (LinkedList.class.isAssignableFrom(collectionType)) {
            return cast(new LinkedList<>());
        }
        if (HashSet.class.isAssignableFrom(collectionType)) {
            return cast(new HashSet<>(Math.max(DEFAULT_CAPACITY, initialCapacity)));
        }
        if (LinkedHashSet.class.isAssignableFrom(collectionType)) {
            return cast(new LinkedHashSet<>(Math.max(DEFAULT_CAPACITY, initialCapacity)));
        }
        if (TreeSet.class.isAssignableFrom(collectionType)) {
            return cast(new TreeSet<>());
        }
        if (ArrayDeque.class.isAssignableFrom(collectionType)) {
            return cast(new ArrayDeque<>(Math.max(DEFAULT_CAPACITY, initialCapacity)));
        }

        if (!Collection.class.isAssignableFrom(collectionType)) {
            throw new IllegalArgumentException("Not a Collection type: " + collectionType.getName());
        }

        return instantiateCollection(collectionType);
    }

    /**
     * Create a collection of the given type using a default capacity of {@code 0}.
     *
     * <h3>Example</h3>
     * <pre>{@code
     * List<String> list = CollectionFactory.createCollection(List.class); // ArrayList(0)
     * }</pre>
     *
     * @param collectionType the requested collection type (interface or concrete class)
     * @param <E> element type
     * @param <C> collection type
     * @return a new collection instance compatible with {@code collectionType}
     */
    public static <E, C extends Collection<E>> C createCollection(Class<C> collectionType) {
        return createCollection(collectionType, 0);
    }

    /**
     * Create a collection that matches the "shape" of the {@code source} collection, while allowing a specific
     * {@code targetType} hint.
     *
     * <p>This is primarily useful for mapping/conversion scenarios where you want the result to resemble the input
     * collection type (set vs list vs queue), while still being able to override the target type.</p>
     *
     * <p>Special handling exists for {@link EnumSet}: if the source is an {@code EnumSet} and the enum type can be
     * inferred (non-empty), a new {@code EnumSet.noneOf(enumType)} is created. If the {@code EnumSet} is empty and the
     * enum type cannot be inferred, a {@link LinkedHashSet} is returned as a safe fallback.</p>
     *
     * <h3>Examples</h3>
     * <pre>{@code
     * // Like a set -> LinkedHashSet
     * Collection<Integer> a = CollectionFactory.createLike(Set.of(1, 2, 3), 4, Collection.class);
     *
     * // Like a queue -> ArrayDeque
     * Collection<Integer> b = CollectionFactory.createLike(new ArrayDeque<>(), 4, Collection.class);
     *
     * // Like an EnumSet -> EnumSet of the same enum type (if non-empty)
     * enum Role { USER, ADMIN }
     * EnumSet<Role> src = EnumSet.of(Role.USER);
     * Collection<Role> c = CollectionFactory.createLike(src, 0, Collection.class);
     * }</pre>
     *
     * @param source the source collection used to derive the target "shape"
     * @param initialCapacity requested initial capacity for capacity-aware implementations
     * @param targetType an additional hint for the target type; if it is not a {@link Collection} type,
     *                   {@link #createCollection(Class, int)} is used directly
     * @param <E> element type
     * @param <C> target collection type
     * @return a new collection instance shaped like {@code source}
     * @throws IllegalArgumentException if {@code source} or {@code targetType} is {@code null}
     */
    @SuppressWarnings({"unchecked", "rawtypes"})
    public static <E, C extends Collection<E>> C createLike(
            Collection<?> source, int initialCapacity, Class<C> targetType) {
        nonNull(source, "source");
        nonNull(targetType, "targetType");

        if (!Collection.class.isAssignableFrom(targetType)) {
            return createCollection(targetType, initialCapacity);
        }

        return switch (source) {
            case EnumSet<?> enumSet -> {
                Class<?> enumType = enumSet.isEmpty() ? null : enumSet.iterator().next().getDeclaringClass();
                if (enumType == null) {
                    yield cast(new LinkedHashSet<>(Math.max(DEFAULT_CAPACITY, initialCapacity)));
                }
                yield (C) cast(EnumSet.noneOf((Class) enumType));
            }
            case SortedSet<?> ignored1 -> cast(new TreeSet<>());
            case Set<?> ignored -> cast(new LinkedHashSet<>(Math.max(DEFAULT_CAPACITY, initialCapacity)));
            case Queue<?> ignored -> cast(new ArrayDeque<>(Math.max(DEFAULT_CAPACITY, initialCapacity)));
            default -> cast(new ArrayList<>(initialCapacity));
        };
    }

    /**
     * Create an empty {@link EnumSet} for the given enum type.
     *
     * <h3>Example</h3>
     * <pre>{@code
     * enum Status { NEW, DONE }
     * EnumSet<Status> set = CollectionFactory.createEnumSet(Status.class);
     * }</pre>
     *
     * @param enumType the enum class
     * @param <E> enum type
     * @return an empty {@link EnumSet} of {@code enumType}
     * @throws IllegalArgumentException if {@code enumType} is {@code null}
     */
    public static <E extends Enum<E>> EnumSet<E> createEnumSet(Class<E> enumType) {
        return EnumSet.noneOf(nonNull(enumType, "enumType"));
    }

    /**
     * Instantiate a collection using a no-arg constructor (reflectively).
     *
     * @param collectionType collection class to instantiate
     * @param <E> element type
     * @param <C> collection type
     * @return a new instance of {@code collectionType}
     * @throws IllegalArgumentException if instantiation fails (e.g., no accessible default constructor)
     */
    private static <E, C extends Collection<E>> C instantiateCollection(Class<C> collectionType) {
        try {
            Constructor<C> constructor = collectionType.getDeclaredConstructor();
            if (!constructor.canAccess(null)) {
                constructor.setAccessible(true);
            }
            return constructor.newInstance();
        } catch (ReflectiveOperationException exception) {
            throw new IllegalArgumentException(
                    "Cannot instantiate collection type: %s (no accessible default constructor?)"
                            .formatted(collectionType.getName()),
                    exception
            );
        }
    }

    /**
     * Narrow a {@link Collection} instance to the requested generic type.
     *
     * @param collection collection instance
     * @param <E> element type
     * @param <C> collection type
     * @return the same instance cast to {@code C}
     */
    @SuppressWarnings("unchecked")
    private static <E, C extends Collection<E>> C cast(Collection<E> collection) {
        return (C) collection;
    }
}
