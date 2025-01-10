package svit.container;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import svit.container.instantiation.BeanInstantiationFactory;
import svit.container.instantiation.BeanInstantiationStrategy;
import svit.container.definition.BeanDefinition;

import java.util.ArrayList;
import java.util.List;

import static svit.reflection.Reflections.getShortName;

public class DefaultBeanFactory implements BeanFactory, BeanInstantiationFactory, BeanContextAware {

    private static final Logger LOGGER = LoggerFactory.getLogger(DefaultBeanFactory.class);

    private final List<BeanInstantiationStrategy> strategies = new ArrayList<>();
    private       BeanContext                     context;

    @Override
    public <T> T createBean(BeanDefinition definition) {
        T instance = (T) createInstance(definition, getBeanContext());

        if (instance == null) {
            throw new BeanInstantiationException(
                    "Failed to instantiate bean '%s' using definition of type '%s'."
                            .formatted(definition.getBeanName(), getShortName(definition.getClass())));
        }

        // assign bean instance for it definition
        definition.setBeanInstance(instance);

        return instance;
    }

    @Override
    public Object createInstance(BeanDefinition definition, BeanContext context) {
        Object instance = null;

        for (BeanInstantiationStrategy strategy : strategies) {
            if (strategy.supports(definition)) {
                definition.setBeanCreationStrategy(strategy);
                instance = strategy.create(definition, context);
                LOGGER.info("New bean instance %s:[%s:%s]".formatted(
                        getShortName(strategy.getClass()), definition.getBeanScope(), definition.getBeanName()));
            }
        }

        return instance;
    }

    @Override
    public void addStrategy(BeanInstantiationStrategy strategy) {
        LOGGER.info("Register new strategy '%s'".formatted(getShortName(strategy.getClass())));
        strategies.add(strategy);
    }

    @Override
    public BeanContext getBeanContext() {
        return context;
    }

    @Override
    public void setBeanContext(BeanContext context) {
        this.context = context;
    }


}