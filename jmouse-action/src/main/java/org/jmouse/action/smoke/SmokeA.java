package org.jmouse.action.smoke;

import org.jmouse.action.*;
import org.jmouse.action.adapter.el.ActionELModule;
import org.jmouse.action.adapter.el.ActionExpressionAdapter;
import org.jmouse.action.adapter.mapper.ActionDefinitionMapper;
import org.jmouse.action.annotation.Action;
import org.jmouse.action.support.ActionAnnotationProcessor;
import org.jmouse.core.annotation.*;
import org.jmouse.core.invoke.ContextMethodArgumentResolver;
import org.jmouse.core.invoke.InvocationRequestMethodArgumentResolver;
import org.jmouse.core.invoke.MethodArgumentResolverComposite;
import org.jmouse.core.invoke.MethodInvoker;
import org.jmouse.core.mapping.Mapper;
import org.jmouse.core.mapping.Mappers;
import org.jmouse.core.scope.AbstractContext;
import org.jmouse.core.scope.BeanProvider;
import org.jmouse.core.scope.Context;
import org.jmouse.el.ExpressionLanguage;

import java.util.List;

public class SmokeA {


    public static void main(String... arguments) {

        ExpressionLanguage el = new ExpressionLanguage();
        ActionELModule.configure(el);

        ActionRegistry registryB = new ConfigurableActionRegistry(new SimpleActionRegistry(), Mappers.defaultMapper())
                .register("autoload", request -> {
                    System.out.println("executing autoload");
                    System.out.println(request.arguments());
                    return null;
                })
                .register("loadEnum", SourceTarget.class)
                .unwrap();

        ActionExecutor executor = ActionExecutor.defaults(registryB);

        ActionExpressionAdapter adapter =
                new ActionExpressionAdapter(el, executor);

        Context context = new AbstractContext() {
            @Override
            public void setBeanProvider(BeanProvider beanProvider) {
                super.setBeanProvider(beanProvider);
            }
        };

        AnnotationProcessingContext annotationContext = new AnnotationProcessingContext.Default();

        AnnotationBootstrapper bootstrapper =
                AnnotationBootstrapper.defaults(AnnotationDiscovery.defaults());

        bootstrapper.bootstrap(
                annotationContext,
                List.of(new ActionAnnotationProcessor.Default(registryB, getMethodInvoker(Mappers.defaultMapper()))),
                SmokeA.class
        );

        adapter.execute(
                "@Action[autoload]{'source':'user'}",
                context
        );

        adapter.execute(
                "@Action[autoloadA]{'source':'A', 'target':22d/7}",
                context
        );

        adapter.execute(
                "@Action[autoloadB]{'source':'B','target':[1, 2, 4, {'a':33D/9}]|list}",
                context
        );

        adapter.execute(
                "@Action[loadEnum]{'source' : 'org.jmouse.action.ActionDefinition' | class, 'target' : class('org.jmouse.action.ActionDefinition')}",
                context
        );
    }

    public static MethodInvoker getMethodInvoker(Mapper objectMapper) {
        ActionDefinitionMapper mapper = new ActionDefinitionMapper(objectMapper);

        MethodArgumentResolverComposite resolvers = new MethodArgumentResolverComposite()
                .addResolver(new ActionRequestMethodArgumentResolver())
                .addResolver(new ContextMethodArgumentResolver())
                .addResolver(new InvocationRequestMethodArgumentResolver())
                .addResolver(new MappedActionArgumentResolver(mapper));

        return new MethodInvoker.Default(new ArrayArgumentsMethodArgumentResolver(
                1, 2, 3, List.of("a", "b", "c")
        ));
    }

    @Action("autoloadA")
    public void autoload(int a, int b, int c, Object d) {
        System.out.println("A");
    }

//    @Action("autoloadB")
//    public void autoload(ActionRequest request, Context context, SourceTarget sourceTarget) {
//        System.out.println("B");
//    }

}
