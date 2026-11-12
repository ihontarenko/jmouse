package org.jmouse.web.http.converter;

import java.io.IOException;
import org.jmouse.core.MediaType;
import org.jmouse.core.Priority;

@Priority(Integer.MIN_VALUE + 100)
public class JacksonJsonHttpMessageConverter extends AbstractJacksonHttpMessageConverter<Object> {

    public JacksonJsonHttpMessageConverter() {
        super(MediaType.APPLICATION_JSON);
    }

    @Override
    public void doWrite(Object data, Class<?> type, HttpOutputMessage outputMessage)
        throws IOException, UnwritableException {
        getObjectMapper(outputMessage).writeValue(outputMessage.getOutputStream(), data);
    }

    @Override
    public Object doRead(Class<?> clazz, HttpInputMessage inputMessage) throws IOException {
        return getObjectMapper(inputMessage)
            .readerFor(clazz)
            .with(tools.jackson.databind.DeserializationFeature.FAIL_ON_TRAILING_TOKENS)
            .readValue(inputMessage.getInputStream());
    }
}
