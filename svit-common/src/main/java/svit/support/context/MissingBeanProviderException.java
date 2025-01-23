package svit.support.context;

public class MissingBeanProviderException extends RuntimeException {

    public MissingBeanProviderException() {
        super();
    }

    public MissingBeanProviderException(String message) {
        super(message);
    }

    public MissingBeanProviderException(String message, Throwable cause) {
        super(message, cause);
    }

    public MissingBeanProviderException(Throwable cause) {
        super(cause);
    }

}
