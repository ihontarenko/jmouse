package org.jmouse.security.web.configuration.feature;

import org.jmouse.context.feature.FeatureSelector;
import org.jmouse.context.feature.FeatureSelectorContext;
import org.jmouse.security.web.configuration.HttpSecurityConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

public class WebSecurityFeatureSelector implements FeatureSelector {

    private static final Logger LOGGER = LoggerFactory.getLogger(WebSecurityFeatureSelector.class);

    @Override
    public Class<?>[] select(FeatureSelectorContext selectorContext) {
        List<Class<?>> classes = new ArrayList<>();

        classes.add(HttpSecurityConfiguration.class);

        if (selectorContext.isClassPresent("jakarta.servlet.Filter")) {
            LOGGER.info("@jakarta.servlet.Filter present!");
        }

        return classes.toArray(Class<?>[]::new);
    }

}