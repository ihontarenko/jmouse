package svit.container.definition;

import svit.container.BeanContext;
import svit.container.naming.BeanNameResolver;
import svit.matcher.Matcher;
import svit.reflection.ClassMatchers;

import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Method;
import java.util.Set;

public class MethodBeanDefinitionCreationStrategy extends AbstractBeanDefinitionCreationStrategy {

    @Override
    public boolean supports(AnnotatedElement element) {
        return element instanceof Method;
    }

    @Override
    public BeanDefinition create(AnnotatedElement element, BeanContext context) {
        Method               method     = (Method) element;
        String               beanName   = context.getNameResolver().resolveName(method);
        MethodBeanDefinition definition = new MethodBeanDefinition(beanName, method.getReturnType());

        definition.setFactoryMethod(method);

        if (method.getParameterCount() != 0) {
            createDependencies(definition.getBeanDependencies(), method.getParameters());
        }

        updateBeanLifecycle(definition, method);

        definition.setAnnotations(Set.of(element.getAnnotations()));

        updateParentDefinition(definition, context);

        return definition;
    }

    public void updateParentDefinition(MethodBeanDefinition definition, BeanContext context) {
        BeanNameResolver  resolver         = context.getNameResolver();
        Class<?>          factoryClass     = definition.getFactoryMethod().getDeclaringClass();
        Matcher<Class<?>> matcher          = ClassMatchers.isSupertype(factoryClass);
        String            beanName         = resolver.resolveName(factoryClass);
        BeanDefinition    parentDefinition = context.getDefinition(beanName);

        if (parentDefinition == null || !matcher.matches(parentDefinition.getBeanClass())) {
            BeanDefinitionFactory factory = context.getBeanDefinitionFactory();
            parentDefinition = factory.createDefinition(factoryClass, context);
            context.registerDefinition(parentDefinition);
        }

        definition.setParentDefinition(parentDefinition);
        parentDefinition.addChildDefinition(definition);
    }

}
