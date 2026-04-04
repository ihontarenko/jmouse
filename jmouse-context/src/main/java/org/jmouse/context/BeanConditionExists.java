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
 * ☑️ Conditional bean registration based on existence of another bean.
 * <p>
 * Register the annotated bean only if the bean with the given name <b>does not exist</b> in the current context.
 * <p>
 * Can be used on classes or methods that define beans.
 *
 * <pre>{@code
 * @BeanConditionExists("dataSource")
 * public class FallbackDataSource { ... }
 * }</pre>
 *
 * @author Ivan Hontarenko (Mr. Jerry Mouse)
 */
@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@BeanCondition(BeanConditionExists.OnBeanAbsences.class)
public @interface BeanConditionExists {

    /**
     * The name of the bean whose presence in the context should be checked.
     * If not specified, will default to an empty string.
     *
     * @return the target bean name
     */
    String value() default "";

    /**
     * The message will be present in log if bean already registered
     */
    String message() default "";

    /**
     * 🔍 Condition logic that checks for absence of the given bean in context.
     * <p>
     * Returns {@code true} if the named bean is <b>not</b> present.
     */
    class OnBeanAbsences implements BeanRegistrationCondition {

        private static final Logger LOGGER = LoggerFactory.getLogger(OnBeanAbsences.class);

        @Override
        public boolean match(ConditionalMetadata metadata, BeanContext context) {
            try {
                BeanConditionExists annotation = metadata.getAnnotation(BeanConditionExists.class);
                String              value      = annotation.value();
                String              beanName   = metadata.getBeanDefinition().getBeanName();

                if (value != null && !value.isBlank()) {
                    boolean present = context.containsDefinition(value);
                    String  message = annotation.message();
                    LOGGER.info(
                            "📦 Bean [{}] → {}",
                            beanName,
                            present ? Strings.isNotEmpty(message) ? message : "already present 🔁" : "new registration 🆕"
                    );
                    return !present;
                }

                return true;
            } catch (IllegalStateException exception) {
                return false;
            }
        }
    }
}
