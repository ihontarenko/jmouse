package org.jmouse.mvc.converter;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.jmouse.core.MediaType;
import org.jmouse.web.context.WebBeanContext;

import java.util.List;

abstract public class AbstractJacksonHttpMessageConverter<T> extends AbstractHttpMessageConverter<T> {

    protected List<JacksonObjectMapperRegistration> mapperRegistrations;

    protected AbstractJacksonHttpMessageConverter(MediaType supportedMediaType) {
        super(supportedMediaType);
    }

    protected AbstractJacksonHttpMessageConverter(List<MediaType> supportedMediaTypes) {
        super(supportedMediaTypes);
    }

    public List<JacksonObjectMapperRegistration> getMapperRegistrations() {
        return mapperRegistrations;
    }

    public void setMapperRegistrations(List<JacksonObjectMapperRegistration> mapperRegistrations) {
        this.mapperRegistrations = mapperRegistrations;
    }

    public JacksonObjectMapperRegistration getObjectMapperRegistration(MediaType mediaType) {
        JacksonObjectMapperRegistration registration = null;

        for (JacksonObjectMapperRegistration mapperRegistration : getMapperRegistrations()) {
            if (mapperRegistration.isApplicable(mediaType)) {
                registration = mapperRegistration;
            }
        }

        return registration;
    }

    public ObjectMapper getObjectMapper(MediaType mediaType) {
        JacksonObjectMapperRegistration registration = getObjectMapperRegistration(mediaType);
        ObjectMapper                    objectMapper = null;

        if (registration != null) {
            objectMapper = registration.objectMapper();
        }

        return objectMapper;
    }

    @Override
    protected boolean supportsType(Class<?> clazz) {
        return true;
    }

    @Override
    public void doInitialize(WebBeanContext context) {
        setMapperRegistrations(
                WebBeanContext.getLocalBeans(JacksonObjectMapperRegistration.class, context)
        );
    }
}
