package org.jmouse.web.mvc.method.converter;

import org.jmouse.core.MediaType;
import org.jmouse.core.Priority;

import java.io.IOException;

@Priority(Integer.MIN_VALUE + 100000)
public class JacksonYamlHttpMessageConverter extends AbstractJacksonHttpMessageConverter<Object> {

    public JacksonYamlHttpMessageConverter() {
        super(MediaType.APPLICATION_YAML);
    }

    @Override
    public void doWrite(Object data, Class<?> type, HttpOutputMessage outputMessage) throws IOException, UnwritableException {
        getObjectMapper(outputMessage).writeValue(outputMessage.getOutputStream(), data);
    }

    @Override
    public Object doRead(Class<? extends Object> clazz, HttpInputMessage inputMessage) throws IOException {
        return getObjectMapper(inputMessage).readValue(inputMessage.getInputStream(), clazz);
    }

}
