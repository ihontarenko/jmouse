package svit.proxy;

public interface MethodInterceptor {

    Object invoke(MethodInvocation invocation) throws Throwable;

}
