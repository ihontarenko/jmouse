package org.jmouse.context.binding;

import org.jmouse.core.convert.Conversion;
import svit.beans.CyclicReferenceDetector;
import svit.beans.DefaultCyclicReferenceDetector;

import java.util.function.Supplier;

public class Binder implements ObjectBinder, BindContext {

    private static final Supplier<? extends RuntimeException> exceptionSupplier = () -> new RuntimeException("Recursive binding detected");
    private final BindingStrategy                 strategy;
    private final DataSource                      source;
    private final BinderFactory                   factory;
    private final Conversion                      conversion;
    private final CyclicReferenceDetector<String> detector;

    public Binder(DataSource source, BinderFactory factory, BindingStrategy strategy) {
        this.strategy = strategy;
        this.source = source;
        this.factory = factory;
        this.detector = new DefaultCyclicReferenceDetector<>();
        this.conversion = new BinderConversion();

        // register default binders
        factory.registerBinder(new MapBinder(this));
        factory.registerBinder(new ArrayBinder(this));
        factory.registerBinder(new SetBinder(this));
        factory.registerBinder(new ListBinder(this));
        factory.registerBinder(new JavaBeanBinder(this));
    }

    public static Binder with(Object source) {
        return new Binder(DataSource.of(source));
    }

    public Binder(DataSource source) {
        this(source, new DefaultBinderFactory(), BindingStrategy.DEEP);
    }

    public <T> BindingResult<T> bind(String path, Bindable<T> bindable) {
        return bind(NamePath.of(path), bindable, source);
    }

    @Override
    public <T> BindingResult<T> bind(NamePath path, Bindable<T> bindable, DataSource source) {
        ObjectBinder binder = factory.getBinderFor(bindable);

        detector.detect(path::path, exceptionSupplier);
        BindingResult<T> result = binder.bind(path, bindable, source);
        detector.remove(path::path);

        return result;
    }

    @Override
    public <T> BindingResult<T> bindValue(NamePath name, Bindable<T> bindable, DataSource source) {
        throw new UnsupportedOperationException("Root binder is not supported this method. This methods can be called only in type-binder specific.");
    }

    @Override
    public DataSource getDataSource() {
        return source;
    }

    @Override
    public ObjectBinder getRootBinder() {
        return this;
    }

    @Override
    public boolean isDeepBinding() {
        return BindingStrategy.DEEP.equals(strategy);
    }

    @Override
    public boolean isShallowBinding() {
        return BindingStrategy.SHALLOW.equals(strategy);
    }

    @Override
    public Conversion getConversion() {
        return conversion;
    }
}
