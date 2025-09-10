package org.jmouse.web.mvc.method.converter;

import org.jmouse.core.io.Resource;

import java.io.IOException;

public class ResourceHttpMessageConverter extends AbstractHttpMessageConverter<Resource> {

    @Override
    protected void doWrite(Resource data, Class<?> type, HttpOutputMessage outputMessage) throws IOException {
        System.out.println(data);
    }

    @Override
    protected Resource doRead(Class<? extends Resource> clazz, HttpInputMessage inputMessage) throws IOException {
        return null;
    }

    @Override
    protected boolean supportsType(Class<?> clazz) {
        return false;
    }

}
