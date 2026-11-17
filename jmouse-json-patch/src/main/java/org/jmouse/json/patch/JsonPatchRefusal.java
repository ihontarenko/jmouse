package org.jmouse.json.patch;

/** ⛔ A patch that cannot be applied to the document it was sent for, and why: the document is not the one the sender had. */
public final class JsonPatchRefusal extends RuntimeException {

    public JsonPatchRefusal(String message) {
        super(message);
    }

    public JsonPatchRefusal(String message, Throwable cause) {
        super(message, cause);
    }
}
