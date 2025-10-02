package org.jmouse.security.core;

import java.util.function.Supplier;

public class SupplierDeferredSecurityContext implements DeferredSecurityContext {

    private final Supplier<SecurityContext>     supplier;
    private final SecurityContextHolderStrategy strategy;
    private       SecurityContext               context;

    public SupplierDeferredSecurityContext(Supplier<SecurityContext> supplier, SecurityContextHolderStrategy strategy) {
        this.supplier = supplier;
        this.strategy = strategy;
    }

    @Override
    public SecurityContext get() {
        return context;
    }

    private void initialize() {
        if (context == null) {
            context = supplier.get();
            if (context == null) {
                context = strategy.newContext();
            }
        }
    }

}
