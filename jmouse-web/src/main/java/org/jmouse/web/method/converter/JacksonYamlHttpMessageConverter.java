package org.jmouse.web.method.converter;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.jmouse.core.MediaType;
import org.jmouse.core.Priority;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

@Priority(Integer.MIN_VALUE + 100000)
public class JacksonYamlHttpMessageConverter extends AbstractJacksonHttpMessageConverter<Object> {

    public JacksonYamlHttpMessageConverter() {
        super(MediaType.APPLICATION_YAML);
    }

    @Override
    public void doWrite(Object data, Class<?> type, HttpOutputMessage outputMessage) throws IOException, UnwritableException {
        outputMessage.getHeaders().setContentType(MediaType.APPLICATION_YAML);
        ObjectMapper objectMapper =  getObjectMapper(outputMessage.getHeaders().getContentType());
        objectMapper.writeValue(outputMessage.getOutputStream(), data);
    }

    @Override
    public Object doRead(Class<? extends Object> clazz, HttpInputMessage inputMessage) throws IOException {
        return getObjectMapper(inputMessage.getHeaders().getContentType()).readValue(inputMessage.getInputStream(), clazz);
    }

}
