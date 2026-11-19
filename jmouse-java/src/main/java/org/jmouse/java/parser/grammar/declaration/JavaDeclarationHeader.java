package org.jmouse.java.parser.grammar.declaration;

import java.util.Objects;
import org.jmouse.java.parser.grammar.JavaParsingInput;
import org.jmouse.java.parser.node.name.JavaModifiersNode;

/** A consumed modifier prefix passed to one selected declaration rule. */
public record JavaDeclarationHeader(JavaParsingInput input, JavaModifiersNode modifiers, int start) {
    public JavaDeclarationHeader {
        Objects.requireNonNull(input, "input");
        Objects.requireNonNull(modifiers, "modifiers");
    }
}
