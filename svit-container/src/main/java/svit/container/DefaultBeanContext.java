package svit.container;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import svit.container.annotation.BeanInitializer;
import svit.container.definition.*;
import svit.container.naming.BeanNameResolver;
import svit.container.processor.BeanPostProcessor;
import svit.matcher.Matcher;
import svit.reflection.ClassMatchers;
import svit.reflection.Reflections;

import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static java.util.stream.Collectors.joining;
import static svit.reflection.Reflections.getShortName;

/**
 * Default implementation of the {@link BeanContext} interface.
 * <p>
 * This class provides a concrete implementation for managing bean definitions,
 * lifecycle management, and dependency resolution. It supports hierarchical
 * contexts with a parent context and offers additional customization via initializers
 * and post-processors.
 * </p>
 * <h3>Main Features:</h3>
 * <ul>
 *     <li>Register and manage beans by name, type, or annotation.</li>
 *     <li>Resolve dependencies and manage the lifecycle of beans.</li>
 *     <li>Support for hierarchical contexts through a parent context.</li>
 *     <li>Prevention of cyclic dependencies during bean creation.</li>
 *     <li>Integration with {@link BeanPostProcessor} for custom bean behavior.</li>
 * </ul>
 *
 * <h3>Example Usage:</h3>
 * <pre>{@code
 * // Create a new DefaultBeanContext
 * DefaultBeanContext context = new DefaultBeanContext();
 *
 * // Register a bean definition
 * BeanDefinition definition = new DefaultBeanDefinition("myService", MyService.class);
 * context.registerDefinition(definition);
 *
 * // Retrieve a bean by its type
 * MyService myService = context.getBean(MyService.class);
 *
 * // Retrieve a bean by its name
 * MyService myServiceByName = context.getBean("myService");
 * }</pre>
 */
public class DefaultBeanContext implements BeanContext {

    private static final Logger LOGGER = LoggerFactory.getLogger(DefaultBeanContext.class);

    /**
     * Tracks bean definitions currently being processed to detect cyclic dependencies.
     */
    private final List<BeanDefinition> visitor = new ArrayList<>();

    /**
     * Holds all registered {@link BeanContextInitializer}s for initializing the context.
     */
    private final List<BeanContextInitializer> initializers = new ArrayList<>();

    /**
     * A mapping of bean names to their corresponding {@link BeanDefinition}s.
     */
    private final Map<String, BeanDefinition> definitions = new ConcurrentHashMap<>();

    /**
     * A container of instantiated singletons beans
     */
    private final BeanInstanceContainer singletonContainer = new SingletonBeanContainer();

    /**
     * A no-ops bean container
     */
    private final BeanInstanceContainer prototypeContainer = new PrototypeBeanContainer();

    /**
     * A list of registered {@link BeanPostProcessor}s for managing bean lifecycle hooks.
     */
    private final List<BeanPostProcessor> processors = new ArrayList<>();

    /**
     * The parent {@link BeanContext}, if any, for hierarchical context resolution.
     */
    private BeanContext parent;

    /**
     * Factory responsible for managing {@link BeanDefinition}s.
     */
    private BeanDefinitionFactory beanDefinitionFactory;

    /**
     * Factory responsible for creating bean instances.
     */
    private BeanFactory beanFactory;

    /**
     * Resolver used for determining bean names.
     */
    private BeanNameResolver nameResolver;

    /**
     * Constructs a new {@code DefaultBeanContext} with the specified parent context.
     *
     * @param parent the parent bean context, or {@code null} if none exists
     */
    public DefaultBeanContext(BeanContext parent) {
        this.parent = parent;
        addInitializer(new DefaultBeanContextInitializer());
    }

    /**
     * Constructs a new {@code DefaultBeanContext} with no parent context.
     */
    public DefaultBeanContext() {
        this(null);
    }

    /**
     * Refreshes the context by invoking all registered {@link BeanContextInitializer}s.
     * This typically involves reloading bean definitions and initializing beans.
     */
    @Override
    public void refresh() {
        initializers.forEach(initializer -> initializer.initialize(this));
    }

