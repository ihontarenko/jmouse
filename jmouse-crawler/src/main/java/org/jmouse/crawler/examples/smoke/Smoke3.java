package org.jmouse.crawler.examples.smoke;

import org.jmouse.core.bind.*;
import org.jmouse.core.mapping.binding.TypeMappingRegistry;
import org.jmouse.core.mapping.binding.TypeMappingBuilder;
import org.jmouse.core.mapping.config.MappingPolicy;
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
import org.jmouse.crawler.runtime.state.persistence.dto.TaskOriginDto;

import java.net.URI;
import java.time.Instant;
import java.util.List;
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

        ProcessingTaskDto processingTaskDto = mapper.map(processingTask, ProcessingTaskDto.class);
        ProcessingTask processingTaskRemapped = mapper.map(processingTaskDto, ProcessingTask.class);

        processingTaskRemapped.equals(processingTask); // true

        System.out.println("Finish");
    }

    public static Mapper mapper() {
        var registry = TypeMappingRegistry.builder()
                .mapping(ProcessingTask.class, ProcessingTaskDto.class, m -> m
                        .bind("user", "username")
                        .ignore("password")
                        .constant("password", "masked")
                        .bind("id", source -> source.id().value())
                ).mapping(ProcessingTaskDto.class, ProcessingTask.class, m -> m
                        .bind("user", "username")
                        .ignore("password")
                        .constant("password", "masked")
                        .bind("id", s -> new TaskId(s.id()))
                        .compute("origin", (source, context) -> {
                            String kind = source.origin().kind();

                            if (kind.equals("retry")) {
                                return TaskOrigin.retry(source.origin().reason());
                            }

                            return TaskOrigin.retry("default-reason");
                        })
                        .compute("hint", (source, context)
                                -> VoronHint.valueOf(source.hint().toUpperCase().trim()))
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

}
