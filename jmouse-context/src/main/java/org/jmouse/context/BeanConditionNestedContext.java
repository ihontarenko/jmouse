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
 * <pre>{@code
 * @BeanConditionNestedContext(rootOnly = true)
 * public class RootScopedBean { ... }
 *
 * @BeanConditionNestedContext(depth = 2)
 * public class NestedBean { ... }
 * }</pre>
 *
 * @author Ivan Hontarenko (Mr. Jerry Mouse)
 */
@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
@BeanCondition(BeanConditionNestedContext.NestedConditionCondition.class)
public @interface BeanConditionNestedContext {

    /**
     * 🔒 Only register in root context (no parent).
     */
    boolean rootOnly() default false;

    /**
     * 🪜 Specific depth (1=root, 2=child, etc.).
     */
    int depth() default 1;

    /**
     * 🧪 Internal condition that checks nesting of context.
     */
    class NestedConditionCondition extends AbstractBeanRegistrationCondition {

        private static final Logger LOGGER = LoggerFactory.getLogger(NestedConditionCondition.class);

        @Override
        public boolean match(ConditionalMetadata metadata, BeanContext context) {
            BeanConditionNestedContext annotation = metadata.getAnnotation(BeanConditionNestedContext.class);

            if (annotation.rootOnly()) {
                boolean result = context.getParentContext() == null;

                if (result) {
                    LOGGER.info("✅ Root-only condition passed: context '{}' has no parent", context.getContextId());
                } else {
                    LOGGER.warn("⛔ Root-only condition failed: context '{}' has a parent", context.getContextId());
                }

                return result;
            }

            // Count nesting depth
            int         nesting = 0;
            BeanContext parent  = context.getParentContext();

            while (parent != null) {
                nesting++;
                parent = parent.getParentContext();
            }

            int     expectedDepth = annotation.depth();
            boolean result        = nesting == expectedDepth;

            if (result) {
                LOGGER.info("✅ Depth condition passed: context '{}' depth is {}", context.getContextId(), nesting);
            } else {
                LOGGER.warn("⛔ Depth condition failed: context '{}' depth is {}, expected {}", context.getContextId(),
                            nesting, expectedDepth);
            }

            return result;
        }
    }
}