    /**
     * Retrieves a bean by its type. Throws an exception if no beans or multiple beans
     * of the specified type exist.
     *
     * @param type the type of the bean
     * @param <T>  the type parameter
     * @return the bean instance
     * @throws BeanContextException if no beans or multiple beans of the specified type exist
     */
    @Override
    public <T> T getBean(Class<T> type) {
        List<String> beanNames = getBeanNames(type);

        if (beanNames.isEmpty()) {
            throw new BeanContextException("No beans found for type: " + type.getName());
        }

        if (beanNames.size() == 1) {
            return getBean(beanNames.getFirst());
        }

        throw new BeanContextException(
                "Multiple beans found for type: '%s'. Please specify the bean name. Available beans: %s"
                        .formatted(type.getName(), beanNames));
    }

    /**
     * Retrieves a bean by its type and name. If the name is {@code null} or blank,
     * it falls back to {@link #getBean(Class)}.
     *
     * @param type the type of the bean
     * @param name the name of the bean, or {@code null} to retrieve by type only
     * @param <T>  the type parameter
     * @return the bean instance
     * @throws BeanContextException if the bean's type does not match the requested type
     */
    @Override
    public <T> T getBean(Class<T> type, String name) {
        if (name == null || name.isBlank()) {
            return getBean(type);
        }

        BeanDefinition definition = getDefinition(name);

        if (definition != null && !ClassMatchers.isSupertype(type).matches(definition.getBeanClass())) {
            throw new BeanContextException(
                    "Bean '%s' is not of requested type: '%s'".formatted(name, type.getName()));
        }

        return getBean(name);
    }

    /**
     * Retrieves a bean by its name. If the bean is not yet instantiated, it is created
     * and initialized using its associated {@link BeanDefinition}.
     *
     * @param name the name of the bean
     * @param <T>  the type parameter
     * @return the bean instance
     * @throws BeanContextException if no bean definition exists for the given name
     */
    @Override
    public <T> T getBean(String name) {
        BeanDefinition   definition    = getDefinition(name);
        ObjectFactory<T> objectFactory = () -> this.createBean(definition);
        T                instance      = null;

        if (definition != null) {
            BeanScope beanScope = definition.getBeanScope();

            // get applicable bean instances container and try to find bean
            BeanInstanceContainer instanceContainer = getBeanInstanceContainer(beanScope);

            // get bean or try to create new one via lambda
            instance = instanceContainer.getBean(name, objectFactory);

            // if bean not found try to search it in parent context
            if (instance == null && parent != null) {
                instance = parent.getBean(definition.getBeanName());
            }
        }

        if (instance == null) {
            throw new BeanContextException("No registered bean found with the name '%s'.".formatted(name));
        }

        return instance;
    }

    /**
     * Creates a bean instance from the given {@link BeanDefinition}, handling
     * cyclic dependency detection, post-processing, and optional registration.
     * <p>
     * This method performs several steps:
     * <ol>
     *   <li>Detects and prevents cyclic dependencies by tracking visited definitions.</li>
     *   <li>Uses the underlying {@link BeanFactory} to create the actual bean instance.</li>
     *   <li>Runs all registered {@link BeanPostProcessor} instances before and after the bean is initialized.</li>
     *   <li>Invokes the method annotated with {@code @Initialization}, if present, on the newly created bean.</li>
     *   <li>If the {@link BeanDefinition} is marked as singleton, the bean is registered in the singleton container.</li>
     * </ol>
     *
     * @param definition the {@link BeanDefinition} describing how the bean should be created
     * @param <T>        the type of the bean
     * @return the created bean instance
     * @throws BeanInstantiationException if bean creation fails due to dependencies or initialization errors
     */
    @Override
    public <T> T createBean(BeanDefinition definition) {
        detectCyclicDependencies(definition);
        visitor.add(definition);

        T instance = beanFactory.createBean(definition);

        // preform post processor BEFORE initializing bean
        for (BeanPostProcessor processor : processors) {
            processor.postProcessBeforeInitialize(instance, this);
        }

        // invoke initializer method in bean object if present
        Reflections.findAllAnnotatedMethods(definition.getBeanClass(), BeanInitializer.class).stream().findFirst()
                .ifPresent(initializer -> Reflections.invokeMethod(instance, initializer));

        visitor.remove(definition);

        // preform post processor AFTER initializing bean
        for (BeanPostProcessor processor : processors) {
            processor.postProcessAfterInitialize(instance, this);
        }

        // register bean instance only if it is SINGLETON or NON_BEAN
        //if (definition.isSingleton()) {
            registerBean(definition.getBeanName(), instance);
//        }

        return instance;
    }

