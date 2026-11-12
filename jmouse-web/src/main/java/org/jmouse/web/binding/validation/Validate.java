package org.jmouse.web.binding.validation;

import java.lang.annotation.*;

@Target({
    ElementType.TYPE,
    ElementType.FIELD,
    ElementType.METHOD,
    ElementType.PARAMETER,
    ElementType.RECORD_COMPONENT
})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface Validate {
    Class<?>[] groups() default {};
}
