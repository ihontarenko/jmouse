package org.jmouse.beans.scanner;

import org.jmouse.beans.BeanScanner;
import org.jmouse.beans.annotation.Bean;
import org.jmouse.beans.annotation.BeanFactories;
import org.jmouse.core.Streamable;
import org.jmouse.core.matcher.Matcher;

import java.util.Collection;

import static org.jmouse.core.reflection.ClassMatchers.isAnnotatedWith;

public class ExplicitClassBeanScanner implements BeanScanner<Class<?>> {

    private final static Matcher<Class<?>> CLASS_MATCHER = isAnnotatedWith(
            BeanFactories.class
    ).or(isAnnotatedWith(
            Bean.class
    ));

    @Override
    public Collection<Class<?>> scan(Class<?>... baseClasses) {
        return Streamable.of(baseClasses).filter(CLASS_MATCHER::matches).toList();
    }

}
