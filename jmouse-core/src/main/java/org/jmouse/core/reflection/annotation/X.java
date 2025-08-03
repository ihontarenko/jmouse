package org.jmouse.core.reflection.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
@interface A {
    String value() default "";
}

@Retention(RetentionPolicy.RUNTIME)
@A
@interface B {
    @AttributeFor(annotation = A.class, attribute = "value")
    String name() default "";
}

@B(name = "test")
class X {}
