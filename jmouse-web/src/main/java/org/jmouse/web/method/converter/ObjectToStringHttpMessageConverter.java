package org.jmouse.web.method.converter;

import org.jmouse.core.MediaType;
import org.jmouse.core.convert.Conversion;
import org.jmouse.web.context.WebBeanContext;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

public class ObjectToStringHttpMessageConverter extends AbstractHttpMessageConverter<Object> {

    private Conversion conversion;

    protected ObjectToStringHttpMessageConverter() {
        super(MediaType.TEXT_PLAIN);
    }

    @Override
    @SuppressWarnings("unchecked")
    protected void doWrite(Object data, Class<?> type, HttpOutputMessage outputMessage) throws IOException {
        Class<Object> sourceType = (Class<Object>) type;
        Class<String> targetType = String.class;
        String        converted  = conversion.convert(data, sourceType, targetType);
        outputMessage.getOutputStream().write(converted.getBytes(StandardCharsets.UTF_8));
    }

    @Override
    protected Object doRead(Class<?> clazz, HttpInputMessage inputMessage) throws IOException {
        return null;
    }

    @Override
    protected boolean supportsType(Class<?> clazz) {
        return conversion.hasConverter(clazz, String.class);
    }

    @Override
    public void doInitialize(WebBeanContext context) {
        conversion = context.getBean(Conversion.class);
    }

}
