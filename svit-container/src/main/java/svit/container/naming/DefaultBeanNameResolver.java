package svit.container.naming;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import svit.container.BeanNameException;
import svit.reflection.Reflections;

import java.lang.reflect.AnnotatedElement;
import java.util.ArrayList;
import java.util.List;

public class DefaultBeanNameResolver implements BeanNameResolver {

    private static final Logger LOGGER = LoggerFactory.getLogger(DefaultBeanNameResolver.class);

    private final List<BeanNameStrategy> strategies = new ArrayList<>();

    public void addStrategy(BeanNameStrategy strategy) {
        LOGGER.info("Register new strategy '{}'", Reflections.getShortName(strategy.getClass()));
        strategies.add(strategy);
    }

    @Override
    public String resolveName(AnnotatedElement element) {
        String beanName = null;

        for (BeanNameStrategy strategy : strategies) {
            if (strategy.supports(element)) {
                beanName = strategy.resolve(element);
            }
        }

        if (beanName == null) {
            throw new BeanNameException(
                    "Failed to resolve bean name for '%s' element".formatted(element));
        }

        return beanName;
    }

}
