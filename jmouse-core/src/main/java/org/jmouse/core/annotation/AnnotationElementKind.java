package org.jmouse.core.annotation;

/**
 * Enumeration describing the kind of annotated program element. 🧩
 *
 * <p>
 * Used by the annotation processing infrastructure to classify
 * {@link AnnotationCandidate candidates} and route them to the
 * appropriate {@link AnnotationProcessor}.
 * </p>
 */
public enum AnnotationElementKind {

    /**
     * Class-level annotation (class, interface, enum, record). 🏷️
     */
    TYPE,

    /**
     * Method-level annotation. 🔧
     */
    METHOD,

    /**
     * Field-level annotation. 🧷
     */
    FIELD
}