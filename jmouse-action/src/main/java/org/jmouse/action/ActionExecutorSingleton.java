package org.jmouse.action;

import org.jmouse.action.adapter.el.ActionELModule;
import org.jmouse.action.adapter.el.ActionExpressionAdapter;
import org.jmouse.action.adapter.mapper.ActionDefinitionMapper;
import org.jmouse.action.annotation.Action;
import org.jmouse.action.support.ActionAnnotationProcessor;
import org.jmouse.core.MemoizedFunction;
import org.jmouse.core.ParameterizedSupplier;
import org.jmouse.core.SingletonFactories;
import org.jmouse.core.annotation.AnnotationBootstrapper;
import org.jmouse.core.annotation.AnnotationDiscovery;
import org.jmouse.core.annotation.AnnotationProcessingContext;
import org.jmouse.core.annotation.AnnotationProcessor;
import org.jmouse.core.invoke.ContextMethodArgumentResolver;
import org.jmouse.core.invoke.InvocationRequestMethodArgumentResolver;
import org.jmouse.core.invoke.MethodArgumentResolverComposite;
import org.jmouse.core.invoke.MethodInvoker;
import org.jmouse.core.mapping.Mapper;
import org.jmouse.core.mapping.Mappers;
import org.jmouse.el.ExpressionLanguage;

import java.util.List;

final public class ActionExecutorSingleton {

    private static final ParameterizedSupplier<Class<?>[], ActionExecutor> ACTION_EXECUTOR_SINGLETON = SingletonFactories.singleton(baseClasses -> {
        ExpressionLanguage expressionLanguage = ExpressionLanguage.getSingleton();
        Mapper             mapper             = Mappers.defaultMapper();

        ActionELModule.configure(expressionLanguage);

        ActionRegistry              registry  = new ConfigurableActionRegistry(
                new SimpleActionRegistry(), mapper).unwrap();
        AnnotationProcessingContext context   = AnnotationProcessingContext.defaults();
        AnnotationProcessor<Action> processor = new ActionAnnotationProcessor.Default(
                registry, getMethodInvoker(mapper));

        AnnotationBootstrapper.defaults(AnnotationDiscovery.defaults()).bootstrap(
                context,
                List.of(processor),
                baseClasses
        );

        return ActionExecutor.defaults(registry);
    });

    private static final MemoizedFunction<Class<?>[], ActionExecutor> ACTION_EXECUTOR_MEMOIZED = SingletonFactories.memoized(ACTION_EXECUTOR_SINGLETON::get);

    public static ActionExecutor getActionExecutor(Class<?>... baseClasses) {
        return ACTION_EXECUTOR_MEMOIZED.get(baseClasses);
    }

    public static ActionExpressionAdapter getActionExpressionAdapter(ActionExecutor actionExecutor) {
        return new ActionExpressionAdapter(ExpressionLanguage.getSingleton(), actionExecutor);
    }

    public static ActionExpressionAdapter getActionExpressionAdapter() {
        return getActionExpressionAdapter(getActionExecutor());
    }

    public static MethodInvoker getMethodInvoker(Mapper objectMapper) {
        ActionDefinitionMapper mapper = new ActionDefinitionMapper(objectMapper);

        MethodArgumentResolverComposite resolvers = new MethodArgumentResolverComposite()
                .addResolver(new ActionRequestMethodArgumentResolver())
                .addResolver(new ContextMethodArgumentResolver())
                .addResolver(new InvocationRequestMethodArgumentResolver())
                .addResolver(new MappedActionArgumentResolver(mapper));

        return new MethodInvoker.Default(resolvers);
    }

}
