package org.jmouse.core.bind;

import org.jmouse.core.convert.Conversion;
import org.jmouse.core.CyclicReferenceDetector;
import org.jmouse.core.DefaultCyclicReferenceDetector;

import java.util.function.Supplier;

/**
 * The {@code Binder} class is responsible for managing the binding process, allowing for flexible object binding strategies.
 * It handles recursive reference detection and provides various binding methods for different data types.
 * <p>
 * This class supports deep and shallow binding strategies and utilizes a factory to register and retrieve binders for different types.
 * It also ensures that cyclic references are detected during the binding process to prevent infinite loops.
 * </p>
 */
public class Binder implements ObjectBinder, BindContext {

    private static final ObjectAccessorWrapper                DEFAULT_WRAPPER    = new StandardAccessorWrapper();
    private static final Supplier<? extends RuntimeException> EXCEPTION_SUPPLIER = () -> new BindException(
            "Recursive binding detected");

    private final ObjectAccessor                  source;
    private final BinderFactory                   factory;
    private final Conversion                      conversion;
    private final CyclicReferenceDetector<String> detector;
    private       BindCallback                    callback;
    private       BindingStrategy                 strategy;
    private       ObjectAccessorWrapper           wrapper;

    /**
     * Constructs a {@code Binder} with the specified data source, binder factory, and binding strategy.
     * Registers the default binders (MapBinder, ArrayBinder, SetBinder, ListBinder, and JavaBeanBinder).
     *
     * @param source the data source for binding
     * @param factory the factory for creating binders
     * @param strategy the binding strategy (deep or shallow)
     */
    public Binder(ObjectAccessor source, BinderFactory factory, BindingStrategy strategy) {
        this.callback = new DefaultBindingCallback();
        this.strategy = strategy;
        this.source = source;
        this.factory = factory;
        this.detector = new DefaultCyclicReferenceDetector<>();
        this.conversion = new BinderConversion();
        this.wrapper = new StandardAccessorWrapper();

        // register default binders
        factory.registerBinder(new ScalarValueBinder(this));
        factory.registerBinder(new MapBinder(this));
        factory.registerBinder(new ArrayBinder(this));
        factory.registerBinder(new SetBinder(this));
        factory.registerBinder(new ListBinder(this));
        factory.registerBinder(new ValueObjectBinder(this));
        factory.registerBinder(new JavaBeanBinder(this));
    }

    /**
     * Creates a new {@code Binder} instance using the given source object.
     *
     * @param source the source object for binding
     * @param callback the binding callback for customization
     *
     * @return a new {@code Binder} instance
     */
    public static Binder with(Object source, BindCallback callback) {
        Binder binder = new Binder(DEFAULT_WRAPPER.wrap(source));
        binder.setCallback(callback);
        return binder;
    }

    /**
     * Creates a new {@code Binder} instance using the given source object.
     *
     * @param source the source object for binding
     * @return a new {@code Binder} instance
     */
    public static Binder forObject(Object source) {
        return new Binder(DEFAULT_WRAPPER.wrap(source));
    }

    /**
     * Constructs a {@code Binder} using the specified data source and default binder factory.
     *
     * @param source the data source for binding
     */
    public Binder(ObjectAccessor source) {
        this(source, new DefaultBinderFactory(), BindingStrategy.DEEP);
    }

    /**
     * Binds the given {@link Bindable} object with the source data using the default path (null).
     *
     * @param <T> the type of the object to bind
     * @param bindable the object to bind
     * @return the result of the binding
     */
    public <T> BindResult<T> bind(Bindable<T> bindable) {
        return bind(null, bindable);
    }

    /**
     * Binds the given {@link Bindable} object with the source data at the specified path.
     *
     * @param <T> the type of the object to bind
     * @param path the path at which to bind the object
     * @param bindable the object to bind
     * @return the result of the binding
     */
    public <T> BindResult<T> bind(String path, Bindable<T> bindable) {
        return bind(PropertyPath.forPath(path), bindable, source, null);
    }

    /**
     * Binds the given {@link Bindable} object with the source data at the specified path and source.
     *
     * @param <T> the type of the object to bind
     * @param path the path at which to bind the object
     * @param bindable the object to bind
     * @param accessor the data source
     * @param callback the binding callback for customization
     * @return the result of the binding
     */
    @Override
    public <T> BindResult<T> bind(PropertyPath path, Bindable<T> bindable, ObjectAccessor accessor, BindCallback callback) {
        try {
            ObjectBinder binder     = factory.getBinderFor(bindable);
            BindCallback customizer = callback == null ? this.callback : callback;

            // Detect recursive binding before proceeding
            detector.detect(path::path, EXCEPTION_SUPPLIER);

            return binder.bind(path, bindable, accessor, customizer);
        } catch (Exception exception) {
            return BindResult.of(doException(path, bindable, exception));
        } finally {
            // Remove the path after binding to prevent future recursion detection for the same path
            detector.remove(path::path);
        }
    }

