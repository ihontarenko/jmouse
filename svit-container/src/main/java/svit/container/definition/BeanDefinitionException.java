package svit.container.definition;

public class BeanDefinitionException extends RuntimeException {

    public BeanDefinitionException() {
        super();
    }

    public BeanDefinitionException(String message) {
        super(message);
    }

    public BeanDefinitionException(String message, Throwable cause) {
        super(message, cause);
    }

    public BeanDefinitionException(Throwable cause) {
        super(cause);
    }

}
