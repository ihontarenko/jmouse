package org.jmouse.java.parser.node.expression;



/** The decoded literal retains Java's declared floating-point precision. */
public sealed interface JavaFloatingLiteralValue {

    record Binary32(float value) implements JavaFloatingLiteralValue {
    }

    record Binary64(double value) implements JavaFloatingLiteralValue {
    }
}
