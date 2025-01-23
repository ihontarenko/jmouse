package svit.support.context;

public class DefaultContextException extends RuntimeException {

    public DefaultContextException() {
    }

    public DefaultContextException(String message) {
        super(message);
    }

    public DefaultContextException(String message, Throwable cause) {
        super(message, cause);
    }

    public DefaultContextException(Throwable cause) {
        super(cause);
    }

}
