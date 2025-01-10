package svit.invocable;

public class ParameterCountException extends RuntimeException {

    public ParameterCountException() {
        super();
    }

    public ParameterCountException(String message) {
        super(message);
    }

    public ParameterCountException(String message, Throwable cause) {
        super(message, cause);
    }

    public ParameterCountException(Throwable cause) {
        super(cause);
    }

}
