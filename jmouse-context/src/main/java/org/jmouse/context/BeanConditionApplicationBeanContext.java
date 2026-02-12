package org.jmouse.context;

import org.jmouse.beans.BeanContext;
import org.jmouse.beans.conditions.AbstractBeanRegistrationCondition;
import org.jmouse.beans.conditions.BeanCondition;
import org.jmouse.beans.conditions.ConditionalMetadata;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.annotation.*;

/**
 * 🧭 Conditional annotation based on context hierarchy.
 * <p>
 * Allows to restrict bean registration depending on the nesting depth
 * of the current {@link BeanContext}.
 * </p>
 *
 * @author Ivan Hontarenko (Mr. Jerry Mouse)
 */
@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
@BeanCondition(BeanConditionApplicationBeanContext.ApplicationBeanContextCondition.class)
public @interface BeanConditionApplicationBeanContext {

    /**
     * 🔒 Only register in sourceRoot context (no parent).
     */
    boolean rootOnly();

    /**
     * 🧪 Internal condition that checks nesting of context.
     */
    class ApplicationBeanContextCondition extends AbstractBeanRegistrationCondition {

        private static final Logger LOGGER = LoggerFactory.getLogger(ApplicationBeanContextCondition.class);

        @Override
        public boolean match(ConditionalMetadata metadata, BeanContext context) {
            BeanConditionApplicationBeanContext annotation = metadata.getAnnotation(
                    BeanConditionApplicationBeanContext.class);
            boolean result = annotation.rootOnly() == (context.getParentContext() == null);
            String  target = annotation.rootOnly() ? "Root" : "Child";

            if (result) {
                LOGGER.info("✅ {}-only condition passed: context '{}' has no parent", target, context.getContextId());
            } else {
                LOGGER.warn("⛔ {}-only condition failed: context '{}' has a parent", target, context.getContextId());
            }

            return result;
        }
    }
}
