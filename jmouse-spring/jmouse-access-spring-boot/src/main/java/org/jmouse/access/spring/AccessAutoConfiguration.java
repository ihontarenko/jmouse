package org.jmouse.access.spring;

import org.jmouse.access.AccessEngine;
import org.jmouse.access.PlaceholderResolver;
import org.jmouse.access.EngineRefusals;
import org.jmouse.access.ScopeCatalog;
import org.jmouse.access.enforcement.AccessContext;
import org.jmouse.access.enforcement.AccessContextDeclarations;
import org.jmouse.access.enforcement.AccessRequirements;
import org.jmouse.access.enforcement.AmbientAccessValues;
import org.jmouse.access.enforcement.AccessTargetBinding;
import org.jmouse.access.enforcement.AmbientPlace;
import org.jmouse.access.enforcement.CurrentSubject;
import org.jmouse.access.enforcement.ExternalAccessRules;
import org.jmouse.access.enforcement.MethodAccessGuard;
import org.jmouse.access.enforcement.ParameterNaming;
import org.jmouse.access.enforcement.RefusalHandler;
import org.jmouse.access.enforcement.RequiresAccess;
import org.jmouse.access.spi.AccessContextScope;
import org.jmouse.access.spi.AccessTargetRegistry;
import org.jmouse.access.spi.ThreadBoundAccessContextScope;
import org.springframework.aop.Advisor;
import org.springframework.aop.Pointcut;
import org.springframework.aop.support.DefaultPointcutAdvisor;
import org.springframework.aop.support.Pointcuts;
import org.springframework.aop.ClassFilter;
import org.springframework.aop.support.StaticMethodMatcherPointcut;
import org.springframework.aop.support.annotation.AnnotationMatchingPointcut;

import java.lang.reflect.Method;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Role;
import org.springframework.core.Ordered;

/**
 * Turns {@link RequiresAccess} into advice, so an endpoint's declaration is enforced rather than
 * merely documented.
 *
 * <p>Everything here is wiring. The mechanism is in {@code jmouse-access} and
 * {@code jmouse-access-enforcement}; this module only decides which Spring bean holds which piece of
 * it, and every one of those decisions backs off to the application's own bean where there is one.
 *
 * <h2>What an application still has to supply</h2>
 *
 * <p>Three things, because all three are about the application rather than about authorization:
 * a {@link ScopeCatalog} and {@link EngineRefusals} (its vocabulary), a {@link CurrentSubject} (how it
 * authenticates) and a {@link RefusalHandler} (what a refusal should do). There is deliberately no
 * default for the first three — an engine that guessed a vocabulary would be an engine deciding about
 * scopes nobody declared.
 */
@Configuration(proxyBeanMethods = false)
@ConditionalOnBean(AccessEngine.class)
public class AccessAutoConfiguration {

    /**
     * How a handler's parameters are named. Spring MVC's answer where {@code spring-web} is present,
     * and the declared names otherwise — a non-web application still gets working enforcement.
     */
    @Bean
    @ConditionalOnMissingBean
    public ParameterNaming accessParameterNaming() {
        return ClassUtilities.isPresent("org.springframework.web.bind.annotation.PathVariable")
                ? new SpringParameterNaming()
                : ParameterNaming.declared();
    }

    /**
     * The ambient place, defaulting to none.
     *
     * <p>A product with a "current workspace" header replaces this; one where every route names its
     * own place needs nothing, and gets a binder that reads only the route.
     */
    @Bean
    @ConditionalOnMissingBean
    public AmbientPlace accessAmbientPlace() {
        return AmbientPlace.none();
    }

    /**
     * ⚠️ {@code all}, never {@code getIfAvailable}, for the reason stated below about ambient values:
     * declarations about somebody else's types naturally arrive one bean per library adopted, so the
     * moment there are two the application must not fail to start with a message about bean ambiguity
     * rather than about access control. Contributing is many; consuming is one.
     */
    @Bean
    @ConditionalOnMissingBean
    public AccessRequirements accessRequirements(
            ScopeCatalog scopes, ObjectProvider<ExternalAccessRules> external) {

        return new AccessRequirements(scopes, ExternalAccessRules.all(external.stream().toList()));
    }

    @Bean
    @ConditionalOnMissingBean
    public AccessTargetBinding accessTargetBinding(
            AccessTargetRegistry targets,
            ScopeCatalog         scopes,
            ParameterNaming      naming,
            AmbientPlace         ambient) {

        return new AccessTargetBinding(targets, scopes, naming, ambient);
    }

    /**
     * Where a published action lives while a call runs.
     *
     * <p>Bound to the thread, which is the honest default and — for an ordinary servlet request —
     * indistinguishable from being bound to the request. ⚠️ An application dispatching authorization
     * onto another thread replaces this, because a publication does not travel and a rule that stops
     * applying is a call that goes through.
     */
    @Bean
    @ConditionalOnMissingBean
    public AccessContextScope accessContextScope() {
        return new ThreadBoundAccessContextScope();
    }

