package org.jmouse.mvc.converter;

import org.jmouse.beans.BeanContext;
import org.jmouse.beans.InitializingBean;
import org.jmouse.core.MediaType;
import org.jmouse.util.Sorter;
import org.jmouse.web.context.WebBeanContext;

import java.util.ArrayList;
import java.util.List;

public class MessageConverterManager implements InitializingBean {

    private final List<HttpMessageConverter<?>> converters = new ArrayList<>();

    public void register(HttpMessageConverter<?> converter) {
        converters.add(converter);
    }

    @SuppressWarnings("unchecked")
    public <T> HttpMessageConverter<T> getWriter(T value, String contentType) {
        for (HttpMessageConverter<?> converter : converters) {
            if (converter.canWrite(value.getClass(), MediaType.forString(contentType))) {
                return (HttpMessageConverter<T>) converter;
            }
        }

        throw new UnsuitableException("No suitable converter found: " + contentType);
    }

    @Override
    @SuppressWarnings({"unchecked", "rawtypes"})
    public void afterCompletion(BeanContext context) {
        List<HttpMessageConverter>    converters        = WebBeanContext.getLocalBeans(
                HttpMessageConverter.class, (WebBeanContext) context);
        List<HttpMessageConverter<?>> messageConverters = (List<HttpMessageConverter<?>>) (List<?>) converters;

        messageConverters = new ArrayList<>(messageConverters);
        Sorter.sort(messageConverters);

        this.converters.addAll(messageConverters);
    }

}