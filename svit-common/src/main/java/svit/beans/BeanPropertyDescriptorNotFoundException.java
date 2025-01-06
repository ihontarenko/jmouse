package svit.beans;

public class BeanPropertyDescriptorNotFoundException extends RuntimeException {

    public BeanPropertyDescriptorNotFoundException() {
        super();
    }

    public BeanPropertyDescriptorNotFoundException(String message) {
        super(message);
    }

    public BeanPropertyDescriptorNotFoundException(String message, Throwable cause) {
        super(message, cause);
    }

    public BeanPropertyDescriptorNotFoundException(Throwable cause) {
        super(cause);
    }

}
