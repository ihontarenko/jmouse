package org.jmouse.security.web;

import org.jmouse.context.feature.FeatureSelectorBinding;
import org.jmouse.security.web.configuration.feature.AuthorizeMethodFeatureSelector;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 🔐 Enables native jMouse method-level authorization support.
 *
 * <p>
 * When placed on a configuration or bootstrap class, this annotation activates
 * feature selection for method security based on the framework's internal
 * authorization mechanism.
 * </p>
 *
 * <p>
 * This annotation is intended for jMouse-native authorization rules such as
 * framework-defined security annotations and related interceptor infrastructure.
 * </p>
 *
 * <p>
 * Feature activation is delegated to {@link AuthorizeMethodFeatureSelector}.
 * </p>
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@FeatureSelectorBinding(AuthorizeMethodFeatureSelector.class)
public @interface EnableAuthorizeMethod {

}