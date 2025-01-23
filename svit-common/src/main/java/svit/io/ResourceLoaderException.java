package svit.io;

public class ResourceLoaderException extends RuntimeException {

    public ResourceLoaderException(String message) {
        super(message);
    }

    public ResourceLoaderException(String message, Throwable cause) {
        super(message, cause);
    }

}
