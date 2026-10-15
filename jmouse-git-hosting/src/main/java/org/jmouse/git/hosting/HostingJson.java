package org.jmouse.git.hosting;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/** Checked decoding of a hosting's JSON; a refusal never carries any of the content it refused. */
final class HostingJson {

    private HostingJson() {
    }

    static Map<?, ?> object(Object value) {
        if (!(value instanceof Map<?, ?> object)) {
            throw invalidResponse();
        }
        return object;
    }

    static List<?> array(Object value) {
        if (!(value instanceof List<?> array)) {
            throw invalidResponse();
        }
        return array;
    }

    /** A required, nonblank string field. */
    static String text(Map<?, ?> object, String field) {
        String value = optionalText(object, field);
        if (value == null || value.isBlank()) {
            throw invalidResponse();
        }
        return value;
    }

    /** A string field that may be absent or null. */
    static String optionalText(Map<?, ?> object, String field) {
        Object value = object.get(field);
        if (value == null) {
            return null;
        }
        if (!(value instanceof String text)) {
            throw invalidResponse();
        }
        return text;
    }

    /** A boolean field that may be absent or null. */
    static Boolean optionalBoolean(Map<?, ?> object, String field) {
        Object value = object.get(field);
        if (value == null) {
            return null;
        }
        if (!(value instanceof Boolean flag)) {
            throw invalidResponse();
        }
        return flag;
    }

    /** An array of strings that may be absent or null, as an empty list. */
    static List<String> optionalTexts(Map<?, ?> object, String field) {
        Object value = object.get(field);
        if (value == null) {
            return List.of();
        }
        List<String> texts = new ArrayList<>();
        for (Object item : array(value)) {
            if (!(item instanceof String text)) {
                throw invalidResponse();
            }
            texts.add(text);
        }
        return texts;
    }

    static HostingException invalidResponse() {
        return new HostingException(HostingException.Reason.INVALID_RESPONSE);
    }
}
