package org.jmouse.context;

import org.jmouse.beans.BeanContext;
import org.jmouse.beans.conditions.AbstractBeanRegistrationCondition;
import org.jmouse.beans.conditions.BeanCondition;
import org.jmouse.beans.conditions.ConditionalMetadata;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.annotation.*;

@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
@BeanConditionNestedContext(rootOnly = true)
public @interface BeanConditionForRootContext {

}
