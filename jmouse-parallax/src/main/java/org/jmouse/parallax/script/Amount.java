package org.jmouse.parallax.script;

import org.jmouse.units.Quantity;

import java.util.Optional;

/**
 * A value as written: a literal with its unit ({@code 6.86 kHz}), or an expression a compiler evaluates against the
 * document's parameters ({@code R * C}).
 *
 * @param expression the text as written, without a literal's unit
 * @param quantity   the literal in SI with its unit; empty for an expression
 */
public record Amount(SourceText expression, Optional<Quantity> quantity) {

    public boolean isLiteral() {
        return quantity.isPresent();
    }
}
