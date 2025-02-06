package svit.expression;

import org.jmouse.common.ast.token.Token;

public enum ExtendedToken implements Token {

    T_ANNOTATION(9100),
    T_CLASS_NAME(9200),
    T_VARIABLE(9300),
    T_PATH_VARIABLE(9700);

    private final int      type;
    private final String[] values;

    ExtendedToken(final int type) {
        this(type, new String[0]);
    }

    ExtendedToken(final int type, final String... values) {
        this.type = type;
        this.values = values;
    }

    @Override
    public int type() {
        return type;
    }

    @Override
    public String[] examples() {
        return values;
    }

    @Override
    public ExtendedToken[] tokens() {
        return values();
    }

}
