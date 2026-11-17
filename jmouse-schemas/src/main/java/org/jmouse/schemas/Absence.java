package org.jmouse.schemas;

/** How a value with nothing in it stands in the JSON a schema describes. */
public enum Absence {

    /** Written as {@code null}: every property is there, and one that may be empty may be null — a model as the engine answers it. */
    WRITTEN_AS_NULL,

    /** An optional property may be left out, or be null — a file a person writes, which says only what it changes. */
    LEFT_OUT
}
