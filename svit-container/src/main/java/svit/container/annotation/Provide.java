package svit.container.annotation;

import svit.container.Lifecycle;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
public @interface Provide {

    String value() default "";

    Lifecycle lifecycle() default Lifecycle.SINGLETON;

}
