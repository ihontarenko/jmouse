package org.jmouse.template.lexer.support;
import org.jmouse.template.lexer.Token;

import java.util.Optional;

public class EnumFinder<T extends Enum<T> & Token> implements Finder<T, String, T[]> {

    @Override
    public Optional<T> find(String value, T[] source) {
        Optional<T> token = Optional.empty();

        for (T current : source) {
            for (String example : current.examples()) {
                if (example.equalsIgnoreCase(value)) {
                    token = Optional.of(current);
                    break;
                }
            }
        }

        return token;
    }

}
