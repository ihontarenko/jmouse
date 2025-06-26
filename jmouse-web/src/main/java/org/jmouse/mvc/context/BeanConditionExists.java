package org.jmouse.mvc.context;

import org.jmouse.beans.BeanContext;
import org.jmouse.beans.conditions.BeanCondition;
import org.jmouse.beans.conditions.BeanRegistrationCondition;
import org.jmouse.beans.conditions.ConditionalMetadata;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@BeanCondition(BeanConditionExists.OnBeanExists.class)
public @interface BeanConditionExists {

    String value() default "";

    class OnBeanExists implements BeanRegistrationCondition {
        @Override
        public boolean match(ConditionalMetadata metadata, BeanContext context) {
            BeanConditionExists annotation = metadata.getAnnotation(BeanConditionExists.class);
            return !context.containsDefinition(annotation.value());
        }
    }

}
