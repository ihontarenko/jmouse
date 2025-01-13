package svit.beans;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import svit.beans.annotation.BeanInitializer;
import svit.beans.definition.*;
import svit.beans.naming.BeanNameResolver;
import svit.beans.processor.BeanPostProcessor;
import svit.reflection.ClassMatchers;
import svit.reflection.Reflections;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

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
 * <p>Main Features:</p>
 * <ul>
 *     <li>Register and manage beans by name, type, or annotation.</li>
 *     <li>Resolve dependencies and manage the lifecycle of beans.</li>
 *     <li>Support for hierarchical contexts through a parent context.</li>
 *     <li>Prevention of cyclic dependencies during bean creation.</li>
 *     <li>Integration with {@link BeanPostProcessor} for custom bean behavior.</li>
 * </ul>
 *
 * <p>Example Usage:</p>
 * <pre>{@code
 * // Create a new DefaultBeanContext
 * DefaultBeanContext context = new DefaultBeanContext();
 *
 * // Register a bean definition
 * BeanDefinition definition = new DefaultBeanDefinition("userService", UserService.class);
 * context.registerDefinition(definition);
 *
 * // Retrieve a bean by its type
 * MyService userService = context.getBean(UserService.class);
 *
 * // Retrieve a bean by its name
 * MyService userServiceByName = context.getBean("userService");
 * }</pre>
 */
public class DefaultBeanContext implements BeanContext, BeanFactory {

    private static final Logger LOGGER = LoggerFactory.getLogger(DefaultBeanContext.class);

    /**
     * A detector for cyclic references in the dependency graph, using {@link DefaultCyclicReferenceDetector}.
     * This ensures that cyclic dependencies are identified and handled during bean creation.
     */
    private final CyclicReferenceDetector<String> referenceDetector = new DefaultCyclicReferenceDetector<>();

    /**
     * Holds all registered {@link BeanContextInitializer}s for initializing the context.
     * This list contains instances of initializers that will be executed during context setup.
     */
    private final List<BeanContextInitializer> initializers = new ArrayList<>(
            List.of(new RequiredBeanContextInitializer())
    );

    /**
     * Keeps track of {@link BeanContextInitializer} classes that have already been initialized.
     * Prevents duplicate initialization of the same type.
     */
    private final Set<Class<? extends BeanContextInitializer>> initialized = new HashSet<>();

    /**
     * A mapping of bean names to their corresponding {@link BeanDefinition}s.
     */
    private final BeanDefinitionContainer definitionContainer;

