package org.jmouse.core.mapping.plugin;

import org.jmouse.core.access.PropertyPath;
import org.jmouse.core.mapping.errors.MappingException;
import org.jmouse.core.reflection.InferredType;

public final class DebugInformationPlugin implements MappingPlugin {

    @Override
    public void onStart(MappingCall call) {
        System.out.println(
                "[MAP:START] " +
                        "sourceType=" + call.sourceType().getSimpleName() +
                        ", targetType=" + type(call.targetType()) +
                        ", path=" + path(call.context().scope().path())
        );
    }

    @Override
    public Object onValue(MappingValue value) {
        System.out.println(
                "[MAP:VALUE] " +
                        "path=" + path(value.context().scope().path()) +
                        ", targetType=" + type(value.targetType()) +
                        ", value=" + safe(value.current())
        );
        return value.current(); // IMPORTANT: do not modify
    }

    @Override
    public void onFinish(MappingResult result) {
        System.out.println(
                "[MAP:FINISH] " +
                        "targetType=" + type(result.targetType()) +
                        ", path=" + path(result.context().scope().path()) +
                        ", result=" + safe(result.target())
        );
    }

    @Override
    public void onError(MappingFailure failure) {
        if (failure.error() instanceof MappingException exception) {
            System.err.println(
                    "[MAP:ERROR] " +
                            "code=" + exception.code() +
                    ", path=" + path(failure.path()) +
                    ", message=" + failure.error().getMessage());
        }
    }

    /* ---------------- helpers ---------------- */

    private static String path(PropertyPath path) {
        return path.toString();
    }

    private static String type(Object type) {
        if (type instanceof InferredType it) {
            return it.toString();
        }
        return String.valueOf(type);
    }

    private static String safe(Object value) {
        if (value == null) return "null";
        try {
            return value.toString();
        } catch (Exception ex) {
            return "<toString-failed:" + value.getClass().getSimpleName() + ">";
        }
    }
}
