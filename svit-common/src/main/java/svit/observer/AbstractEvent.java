package svit.observer;

import java.util.Objects;

abstract public class AbstractEvent<T> implements Event<T> {

    protected final String name;
    protected final T      payload;
    protected final Object caller;

    public AbstractEvent(String name, T payload, Object caller) {
        this.name = name;
        this.payload = payload;
        this.caller = caller;
    }

    public AbstractEvent(String name, T payload) {
        this(name, payload, null);
    }

    @Override
    public String name() {
        return name;
    }

    @Override
    public T payload() {
        return payload;
    }

    @Override
    public Class<? extends T> payloadType() {
        return (Class<? extends T>) Objects.requireNonNull(payload()).getClass();
    }

    @Override
    public Object caller() {
        return caller;
    }
}
