package org.jmouse.web.method.converter;

import org.jmouse.core.MediaType;
import org.jmouse.core.convert.Conversion;
import org.jmouse.web.context.WebBeanContext;
import org.jmouse.web.http.request.Headers;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
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
        String        converted  = conversion.convert(data, sourceType, String.class);
        outputMessage.getOutputStream().write(converted.getBytes(StandardCharsets.UTF_8));
    }

    @Override
    protected Object doRead(Class<?> clazz, HttpInputMessage message) throws IOException {
        Headers     headers       = message.getHeaders();
        long        contentLength = headers.getContentLength();
        InputStream stream        = message.getInputStream();
        byte[]      bytes         = contentLength > 0 ? stream.readNBytes((int) contentLength) : stream.readAllBytes();
        return conversion.convert(new String(bytes, getCharset(headers.getContentType())), clazz);
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
    }

    private Charset getCharset(MediaType mediaType) {
        if (mediaType != null) {
            return mediaType.getCharset();
        }

        return StandardCharsets.UTF_8;
    }

}
