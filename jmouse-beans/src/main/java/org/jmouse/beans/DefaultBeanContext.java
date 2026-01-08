package org.jmouse.beans;

import org.jmouse.beans.definition.BeanDefinition;
import org.jmouse.beans.definition.BeanDefinitionFactory;
import org.jmouse.beans.definition.DuplicateBeanDefinitionException;
import org.jmouse.beans.definition.ObjectFactoryBeanDefinition;
import org.jmouse.beans.events.BeanContextEvent;
import org.jmouse.beans.events.BeanEventName;
import org.jmouse.beans.events.BeanContextEventPayload;
import org.jmouse.beans.events.BeanContextEventPayload.*;
import org.jmouse.core.CyclicReferenceDetector;
import org.jmouse.core.DefaultCyclicReferenceDetector;
import org.jmouse.core.Delegate;
import org.jmouse.core.context.ExecutionContextHolder;
import org.jmouse.core.events.*;
import org.jmouse.core.trace.SpanScopes;
import org.jmouse.core.trace.TraceContext;
import org.jmouse.core.trace.TraceKeys;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.jmouse.beans.annotation.BeanInitializer;
import org.jmouse.beans.naming.BeanNameResolver;
import org.jmouse.beans.processor.BeanPostProcessor;
import org.jmouse.core.reflection.ClassMatchers;
import org.jmouse.core.reflection.Reflections;
import org.jmouse.util.Arrays;
import org.jmouse.core.Sorter;

import java.lang.reflect.Method;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.function.Supplier;

import static org.jmouse.beans.BeanLookupStrategy.INHERIT_DEFINITION;
import static org.jmouse.beans.events.BeanEventName.*;
import static org.jmouse.core.reflection.Reflections.getShortName;

/**
 * DirectAccess implementation of the {@link BeanContext} interface.
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
 * UserService userService = context.getBean(UserService.class);
 *
 * // Retrieve a bean by its name
 * UserService userServiceByName = context.getBean("userService");
 * }</pre>
 */
public class DefaultBeanContext implements BeanContext, BeanFactory {

    private static final String DEFAULT_CONTEXT_NAME = "DEFAULT-BEANS-CONTEXT";
    private static final Logger LOGGER               = LoggerFactory.getLogger(DefaultBeanContext.class);

