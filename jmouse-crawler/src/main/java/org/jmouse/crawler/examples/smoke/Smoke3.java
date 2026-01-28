package org.jmouse.crawler.examples.smoke;

import org.jmouse.core.mapping.*;
import org.jmouse.core.mapping.binding.TypeMappingRegistry;
import org.jmouse.core.mapping.binding.annotation.AnnotationRuleSource;
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
                URI.create("https://jmouse.org/"),
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
                .ruleSource(new AnnotationRuleSource())
                .mapping(ProcessingTask.class, ProcessingTaskDto.class, m -> m
                        .bind("user", "username")
                        .ignore("password")
                        .constant("password", "masked")
                        .bind("id", source -> source.id().value())
                ).mapping(ProcessingTaskDto.class, ProcessingTask.class, m -> m
                        .bind("user", "username")
                        .bind("parent", "parentURI")
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


        return Mappers.builder()
                .registry(registry)
                .build();
    }

}