    /**
     * Retrieves the names of all beans that match the specified type.
     * <p>
     * This method searches the current context and, if applicable, the parent context.
     * It uses a matcher to determine if the type of each bean matches the requested type.
     * </p>
     *
     * @param type the type of beans to search for
     * @return a list of bean names that match the specified type
     */
    @Override
    public List<String> getBeanNames(Class<?> type) {
        List<String>      names   = new ArrayList<>();
        Matcher<Class<?>> matcher = ClassMatchers.isSupertype(type);

        for (Map.Entry<String, BeanDefinition> entry : definitions.entrySet()) {
            if (matcher.matches(entry.getValue().getBeanClass())) {
                names.add(entry.getKey());
            }
        }

        if (parent != null) {
            names.addAll(parent.getBeanNames(type));
        }

        return names;
    }

    /**
     * Retrieves all beans that match the specified type.
     * <p>
     * This method iterates through the bean definitions in the current context
     * and retrieves matching beans. It includes beans from the parent context if applicable.
     * </p>
     *
     * @param type the type of beans to retrieve
     * @param <T>  the type parameter of the beans
     * @return a list of bean instances that match the specified type
     */
    @Override
    public <T> List<T> getBeans(Class<T> type) {
        List<T> beans = new ArrayList<>();
        Matcher<Class<?>> matcher = ClassMatchers.isSupertype(type);

        for (Map.Entry<String, BeanDefinition> entry : definitions.entrySet()) {
            if (matcher.matches(entry.getValue().getBeanClass())) {
                beans.add(getBean(entry.getKey()));
            }
        }

        if (parent != null) {
            beans.addAll(parent.getBeans(type));
        }

        return beans;
    }

    /**
     * Retrieves all beans annotated with the specified annotation.
     * <p>
     * This method iterates through the bean definitions and checks if each bean
     * is annotated with the specified annotation.
     * </p>
     *
     * @param annotation the annotation to search for
     * @return a list of beans annotated with the specified annotation
     */
    @Override
    public List<Object> getAnnotatedBeans(Class<? extends Annotation> annotation) {
        List<Object> beans = new ArrayList<>();

        for (BeanDefinition definition : definitions.values()) {
            if (definition.isAnnotatedWith(annotation)) {
                beans.add(getBean(definition.getBeanName()));
            }
        }

        return beans;
    }

    /**
     * Registers a bean instance of the specified type and beanScope.
     *
     * @param type      the type of the bean.
     * @param bean      the bean instance to register.
     * @param beanScope the beanScope scope for the bean.
     */
    @Override
    public void registerBean(Class<?> type, Object bean, BeanScope beanScope) {
        registerBean(nameResolver.resolveName(type), bean, beanScope);
    }

    /**
     * Registers an existing bean instance with the container.
     * <p>
     * This method creates a new {@link BeanDefinition} for the provided instance
     * and stores it in the context along with the bean itself.
     * </p>
     *
     * @param name the name of the bean
     * @param bean the bean instance
     */
    @Override
    public void registerBean(String name, Object bean) {
        registerBean(name, bean, BeanScope.SINGLETON);
    }

