package svit.beans.naming;

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
        return Strings.uncapitalize(Reflections.getShortName((Class<?>)element));
    }

}
