package org.jmouse.mvc.mapping;

import jakarta.servlet.http.HttpServletRequest;
import org.jmouse.beans.definition.BeanDefinition;
import org.jmouse.core.reflection.MethodFinder;
import org.jmouse.core.reflection.MethodMatchers;
import org.jmouse.core.reflection.annotation.MergedAnnotation;
import org.jmouse.core.reflection.annotation.MergedAnnotations;
import org.jmouse.mvc.AbstractHandlerPathMapping;
import org.jmouse.mvc.HandlerMethod;
import org.jmouse.mvc.MappedHandler;
import org.jmouse.mvc.mapping.annnotation.Controller;
import org.jmouse.mvc.mapping.annnotation.Mapping;
import org.jmouse.web.context.WebBeanContext;

import java.lang.reflect.Method;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public class AnnotatedControllerHandlerMapping extends AbstractHandlerPathMapping<HandlerMethod> {

    @Override
    protected MappedHandler doGetHandler(HttpServletRequest request) {
        return null;
    }

    @Override
    protected void doInitialize(WebBeanContext context) {
        List<String> beanNames = context.getBeanNames(Object.class);

        for (String beanName : beanNames) {
            if (context.isLocalBean(beanName)) {
                BeanDefinition definition = context.getDefinition(beanName);

                if (definition.isAnnotatedWith(Controller.class)) {
                    Object handlerBean = context.getBean(definition.getBeanName());

                    Collection<Method> methods = new MethodFinder().find(
                            definition.getBeanClass(), MethodMatchers.isPublic());

                    for (Method method : methods) {
                        MergedAnnotation annotation = MergedAnnotation.wrapWithSynthetic(method);
                        MergedAnnotations annotations = MergedAnnotations.ofAnnotatedElement(method);

                        if (annotation.isAnnotationPresent(Mapping.class)) {
                            System.out.println(annotation);
                            Mapping mapping = annotation.createSynthesizedAnnotation(Mapping.class);
                        }

                        Optional<MergedAnnotation> first = annotations.get(Mapping.class);

                        if (first.isPresent()) {
                            Mapping mapping = first.get().createSynthesizedAnnotation(Mapping.class);
                            mapping.path();
                        }
                    }
                }

            }
        }

    }



}