    /**
     * Handles exceptions that occur during the binding process.
     * <p>
     * This method invokes the {@code onFailure} callback and, if it results in another exception,
     * wraps it into a {@link BindException}. If the callback returns a value, it is cast and returned.
     * </p>
     *
     * @param path      the property path where the exception occurred
     * @param bindable  the bindable target type
     * @param exception the original exception that occurred
     * @param <T>       the expected result type
     * @return the fallback value returned by the callback, if any
     * @throws BindException if the callback throws an exception that is not already a {@link BindException}
     */
    @SuppressWarnings({"unchecked"})
    private <T> T doException(PropertyPath path, Bindable<T> bindable, Exception exception) {
        try {
            return (T) callback.onFailure(path, bindable, this, exception);
        } catch (Exception cause) {
            BindException bindException;

            if (cause instanceof BindException) {
                bindException = (BindException) cause;
            } else {
                bindException = new BindException(path, bindable, cause);
            }

            throw bindException;
        }
    }

    /**
     * This method is not supported for the root binder and is meant to be used in type-specific binders.
     *
     * @throws UnsupportedOperationException if called on the root binder
     */
    @Override
    public <T> BindResult<T> bindValue(PropertyPath name, Bindable<T> bindable, ObjectAccessor accessor, BindCallback callback) {
        throw new UnsupportedOperationException(
                "Root binder is not supported this method. This methods can be called only in type-binder specific.");
    }

    /**
     * Returns the data source associated with this binder.
     *
     * @return the data source
     */
    @Override
    public ObjectAccessor getDataSource() {
        return source;
    }

    /**
     * Returns the root binder for this binding process.
     *
     * @return the root binder
     */
    @Override
    public ObjectBinder getRootBinder() {
        return this;
    }

    /**
     * Checks if the binding strategy is deep binding.
     *
     * @return {@code true} if deep binding is enabled, otherwise {@code false}
     */
    @Override
    public boolean isDeepBinding() {
        return BindingStrategy.DEEP.equals(strategy);
    }

    /**
     * Checks if the binding strategy is shallow binding.
     *
     * @return {@code true} if shallow binding is enabled, otherwise {@code false}
     */
    @Override
    public boolean isShallowBinding() {
        return BindingStrategy.SHALLOW.equals(strategy);
    }

    @Override
    public void useDeepBinding() {
        strategy = BindingStrategy.DEEP;
    }

    @Override
    public void useShallowBinding() {
        strategy = BindingStrategy.SHALLOW;
    }

    /**
     * Returns the conversion logic associated with this binder.
     *
     * @return the conversion
     */
    @Override
    public Conversion getConversion() {
        return conversion;
    }

    /**
     * Return the default {@link BindCallback} associated with this binder.
     *
     * @return the default bind callback, or {@code null} if none is configured
     */
    public BindCallback getCallback() {
        return callback;
    }

    /**
     * Set the default {@link BindCallback} for this binder.
     * <p>
     * If a previous callback is already configured, it will be set as the
     * parent of the given callback, forming a callback chain.
     * </p>
     *
     * @param callback the default bind callback to set
     */
    public void setCallback(BindCallback callback) {
        BindCallback previous = this.callback;

        if (previous != null) {
            callback.withParent(previous);
        }

        this.callback = callback;
    }

    /**
     * Return the {@link ObjectAccessorWrapper} to use.
     * <p>
     * Falls back to a default wrapper if none is explicitly configured.
     * </p>
     *
     * @return the configured wrapper, or the default wrapper
     */
    public ObjectAccessorWrapper getWrapper() {
        return wrapper == null ? DEFAULT_WRAPPER : wrapper;
    }

    /**
     * Set the {@link ObjectAccessorWrapper} to use.
     *
     * @param wrapper the wrapper to set
     */
    public void setWrapper(ObjectAccessorWrapper wrapper) {
        this.wrapper = wrapper;
    }

    /**
     * Return the {@link BinderFactory} associated with this binder.
     *
     * @return the binder factory
     */
    public BinderFactory getFactory() {
        return factory;
    }

}
