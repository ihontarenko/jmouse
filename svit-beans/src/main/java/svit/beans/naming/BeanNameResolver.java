package svit.beans.naming;

import java.lang.reflect.AnnotatedElement;

public interface BeanNameResolver {

    String resolveName(AnnotatedElement beanClass);

    void addStrategy(BeanNameStrategy strategy);

}
