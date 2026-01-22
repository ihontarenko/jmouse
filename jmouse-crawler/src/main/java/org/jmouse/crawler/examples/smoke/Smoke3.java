package org.jmouse.crawler.examples.smoke;

import org.jmouse.core.bind.*;
import org.jmouse.core.trace.TraceContext;
import org.jmouse.crawler.api.ProcessingTask;
import org.jmouse.crawler.api.TaskId;
import org.jmouse.crawler.api.TaskOrigin;
import org.jmouse.crawler.examples.smoke.smoke2.VoronHint;
import org.jmouse.crawler.runtime.state.persistence.dto.ProcessingTaskDto;

import java.net.URI;
import java.time.Instant;

public class Smoke3 {

    public static void main(String[] args) {
        ProcessingTask processingTask = new ProcessingTask(
                TaskId.random(),
                TraceContext.root(),
                URI.create("https://google.com"),
                1,
                null,
                TaskOrigin.retry("reason-a"),
                1,
                Instant.now(),
                1,
                VoronHint.LISTING
        );

        VirtualPropertyResolver resolver = VirtualPropertyResolver.defaultResolver();
        resolver.addVirtualProperty(new TaskOriginKindVirtualProperty());

        Binder binder = Binder.with(processingTask, new DefaultBindingCallback());

        PropertyValueResolver valueResolver = new DefaultPropertyValueResolver(binder.getObjectAccessor(), resolver);

        ObjectAccessor accessor = binder.getWrapper().wrap(valueResolver);
        binder.setObjectAccessor(accessor);

        Bind.with(binder).to(ProcessingTaskDto.class);

        System.out.println("Finish");
    }

    static class TaskOriginKindVirtualProperty implements VirtualProperty<TaskOrigin> {

        @Override
        public Class<TaskOrigin> getType() {
            return TaskOrigin.class;
        }

        @Override
        public String getName() {
            return "kind";
        }

        @Override
        public boolean isReadable() {
            return true;
        }

        @Override
        public Object readValue(TaskOrigin object) {
            return VirtualProperty.super.readValue(object);
        }
    }

}
