package org.jmouse.context;

import org.jmouse.beans.BeanContext;
import org.jmouse.beans.conditions.BeanCondition;
import org.jmouse.beans.conditions.BeanRegistrationCondition;
import org.jmouse.beans.conditions.ConditionalMetadata;
import org.jmouse.util.Strings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 📦 Conditional annotation that registers a bean only if a bean with the given name is absent.
 *
 * <p>
 * Acts as a guard to prevent overriding or duplicating existing bean definitions
 * by checking the {@link BeanContext} for a bean with the specified name.
 * </p>
 *
 * <p>
 * Typical use cases:
 * </p>
 * <ul>
 *     <li>fallback/default bean definitions</li>
 *     <li>auto-configuration scenarios</li>
 *     <li>optional infrastructure components</li>
 * </ul>
 *
 * <p>
 * If a bean with the specified name already exists, the annotated bean definition
 * will be skipped.
 * </p>
 *
 * <pre>{@code
 * @BeanIfNameAbsent("dataSource")
 * public class FallbackDataSource { ... }
 * }</pre>
 */
@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@BeanCondition(BeanIfNameAbsent.OnBeanAbsences.class)
public @interface BeanIfNameAbsent {

    /**
     * 🎯 Name of the bean that must be absent for registration to proceed.
     *
     * @return target bean name
     */
    String value() default "";

    /**
     * 📝 Optional custom message for logging when the bean is already present.
     *
     * @return custom log message
     */
    String message() default "";

    /**
     * 🔎 Condition that checks for absence of a bean by name.
     *
     * <p>
     * Evaluates whether the {@link BeanContext} contains a bean definition
     * with the specified name. If such a bean exists, registration is skipped.
     * </p>
     */
    class OnBeanAbsences implements BeanRegistrationCondition {

        private static final Logger LOGGER = LoggerFactory.getLogger(OnBeanAbsences.class);

        /**
         * Determines whether the bean should be registered.
         *
         * @param metadata conditional metadata for the bean definition
         * @param context  current bean context
         * @return {@code true} if the bean name is absent; {@code false} otherwise
         */
        @Override
        public boolean match(ConditionalMetadata metadata, BeanContext context) {
            try {
                BeanIfNameAbsent annotation = metadata.getAnnotation(BeanIfNameAbsent.class);
                String           value      = annotation.value();
                String           beanName   = metadata.getBeanDefinition().getBeanName();

                if (value != null && !value.isBlank()) {
                    boolean present = context.containsDefinition(value);
                    String  message = annotation.message();

                    LOGGER.info(
                            "📦 Conditional check → name: [{}], bean: [{}], status: {}",
                            value,
                            beanName,
                            present
                                    ? (Strings.isNotEmpty(message) ? message : "already present 🔁 (skipping)")
                                    : "not found 🆕 (registering)"
                    );

                    return !present;
                }

                return true;
            } catch (IllegalStateException exception) {
                LOGGER.debug("Conditional evaluation failed → skipping bean registration", exception);
                return false;
            }
        }
    }
}