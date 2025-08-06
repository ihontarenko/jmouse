package org.jmouse.mvc.mapping.annnotation;

import org.jmouse.core.reflection.annotation.MapTo;
import org.jmouse.web.request.http.HttpHeader;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.PARAMETER)
@Retention(RetentionPolicy.RUNTIME)
public @interface RequestHeader {

    @MapTo(attribute = "name")
    HttpHeader value();

    @MapTo(attribute = "value")
    HttpHeader name() default HttpHeader.CONTENT_TYPE;

}
