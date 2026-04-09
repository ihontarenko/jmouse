package org.jmouse.security.web;

import org.jmouse.context.feature.FeatureSelectorBinding;
import org.jmouse.security.web.configuration.feature.WebSecurityFeatureSelector;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 🔐 Enables core web security support.
 *
 * <p>
 * When declared on a bootstrap or configuration class, this annotation activates
 * web security feature selection and imports the base infrastructure required for
 * HTTP request security processing.
 * </p>
 *
 * <p>
 * Feature activation is delegated to {@link WebSecurityFeatureSelector}.
 * </p>
 *
 * <p>
 * Typical imported infrastructure includes HTTP security configuration,
 * security filter ordering, and filter chain delegation.
 * </p>
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@FeatureSelectorBinding(WebSecurityFeatureSelector.class)
public @interface EnableWebSecurity {
}