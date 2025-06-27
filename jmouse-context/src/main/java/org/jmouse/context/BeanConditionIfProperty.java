package org.jmouse.context;

import org.jmouse.beans.BeanContext;
import org.jmouse.beans.conditions.AbstractBeanRegistrationCondition;
import org.jmouse.beans.conditions.BeanCondition;
import org.jmouse.beans.conditions.ConditionalMetadata;
import org.jmouse.core.env.Environment;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.annotation.*;

/**
 * 🧩 Registers a bean if a property exists (optionally with expected value).
 */
@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
@BeanCondition(BeanConditionIfProperty.PropertyCondition.class)
public @interface BeanConditionIfProperty {

    /**
     * 🔑 Property key to check.
     */
    String name();

    /**
     * 🧾 Expected value (optional).
     */
    String value() default "";

    /**
     * 🔄 Whether to match if property is missing.
     */
    boolean matchIfMissing() default false;

    /**
     * 📐 Matching strategy.
     */
    ComparisonOperator operator() default ComparisonOperator.EQ;

    /**
     * 🔀 Comparison logic for property matching.
     */
    enum ComparisonOperator {
        EQ, CONTAINS, STARTS, ENDS
    }

    /**
     * ✅ Condition that checks environment for property presence and value.
     */
    class PropertyCondition extends AbstractBeanRegistrationCondition {

        private static final Logger LOGGER = LoggerFactory.getLogger(PropertyCondition.class);

        @Override
        public boolean match(ConditionalMetadata metadata, BeanContext context) {
            BeanConditionIfProperty annotation = metadata.getAnnotation(BeanConditionIfProperty.class);
            boolean                 match      = annotation.matchIfMissing();

            if (context instanceof ApplicationBeanContext applicationContext) {
                Environment environment = applicationContext.getEnvironment();
                String      value       = environment.getProperty(annotation.name());

                if (value != null) {
                    match = switch (annotation.operator()) {
                        case EQ -> value.equals(annotation.value());
                        case CONTAINS -> value.contains(annotation.value());
                        case ENDS -> value.endsWith(annotation.value());
                        case STARTS -> value.startsWith(annotation.value());
                    };
                }

                if (match) {
                    LOGGER.info("✅ Condition passed: property '{}' is {} '{}'",
                                annotation.name(), annotation.operator(), value);
                } else {
                    LOGGER.info("⛔ Condition failed: property '{}' does not {} '{}'",
                                 annotation.name(), annotation.operator(), annotation.value());
                }
            }

            return match;
        }
    }
}
