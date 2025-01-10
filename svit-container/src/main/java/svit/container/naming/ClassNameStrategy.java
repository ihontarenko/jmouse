package svit.container.naming;

import svit.reflection.Reflections;
import svit.util.Strings;

import java.lang.reflect.AnnotatedElement;

public class ClassNameStrategy implements BeanNameStrategy {

    @Override
    public boolean supports(AnnotatedElement element) {
        return element instanceof Class<?>;
    }

    @Override
    public String resolve(AnnotatedElement element) {
        return Strings.underscored(Reflections.getShortName((Class<?>)element));
    }

}
