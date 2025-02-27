package org.jmouse.template.lexer.support;

import org.jmouse.template.lexer.Token;

import java.util.Optional;

public class EnumFinder<T extends Enum<T> & Token.Type> implements Finder<T, String, T[]> {

    @Override
    public Optional<T> find(String value, T[] source) {
        Optional<T> token = Optional.empty();

        for (T current : source) {
            for (String template : current.getTokenTemplates()) {
                if (template.equalsIgnoreCase(value)) {
                    token = Optional.of(current);
                    break;
                }
            }
        }

        return token;
    }

}
