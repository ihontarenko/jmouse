package svit.observer;

public interface Event<T> {

    String name();

    T payload();

    Class<? extends T> payloadType();

    Object caller();

}
