package org.jmouse.web.mvc.negotiation;

import org.jmouse.web.mvc.BeanConfigurer;
import org.jmouse.web.negotiation.MappedMediaTypeLookup;

public class MappingMediaTypeLookupConfigurer<L extends MappedMediaTypeLookup, P extends MappingMediaTypeLookupProperties>
        implements BeanConfigurer<L> {

    private final P properties;

    public MappingMediaTypeLookupConfigurer(P properties) {
        this.properties = properties;
    }

    @Override
    public void configure(L parameterLookup) {
        parameterLookup.setKeyName(properties.getKeyName());
        properties.getMediaTypes().forEach(parameterLookup::addExtension);
    }

}
