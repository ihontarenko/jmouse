package svit.support.invocable;

public class AmbiguousDescriptorException extends RuntimeException {

    public AmbiguousDescriptorException() {
        super();
    }

    public AmbiguousDescriptorException(String message) {
        super(message);
    }

    public AmbiguousDescriptorException(String message, Throwable cause) {
        super(message, cause);
    }

    public AmbiguousDescriptorException(Throwable cause) {
        super(cause);
    }

}
