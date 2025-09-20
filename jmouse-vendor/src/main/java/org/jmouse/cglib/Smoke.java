package org.jmouse.cglib;

public class Smoke {

    static class Foo { String hi() { return "ok"; } }

    public static void main(String[] args) {
        var e = new org.jmouse.cglib.proxy.Enhancer();
        e.setSuperclass(Foo.class);
        e.setCallback((org.jmouse.cglib.proxy.MethodInterceptor) (obj, m, a, mp) -> {
            if (m.getName().equals("hi")) return "intercepted";
            return mp.invokeSuper(obj, a);
        });
        Foo proxy = (Foo) e.create();
        assert "intercepted".equals(proxy.hi());
    }

}
