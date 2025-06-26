package org.jmouse.testing_ground.annotations;

import org.jmouse.beans.conditions.BeanCondition;
import org.jmouse.core.reflection.annotation.AnnotationData;
import org.jmouse.core.reflection.annotation.AnnotationScanner;
import org.jmouse.core.reflection.annotation.MergedAnnotation;
import org.jmouse.mvc.ServletDispatcherConfiguration;
import org.jmouse.util.Streamable;

import java.util.List;
import java.util.Optional;
import java.util.Set;

public class Test {

    public static void main(String[] args) {

        Set<AnnotationData> annotations =
                AnnotationScanner.scan(ServletDispatcherConfiguration.class);

        List<MergedAnnotation> mergedAnnotations = Streamable.of(annotations).map(MergedAnnotation::new).toList();

        MergedAnnotation mergedAnnotation = new MergedAnnotation(annotations.iterator().next());

        Optional<MergedAnnotation> optional = mergedAnnotation.getMerged(BeanCondition.class);

        for (var data : annotations) {
            System.out.println("@" + data.annotationType().getSimpleName()
                                       + " on " + data.annotatedElement()
                                       + " (depth=" + data.depth() + ")");

            data.getMetaOf().ifPresent(parent ->
                                               System.out.println(" └ via @" + parent.annotationType().getSimpleName()));
        }
    }

}
