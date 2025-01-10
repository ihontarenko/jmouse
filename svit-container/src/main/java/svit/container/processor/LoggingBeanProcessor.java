package svit.container.processor;

import svit.container.BeanContext;

import java.util.function.Consumer;

public class LoggingBeanProcessor implements BeanPostProcessor {

    private final Consumer<String> logger;

    public LoggingBeanProcessor(Consumer<String> logger) {
        this.logger = logger;
    }

    @Override
    public void postProcessAfterInitialize(Object bean, BeanContext context) {
        logger.accept(
                "-- BEAN CREATED: [%s@%x] [%s]".formatted(bean.getClass().getName(), bean.hashCode(), bean)
        );
    }

}
