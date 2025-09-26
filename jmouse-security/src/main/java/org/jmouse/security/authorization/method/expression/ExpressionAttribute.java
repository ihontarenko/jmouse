package org.jmouse.security.authorization.method.expression;

public interface ExpressionAttribute {

    ExpressionAttribute NULL_ATTRIBUTE = () -> null;

    String expression();



}