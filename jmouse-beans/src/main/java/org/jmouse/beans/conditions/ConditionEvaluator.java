package org.jmouse.beans.conditions;

import org.jmouse.beans.BeanContext;
import org.jmouse.beans.definition.BeanDefinition;
import org.jmouse.core.reflection.Reflections;
import org.jmouse.core.reflection.annotation.AnnotationScanner;
import org.jmouse.core.reflection.annotation.MergedAnnotation;
import org.jmouse.util.Streamable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Constructor;
import java.util.List;

/**
 * ✅ Evaluates conditional annotations and decides if a bean should be included.
 */
public final class ConditionEvaluator {

    private static final Logger LOGGER = LoggerFactory.getLogger(ConditionEvaluator.class);

    public ConditionEvaluator() {
    }

    /**
     * 🧪 Evaluates if a bean satisfies all defined conditions.
     *
     * @param definition the bean definition
     * @param context    the current bean context
     * @return {@code true} if all conditions pass, {@code false} otherwise
     */
    public boolean evaluate(BeanDefinition definition, BeanContext context) {
        List<MergedAnnotation> mergedAnnotations = getMergedAnnotations(definition);

        if (!mergedAnnotations.isEmpty()) {
            for (MergedAnnotation mergedAnnotation : mergedAnnotations) {
                ConditionalMetadata metadata   = createConditionalMetadata(definition, mergedAnnotation);
                BeanRestriction     annotation = mergedAnnotation.getAnnotation(BeanRestriction.class);

                for (Class<? extends BeanCondition> beanConditionClass : annotation.value()) {
                    @SuppressWarnings("unchecked")
                    Constructor<? extends BeanCondition> constructor =
                            (Constructor<? extends BeanCondition>) Reflections.findFirstConstructor(beanConditionClass);

                    BeanCondition beanCondition = Reflections.instantiate(constructor);

                    if (!beanCondition.match(metadata, context)) {
                        LOGGER.debug("🧩 Bean '{}' skipped due to failed condition: @{} ({})",
                                     definition.getBeanName(),
                                     mergedAnnotation.getAnnotationType().getSimpleName(),
                                     beanConditionClass.getSimpleName());
                        return false;
                    }
                }
            }
        }

        return true;
    }

    private ConditionalMetadata createConditionalMetadata(BeanDefinition definition, MergedAnnotation annotation) {
        return new SimpleConditionalMetadata(annotation, definition);
    }

    private List<MergedAnnotation> getMergedAnnotations(BeanDefinition definition) {
        return Streamable.of(AnnotationScanner.scan(definition.getAnnotatedElement()))
                .map(MergedAnnotation::new)
                .filter(ma -> ma.isAnnotationPresent(BeanRestriction.class))
                .toList();
    }
}
