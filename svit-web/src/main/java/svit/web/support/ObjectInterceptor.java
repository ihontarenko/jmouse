package svit.web.support;

import svit.beans.proxy.GlobalLoggingMethodInterceptor;
import svit.convert.Conversion;
import svit.proxy.annotation.ProxyMethodInterceptor;

@ProxyMethodInterceptor(Conversion.class)
public class ObjectInterceptor extends GlobalLoggingMethodInterceptor {
    
}
