package org.jmouse.common.ast.recognizer;

import org.jmouse.common.ast.token.Token;

import java.util.Optional;
import java.util.function.Predicate;
import java.util.function.Supplier;

public class PatternTokenRecognizer extends PredicateSupplierRecognizer<Token, String> {

    private final Token  token;
    private final String regularExpression;

    public PatternTokenRecognizer(String regularExpression, Token token, int order) {
        super(order);
        
        this.regularExpression = regularExpression;
        this.token = token;
    }

    @Override
    public Supplier<Optional<Token>> getSupplier() {
        return () -> Optional.ofNullable(token);
    }

    @Override
    public Predicate<String> getPredicate() {
        return input -> input.matches(regularExpression);
    }

}
