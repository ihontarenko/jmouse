package svit.beans;

public class BeanNameException extends RuntimeException {

    public BeanNameException() {
        super();
    }

    public BeanNameException(String message) {
        super(message);
    }

    public BeanNameException(String message, Throwable cause) {
        super(message, cause);
    }

    public BeanNameException(Throwable cause) {
        super(cause);
    }

}
