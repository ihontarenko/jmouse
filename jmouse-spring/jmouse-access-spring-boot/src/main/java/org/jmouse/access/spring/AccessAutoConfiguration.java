package org.jmouse.access.spring;

import org.jmouse.access.AccessEngine;
import org.jmouse.access.EngineRefusals;
import org.jmouse.access.ScopeCatalog;
import org.jmouse.access.enforcement.AccessRequirements;
import org.jmouse.access.enforcement.AccessTargetBinding;
import org.jmouse.access.enforcement.AmbientPlace;
import org.jmouse.access.enforcement.CurrentSubject;
import org.jmouse.access.enforcement.MethodAccessGuard;
import org.jmouse.access.enforcement.ParameterNaming;
import org.jmouse.access.enforcement.RefusalHandler;
import org.jmouse.access.enforcement.RequiresAccess;
import org.jmouse.access.spi.AccessTargetRegistry;
import org.springframework.aop.Advisor;
import org.springframework.aop.Pointcut;
import org.springframework.aop.support.DefaultPointcutAdvisor;
import org.springframework.aop.support.Pointcuts;
import org.springframework.aop.support.annotation.AnnotationMatchingPointcut;
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

    @Bean
    @ConditionalOnMissingBean
    public AccessRequirements accessRequirements(ScopeCatalog scopes) {
        return new AccessRequirements(scopes);
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

    @Bean
    @ConditionalOnMissingBean
    public MethodAccessGuard methodAccessGuard(
            AccessEngine        engine,
            AccessRequirements  requirements,
            AccessTargetBinding binding,
            EngineRefusals      refusals) {

        return new MethodAccessGuard(engine, requirements, binding, refusals);
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
            ObjectProvider<MethodAccessGuard> guard,
            ObjectProvider<CurrentSubject>    subject,
            ObjectProvider<RefusalHandler>    refusals) {

        Pointcut pointcut = Pointcuts.union(
                AnnotationMatchingPointcut.forClassAnnotation(RequiresAccess.class),
                AnnotationMatchingPointcut.forMethodAnnotation(RequiresAccess.class));

        DefaultPointcutAdvisor advisor = new DefaultPointcutAdvisor(
                pointcut, new AccessAuthorizationInterceptor(guard, subject, refusals));

        advisor.setOrder(Ordered.LOWEST_PRECEDENCE);

        return advisor;
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
