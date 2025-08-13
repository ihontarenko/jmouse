package org.jmouse.web.method.converter;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.jmouse.core.MediaType;
import org.jmouse.util.Priority;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

@Priority(Integer.MIN_VALUE + 100000)
public class JacksonYamlHttpMessageConverter extends AbstractJacksonHttpMessageConverter<Object> {

    private final ObjectMapper objectMapper;

    public JacksonYamlHttpMessageConverter() {
        super(MediaType.APPLICATION_YAML);
        this.objectMapper = new ObjectMapper();
    }

    public JacksonYamlHttpMessageConverter(ObjectMapper objectMapper) {
        super(List.of(
                MediaType.APPLICATION_YAML
        ));
        this.objectMapper = objectMapper;
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
        outputMessage.getHeaders().setContentType(MediaType.APPLICATION_YAML);
        ObjectMapper objectMapper =  getObjectMapper(outputMessage.getHeaders().getContentType());
        objectMapper.writeValue(outputMessage.getOutputStream(), data);
    }

    @Override
    public Object doRead(Class<? extends Object> clazz, HttpInputMessage inputMessage) throws IOException {
        return objectMapper.readValue(inputMessage.getInputStream(), clazz);
    }

}
