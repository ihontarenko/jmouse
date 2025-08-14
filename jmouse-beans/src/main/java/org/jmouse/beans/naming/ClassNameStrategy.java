package org.jmouse.beans.naming;

import org.jmouse.core.reflection.Reflections;
import org.jmouse.util.Strings;

import java.lang.reflect.AnnotatedElement;

public class ClassNameStrategy implements BeanNameStrategy {

    @Override
    public boolean supports(AnnotatedElement element) {
        return element instanceof Class<?>;
    }

    @Override
    public String resolve(AnnotatedElement element) {
        return Strings.uncapitalize(Reflections.getShortName((Class<?>)element));
    }

}
