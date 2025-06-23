package org.jmouse.mvc.initializer;

import org.jmouse.mvc.BeanInstanceInitializer;
import org.jmouse.mvc.mapping.DirectRequestPathMapping;

public class DirectRequestPathMappingInitializer implements BeanInstanceInitializer<DirectRequestPathMapping> {

    @Override
    public void initialize(DirectRequestPathMapping object) {
        object.addController("/qwerty",
                (request, response) -> response.getWriter().write("QWERTY"));
    }

    @Override
    public Class<DirectRequestPathMapping> objectClass() {
        return DirectRequestPathMapping.class;
    }

}
