package org.jmouse.web.method.converter;

import org.jmouse.core.MediaType;
import org.jmouse.web.http.request.Headers;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

public class StringHttpMessageConverter extends AbstractHttpMessageConverter<String> {

    protected StringHttpMessageConverter() {
        super(MediaType.TEXT_PLAIN, MediaType.ALL);
    }

    @Override
    protected void doWrite(String data, Class<?> type, HttpOutputMessage outputMessage) throws IOException {
        outputMessage.getOutputStream().write(data.getBytes(getCharset(outputMessage.getHeaders().getContentType())));
    }

    @Override
    protected String doRead(Class<? extends String> clazz, HttpInputMessage message) throws IOException {
        Headers     headers       = message.getHeaders();
        long        contentLength = headers.getContentLength();
        InputStream stream        = message.getInputStream();
        byte[]      bytes         = contentLength > 0 ? stream.readNBytes((int) contentLength) : stream.readAllBytes();
        return new String(bytes, getCharset(headers.getContentType()));
    }

    @Override
    protected boolean supportsType(Class<?> clazz) {
        return String.class == clazz;
    }

    @Override
    public boolean isWritable(Class<?> clazz, MediaType mediaType) {
        return super.isWritable(clazz, mediaType);
    }

    @Override
    public boolean isReadable(Class<?> clazz, MediaType mediaType) {
        return super.isReadable(clazz, mediaType);
    }

    private Charset getCharset(MediaType mediaType) {
        if (mediaType != null) {
            return mediaType.getCharset();
        }

        return StandardCharsets.UTF_8;
    }

}
