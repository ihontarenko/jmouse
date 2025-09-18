package org.jmouse.beans.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 🪞 Marks a bean (class or factory method) as proxied.
 *
 * <p>Used by the container to wrap the target in a proxy
 * (e.g. for AOP, lazy-init, or scoped lifecycle).</p>
 *
 * <ul>
 *   <li>🏷️ May be applied to a {@code @Bean} method.</li>
 *   <li>🏷️ May be applied directly on a component type.</li>
 * </ul>
 */
@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
public @interface ProxiedBean {
}
