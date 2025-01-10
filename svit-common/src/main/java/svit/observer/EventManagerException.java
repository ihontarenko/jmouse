package svit.observer;

public class EventManagerException extends RuntimeException {

    public EventManagerException() {
    }

    public EventManagerException(String message) {
        super(message);
    }

    public EventManagerException(String message, Throwable cause) {
        super(message, cause);
    }

    public EventManagerException(Throwable cause) {
        super(cause);
    }

}
