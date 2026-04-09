package org.jmouse.context.feature;

import java.lang.annotation.*;

@Target(ElementType.ANNOTATION_TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface FeatureSelectorBinding {
    Class<? extends FeatureSelector>[] value();
}