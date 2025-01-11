package svit.container;

import org.slf4j.Logger;
import svit.container.annotation.Provide;
import svit.container.definition.strategy.ConstructorBeanDefinitionCreationStrategy;
import svit.container.definition.strategy.MethodBeanDefinitionCreationStrategy;
import svit.container.definition.strategy.ObjectFactoryBeanDefinitionCreationStrategy;
import svit.container.instantiation.*;
import svit.container.naming.AnnotationBeanNameStrategy;
import svit.container.naming.BeanNameResolver;
import svit.container.naming.ClassNameStrategy;
import svit.container.naming.DefaultBeanNameResolver;
import svit.container.definition.*;
import svit.container.processor.BeanContextAwareBeanPostProcessor;
import svit.container.processor.InjectDependencyBeanPostProcessor;
import svit.reflection.ClassFinder;

import static org.slf4j.LoggerFactory.getLogger;
import static svit.reflection.Reflections.getShortName;

/**
 * Default implementation of {@link BeanContextInitializer}.
 * Provides initialization logic for setting up a {@link BeanContext} with default factories, resolvers, and post-processors.
 */
final class RequiredBeanContextInitializer implements BeanContextInitializer {

    private static final Logger LOGGER = getLogger(getShortName(BeanContextInitializer.class) + ".DEFAULT_INITIALIZER");

    /**
     * Initializes the provided {@link BeanContext} with default settings.
     *
     * @param context the {@link BeanContext} to initialize.
     */
    @Override
    public void initialize(BeanContext context) {
        registerDefaultBeanFactory(context);
        registerDefaultBeanDefinitionFactory(context);
        registerDefaultBeanNameResolver(context);
        registerDefaultPostProcessors(context);

        // Self-referencing registration
        LOGGER.info("Self referencing: Bean type '{}' -> Bean object '{}'",
                    getShortName(BeanContext.class), getShortName(context.getClass()));
        context.registerBean(BeanContext.class, context);

        defaultScanning(context);
    }

    /**
     * Performs default scanning for classes annotated with {@link Provide} and registers their definitions.
     *
     * @param context the {@link BeanContext} in which to register the definitions.
     */
    private void defaultScanning(BeanContext context) {
        BeanDefinitionFactory factory = context.getBeanDefinitionFactory();

        LOGGER.info("Start default scanning");

        for (Class<?> annotatedClass : ClassFinder.findAnnotatedClasses(Provide.class, BeanContext.class)) {
            if (annotatedClass.isInterface()) {
                for (Class<?> implementation : ClassFinder.findImplementations(annotatedClass, BeanContext.class)) {
                    BeanDefinition definition = factory.createDefinition(implementation, context);
                    context.registerDefinition(definition);
                }
            }
        }

        LOGGER.info("Finish default scanning");
    }

    /**
     * Registers default bean post-processors.
     *
     * @param context the {@link BeanContext} to configure.
     */
    private void registerDefaultPostProcessors(BeanContext context) {
        LOGGER.info("Initialize post-processors");
        context.addBeanPostProcessor(new InjectDependencyBeanPostProcessor());
        context.addBeanPostProcessor(new BeanContextAwareBeanPostProcessor());
    }

    /**
     * Registers the default {@link BeanFactory} and its instantiation strategies.
     *
     * @param context the {@link BeanContext} to configure.
     */
    private void registerDefaultBeanFactory(BeanContext context) {
        BeanFactory factory = new DefaultBeanFactory();

        LOGGER.info("Initialize default bean factory");

        factory.setBeanContext(context);

        if (factory instanceof BeanInstantiationFactory instantiation) {
            instantiation.addStrategy(new ConstructorBeanInstantiationStrategy());
            instantiation.addStrategy(new MethodBeanInstantiationStrategy());
            instantiation.addStrategy(new ObjectFactoryBeanInstantiationStrategy());
        }

        context.setBeanFactory(factory);
    }

    /**
     * Registers the default {@link BeanNameResolver} with its naming strategies.
     *
     * @param context the {@link BeanContext} to configure.
     */
    private void registerDefaultBeanNameResolver(BeanContext context) {
        BeanNameResolver resolver = new DefaultBeanNameResolver();

        LOGGER.info("Initialize new bean name resolver");

        resolver.addStrategy(new AnnotationBeanNameStrategy());
        resolver.addStrategy(new ClassNameStrategy());

        context.setNameResolver(resolver);
    }

    /**
     * Registers the default {@link BeanDefinitionFactory} with its creation strategies.
     *
     * @param context the {@link BeanContext} to configure.
     */
    private void registerDefaultBeanDefinitionFactory(BeanContext context) {
        BeanDefinitionFactory factory = new SimpleBeanDefinitionFactory();

        LOGGER.info("Initialize new bean definition factory");

        factory.addStrategy(new ConstructorBeanDefinitionCreationStrategy());
        factory.addStrategy(new MethodBeanDefinitionCreationStrategy());
        factory.addStrategy(new ObjectFactoryBeanDefinitionCreationStrategy());

        context.setBeanDefinitionFactory(factory);
    }
}
