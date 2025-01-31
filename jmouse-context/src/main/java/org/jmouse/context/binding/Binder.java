package org.jmouse.context.binding;

public class Binder implements ObjectBinder, BindContext {

    private final BindingStrategy strategy;
    private final DataSource      source;
    private final BinderFactory   factory;

    public Binder(DataSource source, BinderFactory factory, BindingStrategy strategy) {
        this.strategy = strategy;
        this.source = source;
        this.factory = factory;

        // register default binders
        factory.registerBinder(new MapBinder(this));
        factory.registerBinder(new ArrayBinder(this));
        factory.registerBinder(new SetBinder(this));
        factory.registerBinder(new ListBinder(this));
        factory.registerBinder(new JavaBeanBinder(this));
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
        return binder.bind(path, bindable, source);
    }

    @Override
    public <T> BindingResult<T> bindValue(NamePath name, Bindable<T> bindable, DataSource source) {
        throw new UnsupportedOperationException(
                "Root binder is not supported this method. This methods can be called only in type-binder specific.");
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
}
