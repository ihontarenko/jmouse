package org.jmouse.java.analysis;

import org.jmouse.java.model.symbol.JavaSymbolKey;
import org.jmouse.java.model.type.JavaType;

import java.util.Objects;

public record JavaCapturedSymbol(JavaSymbolKey symbol, JavaType type) implements JavaCapturedValue {
    public JavaCapturedSymbol {
        Objects.requireNonNull(symbol, "symbol");
        Objects.requireNonNull(type, "type");
    }
}