    /**
     * Registers a bean instance with the given name and a specified {@link BeanScope}.
     *
     * @param name      the name of the bean.
     * @param bean      the bean instance to register.
     * @param beanScope the lifecycle scope for the bean.
     */
    @Override
    public void registerBean(String name, Object bean, BeanScope beanScope) {
        BeanDefinition definition = getDefinition(name);

        if (definition == null) {

            definition = new DefaultBeanDefinition(name, bean.getClass());

            if (beanScope == BeanScope.PROTOTYPE || bean instanceof ObjectFactory<?>) {
                ObjectFactory<Object> objectFactory = () -> bean;

                if (bean instanceof ObjectFactory<?>) {
                    objectFactory = (ObjectFactory<Object>) bean;
                    LOGGER.warn("The provided bean '{}' is identified as an ObjectFactory instance", name);
                }

                definition = new ObjectFactoryBeanDefinition(name, definition.getBeanClass(), objectFactory);

                if (beanScope == BeanScope.PROTOTYPE) {
                    String warningMessage = "The bean '{}' was automatically converted to an ObjectFactory definition. " +
                            "Prototype-scoped beans do not maintain a dedicated container and are created as needed, " +
                            "existing only within their definition objects.";

                    LOGGER.warn(warningMessage, name);
                }
            }

            // complement bean information
            definition.setBeanInstance(bean);
            definition.setBeanScope(beanScope);

            // register bean definition
            registerDefinition(definition);

        } else {
            LOGGER.warn("The bean '{}' already has definition", name);
        }

        // todo: check bean existing
        // todo: BeanInstanceContainer::containsBean(definition)
        // todo: BeanInstanceContainer::containsBean(name)
        // Register the bean in the container only if it's not an ObjectFactory and not PROTOTYPE scoped
        getBeanInstanceContainer(beanScope).registerBean(name, bean);
        LOGGER.info("The bean '{}' with scope '{}' was registered using the '{}' definition.",
                    name, beanScope, getShortName(definition.getClass()));
    }

    /**
     * Registers a new {@link BeanDefinition} in the container.
     * <p>
     * If a bean with the same name already exists, an exception is thrown.
     * </p>
     *
     * @param definition the bean definition to register
     * @throws DuplicateBeanDefinitionException if a bean with the same name is already registered
     */
    @Override
    public void registerDefinition(BeanDefinition definition) {
        String beanName = definition.getBeanName();

        if (definitions.containsKey(beanName)) {
            throw new DuplicateBeanDefinitionException(definition);
        }

        definitions.put(beanName, definition);
    }

    /**
     * Returns a {@link BeanInstanceContainer} based on the specified {@link BeanScope}.
     * <p>
     * This implementation supports the following lifecycles:
     * <ul>
     *     <li>{@link BeanScope#SINGLETON} and {@link BeanScope#NON_BEAN} — returns the singleton container.</li>
     *     <li>{@link BeanScope#PROTOTYPE} — throws a {@link BeanContextException}, as prototypes do not maintain a dedicated container.</li>
     *     <li>{@link BeanScope#REQUEST} and {@link BeanScope#SESSION} — throws a {@link BeanContextException},
     *         indicating that request or session scopes are not available in this context.</li>
     * </ul>
     *
     * @param beanScope the beanScope phase for which to retrieve the container
     * @return the {@link BeanInstanceContainer} associated with the beanScope, if supported
     * @throws BeanContextException if the beanScope is {@link BeanScope#PROTOTYPE}, {@link BeanScope#REQUEST}, or {@link BeanScope#SESSION}
     */
    @Override
    public BeanInstanceContainer getBeanInstanceContainer(BeanScope beanScope) {
        return switch (beanScope) {
            case SINGLETON, NON_BEAN -> singletonContainer;
            case PROTOTYPE -> {
                LOGGER.warn("Prototypes dummy bean instances container");
                yield prototypeContainer;
            }
            case REQUEST, SESSION -> throw new BeanContextException(
                    "Request and Session bean instances container is unavailable in this context '%s'"
                            .formatted(getShortName(getClass())));
        };
    }

