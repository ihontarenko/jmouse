package org.jmouse.json;

/**
 * 🧾 JSON that is JSON but cannot be read into the type asked for, or a value that cannot be written: said with the type and
 * why.
 *
 * @author Ivan Hontarenko (Mr. Jerry Mouse)
 * @author ihontarenko@gmail.com
 */
public final class JsonMappingException extends RuntimeException {

    public JsonMappingException(String message, Throwable cause) {
        super(message, cause);
    }

    public JsonMappingException(String message) {
        super(message);
    }
}
