package svit.support.invocable;

public class ParameterTypeException extends RuntimeException {

    public ParameterTypeException() {
        super();
    }

    public ParameterTypeException(String message) {
        super(message);
    }

    public ParameterTypeException(String message, Throwable cause) {
        super(message, cause);
    }

    public ParameterTypeException(Throwable cause) {
        super(cause);
    }

}
