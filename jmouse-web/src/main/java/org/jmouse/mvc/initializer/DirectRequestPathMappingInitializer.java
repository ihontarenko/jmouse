package org.jmouse.mvc.initializer;

import org.jmouse.mvc.WebMvcInitializer;
import org.jmouse.mvc.mapping.DirectRequestPathMapping;

public class DirectRequestPathMappingInitializer implements WebMvcInitializer<DirectRequestPathMapping> {

    @Override
    public void initialize(DirectRequestPathMapping object) {

    }

    @Override
    public Class<DirectRequestPathMapping> objectClass() {
        return DirectRequestPathMapping.class;
    }

}
