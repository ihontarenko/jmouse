package svit.mapping;

import svit.reflection.ClassFinder;
import svit.reflection.ReflectionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.function.Consumer;

import static svit.reflection.Reflections.findFirstConstructor;
import static svit.reflection.Reflections.instantiate;

/**
 * Factory class for creating and configuring instances of {@link Mapping}.
 * Provides methods to create default mappings or customized mappings by registering mappers dynamically.
 */
final public class MappingFactory {

    private static final Logger LOGGER = LoggerFactory.getLogger(MappingFactory.class);

    /**
     * Creates a default instance of {@link Mapping} without any registered mappers.
     *
     * @return a new instance of {@link Mapping.DefaultMapping}
     */
    public static Mapping createDefault() {
        return new Mapping.DefaultMapping();
    }

    /**
     * Creates an instance of {@link Mapping} and registers all implementations of {@link Mapper}
     * found in the {@code  Class<?>... baseClasses} package.
     *
     * @return a configured {@link Mapping} instance
     */
    public static Mapping create(Class<?>... baseClasses) {
        return create(mapping -> {
            for (Class<?> mapperClass : ClassFinder.findImplementations(Mapper.class, baseClasses)) {
                try {
                    Object mapper = instantiate(findFirstConstructor(mapperClass));
                    mapping.register((Mapper<?, ?>) mapper);
                } catch (ReflectionException reflectionException) {
                    LOGGER.error("Failed to create mapper instance [{}]", reflectionException.getMessage());
                }
            }
        });
    }

    /**
     * Creates an instance of {@link Mapping} and applies additional configuration
     * through a provided {@link Consumer}.
     *
     * @param configurator a {@link Consumer} to configure the created {@link Mapping} instance
     * @return a configured {@link Mapping} instance
     */
    public static Mapping create(Consumer<Mapping> configurator) {
        Mapping mapping = createDefault();
        configurator.accept(mapping);
        return mapping;
    }

}
