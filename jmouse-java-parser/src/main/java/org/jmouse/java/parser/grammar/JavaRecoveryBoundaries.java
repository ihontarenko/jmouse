package org.jmouse.java.parser.grammar;

import java.util.Set;
import org.jmouse.java.parser.lexer.JavaKeyword;
import org.jmouse.java.parser.lexer.JavaToken;

/** Standard Java production boundaries, independent from any concrete AST node. */
public final class JavaRecoveryBoundaries {
    public static final JavaRecoveryBoundary TOP_LEVEL = new JavaRecoveryBoundary(
            Set.of(JavaToken.SEMICOLON, JavaToken.RIGHT_BRACE),
            Set.of(JavaToken.SEMICOLON, JavaToken.RIGHT_BRACE), JavaRecoveryBlockExit.STOP);
    public static final JavaRecoveryBoundary MEMBER = new JavaRecoveryBoundary(
            Set.of(JavaToken.SEMICOLON, JavaToken.RIGHT_BRACE),
            Set.of(JavaToken.SEMICOLON), JavaRecoveryBlockExit.STOP);
    public static final JavaRecoveryBoundary STATEMENT = MEMBER;
    public static final JavaRecoveryBoundary ENUM_CONSTANT = new JavaRecoveryBoundary(
            Set.of(JavaToken.COMMA, JavaToken.SEMICOLON, JavaToken.RIGHT_BRACE),
            Set.of(), JavaRecoveryBlockExit.CONTINUE);
    public static final JavaRecoveryBoundary SWITCH_ENTRY = new JavaRecoveryBoundary(
            Set.of(JavaToken.SEMICOLON, JavaToken.RIGHT_BRACE), Set.of(JavaToken.SEMICOLON),
            Set.of(JavaKeyword.CASE.spelling(), JavaKeyword.DEFAULT.spelling()), JavaRecoveryBlockExit.STOP);

    private JavaRecoveryBoundaries() {
    }
}
