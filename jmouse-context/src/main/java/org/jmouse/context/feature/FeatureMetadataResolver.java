package org.jmouse.context.feature;

import java.lang.annotation.Annotation;

public interface FeatureMetadataResolver {

    FeatureMetadata resolve(Class<? extends Annotation> annotationType);

}