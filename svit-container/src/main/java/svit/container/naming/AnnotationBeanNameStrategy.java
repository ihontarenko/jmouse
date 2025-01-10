package svit.container.naming;

import svit.container.annotation.Configuration;
import svit.container.annotation.Provide;
import svit.container.annotation.Qualifier;

import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Method;

public class AnnotationBeanNameStrategy implements BeanNameStrategy {

    @Override
    public boolean supports(AnnotatedElement element) {
        return element instanceof Method || element instanceof Class<?>;
    }

    @Override
    public String resolve(AnnotatedElement element) {
        String name = null;

        // get name from annotation if present
        if (element.isAnnotationPresent(Qualifier.class)) {
            name = element.getAnnotation(Qualifier.class).value();
        }

        // get name from annotation if present
        if ((name == null || name.isBlank()) && element.isAnnotationPresent(Provide.class)) {
            name = element.getAnnotation(Provide.class).value();
        }

        // get name from annotation if present
        if ((name == null || name.isBlank()) && element.isAnnotationPresent(Configuration.class)) {
            name = element.getAnnotation(Configuration.class).name();
        }

        // get name by class-name strategy
        if ((name == null || name.isBlank()) && element instanceof Class<?> klass) {
            name = new ClassNameStrategy().resolve(klass);
        }

        // get method name
        if ((name == null || name.isBlank()) && element instanceof Method method) {
            name = method.getName();
        }

        return name;
    }

}
