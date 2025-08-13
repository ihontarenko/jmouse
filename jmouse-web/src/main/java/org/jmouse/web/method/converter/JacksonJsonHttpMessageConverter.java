package org.jmouse.web.method.converter;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.jmouse.core.MediaType;
import org.jmouse.util.Priority;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

@Priority(Integer.MIN_VALUE + 100)
public class JacksonJsonHttpMessageConverter extends AbstractJacksonHttpMessageConverter<Object> {

    public JacksonJsonHttpMessageConverter() {
        super(MediaType.APPLICATION_JSON);
    }

    public JacksonJsonHttpMessageConverter(ObjectMapper objectMapper) {
        super(List.of(
                MediaType.APPLICATION_JSON
        ));
    }

    @Override
    public boolean isWritable(Class<?> clazz, MediaType mediaType) {
        boolean isWritable = super.isWritable(clazz, mediaType);

        if (!isWritable) {
            return false;
        }

        ObjectMapper objectMapper = getObjectMapper(mediaType);

        if (objectMapper == null) {
            return false;
        }

        AtomicReference<Throwable> atomicReference = new AtomicReference<>();
        if (!objectMapper.canSerialize(clazz, atomicReference)) {
            return false;
        }

        return isWritable;
    }

    @Override
    public void doWrite(Object data, Class<?> type, HttpOutputMessage outputMessage) throws IOException, UnwritableException {
        outputMessage.getHeaders().setContentType(MediaType.APPLICATION_JSON);
        ObjectMapper objectMapper =  getObjectMapper(outputMessage.getHeaders().getContentType());
        objectMapper.writeValue(outputMessage.getOutputStream(), data);
    }

    @Override
    public Object doRead(Class<? extends Object> clazz, HttpInputMessage inputMessage) throws IOException {
        ObjectMapper objectMapper =  getObjectMapper(inputMessage.getHeaders().getContentType());
        return objectMapper.readValue(inputMessage.getInputStream(), clazz);
    }

}
