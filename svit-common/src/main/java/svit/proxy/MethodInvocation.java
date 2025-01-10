package svit.proxy;

import java.lang.reflect.Method;

public interface MethodInvocation {

    Object proceed() throws Throwable;

    Method getMethod();

    Object[] getArguments();

    Object getTarget();

    int getOrdinal();

    ProxyConfig getProxyConfig();

    Object getProxy();

}
