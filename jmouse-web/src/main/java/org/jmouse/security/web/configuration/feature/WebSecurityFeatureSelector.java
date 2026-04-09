package org.jmouse.security.web.configuration.feature;

import org.jmouse.context.feature.FeatureSelector;
import org.jmouse.context.feature.FeatureSelectorContext;
import org.jmouse.security.web.SecurityFilterChainDelegator;
import org.jmouse.security.web.SecurityFilterOrder;
import org.jmouse.security.web.configuration.HttpSecurityConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

/**
 * 🔐 {@link FeatureSelector} that enables core web security infrastructure.
 *
 * <p>
 * This selector contributes the essential components required for HTTP-level
 * security integration in the web layer.
 * </p>
 *
 * <p>
 * Included components:
 * </p>
 * <ul>
 *     <li>{@link HttpSecurityConfiguration} — HTTP security configuration</li>
 *     <li>{@link SecurityFilterOrder} — canonical security filter ordering</li>
 *     <li>{@link SecurityFilterChainDelegator} — dispatcher for the active security filter chain</li>
 * </ul>
 *
 * <p>
 * This selector is typically activated via {@link org.jmouse.security.web.EnableWebSecurity}.
 * </p>
 */
public class WebSecurityFeatureSelector implements FeatureSelector {

    private static final Logger LOGGER = LoggerFactory.getLogger(WebSecurityFeatureSelector.class);

    /**
     * Selects core classes required for web security support.
     *
     * <p>
     * The selected classes are imported into the bean context and form the base
     * infrastructure for request-level security processing.
     * </p>
     *
     * @param selectorContext feature selection context
     * @return selected web security infrastructure classes
     */
    @Override
    public Class<?>[] select(FeatureSelectorContext selectorContext) {
        List<Class<?>> classes = new ArrayList<>();

        classes.add(HttpSecurityConfiguration.class);
        classes.add(SecurityFilterOrder.class);
        classes.add(SecurityFilterChainDelegator.class);

        LOGGER.info(
                "Enabling web security feature → imported components: [{}, {}, {}]",
                HttpSecurityConfiguration.class.getSimpleName(),
                SecurityFilterOrder.class.getSimpleName(),
                SecurityFilterChainDelegator.class.getSimpleName()
        );

        return classes.toArray(Class<?>[]::new);
    }

}