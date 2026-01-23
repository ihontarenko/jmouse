package org.jmouse.core.mapping.errors;

import org.jmouse.core.bind.PropertyPath;

import java.util.Objects;

/**
 * A single mapping issue (warning or error) with path-aware details.
 */
public final class MappingProblem {

    public enum Severity { INFO, WARNING, ERROR }

    private final Severity     severity;
    private final String       code;
    private final String       message;
    private final PropertyPath targetPath;
    private final PropertyPath sourcePath;
    private final Class<?>     expectedTargetType;
    private final Class<?>     actualSourceType;
    private final Throwable    cause;

    private MappingProblem(Builder builder) {
        this.severity = Objects.requireNonNull(builder.severity, "severity");
        this.code = Objects.requireNonNull(builder.code, "code");
        this.message = Objects.requireNonNull(builder.message, "message");
        this.targetPath = builder.targetPath;
        this.sourcePath = builder.sourcePath;
        this.expectedTargetType = builder.expectedTargetType;
        this.actualSourceType = builder.actualSourceType;
        this.cause = builder.cause;
    }

    public static Builder builder() {
        return new Builder();
    }

    public Severity severity() { return severity; }
    public String code() { return code; }
    public String message() { return message; }
    public PropertyPath targetPath() { return targetPath; }
    public PropertyPath sourcePath() { return sourcePath; }
    public Class<?> expectedTargetType() { return expectedTargetType; }
    public Class<?> actualSourceType() { return actualSourceType; }
    public Throwable cause() { return cause; }

    public static final class Builder {
        private Severity     severity = Severity.ERROR;
        private String       code;
        private String       message;
        private PropertyPath targetPath;
        private PropertyPath sourcePath;
        private Class<?>     expectedTargetType;
        private Class<?>     actualSourceType;
        private Throwable    cause;

        public Builder severity(Severity value) { this.severity = value; return this; }
        public Builder code(String value) { this.code = value; return this; }
        public Builder message(String value) { this.message = value; return this; }
        public Builder targetPath(PropertyPath value) { this.targetPath = value; return this; }
        public Builder sourcePath(PropertyPath value) { this.sourcePath = value; return this; }
        public Builder expectedTargetType(Class<?> value) { this.expectedTargetType = value; return this; }
        public Builder actualSourceType(Class<?> value) { this.actualSourceType = value; return this; }
        public Builder cause(Throwable value) { this.cause = value; return this; }

        public MappingProblem build() { return new MappingProblem(this); }
    }
}
