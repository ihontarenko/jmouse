package svit.scanner.filter.type;

public class IsInterfaceClassFilter extends AbstractTypeFilter {

    public IsInterfaceClassFilter() {
        this(false);
    }

    public IsInterfaceClassFilter(boolean invert) {
        super(invert);
    }

    @Override
    public boolean accept(Class<?> clazz) {
        return clazz.isInterface() != invert();
    }

}