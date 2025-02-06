package org.jmouse.context.bind;

public class BindingExceptionCallback implements BindCallback {

    @Override
    public Object onFailure(NamePath name, Bindable<?> bindable, BindContext context, Exception error) throws Exception {
        return BindCallback.super.onFailure(name, bindable, context, error);
    }

}
