package org.jmouse.web_app.controller;

import org.jmouse.core.reflection.annotation.MergedAnnotation;
import org.jmouse.core.reflection.annotation.MergedAnnotations;
import org.jmouse.mvc.mapping.annnotation.GetMapping;

import java.lang.reflect.Method;

public class Synt {

    public static void main(String[] args) throws NoSuchMethodException {
        Method           method  = IndexController.class.getMethod("demo");
        MergedAnnotation merged  = MergedAnnotation.wrapWithSynthetic(method);
        MergedAnnotation mapping = merged.getAnnotation(GetMapping.class).get();

        MergedAnnotations annotations = MergedAnnotations.ofAnnotatedElement(method);

        Object value = mapping.getMapping().getAttributeValue("path", Object.class);

        System.out.println(merged);
    }

}
