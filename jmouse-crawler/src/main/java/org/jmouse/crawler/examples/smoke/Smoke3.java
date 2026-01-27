package org.jmouse.crawler.examples.smoke;

import org.jmouse.core.bind.*;
import org.jmouse.core.mapping.bindings.MappingRulesRegistry;
import org.jmouse.core.mapping.bindings.TypeMappingBuilder;
import org.jmouse.core.mapping.config.MappingPolicy;
import org.jmouse.core.mapping.examples.Smoke1;
import org.jmouse.core.mapping.plan.MappingPlanRegistry;
import org.jmouse.core.mapping.plan.array.ArrayPlanContributor;
import org.jmouse.core.mapping.plan.bean.JavaBeanPlanContributor;
import org.jmouse.core.mapping.plan.collection.CollectionPlanContributor;
import org.jmouse.core.mapping.plan.map.MapPlanContributor;
import org.jmouse.core.mapping.plan.record.RecordPlanContributor;
import org.jmouse.core.mapping.plan.scalar.ScalarPlanContributor;
import org.jmouse.core.mapping.runtime.Mapper;
import org.jmouse.core.mapping.runtime.MappingContext;
import org.jmouse.core.mapping.runtime.ObjectMapper;
import org.jmouse.core.trace.TraceContext;
import org.jmouse.crawler.api.ProcessingTask;
import org.jmouse.crawler.api.TaskId;
import org.jmouse.crawler.api.TaskOrigin;
import org.jmouse.crawler.examples.smoke.smoke2.VoronHint;
import org.jmouse.crawler.runtime.state.persistence.dto.ProcessingTaskDto;

import java.net.URI;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

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

        Mapper mapper = mapper();

        mapper.map(processingTask, ProcessingTaskDto.class);

        System.out.println("Finish");
    }

    public static Mapper mapper() {
        var registry = MappingRulesRegistry.builder()
                .register(
                        new TypeMappingBuilder<>(ProcessingTask.class, ProcessingTaskDto.class)
                                .bind("user", "username")
                                .ignore("password")
                                .constant("password", "masked")
                                .bind("id", source -> source.id().value())
                                .build()
                )
                .build();

        AtomicReference<Mapper> reference = new AtomicReference<>();

        MappingContext context = new MappingContext(
                reference::get,
                new MappingPlanRegistry(List.of(
                        new JavaBeanPlanContributor(),
                        new RecordPlanContributor(),
                        new ScalarPlanContributor(),
                        new MapPlanContributor(),
                        new CollectionPlanContributor(),
                        new ArrayPlanContributor()
                )),
                new StandardAccessorWrapper(),
                new BinderConversion(),
                registry,
                MappingPolicy.defaults()
        );

        ObjectMapper mapper = new ObjectMapper(context);
        reference.set(mapper);

        return mapper;
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
