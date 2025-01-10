package svit.ast.recognizer;

import java.util.Optional;
import java.util.function.Predicate;
import java.util.function.Supplier;

abstract public class PredicateSupplierRecognizer<R, T> extends AbstractRecognizer<R, T> {

    public PredicateSupplierRecognizer(int priority) {
        super(priority);
    }

    abstract public Supplier<Optional<R>> getSupplier();
    abstract public Predicate<T> getPredicate();

    @Override
    public Optional<R> recognize(T subject) {
        return getPredicate().test(subject) ? getSupplier().get() : Optional.empty();
    }

}
