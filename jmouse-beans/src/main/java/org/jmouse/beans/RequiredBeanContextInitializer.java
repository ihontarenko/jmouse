package org.jmouse.beans;

import org.jmouse.beans.definition.BeanDefinitionFactory;
import org.jmouse.beans.definition.SimpleBeanDefinitionFactory;
import org.jmouse.beans.instantiation.BeanInstantiationFactory;
import org.jmouse.beans.instantiation.ConstructorBeanInstantiationStrategy;
import org.jmouse.beans.instantiation.MethodBeanInstantiationStrategy;
import org.jmouse.beans.instantiation.ObjectFactoryBeanInstantiationStrategy;
import org.slf4j.Logger;
import org.jmouse.beans.definition.strategy.ConstructorBeanDefinitionCreationStrategy;
import org.jmouse.beans.definition.strategy.MethodBeanDefinitionCreationStrategy;
import org.jmouse.beans.definition.strategy.ObjectFactoryBeanDefinitionCreationStrategy;
import org.jmouse.beans.naming.AnnotationBeanNameStrategy;
import org.jmouse.beans.naming.BeanNameResolver;
import org.jmouse.beans.naming.DefaultBeanNameResolver;
import org.jmouse.beans.processor.BeanContextAwareBeanPostProcessor;
import org.jmouse.beans.processor.InjectDependencyBeanPostProcessor;
import org.jmouse.beans.processor.ProxyBeanPostProcessor;
import org.jmouse.core.proxy.AnnotationProxyFactory;
import org.jmouse.core.proxy.ProxyFactory;
import org.jmouse.util.Priority;

import static org.slf4j.LoggerFactory.getLogger;
import static org.jmouse.core.reflection.Reflections.getShortName;

/**
 * DirectAccess implementation of {@link BeanContextInitializer}.
 * Provides initialization logic for setting up a {@link BeanContext} with default factories, resolvers, and post-processors.
 */
@Priority(Integer.MIN_VALUE)
final class RequiredBeanContextInitializer implements BeanContextInitializer {

    private static final Logger LOGGER = getLogger(getShortName(BeanContextInitializer.class) + ".DEFAULT_INITIALIZER");

    /**
     * Initializes the provided {@link BeanContext} with default settings.
     *
     * @param context the {@link BeanContext} to initialize.
     */
    @Override
    public void initialize(BeanContext context) {
        initializeRequiredBeanInstanceContainers(context);

        registerDefaultBeanFactory(context);
        registerDefaultBeanDefinitionFactory(context);
        registerDefaultBeanNameResolver(context);
        registerDefaultPostProcessors(context);

        // Self-referencing registration
        LOGGER.info("Self referencing: Bean type '{}' -> Bean structured '{}'",
                    getShortName(BeanContext.class), getShortName(context.getClass()));
        context.registerBean(context.getClass(), context);
        context.registerBean(ProxyFactory.class, new AnnotationProxyFactory(context.getBaseClasses()));
    }

    /**
     * Initializes the required {@link BeanContainer}s for basic scopes.
     * <p>
     * Clears any previously registered containers and sets up containers for the
     * {@link BeanScope#SINGLETON} and {@link BeanScope#PROTOTYPE} scopes.
     * </p>
     *
     * @param context the {@link BeanContext} for which the containers are initialized.
     */
    private void initializeRequiredBeanInstanceContainers(BeanContext context) {
        context.registerBeanContainer(BeanScope.SINGLETON, new SingletonBeanContainer());
        context.registerBeanContainer(BeanScope.PROTOTYPE, new PrototypeBeanContainer());
    }

    /**
     * Registers default structured post-processors.
     *
     * @param context the {@link BeanContext} to configure.
     */
    private void registerDefaultPostProcessors(BeanContext context) {
        LOGGER.info("Initialize post-processors");
        context.addBeanPostProcessor(new InjectDependencyBeanPostProcessor());
        context.addBeanPostProcessor(new ProxyBeanPostProcessor());
        context.addBeanPostProcessor(new BeanContextAwareBeanPostProcessor());
    }

    /**
     * Registers the default {@link BeanFactory} and its instantiation strategies.
     *
     * @param context the {@link BeanContext} to configure.
     */
    private void registerDefaultBeanFactory(BeanContext context) {
        BeanFactory factory = new DefaultBeanFactory();

        LOGGER.info("Initialize default structured factory");

        if (factory instanceof BeanContextAware contextAware) {
            contextAware.setBeanContext(context);
        }

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

        LOGGER.info("Initialize new structured name resolver");

        resolver.addStrategy(new AnnotationBeanNameStrategy());

        context.setNameResolver(resolver);
    }

    /**
     * Registers the default {@link BeanDefinitionFactory} with its creation strategies.
     *
     * @param context the {@link BeanContext} to configure.
     */
    private void registerDefaultBeanDefinitionFactory(BeanContext context) {
        BeanDefinitionFactory factory = new SimpleBeanDefinitionFactory();

        LOGGER.info("Initialize new structured definition factory");

        factory.addStrategy(new ConstructorBeanDefinitionCreationStrategy());
        factory.addStrategy(new MethodBeanDefinitionCreationStrategy());
        factory.addStrategy(new ObjectFactoryBeanDefinitionCreationStrategy());

        context.setBeanDefinitionFactory(factory);
    }
}
