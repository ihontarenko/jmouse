package svit.ast;

import java.util.function.Predicate;

@SuppressWarnings({"unused"})
public enum Regexps {

    R_QUOTED_STRING_1("\"[^\"]*\""),
    R_QUOTED_STRING_2("'[^']*'"),
    R_LOGICAL_OPERATOR_1("&{2}|\\|{2}"),
    R_LOGICAL_OPERATOR_2("[><!=]+=?"),
    R_NUMBER("[.]?\\d+[.]?\\d*"),
    R_UINT("\\d+"),
    R_INT("[+-]?\\d+"),
    R_FLOAT_1("\\d*\\.\\d*"),
    R_FLOAT_2("[+-]?\\d+(\\.\\d+)?([Ee][+-]?\\d+)?"),
    R_IDENTIFIER("\\w+"),
    R_ALPHA("[a-zA-Z]+"),
    R_SEMICOLON_COMMENT(";[^\n]+|\\([\\w\\s]+\\)"),
    R_NEW_LINE("\n+"),
    R_SPECIAL_SYMBOLS("\\S+?");

    private final String regularExpression;

    Regexps(String regularExpression) {
        this.regularExpression = regularExpression;
    }

    public Predicate<String> predicate() {
        return input -> input.matches(this.regularExpression);
    }

    public String regularExpression() {
        return this.regularExpression;
    }

    @Override
    public String toString() {
        return regularExpression;
    }

}
