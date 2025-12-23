package org.jmouse.beans;

@FunctionalInterface
public interface BeanWarmer<C extends BeanContext> {
    void warmup(C context);
}