    /**
     * Retrieves a {@link BeanDefinition} by its name.
     *
     * @param name the name of the bean
     * @return the bean definition, or {@code null} if no definition is found
     */
    @Override
    public BeanDefinition getDefinition(String name) {
        return definitions.get(name);
    }

    /**
     * Adds a {@link BeanContextInitializer} to the context.
     * <p>
     * Initializers are invoked when the context is refreshed to perform
     * custom initialization logic.
     * </p>
     *
     * @param initializer the initializer to add
     */
    @Override
    public void addInitializer(BeanContextInitializer initializer) {
        this.initializers.add(initializer);
    }


    /**
     * Adds a {@link BeanPostProcessor} to the context.
     * <p>
     * Post-processors are used to extend or modify the lifecycle behavior
     * of beans during initialization.
     * </p>
     *
     * @param processor the post-processor to add
     */
    @Override
    public void addBeanPostProcessor(BeanPostProcessor processor) {
        LOGGER.info("Register new post-processor '{}'", getShortName(processor.getClass()));
        this.processors.add(processor);
    }

    /**
     * Sets the parent {@link BeanContext} for this context.
     * <p>
     * The parent context is used for hierarchical resolution of beans.
     * </p>
     *
     * @param parent the parent context to set
     */
    @Override
    public void setParentContext(BeanContext parent) {
        this.parent = parent;
    }

    /**
     * Retrieves the parent {@link BeanContext}.
     *
     * @return the parent context, or {@code null} if none is set
     */
    @Override
    public BeanContext getParentContext() {
        return parent;
    }

    /**
     * Retrieves the current {@link BeanFactory}.
     *
     * @return the bean factory
     */
    @Override
    public BeanFactory getBeanFactory() {
        return beanFactory;
    }

    /**
     * Retrieves the current {@link BeanDefinitionFactory}.
     *
     * @return the bean definition factory
     */
    @Override
    public BeanDefinitionFactory getBeanDefinitionFactory() {
        return beanDefinitionFactory;
    }

    /**
     * Retrieves the current {@link BeanNameResolver}.
     *
     * @return the bean name resolver
     */
    @Override
    public BeanNameResolver getNameResolver() {
        return nameResolver;
    }

    /**
     * Sets the current {@link BeanFactory}.
     *
     * @param beanFactory the bean factory to set
     */
    @Override
    public void setBeanFactory(BeanFactory beanFactory) {
        this.beanFactory = beanFactory;
    }

    /**
     * Sets the current {@link BeanDefinitionFactory}.
     *
     * @param definitionFactory the bean definition factory to set
     */
    @Override
    public void setBeanDefinitionFactory(BeanDefinitionFactory definitionFactory) {
        this.beanDefinitionFactory = definitionFactory;
    }

    /**
     * Sets the current {@link BeanNameResolver}.
     *
     * @param nameResolver the bean name resolver to set
     */
    @Override
    public void setNameResolver(BeanNameResolver nameResolver) {
        this.nameResolver = nameResolver;
    }


    /**
     * Detects cyclic dependencies during bean creation and throws an exception
     * if a cyclic dependency is found.
     *
     * @param definition the bean definition being processed
     * @throws BeanInstantiationException if a cyclic dependency is detected
     */
    private void detectCyclicDependencies(BeanDefinition definition) {
        // if definition is already tried to create that is mean that we are in cyclic
        if (visitor.contains(definition)) {

            // also add current definition for exception message
            visitor.add(definition);
            String dependencyPath = visitor.stream().map(BeanDefinition::getBeanName)
                    .collect(joining("\n\t -> "));

            // clean-un visitor
            visitor.clear();

            throw new BeanInstantiationException(
                    "Cyclic dependencies detected during bean creation. dependencies chain: [\n\t -> %s\n]"
                            .formatted(dependencyPath));
        }
    }

}
