package org.jmouse.core.io.reader;

import org.jmouse.core.io.Resource;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Properties;
import java.util.regex.Pattern;

public class PropertiesResourceReader implements PropertyResourceReader {

    private static final Pattern EXTENSION = Pattern.compile("(?i)\\.properties$");

    @Override
    public boolean supports(Resource resource) {
        return EXTENSION.matcher(resource.getName()).matches();
    }

    @Override
    public Map<String, Object> read(Resource resource) throws IOException {
        Properties          properties = new Properties();
        Map<String, Object> result     = new LinkedHashMap<>();

        properties.load(resource.getInputStream());
        properties.forEach((key, value) -> result.put(key.toString(), value.toString()));

        return result;
    }

}
