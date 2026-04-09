package org.jmouse.security.web.configuration.feature;

import org.jmouse.context.feature.FeatureSelector;
import org.jmouse.context.feature.FeatureSelectorContext;
import org.jmouse.security.web.access.method.Jsr250MethodSecurityConfigurer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

/**
 * 🔐 {@link FeatureSelector} for enabling JSR-250 method-level security.
 *
 * <p>
 * This selector conditionally activates method security support based on
 * JSR-250 annotations (e.g. {@code @RolesAllowed}) being present on the classpath.
 * </p>
 *
 * <p>
 * If detected, it contributes:
 * </p>
 * <ul>
 *     <li>{@link Jsr250MethodSecurityConfigurer} — registers interceptor for {@code @RolesAllowed}</li>
 * </ul>
 *
 * <p>
 * This allows seamless integration with Jakarta security annotations without
 * requiring explicit configuration beyond enabling this feature.
 * </p>
 */
public class Jsr250MethodSecurityFeatureSelector implements FeatureSelector {

    private static final Logger LOGGER = LoggerFactory.getLogger(Jsr250MethodSecurityFeatureSelector.class);

    /**
     * Selects JSR-250 method security infrastructure based on classpath inspection.
     *
     * <p>
     * If {@code jakarta.annotation.security.RolesAllowed} is present,
     * method security support is enabled. Otherwise, the feature is skipped.
     * </p>
     *
     * @param selectorContext feature selection context
     * @return selected classes for JSR-250 method security (may be empty)
     */
    @Override
    public Class<?>[] select(FeatureSelectorContext selectorContext) {
        List<Class<?>> classes = new ArrayList<>();

        if (selectorContext.isClassPresent("jakarta.annotation.security.RolesAllowed")) {
            LOGGER.info(
                    "Detected JSR-250 annotations on classpath → enabling method security (annotation: @{})",
                    "RolesAllowed"
            );
            classes.add(Jsr250MethodSecurityConfigurer.class);
        } else {
            LOGGER.debug(
                    "JSR-250 annotations not found → method security integration is disabled"
            );
        }

        return classes.toArray(Class<?>[]::new);
    }

}