    /**
     * A mapping of {@link Scope} to their respective {@link BeanContainer}.
     * <p>
     * This map is used to dynamically associate scopes with their corresponding
     * containers, allowing for flexible management of bean instances based on scope.
     */
    private final Map<Scope, BeanContainer> containers = new ConcurrentHashMap<>();

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
        this.definitionContainer = new SimpleBeanDefinitionContainer();
    }

    /**
     * Constructs a new {@code DefaultBeanContext} with no parent context.
     */
    public DefaultBeanContext() {
        this(null);
    }

    /**
     * Executes all registered {@link BeanContextInitializer}s to initialize the context.
     * <p>
     * If an initializer has already been executed (tracked via the {@code initialized} set),
     * it will be skipped to prevent duplicate initialization.
     */
    @Override
    public void refresh() {
        LOGGER.warn("=========================================");
        LOGGER.warn("========== START INITIALIZING! ==========");
        LOGGER.warn("=========================================");

        for (BeanContextInitializer initializer : initializers) {
            Class<? extends BeanContextInitializer> initializerClass = initializer.getClass();

            if (initialized.contains(initializerClass)) {
                LOGGER.warn("Initializer '{}' is already initialized.", getShortName(initializerClass));
                continue;
            }

            initializer.initialize(this);
            initialized.add(initializerClass);
            LOGGER.info("Initializer '{}' was successfully executed.", getShortName(initializerClass));
        }

        LOGGER.warn("==========================================");
        LOGGER.warn("========== FINISH INITIALIZING! ==========");
        LOGGER.warn("==========================================");
    }

    /**
     * Clears all tracked initializations, allowing the registered {@link BeanContextInitializer}s
     * to be executed again.
     * <p>
     * This method is useful when the context needs to be reinitialized from scratch.
     * <p>
     */
    @Override
    public void cleanup() {
        // Clear the set of initialized classes to allow reinitialization
        initialized.clear();
        LOGGER.warn("WARNING! Initializer states cleared!");
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
            BeanContainer instanceContainer = getBeanContainer(beanScope);

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

        if (definition == null) {
            throw new BeanContextException("Bean definition required");
        }

        Supplier<BeanInstantiationException> exceptionSupplier = ()
                -> new BeanInstantiationException(
                        "Cyclic dependency detected for bean: %s".formatted(definition.getBeanName()));

        try {
            // Detect cyclic references using the general-purpose Identifier interface
            referenceDetector.detect(definition::getBeanName, exceptionSupplier);

            // resolved an instantiated raw bean
            T instance = beanFactory.createBean(definition);

            // Initializes a bean instance by applying pre-initialization and post-initialization
            initializeBean(instance, definition);

            // IMPORTANT! endpoint of bean instance registration after creation
            // registerBean(definition.getBeanName(), instance, definition.getBeanScope());

            return instance;
        } finally {
            referenceDetector.remove(definition::getBeanName);
        }
    }

    /**
     * Initializes a bean instance by applying pre-initialization and post-initialization
     * processing steps and invoking any annotated initializer methods.
     *
     * @param instance   the bean instance to initialize.
     * @param definition the {@link BeanDefinition} associated with the bean.
     */
    @Override
    public void initializeBean(Object instance, BeanDefinition definition) {
        // Perform pre-initialization steps using registered BeanPostProcessors
        for (BeanPostProcessor processor : processors) {
            processor.postProcessBeforeInitialize(instance, this);
        }

        // Invoke the initializer method if present in the bean class
        Reflections.findAllAnnotatedMethods(definition.getBeanClass(), BeanInitializer.class).stream().findFirst()
                .ifPresent(initializer -> Reflections.invokeMethod(instance, initializer));

        if (instance instanceof svit.beans.BeanInitializer beanInitializer) {
            beanInitializer.initializeBean(instance, definition);
        }

        // Perform post-initialization steps using registered BeanPostProcessors
        for (BeanPostProcessor processor : processors) {
            processor.postProcessAfterInitialize(instance, this);
        }
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
        List<String> names = new ArrayList<>();

        for (BeanDefinition definition : getDefinitions(BeanDefinitionMatchers.isSupertype(type))) {
            names.add(definition.getBeanName());
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


        for (String beanName : getBeanNames(type)) {
            beans.add(getBean(beanName));
        }

        if (parent != null) {
            beans.addAll(parent.getBeans(type));
        }

        return beans;
    }

    /**
     * Registers a bean instance of the specified type and beanScope.
     *
     * @param type      the type of the bean.
     * @param bean      the bean instance to register.
     * @param scope the beanScope scope for the bean.
     */
    @Override
    public void registerBean(Class<?> type, Object bean, BeanScope scope) {
        registerBean(nameResolver.resolveName(type), bean, scope);
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
     * <p>
     * Registration of the bean in the container occurs only if the bean is not an instance of {@link ObjectFactory}
     * and does not have the {@link BeanScope#PROTOTYPE} scope.
     * </p>
     * <p>
     * <b>NOTE:</b> The method {@link PrototypeBeanContainer#containsBean} will always return <b>TRUE</b> for the
     * prototypes container, even though prototype-scoped beans are not stored persistently in the container.
     * </p>
     *
     * <p>Example Usage:</p>
     * <pre>{@code
     * // Registering a singleton bean
     * container.registerBean("myBean", beanInstance, BeanScope.SINGLETON);
     *
     * // Registering a prototype-scoped bean (will not have a dedicated container)
     * container.registerBean("prototypeBean", beanInstance, BeanScope.PROTOTYPE);
     * }</pre>
     *
     * @param name      the name of the bean.
     * @param bean      the bean instance to register.
     * @param beanScope the lifecycle scope for the bean.
     * @throws BeanContextException if the bean cannot be registered due to scope restrictions or other errors.
     */
    @Override
    public void registerBean(String name, Object bean, BeanScope beanScope) {
        BeanDefinition definition = getDefinition(name);

        // IMPORTANT! If no definition exists, the bean is being registered manually (externally),
        // not via the automatic BeanContext mechanism
        if (definition == null) {
            boolean               isObjectFactory = bean instanceof ObjectFactory<?>;
            ObjectFactory<Object> factory         = isObjectFactory ? (ObjectFactory<Object>) bean : () -> bean;
            Class<?>              beanClass       = isObjectFactory ? ObjectFactory.class : bean.getClass();

            if (!isObjectFactory) {
                LOGGER.info("The bean '{}' was wrapped into an ObjectFactory<{}>", name, beanClass);
            }

            definition = new ObjectFactoryBeanDefinition(name, beanClass, factory);

            // Add additional information to the BeanDefinition
            // - Assign the actual bean instance
            // - Set the scope of the bean (e.g., SINGLETON, PROTOTYPE, etc.)
            definition.setBeanInstance(bean);
            definition.setBeanScope(beanScope);

            // register bean definition
            registerDefinition(definition);
        }

        // todo: think what we should do if bean is object factory
        BeanContainer container = getBeanContainer(beanScope);
        LOGGER.info("Bean '{}' attached to the '{}' container", name, getShortName(container.getClass()));

        Object object = definition.getBeanInstance();

        if (object == null) {
            throw new BeanContextException("Unexpected null pointer. Bean instance must be present in definition");
        }

        container.registerBean(name, object);
    }

    /**
     * Registers a bean instance with the given name using an {@link ObjectFactory} and a specified {@link BeanScope}.
     * <p>
     * The default implementation throws a {@link BeanContextException} to indicate that
     * scope-based registration is not supported. Override this method in a subclass
     * if this functionality is required.
     *
     * @param name      the name of the bean.
     * @param objectFactory      the factory for creating the bean instance.
     * @param beanScope the scope of the bean.
     * @throws BeanContextException if this operation is not supported.
     */
    @Override
    public void registerBean(String name, ObjectFactory<Object> objectFactory, BeanScope beanScope) {
        registerBean(name, (Object) objectFactory, beanScope);
    }

    /**
     * Checks whether a bean with the specified name is already registered in this container.
     *
     * @param name the name of the bean.
     * @return {@code true} if a bean with the given name exists, otherwise {@code false}.
     */
    @Override
    public boolean containsBean(String name) {
        BeanDefinition definition = getDefinition(name);

        if (definition == null) {
            return false;
        }

        return getBeanContainer(definition.getBeanScope()).containsBean(name);
    }

    /**
     * Returns a {@link BeanContainer} based on the specified {@link BeanScope}.
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
     * @return the {@link BeanContainer} associated with the beanScope, if supported
     * @throws BeanContextException if the beanScope is {@link BeanScope#PROTOTYPE}, {@link BeanScope#REQUEST}, or {@link BeanScope#SESSION}
     */
    @Override
    public BeanContainer getBeanContainer(Scope beanScope) {
        BeanContainer instanceContainer = containers.get(beanScope);

        if (instanceContainer == null) {
            throw new BeanContextException("Unsupported bean scope '%s' detected in context '%s'."
                    .formatted(beanScope, getShortName(getClass())));
        }

        return instanceContainer;
    }

    /**
     * Registers a {@link BeanContainer} for a specific {@link Scope}.
     * <p>
     * This method allows mapping a scope to a container that manages bean instances
     * within that scope. For example, you can register separate containers for
     * singleton, prototype, request, or session scopes.
     * </p>
     *
     * @param scope     the {@link Scope} for which the container is being registered.
     * @param container the {@link BeanContainer} to be associated with the given scope.
     * @throws IllegalArgumentException if the provided scope or container is null.
     */
    @Override
    public void registerBeanContainer(Scope scope, BeanContainer container) {
        containers.put(scope, container);
    }

    /**
     * Removes all registered {@link BeanContainer}s.
     * <p>
     * This method clears all previously registered containers, effectively resetting
     * the context's scope-based container management.
     * </p>
     */
    @Override
    public void removeBeanInstanceContainers() {
        containers.clear();
    }

    /**
     * Retrieves a {@link BeanDefinition} by its name.
     *
     * @param name the name of the bean
     * @return the bean definition, or {@code null} if no definition is found
     */
    @Override
    public BeanDefinition getDefinition(String name) {
        return definitionContainer.getDefinition(name);
    }

    /**
     * Retrieves all registered {@link BeanDefinition}s in the container.
     *
     * @return a collection of all registered bean definitions.
     */
    @Override
    public Collection<BeanDefinition> getDefinitions() {
        return definitionContainer.getDefinitions();
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
        definitionContainer.registerDefinition(definition);
    }

    /**
     * Checks if a {@link BeanDefinition} with the given name exists in the container.
     *
     * @param name the name of the bean definition
     * @return {@code true} if the container contains a definition with the specified name,
     * {@code false} otherwise
     */
    @Override
    public boolean containsDefinition(String name) {
        return definitionContainer.containsDefinition(name);
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

}
