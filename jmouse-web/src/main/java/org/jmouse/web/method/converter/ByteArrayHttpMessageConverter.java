package org.jmouse.web.method.converter;

import jakarta.servlet.http.HttpServletResponse;
import org.jmouse.core.MediaType;
import org.jmouse.core.Priority;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;

@Priority(Integer.MIN_VALUE)
public class ByteArrayHttpMessageConverter extends AbstractHttpMessageConverter<byte[]> {

    protected ByteArrayHttpMessageConverter() {
        super(List.of(MediaType.APPLICATION_OCTET_STREAM, MediaType.ALL));
    }

    @Override
    protected void doWrite(byte[] data, Class<?> type, HttpOutputMessage outputMessage) throws IOException {
        outputMessage.getHeaders().setContentLength(data.length);
        outputMessage.getOutputStream().write(data);

        if (outputMessage instanceof HttpServletResponse response && !response.isCommitted()) {
            response.flushBuffer();
        }
    }

    @Override
    protected byte[] doRead(Class<? extends byte[]> clazz, HttpInputMessage inputMessage) throws IOException {
        long        length      = inputMessage.getHeaders().getContentLength();
        InputStream inputStream = inputMessage.getInputStream();

        if (length >= 0 && length < Integer.MAX_VALUE) {
            return inputStream.readNBytes((int) length);
        }

        return inputStream.readAllBytes();
    }

    @Override
    protected boolean supportsType(Class<?> clazz) {
        return byte[].class == clazz;
    }

}
