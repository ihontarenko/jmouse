package org.jmouse.web.mvc.method.converter;

import org.jmouse.core.MediaType;
import org.jmouse.core.convert.Conversion;
import org.jmouse.web.context.WebBeanContext;
import org.jmouse.web.http.request.Headers;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

public class ObjectToStringHttpMessageConverter extends AbstractHttpMessageConverter<Object> {

    private Conversion                   conversion;
    private HttpMessageConverter<String> messageConverter;

    protected ObjectToStringHttpMessageConverter() {
        super(MediaType.TEXT_PLAIN);
    }

    @Override
    @SuppressWarnings("unchecked")
    protected void doWrite(Object data, Class<?> type, HttpOutputMessage message) throws IOException {
        Class<Object> sourceType = (Class<Object>) type;
        String        converted  = conversion.convert(data, sourceType, String.class);
        messageConverter.write(converted, type, message);
    }

    @Override
    protected Object doRead(Class<?> clazz, HttpInputMessage message) throws IOException {
        return conversion.convert(messageConverter.read(String.class, message), clazz);
    }

    @Override
    protected boolean supportsType(Class<?> clazz) {
        return true;
    }

    @Override
    public boolean isWritable(Class<?> clazz, MediaType mediaType) {
        return super.isWritable(clazz, mediaType) && conversion.hasConverter(clazz, String.class);
    }

    @Override
    public boolean isReadable(Class<?> clazz, MediaType mediaType) {
        return super.isReadable(clazz, mediaType) && conversion.hasConverter(String.class, clazz);
    }

    @Override
    public void doInitialize(WebBeanContext context) {
        conversion = context.getBean(Conversion.class);
        messageConverter = context.getBean(StringHttpMessageConverter.class);
    }

}
