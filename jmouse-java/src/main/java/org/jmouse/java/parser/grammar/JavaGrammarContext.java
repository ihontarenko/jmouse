package org.jmouse.java.parser.grammar;

import org.jmouse.core.context.ContextKey;
import org.jmouse.java.parser.grammar.declaration.JavaEnclosingType;

/** Typed contextual grammar state; ParseSession restores nested bindings on every exit. */
public final class JavaGrammarContext {
    public enum PrimaryForm {
        STANDARD,
        CONSTRUCTOR_QUALIFIER
    }

    public static final ContextKey<PrimaryForm> PRIMARY_FORM =
            ContextKey.of(JavaParseContextNames.PRIMARY_FORM, PrimaryForm.class);

    public enum ParameterForm {
        FORMAL,
        LAMBDA
    }

    public static final ContextKey<ParameterForm> PARAMETER_FORM =
            ContextKey.of(JavaParseContextNames.PARAMETER_FORM, ParameterForm.class);

    public enum ExpressionForm {
        VALUE,
        CONDITIONAL
    }

    public static final ContextKey<ExpressionForm> EXPRESSION_FORM =
            ContextKey.of(JavaParseContextNames.EXPRESSION_FORM, ExpressionForm.class);
    public static final ContextKey<JavaEnclosingType> ENCLOSING_TYPE =
            ContextKey.of(JavaParseContextNames.ENCLOSING_TYPE, JavaEnclosingType.class);

    public enum SwitchForm {
        STATEMENT,
        EXPRESSION
    }

    public static final ContextKey<SwitchForm> SWITCH_FORM =
            ContextKey.of(JavaParseContextNames.SWITCH_FORM, SwitchForm.class);

    private JavaGrammarContext() {}
}
