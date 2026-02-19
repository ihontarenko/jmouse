package org.jmouse.pipeline;

public record PipelineResult(
        Enum<?> code,
        Object payload,
        String jumpTo,
        boolean stop,
        Throwable error
) {

    public static PipelineResult of(Enum<?> code) {
        return new PipelineResult(code, null, null, false, null);
    }

    public static PipelineResult of(Enum<?> code, Object payload) {
        return new PipelineResult(code, payload, null, false, null);
    }

    public static PipelineResult jump(String linkName) {
        return new PipelineResult(null, null, linkName, false, null);
    }

    public static PipelineResult finish() {
        return new PipelineResult(null, null, null, true, null);
    }

    public static PipelineResult error(Throwable error) {
        return new PipelineResult(null, null, null, true, error);
    }

    public boolean hasJump() {
        return jumpTo != null && !jumpTo.isBlank();
    }

    /** Key used for transitions map (String → link). */
    public String codeKey() {
        return code == null ? null : code.name();
    }
}

