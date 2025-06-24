package org.jmouse.context;

import org.jmouse.beans.BeanContext;
import org.jmouse.beans.conditions.AbstractBeanCondition;
import org.jmouse.beans.conditions.BeanRestriction;
import org.jmouse.beans.conditions.ConditionalMetadata;
import org.jmouse.core.env.Environment;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.annotation.*;

/**
 * Registers a bean only if a specific property is present (and optionally has a specific value).
 */
@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
@BeanRestriction(BeanRestrictionIfProperty.IfPropertyBeanCondition.class)
public @interface BeanRestrictionIfProperty {

    String name();

    String value() default "";

    boolean matchIfMissing() default false;

    ComparisonOperator operator() default ComparisonOperator.EQ;

    enum ComparisonOperator {
        EQ, CONTAINS
    }

    class IfPropertyBeanCondition extends AbstractBeanCondition {

        private static final Logger LOGGER = LoggerFactory.getLogger(IfPropertyBeanCondition.class);

        @Override
        public boolean match(ConditionalMetadata metadata, BeanContext context) {
            BeanRestrictionIfProperty annotation = metadata.getAnnotation(BeanRestrictionIfProperty.class);
            boolean                   match      = annotation.matchIfMissing();

            if (context instanceof ApplicationBeanContext applicationContext) {
                Environment environment = applicationContext.getEnvironment();
                String      value       = environment.getProperty(annotation.name());

                if (value != null) {
                    match = switch (annotation.operator()) {
                        case EQ -> value.equals(annotation.value());
                        case CONTAINS -> value.contains(annotation.value());
                    };
                }

                if (match) {
                    LOGGER.info("Satisfied bean condition for {}={}", annotation.name(), value);
                }
            }

            return match;
        }

    }

}
