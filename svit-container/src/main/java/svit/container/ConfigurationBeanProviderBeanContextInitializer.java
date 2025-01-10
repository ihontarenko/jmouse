package svit.container;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import svit.container.annotation.Configuration;
import svit.container.annotation.Provide;
import svit.container.definition.BeanDefinition;
import svit.container.definition.BeanDefinitionFactory;
import svit.reflection.ClassFinder;
import svit.reflection.MethodFinder;

import java.lang.reflect.Method;
import java.util.Collection;

import static svit.reflection.Reflections.getShortName;

public class ConfigurationBeanProviderBeanContextInitializer implements BeanContextInitializer {

    private final Class<?>[] baseClasses;

    private static final Logger LOGGER = LoggerFactory.getLogger(
            getShortName(BeanContextInitializer.class) + ".CONFIGURATION_BEAN_PROVIDER");

    public ConfigurationBeanProviderBeanContextInitializer(Class<?>... baseClasses) {
        this.baseClasses = baseClasses;
    }

    @Override
    public void initialize(BeanContext context) {
        BeanDefinitionFactory factory = context.getBeanDefinitionFactory();

        LOGGER.info("Start scan @Configuration files");

        for (Class<?> klass : ClassFinder.findAnnotatedClasses(Configuration.class, baseClasses)) {
            Collection<Method> providers   = new MethodFinder().filter(klass).annotated(Provide.class).find();

            LOGGER.info("Configuration class '{}' provides '{}' beans", klass, providers.size());

            for (Method provider : providers) {
                BeanDefinition definition = factory.createDefinition(provider, context);
                context.registerDefinition(definition);
            }
        }

        LOGGER.info("Finish scan @Configuration files");
    }

}
