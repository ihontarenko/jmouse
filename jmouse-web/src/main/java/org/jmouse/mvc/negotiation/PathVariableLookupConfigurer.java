package org.jmouse.mvc.negotiation;

import org.jmouse.beans.annotation.Bean;
import org.jmouse.beans.annotation.BeanConstructor;
import org.jmouse.web.negotiation.PathVariableLookup;

@Bean
public class PathVariableLookupConfigurer
        extends MappingMediaTypeLookupConfigurer<PathVariableLookup, PathVariableLookupProperties> {

    @BeanConstructor
    public PathVariableLookupConfigurer(PathVariableLookupProperties properties) {
        super(properties);
    }

}
