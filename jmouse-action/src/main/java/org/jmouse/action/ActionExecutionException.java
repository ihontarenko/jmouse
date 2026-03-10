package org.jmouse.action;

/**
 * Thrown when action execution fails. 💥
 */
public class ActionExecutionException extends ActionException {

    public ActionExecutionException(String message, Throwable cause) {
        super(message, cause);
    }

}
