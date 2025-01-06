package svit.scanner.filter.type;

public class IsAnnotationClassFilter extends AbstractTypeFilter {

    public IsAnnotationClassFilter() {
        this(false);
    }

    public IsAnnotationClassFilter(boolean invert) {
        super(invert);
    }

    @Override
    public boolean accept(Class<?> clazz) {
        return clazz.isAnnotation() != invert();
    }

}