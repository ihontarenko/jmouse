package svit.beans.processor;

import svit.beans.BeanContext;
import svit.beans.BeanContextAware;

public class BeanContextAwareBeanPostProcessor implements BeanPostProcessor {

    @Override
    public void postProcessAfterInitialize(Object object, BeanContext context) {
        if (object instanceof BeanContextAware bean) {
            bean.setBeanContext(context);
        }
    }

}
