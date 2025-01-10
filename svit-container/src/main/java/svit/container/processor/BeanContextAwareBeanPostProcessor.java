package svit.container.processor;

import svit.container.BeanContext;
import svit.container.BeanContextAware;

public class BeanContextAwareBeanPostProcessor implements BeanPostProcessor {

    @Override
    public void postProcessAfterInitialize(Object object, BeanContext context) {
        if (object instanceof BeanContextAware bean) {
            bean.setBeanContext(context);
        }
    }

}
