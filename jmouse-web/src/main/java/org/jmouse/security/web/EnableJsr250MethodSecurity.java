package org.jmouse.security.web;

import org.jmouse.context.feature.FeatureSelectorBinding;
import org.jmouse.security.web.configuration.feature.Jsr250MethodSecurityFeatureSelector;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 🔐 Enables JSR-250 method-level security support.
 *
 * <p>
 * Activates integration with Jakarta security annotations such as
 * {@code @RolesAllowed}. When enabled, method invocations are intercepted
 * and authorization rules are enforced accordingly.
 * </p>
 *
 * <p>
 * Feature activation is delegated to {@link Jsr250MethodSecurityFeatureSelector},
 * which conditionally enables the infrastructure based on classpath availability.
 * </p>
 *
 * <p>
 * This feature is optional and typically used alongside
 * {@code @EnableWebSecurity}.
 * </p>
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@FeatureSelectorBinding(Jsr250MethodSecurityFeatureSelector.class)
public @interface EnableJsr250MethodSecurity {

}