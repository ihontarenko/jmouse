package svit.container.definition;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import svit.container.BeanContext;
import svit.container.BeanScope;
import svit.reflection.Reflections;

import java.lang.reflect.AnnotatedElement;
import java.util.ArrayList;
import java.util.List;

public class DefaultBeanDefinitionFactory implements BeanDefinitionFactory {

    private static final Logger LOGGER = LoggerFactory.getLogger(DefaultBeanDefinitionFactory.class);

    private final List<BeanDefinitionCreationStrategy> strategies = new ArrayList<>();

    public void addStrategy(BeanDefinitionCreationStrategy strategy) {
        LOGGER.info("Register new strategy '{}'", Reflections.getShortName(strategy.getClass()));
        strategies.add(strategy);
    }

    @Override
    public BeanDefinition createDefinition(AnnotatedElement element, BeanContext context) {
        BeanDefinition definition = null;

        for (BeanDefinitionCreationStrategy strategy : strategies) {
            if (strategy.supports(element)) {
                definition = strategy.create(element, context);
            }
        }

        if (definition == null) {
            throw new BeanDefinitionException(
                    "Failed to create bean definition for '%s' element".formatted(element));
        }

        if (definition.getBeanScope() == null) {
            definition.setBeanScope(BeanScope.SINGLETON);
        }

        return definition;
    }

}
