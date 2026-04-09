package org.jmouse.security.web.configuration.feature;

import org.jmouse.context.feature.FeatureSelector;
import org.jmouse.context.feature.FeatureSelectorContext;
import org.jmouse.security.web.access.method.AuthorizeMethodSecurityConfigurer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

/**
 * 🔎 {@link FeatureSelector} that enables native jMouse method authorization infrastructure.
 *
 * <p>
 * This selector contributes framework-internal method security components required
 * for processing authorization annotations through proxy-based interception.
 * </p>
 *
 * <p>
 * Currently it enables:
 * </p>
 * <ul>
 *     <li>{@link AuthorizeMethodSecurityConfigurer} — registers method security interceptor for native authorization annotations</li>
 * </ul>
 *
 * <p>
 * This selector is typically activated via {@code @EnableAuthorizeMethod}.
 * </p>
 */
public class AuthorizeMethodFeatureSelector implements FeatureSelector {

    private static final Logger LOGGER = LoggerFactory.getLogger(AuthorizeMethodFeatureSelector.class);

    /**
     * Selects classes required for native method authorization support.
     *
     * <p>
     * The returned classes are imported into the bean context as feature
     * infrastructure for method-level security.
     * </p>
     *
     * @param selectorContext feature selection context
     * @return selected infrastructure classes for native method authorization
     */
    @Override
    public Class<?>[] select(FeatureSelectorContext selectorContext) {
        List<Class<?>> classes = new ArrayList<>();

        classes.add(AuthorizeMethodSecurityConfigurer.class);

        LOGGER.info("Enabling native method authorization feature → imported components: [{}]",
                    AuthorizeMethodSecurityConfigurer.class.getSimpleName()
        );

        return classes.toArray(Class<?>[]::new);
    }

}