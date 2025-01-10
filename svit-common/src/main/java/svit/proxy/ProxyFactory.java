package svit.proxy;

public class ProxyFactory implements Proxy {

    protected final ProxyConfig proxyConfig;

    public ProxyFactory(Object target) {
        this.proxyConfig = new ProxyConfig(target);
    }

    public void addInterceptor(MethodInterceptor interceptor) {
        this.proxyConfig.addInterceptor(interceptor);
    }

    @Override
    public <T> T getProxy(ClassLoader classLoader) {
        return (T) new JdkProxy(this.proxyConfig).getProxy(classLoader);
    }

    @Override
    public ProxyEngine getProxyEngine() {
        throw new UnsupportedOperationException("PROXY ENGINE IS UNKNOWN");
    }

}