package svit.scanner.filter.type;

public class LambdaClassFilter extends ClassAnnotatedClassFilter {

    public LambdaClassFilter(boolean invert) {
        super(FunctionalInterface.class, invert);
    }

    public LambdaClassFilter() {
        this(false);
    }

}