    /**
     * What a route publishes about itself.
     *
     * @param placeholders how a {@code ${…}} in an {@code @AccessValue(is = …)} is filled. Optional:
     *                     it comes from the policy auto-configuration, which is only present where an
     *                     installation has policy files — and a declaration using a placeholder in an
     *                     installation with none refuses at startup rather than silently comparing
     *                     against the literal text
     */
    @Bean
    @ConditionalOnMissingBean
    public AccessContextDeclarations accessContextDeclarations(
            ParameterNaming                      naming,
            ObjectProvider<PlaceholderResolver>  placeholders,
            ObjectProvider<AmbientAccessValues>  ambient) {

        // ⚠️ `all`, never `getIfAvailable`. Attaching values is naturally spread across a product —
        // one bean per thing it knows about the surrounding request — so the moment it grows a second
        // contributor `getIfAvailable` stops the application from starting, with a message about bean
        // ambiguity rather than about access control. Contributing is many; consuming is one.
        return new AccessContextDeclarations(
                naming,
                placeholders.getIfAvailable(PlaceholderResolver::none),
                AmbientAccessValues.all(ambient.stream().toList()));
    }

    @Bean
    @ConditionalOnMissingBean
    public MethodAccessGuard methodAccessGuard(
            AccessEngine              engine,
            AccessRequirements        requirements,
            AccessTargetBinding       binding,
            EngineRefusals            refusals,
            AccessContextDeclarations contexts,
            AccessContextScope        published) {

        return new MethodAccessGuard(engine, requirements, binding, refusals, contexts, published);
    }

    /**
     * The advice itself.
     *
     * <p>Registered at {@code ROLE_INFRASTRUCTURE} for the same reason Spring Security registers its
     * own method-security advisors that way: it is machinery rather than application state, and the
     * infrastructure proxy creator is what picks it up. The {@code static} factory method is the other
     * half of that — an advisor bean must be built without instantiating the configuration class that
     * declares it.
     *
     * <p>It runs last, so authentication and any surviving {@code @PreAuthorize} have already had
     * their say: somebody who is not signed in should read "sign in", not "that module is off in a
     * place you are not in".
     */
    @Bean
    @Role(BeanDefinition.ROLE_INFRASTRUCTURE)
    @ConditionalOnMissingBean(name = "accessAuthorizationAdvisor")
    static Advisor accessAuthorizationAdvisor(
            ObjectProvider<MethodAccessGuard>   guard,
            ObjectProvider<CurrentSubject>      subject,
            ObjectProvider<RefusalHandler>      refusals,
            ObjectProvider<ExternalAccessRules> external) {

        /*
         * ⚠️ @AccessContext is part of the pointcut, not only @RequiresAccess.
         *
         * A route may publish what it is doing without being gated itself — an agent tool, a
         * listing whose own permission is enough — and the publication still has to reach the
         * programmatic checks made inside it. Left out, those routes would be un-advised and the
         * values would silently never appear, which is the failure this whole cluster exists to
         * remove rather than relocate.
         */
        Pointcut annotated = Pointcuts.union(
                Pointcuts.union(
                        AnnotationMatchingPointcut.forClassAnnotation(RequiresAccess.class),
                        AnnotationMatchingPointcut.forMethodAnnotation(RequiresAccess.class)),
                Pointcuts.union(
                        AnnotationMatchingPointcut.forClassAnnotation(AccessContext.class),
                        AnnotationMatchingPointcut.forMethodAnnotation(AccessContext.class)));

        /*
         * ⚠️ And the types a product declares a rule ABOUT rather than ON.
         *
         * A library's controller cannot carry the annotation, so without this it is un-advised — and
         * the only remaining way to guard it is a URL rule in the web layer, gated on a role, which
         * reaches none of the axes. `ExternalAccessRules` says why that is worse than it looks. Asked
         * lazily, because this advisor is built while the context is still coming up and the
         * declarations are ordinary application beans.
         */
        Pointcut pointcut = Pointcuts.union(annotated, declaredExternally(external));

        DefaultPointcutAdvisor advisor = new DefaultPointcutAdvisor(
                pointcut, new AccessAuthorizationInterceptor(guard, subject, refusals));

        advisor.setOrder(Ordered.LOWEST_PRECEDENCE);

        return advisor;
    }

    /**
     * Every type some {@link ExternalAccessRules} bean speaks about, as a pointcut.
     *
     * <p>⚠️ A {@link StaticMethodMatcherPointcut} rather than a class filter alone, because the rules
     * may name a single method on a type whose other routes stay un-gated — and matching the whole type
     * would advise methods nothing has a requirement for, which is harmless but reads as guarded.
     */
    private static Pointcut declaredExternally(ObjectProvider<ExternalAccessRules> external) {
        return new StaticMethodMatcherPointcut() {

            @Override
            public boolean matches(Method method, Class<?> targetClass) {
                return rules().forMethod(method, targetClass).isPresent();
            }

            @Override
            public ClassFilter getClassFilter() {
                return rules()::covers;
            }

            private ExternalAccessRules rules() {
                return ExternalAccessRules.all(external.stream().toList());
            }
        };
    }

    /** Whether an optional integration is on the classpath, without dragging it in to find out. */
    private static final class ClassUtilities {

        private ClassUtilities() {
        }

        static boolean isPresent(String className) {
            try {
                Class.forName(className, false, ClassUtilities.class.getClassLoader());
                return true;
            } catch (ClassNotFoundException absent) {
                return false;
            }
        }
    }
}
