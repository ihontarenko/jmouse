package svit.proxy;

public interface Proxy {

    default <T> T getProxy() {
        return getProxy(Thread.currentThread().getContextClassLoader());
    }

    <T> T getProxy(ClassLoader classLoader);

    ProxyEngine getProxyEngine();

}
