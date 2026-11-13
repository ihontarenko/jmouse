package org.jmouse.settings;

/** 🎚️ The form a parameter's value takes, and how it is written as text in its row. */
public enum ParameterKind {
    /** On or off: {@code true}, {@code false}. */
    SWITCH,
    /** A whole number: {@code 24}. */
    WHOLE_NUMBER,
    /** A number: {@code 0.25}. */
    NUMBER,
    /** A stretch of time, as ISO-8601 writes it: {@code PT60S}. */
    DURATION,
    /** A line of text. */
    TEXT,
    /** One of the parameter's {@link Parameter.Choice}s, by its value. */
    CHOICE,
    /** A key to a service: written, used, and never read back. */
    SECRET,
    /** Folders of the server, each whole, one to a line. */
    FOLDERS,
    /** One folder of the server, whole. */
    FOLDER,
    /** How a program of the server is started, one word to a line: a word may hold spaces, as a path does. */
    COMMAND;

    /** Whether its value says what the server has: told only to whoever may change it, as an address to attack. */
    public boolean namesTheServer() {
        return this == FOLDERS || this == FOLDER || this == COMMAND;
    }
}
