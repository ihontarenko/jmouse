package org.jmouse.beans.annotation;

import org.jmouse.beans.BeanContext;
import org.jmouse.beans.conditions.BeanCondition;
import org.jmouse.beans.conditions.BeanRegistrationCondition;
import org.jmouse.beans.conditions.ConditionalMetadata;
import org.jmouse.util.IgnoreReasonGenerator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 🚫 Excludes a class or method from bean registration.
 * <p>
 * Useful to prevent specific components from being scanned or registered in the context.
 * </p>
 *
 * <pre>{@code
 * @Ignore(reason = "Deprecated component, replaced by NewComponent")
 * public class LegacyBean { ... }
 * }</pre>
 *
 * @author Ivan Hontarenko (Mr. Jerry Mouse)
 */
@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@BeanCondition(Ignore.AlwaysFalseCondition.class)
public @interface Ignore {

    /**
     * 💡 Optional reason why the bean is skipped.
     *
     * @return textual explanation, if any
     */
    String reason() default "";

    /**
     * 🤪 Whether to auto-generated a funny reason
     */
    boolean useFunny() default false;

    /**
     * ❌ Condition that always evaluates to false, preventing the bean from being registered.
     */
    class AlwaysFalseCondition implements BeanRegistrationCondition {

        private static final Logger LOGGER = LoggerFactory.getLogger(AlwaysFalseCondition.class);

        @Override
        public boolean match(ConditionalMetadata metadata, BeanContext context) {
            Ignore  ignore     = metadata.getAnnotation(Ignore.class);
            String  reason     = ignore.reason();
            String  logMessage = "ignored element: %s".formatted(metadata.getAnnotatedElement());
            boolean useFunny   = ignore.useFunny() && reason.isBlank();

            if (useFunny) {
                reason = new IgnoreReasonGenerator().generate();
            }

            if (reason != null && !reason.isBlank()) {
                logMessage = "'%s' — reason: %s".formatted(metadata.getAnnotatedElement(), reason);
            }

            LOGGER.warn("❌ Skipping registration for {}", logMessage);

            return false;
        }

    }

}

