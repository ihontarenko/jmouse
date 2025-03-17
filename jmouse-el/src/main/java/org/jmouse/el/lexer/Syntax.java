package org.jmouse.el.lexer;

public class Syntax {

    public static final String STRINGS           = "\"[^\"]*\"|'[^']*'";
    public static final String NUMBER            = "[.]?\\d+[.]?\\d*";
    public static final String UNSIGNED_INTEGER  = "\\d+";
    public static final String SIGNED_INTEGER    = "[+-]?\\d+";
    public static final String FRACTIONAL_NUMBER = "\\d+\\.\\d*|[+-]?\\d+(\\.\\d+)?([Ee][+-]?\\d+)?";
    public static final String IDENTIFIER        = "\\w+";
    public static final String LETTERS           = "[a-zA-Z]+";
    public static final String SEMICOLON_COMMENT = ";[^\n]+|\\([\\w\\s]+\\)";
    public static final String NEW_LINE          = "\n+";
    public static final String SPECIAL_SYMBOLS   = "\\S+?";
    public static final String LOGICAL_OPERATOR  = "&{2}|\\|{2}|[><!=]+=?";

}
