package svit.beans.naming;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import svit.beans.BeanNameException;
import org.jmouse.core.reflection.Reflections;

import java.lang.reflect.AnnotatedElement;
import java.util.ArrayList;
import java.util.List;

/**
 * Default implementation of {@link BeanNameResolver}.
 * <p>
 * This class manages a list of {@link BeanNameStrategy} instances and uses them to resolve
 * bean names based on annotated elements. Strategies are evaluated in the order they are
 * added, and the first matching strategy is used to resolve the name.
 * </p>
 */
public class DefaultBeanNameResolver implements BeanNameResolver {

    private static final Logger LOGGER = LoggerFactory.getLogger(DefaultBeanNameResolver.class);

    /**
     * List of registered {@link BeanNameStrategy} instances used for resolving bean names.
     */
    private final List<BeanNameStrategy> strategies = new ArrayList<>();

    /**
     * Adds a new {@link BeanNameStrategy} to the resolver.
     * <p>
     * Strategies are evaluated in the order they are added, and the first supporting
     * strategy is used for resolving names.
     * </p>
     *
     * @param strategy the {@link BeanNameStrategy} to add.
     */
    @Override
    public void addStrategy(BeanNameStrategy strategy) {
        LOGGER.info("Register new strategy '{}'", Reflections.getShortName(strategy.getClass()));
        strategies.add(strategy);
    }

    /**
     * Resolves the name of a bean based on the provided annotated element.
     * <p>
     * The method iterates through all registered strategies and uses the first one
     * that supports the given element to resolve the bean name.
     * </p>
     *
     * @param element the annotated element representing the bean (e.g., class, method).
     * @return the resolved bean name.
     * @throws BeanNameException if no strategy supports the given element.
     */
    @Override
    public String resolveName(AnnotatedElement element) {
        String beanName = null;

        for (BeanNameStrategy strategy : strategies) {
            if (strategy.supports(element)) {
                beanName = strategy.resolve(element);
                break;
            }
        }

        if (beanName == null) {
            throw new BeanNameException("Failed to resolve bean name for '%s' element".formatted(element));
        }

        return beanName;
    }
}