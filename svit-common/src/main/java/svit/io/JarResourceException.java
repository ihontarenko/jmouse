package svit.io;

public class JarResourceException extends ResourceException {

    public JarResourceException(Resource resource, Throwable cause) {
        super(resource, cause);
    }

    public JarResourceException(Resource resource) {
        super(resource);
    }

    public JarResourceException(String message, Throwable cause) {
        super(message, cause);
    }

    public JarResourceException(String message) {
        super(message);
    }

}
