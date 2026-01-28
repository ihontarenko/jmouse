package org.jmouse.core.mapping;

import org.jmouse.core.bind.BinderConversion;
import org.jmouse.core.bind.StandardAccessorWrapper;
import org.jmouse.core.mapping.binding.TypeMappingRegistry;
import org.jmouse.core.mapping.config.MappingConfig;
import org.jmouse.core.mapping.config.MappingPolicy;
import org.jmouse.core.mapping.plan.MappingPlanRegistry;
import org.jmouse.core.mapping.plan.array.ArrayPlanContributor;
import org.jmouse.core.mapping.plan.bean.JavaBeanPlanContributor;
import org.jmouse.core.mapping.plan.collection.ListPlanContributor;
import org.jmouse.core.mapping.plan.map.MapPlanContributor;
import org.jmouse.core.mapping.plan.record.RecordPlanContributor;
import org.jmouse.core.mapping.plan.scalar.ScalarPlanContributor;

import java.util.List;

public class Mappers {

    public static Mapper defaultMapper() {
        return builder().build();
    }

    public static MapperBuilder builder() {
        return new MapperBuilder()
                .wrapper(new StandardAccessorWrapper())
                .conversion(new BinderConversion())
                .policy(MappingPolicy.defaults())
                .config(MappingConfig.builder().build())
                .registry(TypeMappingRegistry.builder().build())
                .planRegistry(new MappingPlanRegistry(List.of(
                        new JavaBeanPlanContributor(),
                        new RecordPlanContributor(),
                        new ScalarPlanContributor(),
                        new MapPlanContributor(),
                        new ListPlanContributor(),
                        new ArrayPlanContributor()
                )));
    }

}
