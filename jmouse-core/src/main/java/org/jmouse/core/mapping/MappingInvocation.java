package org.jmouse.core.mapping;

import org.jmouse.core.Verify;
import org.jmouse.core.bind.PropertyPath;
import org.jmouse.core.mapping.errors.MappingException;
import org.jmouse.core.mapping.plugin.*;
import org.jmouse.core.reflection.InferredType;

public final class MappingInvocation {

    private final MappingContext context;
    private final PluginBus      bus;
    private final boolean        root;

    private MappingInvocation(MappingContext context, PluginBus bus, boolean root) {
        this.context = Verify.nonNull(context, "context");
        this.bus = Verify.nonNull(bus, "bus");
        this.root = root;
    }

    public static MappingInvocation begin(MappingContext base, Object source, Class<?> sourceType, InferredType targetType) {
        boolean root = base.scope().root() == null;

        MappingContext scoped = root
                ? base.withScope(MappingScope.root(source))
                : base;

        PluginBus bus = root ? scoped.plugins() : PluginBus.Noop.INSTANCE;

        bus.onStart(new MappingCall(source, sourceType, targetType, scoped));

        return new MappingInvocation(scoped, bus, root);
    }

    public MappingContext context() {
        return context;
    }

    public <T> T finish(Object source, T value, InferredType targetType) {
        bus.onFinish(new MappingResult(source, value, targetType, context));
        return value;
    }

    public RuntimeException fail(MappingException exception) {
        PropertyPath path = context.scope().path();
        bus.onError(new MappingFailure(exception, path, context));
        return exception;
    }
}
