package org.jmouse.template.loader;

import java.io.Reader;
import java.io.StringReader;

public class StringLoader implements TemplateLoader<String> {

    @Override
    public Reader load(String content) {
        return new StringReader(content);
    }

}
