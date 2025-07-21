package org.jmouse.context;

import java.lang.annotation.*;

@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
@BeanConditionApplicationBeanContext(rootOnly = true)
public @interface BeanForRootContext {

}
