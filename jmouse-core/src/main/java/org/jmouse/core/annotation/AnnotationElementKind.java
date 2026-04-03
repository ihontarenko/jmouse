package org.jmouse.core.annotation;

/**
 * Kind of annotated program element. 🧩
 *
 * <p>Used to route {@link AnnotationCandidate} to appropriate processors.</p>
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
     * Parameter-level annotation. 🎯
     */
    PARAMETER,

    /**
     * Field-level annotation. 🧷
     */
    FIELD
}