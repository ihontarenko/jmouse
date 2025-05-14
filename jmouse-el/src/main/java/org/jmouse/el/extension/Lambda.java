package org.jmouse.el.extension;

import org.jmouse.el.node.expression.LambdaNode;

@FunctionalInterface
public interface Lambda {

    Object apply(Object... arguments);

    record Default(LambdaNode node) implements Lambda {

        @Override
        public Object apply(Object... arguments) {
            return null;
        }

    }

}
