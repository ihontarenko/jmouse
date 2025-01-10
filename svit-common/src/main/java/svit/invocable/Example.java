package svit.invocable;

import svit.util.Strings;

public class Example {

    public static void main(String[] args) {
        Invocable invocable = new StaticMethodInvocable(Strings.class, "underscored", String.class, boolean.class);

        invocable.addParameter(new MethodParameter(0, "helloWorld"));
        invocable.addParameter(new MethodParameter(1, true));

        InvokeResult result = invocable.invoke();

        System.out.println((Object) result.getReturnValue());
    }

}
