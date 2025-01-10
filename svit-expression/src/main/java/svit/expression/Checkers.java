package svit.expression;

import svit.ast.lexer.Lexer;
import svit.ast.token.DefaultToken;

final public class Checkers {

    public static final Lexer.Checker VARIABLE = lexer
            -> lexer.isNext(ExtendedToken.T_VARIABLE);
    public static final Lexer.Checker VARIABLE_DEFINITION = lexer
            -> lexer.sequence(ExtendedToken.T_VARIABLE, DefaultToken.T_DIVIDE, DefaultToken.T_IDENTIFIER);
    public static final Lexer.Checker STATIC_METHOD = lexer
            -> lexer.sequence(ExtendedToken.T_VARIABLE, DefaultToken.T_OPEN_BRACE);
    public static final Lexer.Checker OBJECT_METHOD = lexer
            -> lexer.sequence(ExtendedToken.T_VARIABLE, DefaultToken.T_DOT, DefaultToken.T_IDENTIFIER, DefaultToken.T_OPEN_BRACE);
    public static final Lexer.Checker PARAMETERS = lexer
            -> lexer.sequence(DefaultToken.T_OPEN_BRACE, DefaultToken.T_IDENTIFIER, DefaultToken.T_EQ);

}