    /**
     * The base classes for scanning and managing beans in the context.
     * <p>
     * This field holds an array of classes that serve as entry points for scanning
     * annotated beans or configurations. By default, this array is empty, indicating
     * no base classes are defined.
     * </p>
     */
    private Class<?>[] baseClasses = {};

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
            List.of(
                    new RequiredBeanContextInitializer(),
                    new ProxyFactoryContextInitializer()
            )
    );

    /**
     * Keeps track of {@link BeanContextInitializer} classes that have already been initialized.
     * Prevents duplicate initialization of the same type.
     */
    private final Set<Integer> initialized = new HashSet<>();

    /**
     * A mapping of bean names to their corresponding {@link BeanDefinition}s.
     */
    private final BeanDefinitionContainer definitionContainer;

    /**
     * Resolves the {@link Scope} of a bean by its name.
     * <p>
     * The {@link ScopeResolver} determines the lifecycle or context in which a bean
     * is created and managed (e.g., singleton, prototype, request, session).
     * </p>
     */
    private final ScopeResolver scopeResolver;

    /**
     * The registry responsible for managing {@link BeanContainer}s for various scopes.
     * <p>
     * This registry facilitates retrieval, registration, and removal of bean containers
     * based on their {@link Scope}, enabling flexible and scoped management of beans.
     * </p>
     */
    private final BeanContainerRegistry containerRegistry;

    /**
     * A list of registered {@link BeanPostProcessor}s for managing bean lifecycle hooks.
     */
    private final List<BeanPostProcessor> processors = new ArrayList<>();

    /**
     * The parent {@link BeanContext}, if any, for hierarchical context resolution.
     */
    private BeanContext parent;

    /**
     * Child {@link BeanContext}s keyed by parent context ID.
     */
    private final Map<String, BeanContext> nested = new ConcurrentHashMap<>(4);

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
     * Unique context ID
     */
    private String contextId;

    /**
     * 🧭 Strategy used to resolve and lookup beans within this context.
     * <p>
     * Allows customization of bean resolution behavior
     * (e.g. local-first, parent-first, chained lookup).
     */
    private BeanLookupStrategy lookupStrategy;

    /**
     * 📡 Internal event manager for {@code BeanContext}.
     * <p>
     * Responsible for publishing lifecycle and state-change events
     * to registered listeners.
     */
    private final EventManager events = new EventManager();

    /**
     * 📨 Active event publish policy for this {@code BeanContext}.
     * <p>
     * Controls which {@link BeanEventName} events
     * are emitted and dispatched to listeners.
     * <p>
     * Marked as {@code volatile} to ensure visibility across threads
     * when the publish policy is updated at runtime.
     *
     * <p>Defaults to {@link EventPublishPolicy#publishAll()}.</p>
     */
    private volatile EventPublishPolicy publishPolicy = EventPublishPolicy.publishAll();

    /**
     * Constructs a new {@code DefaultBeanContext} with the specified parent context and base classes.
     * <p>
     * If a parent context is provided, this context may inherit beans from the parent. Additionally,
     * the provided base classes are used as entry points for scanning and resolving beans.
     *
     * @param parent     the parent bean context, or {@code null} if this is a root context
     * @param baseClasses an array of classes to serve as the base for bean scanning and resolution.
     *                    If {@code null}, no base classes are set.
     */
    public DefaultBeanContext(BeanContext parent, Class<?>... baseClasses) {
        this.parent = parent;
        this.definitionContainer = new DefaultBeanDefinitionContainer();
        this.scopeResolver = new BeanDefinitionScopeResolver(definitionContainer);
        this.containerRegistry = new DelegateBeanContainerRegistry(
                new ScopedHashMapBeanContainer(this.scopeResolver)
        );

        addBaseClasses(baseClasses);
        setContextId(DEFAULT_CONTEXT_NAME);
    }

    /**
     * Constructs a new {@code DefaultBeanContext} with the specified parent context.
     * <p>
     * This constructor automatically retrieves the base classes from the parent context.
     *
     * @param parent the parent bean context, must not be {@code null}
     */
    public DefaultBeanContext(BeanContext parent) {
        this(parent, parent == null ? new Class[0] : parent.getBaseClasses());
    }

    /**
     * Constructs a new {@code DefaultBeanContext} with the specified base classes.
     * <p>
     * This constructor is typically used when no parent context is required.
     *
     * @param baseClasses an array of classes to serve as the base for bean scanning and resolution.
     *                    If {@code null}, no base classes are set.
     */
    public DefaultBeanContext(Class<?>... baseClasses) {
        this(null, baseClasses);
    }

    /**
     * Executes all registered {@link BeanContextInitializer}s to initialize the context.
     * <p>
     * If an initializer has already been executed (tracked via the {@code initialized} set),
     * it will be skipped to prevent duplicate initialization.
     */
    @Override
    public final void refresh() {
        LOGGER.warn("=========================================");
        LOGGER.warn("========== START INITIALIZING! ==========");
        LOGGER.warn("=========================================");

        doRefresh();

        LOGGER.warn("==========================================");
        LOGGER.warn("========== FINISH INITIALIZING! ==========");
        LOGGER.warn("==========================================");
    }

    protected void doRefresh() {
        Sorter.sort(initializers);

        emit(CONTEXT_REFRESH_STARTED, new ContextPayload(this));

        try {
            for (BeanContextInitializer initializer : initializers) {
                int                                     hashCode         = Objects.hash(initializer);
                Class<? extends BeanContextInitializer> initializerClass = initializer.getClass();

                if (initialized.contains(hashCode)) {
                    LOGGER.warn("Initializer '{}' is already initialized.", getShortName(initializerClass));
                    continue;
                }

                initializer.initialize(this);

                initialized.add(hashCode);
                LOGGER.info("Initializer '{}' was successfully executed.", getShortName(initializerClass));
            }

            onRefresh();
        } catch (Throwable e) {
            emit(CONTEXT_REFRESH_FAILED, new ErrorPayload(this, CONTEXT_REFRESH_STARTED, null, null, e));
            throw e;
        } finally {
            emit(CONTEXT_REFRESH_COMPLETED, new ContextPayload(this));
        }
    }

    protected void onRefresh() {}

    /**
     * Clears all tracked initializations, allowing the registered {@link BeanContextInitializer}s
     * to be executed again.
     * <p>
     * This method is useful when the context needs to be reinitialized from scratch.
     * <p>
     */
    @Override
    public void cleanup() {
        doCleanup();
        LOGGER.warn("WARNING! Initializer states cleared!");
    }

    protected void doCleanup() {
        // Clear the set of initialized classes to allow reinitialization
        initialized.clear();
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
        return SpanScopes.rootIfAbsent(() -> doLookup(type));
    }

    protected <T> T doLookup(Class<T> type) {
        emit(BEAN_LOOKUP_STARTED, new LookupPayload(this, null, type));

        List<String> beanNames = getBeanNames(type);

        if (beanNames.isEmpty()) {
            emit(BEAN_LOOKUP_NOT_FOUND, new LookupPayload(this, null, type));
            throw new BeanNotFoundException("No beans associated with '%s' type".formatted(type.getName()));
        }

        if (beanNames.size() == 1) {
            String name = beanNames.getFirst();
            emit(BEAN_LOOKUP_RESOLVED, new LookupPayload(this, name, type));
            return getBean(name);
        }

        Optional<String> primaryName = beanNames.stream()
                .filter(name -> {
                    BeanDefinition definition = getDefinition(name);

                    if (definition == null) {
                        return false;
                    }

                    return definition.isPrimary();
                }).findFirst();

        if (primaryName.isPresent()) {
            String name = primaryName.get();
            emit(BEAN_LOOKUP_PRIMARY_SELECTED, new LookupPayload(this, name, type));
            return getBean(name);
        }

        emit(BEAN_LOOKUP_AMBIGUOUS, new AmbiguousLookupPayload(this, type, List.copyOf(beanNames)));

        throw new BeanAmbiguousException(
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
     * 🧠 Resolves a bean by name using the configured lookup strategy.
     * <p>
     * If the definition is not found locally, it may be inherited from parent context or delegated.
     *
     * @param name the name of the bean
     * @return the resolved bean instance
     * @throws BeanNotFoundException if the bean cannot be found in any context
     */
    @Override
    public <T> T getBean(String name) {
        return SpanScopes.child(() -> doLookup(name));
    }

    protected <T> T doLookup(String name) {
        BeanDefinition   definition    = getDefinition(name);
        BeanDefinition   tmp           = definition;
        ObjectFactory<T> objectFactory = () -> this.createBean(tmp);
        T                instance      = null;

        emit(BEAN_LOOKUP_STARTED, new LookupPayload(this, name, null));

        try {
            if (definition != null) {
                Scope beanScope = scopeResolver.resolveScope(name);

                // get applicable bean instances container and try to find bean
                BeanContainer instanceContainer = getBeanContainer(beanScope);

                // get bean or try to create new one via lambda
                instance = instanceContainer.getBean(name, objectFactory);
            }

            if (instance == null && getBeanLookupStrategy() == INHERIT_DEFINITION && parent != null) {
                definition = parent.getDefinition(name);

                if (definition != null) {
                    registerDefinition(definition);
                    instance = getBean(name);
                }
            }

            // if bean not found try to search it in parent context
            if (instance == null && parent != null) {
                instance = parent.getBean(name);
            }

            // If still not found — fail
            if (instance == null) {
                throw new BeanNotFoundException(
                        "Bean '%s' could not be resolved via lookup strategy '%s'"
                                .formatted(name, getBeanLookupStrategy()));
            }

            emit(BEAN_LOOKUP_RESOLVED, new LookupPayload(this, name, null));
            return instance;
        } catch (BeanNotFoundException e) {
            emit(BEAN_LOOKUP_NOT_FOUND, new LookupPayload(this, name, null));
            throw e;
        } catch (RuntimeException e) {
            emit(CONTEXT_INTERNAL_ERROR, new ErrorPayload(this, BEAN_LOOKUP_STARTED, name, getDefinition(name), e));
            throw e;
        }
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
        return doCreateBean(definition);
    }

    protected  <T> T doCreateBean(BeanDefinition definition) {
        if (definition == null) {
            throw new BeanContextException("Bean definition required");
        }

        if (!definition.isEnabled()) {
            throw new BeanContextException("Bean definition disabled");
        }

        Supplier<BeanInstantiationException> exceptionSupplier = ()
                -> new BeanInstantiationException(
                "Cyclic dependency detected for bean: %s".formatted(definition.getBeanName()));

        emit(BEAN_CREATION_STARTED, new CreatePayload(this, definition, null));

        try {
            // Detect cyclic references using the general-purpose Identifier interface
            referenceDetector.detect(definition::getBeanName, exceptionSupplier);

            // resolve an instantiate raw bean
            T instance = beanFactory.createBean(definition);
            emit(BEAN_CREATION_COMPLETED, new CreatePayload(this, definition, instance));

            // Initializes a bean instance by applying pre-initialization and post-initialization
            instance = initializeBean(instance, definition);
            emit(BEAN_INITIALIZATION_COMPLETED, new CreatePayload(this, definition, instance));

            return instance;
        } catch (Exception exception) {
            throw new BeanInitializationException("Bean '%s' initialization failed!".formatted(
                    definition.getBeanName()), exception);
        } finally {
            referenceDetector.remove(definition::getBeanName);
        }
    }

    /**
     * Performs initialization logic on a bean instance.
     * <p>
     * This method is invoked to prepare the bean instance for use. Implementations may apply additional
     * configurations, wrap the bean in a proxy, or perform validation based on the provided {@link BeanDefinition}.
     * </p>
     *
     * @param instance   the bean instance to initialize.
     * @param definition the {@link BeanDefinition} associated with the bean, providing descriptor for initialization.
     * @return the initialized bean instance, potentially wrapped or modified.
     */
    @Override
    public <T> T initializeBean(T instance, BeanDefinition definition) {
        return doInitializeBean(instance, definition);
    }

    @SuppressWarnings({"unchecked"})
    protected <T> T doInitializeBean(T instance, BeanDefinition definition) {
        try {
            // Perform pre-initialization steps using registered BeanPostProcessors
            for (BeanPostProcessor processor : processors) {
                emit(BEAN_INITIALIZATION_BEFORE_PROCESSING,
                     new InitPayload(this, definition, instance, processor.getClass().getName()));
                instance = (T) processor.postProcessBeforeInitialize(instance, definition, this);
            }

            // Invoke the initializer method if present in the bean class
            for (Method initializer : Reflections.findAllAnnotatedMethods(
                    definition.getBeanClass(), BeanInitializer.class)) {
                Reflections.invokeMethod(instance, initializer, this);
                emit(BEAN_INITIALIZER_INVOKED,
                     new InitPayload(this, definition, instance, initializer.toGenericString()));
            }

            // Perform post-initialization steps using registered BeanPostProcessors
            for (BeanPostProcessor processor : processors) {
                emit(BEAN_INITIALIZATION_AFTER_PROCESSING,
                     new InitPayload(this, definition, instance, processor.getClass().getName()));
                instance = (T) processor.postProcessAfterInitialize(instance, definition, this);
            }

            if (instance instanceof InitializingBean initializingBean) {
                initializingBean.afterCompletion(this);
            }
        } catch (Exception exception) {
            emit(BEAN_INITIALIZATION_FAILED, new ErrorPayload(
                    this, BEAN_INITIALIZATION_STARTED, definition.getBeanName(), definition, exception));
            throw exception;
        }

        return instance;
    }

    /**
     * Checks whether a bean with the given name is defined **locally** in this context,
     * excluding any parent contexts.
     *
     * @param name the name of the bean
     * @return {@code true} if the bean exists in this context only; {@code false} if it's inherited or absent
     */
    @Override
    public boolean isLocalBean(String name) {
        return getDefinition(name) != null;
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
        Set<String> names = new HashSet<>();

        for (BeanDefinition definition : getDefinitions(BeanDefinitionMatchers.isSupertype(type))) {
            names.add(definition.getBeanName());
        }

        if (parent != null) {
            names.addAll(parent.getBeanNames(type));
        }

        return List.copyOf(names);
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
        return List.copyOf(getBeansOfType(type).values());
    }

    /**
     * Retrieves a map of bean names to their instances, matching the specified type.
     *
     * @param type the class type of the beans
     * @return a map containing bean names as keys and their corresponding bean instances as values
     */
    @Override
    public <T> Map<String, T> getBeansOfType(Class<T> type) {
        Map<String, T> beans = new LinkedHashMap<>(4);

        for (String name : getBeanNames(type)) {
            beans.put(name, getBean(name));
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
    public void registerBean(Class<?> type, Object bean, Scope scope) {
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
     * @param scope     the lifecycle scope for the bean.
     * @throws BeanContextException if the bean cannot be registered due to scope restrictions or other errors.
     */
    @Override
    @SuppressWarnings("unchecked")
    public void registerBean(String name, Object bean, Scope scope) {
        boolean       isLazy    = bean instanceof ObjectFactory<?>;
        BeanContainer container = getBeanContainer(scope);

        // Create definition if it on present in definition registry
        // If no definition exists, the bean is being registered manually (externally)
        if (!containsDefinition(name)) {
            Class<?>              beanClass     = isLazy ? ObjectFactory.class : bean.getClass();
            ObjectFactory<Object> objectFactory = isLazy ? (ObjectFactory<Object>) bean : () -> bean;
            BeanDefinition        definition    = new ObjectFactoryBeanDefinition(name, beanClass, objectFactory);

            if (!isLazy) {
                LOGGER.info("The bean '{}' was wrapped into an ObjectFactory<{}>",
                        name, getShortName(beanClass));
                definition.setBeanInstance(bean);
            }

            definition.setScope(scope);
            registerDefinition(definition);
        }

        // Do nothing if passed bean presented as ObjectFactory bean
        if (!isLazy) {
            LOGGER.info("Bean '{}' registered to the '{}' container", name, getShortName(container.getClass()));
            getBeanContainer(scope).registerBean(name, bean);
        }
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
     * @param scope the scope of the bean.
     * @throws BeanContextException if this operation is not supported.
     */
    @Override
    public void registerBean(String name, ObjectFactory<Object> objectFactory, Scope scope) {
        registerBean(name, (Object) objectFactory, scope);
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

        return getBeanContainer(definition.getScope()).containsBean(name);
    }

    /**
     * ⚙️ Set lookup strategy for missing beans.
     * <p>Defines how this context behaves when a bean is not found locally.
     *
     * @param lookupStrategy the strategy to apply (e.g. inherit or delegate)
     */
    @Override
    public void setBeanLookupStrategy(BeanLookupStrategy lookupStrategy) {
        this.lookupStrategy = lookupStrategy;
    }

    /**
     * 📌 Defines how this context will behave when a bean is not found locally.
     * <p>Default strategy is {@code DELEGATE_TO_PARENT} — try parent context first.
     *
     * @return the configured lookup strategy
     */
    @Override
    public BeanLookupStrategy getBeanLookupStrategy() {
        return lookupStrategy == null
                ? BeanContext.super.getBeanLookupStrategy()
                : lookupStrategy;
    }

    /**
     * Sets the base classes to be scanned and processed by this context.
     * <p>
     * These classes are used to detect annotations, definitions, and additional context information
     * necessary for bean registration and initialization.
     * </p>
     *
     * @param baseClasses the array of base classes to be set.
     */
    @Override
    public void addBaseClasses(Class<?>... baseClasses) {
        this.baseClasses = Arrays.unique(Arrays.concatenate(baseClasses, this.baseClasses));
    }

    /**
     * Sets the base classes to be scanned and processed by this context.
     *
     * @param baseClasses the array of base classes to be set.
     */
    @Override
    public void setBaseClasses(Class<?>... baseClasses) {
        this.baseClasses = baseClasses;
    }

    /**
     * Retrieves the base classes that are currently being used by this context.
     *
     * @return an array of base classes that the context is working with.
     */
    @Override
    public Class<?>[] getBaseClasses() {
        if (Arrays.empty(baseClasses)) {
            addBaseClasses(getClass());
        }

        return baseClasses;
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
     * @param scope the scope phase for which to retrieve the container
     * @return the {@link BeanContainer} associated with the scope, if supported
     * @throws BeanContextException if the scope is unsupported
     */
    @Override
    public BeanContainer getBeanContainer(Scope scope) {
        BeanContainer instanceContainer = containerRegistry.getBeanContainer(scope);

        // some delegated implementation may throw the same exception
        // but not always
        if (instanceContainer == null) {
            throw new UnsupportedScopeException(scope, getClass());
        }

        return instanceContainer;
    }

    /**
     * Registers a {@link BeanContainer} for a specific {@link Scope}.
     * <p>
     * This method allows mapping a scope to a container that manages bean instances
     * within that scope. For binder, you can register separate containers for
     * singleton, prototype, request, or session scopes.
     * </p>
     *
     * @param scope     the {@link Scope} for which the container is being registered.
     * @param container the {@link BeanContainer} to be associated with the given scope.
     * @throws IllegalArgumentException if the provided scope or container is null.
     */
    @Override
    public void registerBeanContainer(Scope scope, BeanContainer container) {
        containerRegistry.registerBeanContainer(scope, container);
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
        containerRegistry.removeBeanInstanceContainers();
    }

    /**
     * Checks if a {@link BeanContainer} is registered for the specified {@link Scope}.
     *
     * @param scope the scope to check for a registered bean container.
     * @return {@code true} if a container is registered for the given scope, {@code false} otherwise.
     * @throws IllegalArgumentException if the scope is {@code null}.
     */
    @Override
    public boolean containsBeanContainer(Scope scope) {
        return containerRegistry.containsBeanContainer(scope);
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
        doRegisterDefinition(definition);
    }

    protected void doRegisterDefinition(BeanDefinition definition) {
        try {
            definitionContainer.registerDefinition(definition);
            emit(BEAN_DEFINITION_REGISTERED, new DefinitionPayload(
                    this, definition));
        } catch (DuplicateBeanDefinitionException exception) {
            emit(BEAN_LOOKUP_AMBIGUOUS, new ErrorPayload(
                    this, BEAN_DEFINITION_REGISTERED, definition.getBeanName(), definition, exception));
            throw exception;
        }
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
     * 🔗 Registers a child {@link BeanContext} under the given parent ID.
     *
     * @param parentId       identifier of the parent context
     * @param currentContext child context to associate with the parent
     */
    @Override
    public void setChildContext(String parentId, BeanContext currentContext) {
        Objects.requireNonNull(parentId, "parentId must not be null");
        if (currentContext == null) {
            // treat null as removal
            nested.remove(parentId);
        } else {
            nested.put(parentId, currentContext);
        }
    }

    /**
     * 🔎 Retrieves the child {@link BeanContext} associated with the given parent ID.
     *
     * @param parentId identifier of the parent context
     * @return the associated child context, or {@code null} if none exists
     */
    @Override
    public BeanContext getChildContext(String parentId) {
        Objects.requireNonNull(parentId, "parentId must not be null");
        return nested.get(parentId);
    }

    /**
     * ❓ Checks whether a child {@link BeanContext} is registered for the given parent ID.
     *
     * @param parentId identifier of the parent context
     * @return {@code true} if a child context exists, {@code false} otherwise
     */
    @Override
    public boolean hasChildContext(String parentId) {
        return nested.containsKey(parentId);
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
        if (parent != null) {
            LOGGER.info("Parent context '{}' attached to '{}'", parent, this);
            this.parent = parent;
        }
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
     * Retrieves the {@link BeanContainerRegistry} associated with this context.
     *
     * @return the current {@link BeanContainerRegistry} used by this context.
     * @throws IllegalStateException if the registry is not set.
     */
    @Override
    public BeanContainerRegistry getBeanContainerRegistry() {
        BeanContainerRegistry registry = containerRegistry;

        if (containerRegistry instanceof Delegate<?> delegate) {
            registry = (BeanContainerRegistry) delegate.getDelegate();
        }

        return registry;
    }

    /**
     * Sets the {@link BeanContainerRegistry} for this context.
     *
     * @param containerRegistry the {@link BeanContainerRegistry} to set.
     *                          Must not be {@code null}.
     * @throws NullPointerException if the provided {@code containerRegistry} is {@code null}.
     */
    @Override
    public void setBeanContainerRegistry(BeanContainerRegistry containerRegistry) {
        if (this.containerRegistry instanceof DelegateBeanContainerRegistry delegate) {
            delegate.setDelegate(containerRegistry);
        }
    }

    /**
     * 📡 Internal event manager for {@code BeanContext}.
     *
     * @return the event manager
     */
    @Override
    public EventManager getEventManager() {
        return events;
    }


    /**
     * Returns the current event publish policy.
     *
     * @return the active {@link EventPublishPolicy}
     */
    public EventPublishPolicy getPublishPolicy() {
        return publishPolicy;
    }

    /**
     * Sets the event publish policy.
     * <p>
     * The publish policy controls how and when
     * {@link BeanEventName} events
     * are emitted by this context.
     *
     * @param publishPolicy the publish policy to apply
     */
    public void setPublishPolicy(EventPublishPolicy publishPolicy) {
        this.publishPolicy = publishPolicy;
    }

    /**
     * Returns the unique identifier of this {@code BeanContext}.
     *
     * @return the context identifier
     */
    public String getContextId() {
        return contextId;
    }

    /**
     * Sets the unique identifier for this {@code BeanContext}.
     * <p>
     * The context ID is mainly used for logging, debugging,
     * and hierarchical context representation.
     *
     * @param contextId the context identifier to assign
     */
    public void setContextId(String contextId) {
        this.contextId = contextId;
    }

    /**
     * Provides a formatter used to render context names.
     * <p>
     * By default, wraps the context ID into square brackets:
     * {@code [contextId]}.
     *
     * @return a formatter function for context names
     */
    private Function<Object, String> getContextName() {
        return "[%s]"::formatted;
    }

    /**
     * Emits a {@link BeanContextEvent} to all registered listeners.
     * <p>
     * Listener failures are isolated and do not affect
     * the context execution flow.
     *
     * @param name    the event name
     * @param payload the event payload
     */
    protected void emit(EventName name, BeanContextEventPayload payload) {
        TraceContext traceContext = ExecutionContextHolder.current().get(TraceKeys.TRACE);
        if (publishPolicy.shouldPublish(name, traceContext, this, payload)) {
            events.publish(new BeanContextEvent(name, payload, this));
        } else {
            LOGGER.info("Skip event: {}, Trace: {}", name, traceContext);
        }
    }

    /**
     * Returns a human-readable representation of this context.
     * <p>
     * Includes the context ID and its parent hierarchy, if present:
     * <pre>
     * [child] -> [parent] -> [root]
     * </pre>
     *
     * @return string representation of the context
     */
    @Override
    public String toString() {
        return getContextName().apply(getContextId())
                + (parent != null ? " -> " + parent : "");
    }

}
