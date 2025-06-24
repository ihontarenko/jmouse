package org.jmouse.testing_ground.annotations;

import org.jmouse.core.reflection.annotation.AnnotationData;
import org.jmouse.core.reflection.annotation.AnnotationScanner;
import org.jmouse.mvc.WebApplicationLauncher;

import java.util.Set;

public class Test {

    public static void main(String[] args) {

        Set<AnnotationData> annotations =
                AnnotationScanner.scan(WebApplicationLauncher.ServletDispatcherConfiguration.class);

        for (var data : annotations) {
            System.out.println("@" + data.annotationType().getSimpleName()
                                       + " on " + data.annotatedElement()
                                       + " (depth=" + data.depth() + ")");

            data.getParent().ifPresent(parent ->
                                               System.out.println(" └ via @" + parent.annotationType().getSimpleName()));
        }
    }

}
