package svit.scanner.filter.type;

import df.common.container.ClassUtils;

public class NameContainsClassFilter extends AbstractTypeFilter {

    private final String name;

    public NameContainsClassFilter(String name, boolean invert) {
        super(invert);
        this.name = name;
    }

    public NameContainsClassFilter(String name) {
        this(name, false);
    }

    @Override
    public boolean accept(Class<?> klass) {
        return invert() != getShortName(klass).contains(name);
    }

    private String getShortName(Class<?> klass) {
        return ClassUtils.getShortName(klass);
    }

}
