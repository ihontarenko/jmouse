package org.jmouse.java.parser.grammar;

/** A work limit terminates reading without pretending that the cursor reached source EOF. */
public enum JavaParsingState {
    READING,
    WORK_LIMIT_REACHED
}
