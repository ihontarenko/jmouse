package org.jmouse.web.method.converter;

import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import org.jmouse.core.MediaType;
import org.jmouse.core.Priority;

import java.io.IOException;
import java.util.List;

@Priority(Integer.MIN_VALUE + 200)
public class JacksonXmlHttpMessageConverter extends AbstractJacksonHttpMessageConverter<Object> {

    public JacksonXmlHttpMessageConverter() {
        super(MediaType.APPLICATION_XML);
    }

    @Override
    public void doWrite(Object data, Class<?> type, HttpOutputMessage outputMessage) throws IOException {
        outputMessage.getHeaders().setContentType(MediaType.APPLICATION_XML);
        getObjectMapper(outputMessage.getHeaders().getContentType()).writeValue(outputMessage.getOutputStream(), data);
    }

    @Override
    public Object doRead(Class<?> clazz, HttpInputMessage inputMessage) throws IOException {
        return getObjectMapper(inputMessage.getHeaders().getContentType()).readValue(inputMessage.getInputStream(), clazz);
    }

}