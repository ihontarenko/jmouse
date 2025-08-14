package org.jmouse.beans.conditions;

import org.jmouse.beans.BeanContext;
import org.jmouse.beans.definition.BeanDefinition;
import org.jmouse.core.reflection.Reflections;
import org.jmouse.core.reflection.annotation.MergedAnnotation;
import org.jmouse.core.Streamable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Constructor;
import java.util.Collections;
import java.util.List;


/**
 * ✅ Evaluates whether a {@link BeanDefinition} satisfies all registered {@link BeanCondition}s.
 * <p>
 * This class inspects the merged annotations of a bean (including meta-annotations),
 * and applies all registered {@link BeanRegistrationCondition} checks.
 * </p>
 *
 * <p>Usage:</p>
 * <pre>{@code
 * ConditionEvaluator evaluator = new ConditionEvaluator();
 * boolean result = evaluator.evaluate(beanDefinition, context);
 * }</pre>
 *
 * @author Ivan Hontarenko
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
                BeanCondition       annotation = mergedAnnotation.getNativeAnnotation(BeanCondition.class);

                for (Class<? extends BeanRegistrationCondition> beanConditionClass : annotation.value()) {
                    @SuppressWarnings("unchecked")
                    Constructor<? extends BeanRegistrationCondition> constructor =
                            (Constructor<? extends BeanRegistrationCondition>) Reflections.findFirstConstructor(beanConditionClass);

                    BeanRegistrationCondition beanCondition = Reflections.instantiate(constructor);

                    if (!beanCondition.match(metadata, context)) {
                        LOGGER.warn("⛔ Bean '{}' skipped due to failed condition: @{} on {}",
                                     definition.getBeanName(),
                                     mergedAnnotation.getAnnotationType().getSimpleName(),
                                     metadata.getAnnotatedElement());
                        return false;
                    }
                }
            }
        }

        return true;
    }


    /**
     * Creates metadata context for evaluating a specific condition annotation.
     */
    private ConditionalMetadata createConditionalMetadata(BeanDefinition definition, MergedAnnotation annotation) {
        return new SimpleConditionalMetadata(annotation, definition);
    }

    /**
     * Extracts all merged annotations on the bean that are annotated with {@link BeanCondition}.
     * This includes both direct and meta-annotations.
     *
     * @param definition the bean definition
     * @return list of relevant {@link MergedAnnotation}s
     */
    private List<MergedAnnotation> getMergedAnnotations(BeanDefinition definition) {
        try {
            return Streamable.of(MergedAnnotation.wrapWithSynthetic(definition.getAnnotatedElement()).getMetas())
                    .filter(ma -> ma.isAnnotationPresent(BeanCondition.class))
                    .toList();
        } catch (UnsupportedOperationException ignored) {
            return Collections.emptyList();
        }
    }
}
