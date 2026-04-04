package org.jmouse.security.web;

import org.jmouse.context.feature.FeatureSelectorBinding;
import org.jmouse.security.web.configuration.feature.WebSecurityFeatureSelector;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@FeatureSelectorBinding(WebSecurityFeatureSelector.class)
public @interface EnableWebSecurity {
}
