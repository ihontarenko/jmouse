package org.jmouse.core.reflection.annotation;

import java.lang.annotation.Annotation;

public interface MetaKeyQualifier {
    String qualify(String key, Annotation annotation);
}
