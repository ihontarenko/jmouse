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
 * 📦 Conditional annotation that registers a bean only if no bean of the given type exists.
 *
 * <p>
 * Acts as a guard to prevent duplicate or conflicting bean definitions by checking
 * the {@link BeanContext} for existing beans of the specified type.
 * </p>
 *
 * <p>
 * Typical use cases:
 * </p>
 * <ul>
 *     <li>providing default/fallback beans</li>
 *     <li>auto-configuration scenarios</li>
 *     <li>optional infrastructure components</li>
 * </ul>
 *
 * <p>
 * If at least one bean of the given type is already registered, the annotated
 * bean definition will be skipped.
 * </p>
 *
 * <p>
 * 💡 Inspired by Spring's {@code @ConditionalOnMissingBean}.
 * </p>
 */
@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@BeanCondition(BeanIfTypeAbsent.OnBeanAbsences.class)
public @interface BeanIfTypeAbsent {

    /**
     * 🎯 Target bean type that must be absent for registration to proceed.
     *
     * @return bean type to check in the {@link BeanContext}
     */
    Class<?> value();

    /**
     * 📝 Optional custom message for logging when a bean of the given type is already present.
     *
     * @return custom log message
     */
    String message() default "";

    /**
     * 🔎 Condition that checks for absence of a bean type.
     *
     * <p>
     * Evaluates whether the {@link BeanContext} contains any beans of the specified type.
     * If such beans exist, registration is skipped.
     * </p>
     */
    class OnBeanAbsences implements BeanRegistrationCondition {

        private static final Logger LOGGER = LoggerFactory.getLogger(OnBeanAbsences.class);

        /**
         * Determines whether the bean should be registered.
         *
         * @param metadata conditional metadata for the bean definition
         * @param context  current bean context
         * @return {@code true} if no bean of the specified type exists; {@code false} otherwise
         */
        @Override
        public boolean match(ConditionalMetadata metadata, BeanContext context) {
            try {
                BeanIfTypeAbsent annotation = metadata.getAnnotation(BeanIfTypeAbsent.class);
                Class<?>         beanType   = annotation.value();
                String           beanName   = metadata.getBeanDefinition().getBeanName();

                if (beanType != null) {
                    boolean present = !context.getBeanNames(beanType).isEmpty();
                    String  message = annotation.message();

                    LOGGER.info(
                            "📦 Conditional check → type: {}, bean: [{}], status: {}",
                            beanType.getSimpleName(),
                            beanName,
                            present ? (Strings.isNotEmpty(message) ? message : "already present 🔁 (skipping)")
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