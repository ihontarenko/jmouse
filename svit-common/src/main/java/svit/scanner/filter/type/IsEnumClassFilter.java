package svit.scanner.filter.type;

public class IsEnumClassFilter extends AbstractTypeFilter {

    public IsEnumClassFilter() {
        this(false);
    }

    public IsEnumClassFilter(boolean invert) {
        super(invert);
    }

    @Override
    public boolean accept(Class<?> clazz) {
        return clazz.isEnum() != invert();
    }

}