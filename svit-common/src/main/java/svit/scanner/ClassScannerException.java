package svit.scanner;

public class ClassScannerException extends RuntimeException {

    public ClassScannerException(String message, Throwable cause) {
        super(message, cause);
    }

    public ClassScannerException(String message) {
        super(message);
    }

    public ClassScannerException(Throwable cause) {
        super(cause);
    }

}
