package svit.container.definition;

import svit.container.BeanContext;

import java.lang.reflect.AnnotatedElement;

public interface BeanDefinitionCreationStrategy {

    boolean supports(AnnotatedElement element);

    BeanDefinition create(AnnotatedElement element, BeanContext context);

}
