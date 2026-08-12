package org.jmouse.access.spring;

import org.aopalliance.intercept.MethodInterceptor;
import org.aopalliance.intercept.MethodInvocation;
import org.jmouse.access.enforcement.AccessRefusal;
import org.jmouse.access.enforcement.CurrentSubject;
import org.jmouse.access.enforcement.MethodAccessGuard;
import org.jmouse.access.enforcement.RefusalHandler;
import org.jmouse.access.enforcement.RequiresAccess;
import org.springframework.aop.support.AopUtils;
import org.springframework.beans.factory.ObjectProvider;

import java.lang.reflect.Method;

/**
 * Enforces {@link RequiresAccess} as Spring advice.
 *
 * <p>Almost nothing happens here, and that is the design: unwrap the proxy to find the real class,
 * ask {@link MethodAccessGuard}, hand any refusal to the {@link RefusalHandler}. Everything that
 * decides lives in {@code jmouse-access-enforcement} and runs the same way under any framework.
 *
 * <p>It runs as advice of its own rather than inside an authorization expression, because a
 * method-level {@code @PreAuthorize} <em>replaces</em> a class-level one — a condition written into a
 * class expression is silently discarded by every method that names a permission. Separate advice
 * composes.
 *
 * <p>Its collaborators are looked up per call. This is security infrastructure wired while the
 * context is still coming up, and holding a transactional bean resolved that early is how one ends up
 * with the unproxied instance rather than the one that opens transactions.
 */
public class AccessAuthorizationInterceptor implements MethodInterceptor {

    private final ObjectProvider<MethodAccessGuard> guard;
    private final ObjectProvider<CurrentSubject>    subject;
    private final ObjectProvider<RefusalHandler>    refusals;

    public AccessAuthorizationInterceptor(
            ObjectProvider<MethodAccessGuard> guard,
            ObjectProvider<CurrentSubject>    subject,
            ObjectProvider<RefusalHandler>    refusals) {

        this.guard    = guard;
        this.subject  = subject;
        this.refusals = refusals;
    }

    /**
     * ⚠️ The window closes <em>after</em> the method has run, and that is the whole ordering.
     *
     * <p>What a route publishes about itself has to be in context for the check that gates it and for
     * every check made inside it — a programmatic {@code engine.decide(…)} three layers down is where
     * decisions are most often really made. Closing before {@code proceed()} would leave the
     * annotation gating the door and nothing else, and a rule reading a value inside would silently
     * never fire.
     */
    @Override
    public Object invoke(MethodInvocation invocation) throws Throwable {
        Method   method      = invocation.getMethod();
        Class<?> targetClass = invocation.getThis() != null
                ? AopUtils.getTargetClass(invocation.getThis())
                : method.getDeclaringClass();

        try (MethodAccessGuard.GuardedCall call = guard.getObject()
                .around(method, targetClass, invocation.getArguments(), subject.getObject().get())) {

            AccessRefusal refusal = call.refusal().orElse(null);

            if (refusal != null) {
                refusals.getObject().onRefusal(refusal, method);
            }

            return invocation.proceed();
        }
    }
}
