package svit.context;

public class ArgumentNotFoundException extends RuntimeException {

    public ArgumentNotFoundException() {
        super();
    }

    public ArgumentNotFoundException(String message) {
        super(message);
    }

    public ArgumentNotFoundException(String message, Throwable cause) {
        super(message, cause);
    }

}
