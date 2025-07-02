package org.jmouse.context;

import org.jmouse.beans.BeanContext;
import org.jmouse.beans.conditions.AbstractBeanRegistrationCondition;
import org.jmouse.beans.conditions.BeanCondition;
import org.jmouse.beans.conditions.ConditionalMetadata;
import org.jmouse.core.bind.accessor.PropertyResolverAccessor;
import org.jmouse.el.ExpressionLanguage;
import org.jmouse.el.evaluation.EvaluationContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.annotation.*;
import java.util.Map;

/**
 * 🧠 Enables conditional bean registration based on expression evaluation.
 * <p>
 * Evaluates EL expressions against the current environment or property source,
 * optionally verifying result via {@link ComparisonOperator}.
 * </p>
 *
 * <pre>{@code
 * @BeanConditionExpression(
 *     value = "application.feature.enabled",
 *     expected = "true",
 *     operator = ComparisonOperator.EQ
 * )
 * @BeanConditionExpression("jmouse.service.nodeNumber % 3 is odd")
 * }</pre>
 *
 * @author Ivan Hontarenko (Mr. Jerry Mouse)
 */
@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
@BeanCondition(BeanConditionExpression.ExpressionCondition.class)
public @interface BeanConditionExpression {

    /**
     * ✏️ Expression to evaluate (e.g. property key, EL condition).
     */
    String value() default "";

    /**
     * ✅ Expected result (used only for string-based comparison).
     */
    String expected() default "";

    /**
     * 🔧 Comparison operator (used if result is a String).
     */
    ComparisonOperator operator() default ComparisonOperator.EQ;

    /**
     * 🧪 Condition that checks the result of an EL expression.
     */
    class ExpressionCondition extends AbstractBeanRegistrationCondition {

        private static final Logger LOGGER = LoggerFactory.getLogger(ExpressionCondition.class);

        @Override
        public boolean match(ConditionalMetadata metadata, BeanContext context) {
            BeanConditionExpression annotation = metadata.getAnnotation(BeanConditionExpression.class);
            String                  expression = annotation.value();
            boolean                 match      = true;

            if (context instanceof ApplicationBeanContext applicationContext &&
                    expression != null && !expression.isBlank()) {

                ExpressionLanguage expressionLanguage = applicationContext.getBean(ExpressionLanguage.class);
                EvaluationContext  evaluationContext  = expressionLanguage.newContext();

                // ⚙️ Load environment values into the context
                Object source = new PropertyResolverAccessor(applicationContext.getEnvironment()).unwrap();
                if (source instanceof Map<?, ?> map) {
                    @SuppressWarnings("unchecked") Map<String, Object> casted = (Map<String, Object>) map;
                    casted.forEach(evaluationContext::setValue);
                }

                Object result = expressionLanguage.evaluate(expression, evaluationContext);
                match = false;

                if (result instanceof Boolean booleanValue) {
                    match = booleanValue;
                    LOGGER.info("✅ Expression '{}' evaluated to boolean: {}", expression, booleanValue);
                } else if (result instanceof String string) {
                    match = switch (annotation.operator()) {
                        case EQ -> string.equals(annotation.expected());
                        case CONTAINS -> string.contains(annotation.expected());
                        case STARTS -> string.startsWith(annotation.expected());
                        case ENDS -> string.endsWith(annotation.expected());
                    };

                    if (match) {
                        LOGGER.info("✅ Expression '{}' matched with [{} '{}']",
                                    expression, annotation.operator(), annotation.expected());
                    } else {
                        LOGGER.info("⛔ Expression '{}' did NOT match [{} '{}']",
                                    expression, annotation.operator(), annotation.expected());
                    }

                } else {
                    LOGGER.warn("⚠️ Expression '{}' returned unsupported result: {}. Expected boolean or string.",
                                expression, result != null ? result.getClass().getName() : "null");
                }
            }

            return match;
        }
    }
}
