package org.jmouse.el.rendering;

import java.util.ArrayList;
import java.util.List;

public class Inheritance implements EntityStack {

    private final List<RenderableEntity> stack = new ArrayList<>();
    private       int                    depth = 0;

    @Override
    public void inherit(RenderableEntity entity) {
        stack.addLast(entity);
    }

    @Override
    public void ascend() {
        if (stack.size() - 1 > depth) {
            depth++;
        }
    }

    @Override
    public void descend() {
        if (depth > 0) {
            depth--;
        }
    }

    @Override
    public RenderableEntity getChild() {
        RenderableEntity entity = null;

        if (depth > 0) {
            entity = stack.get(depth - 1);
        }

        return entity;
    }

    @Override
    public RenderableEntity getParent() {
        RenderableEntity entity = null;

        if (stack.size() - 1 > depth) {
            entity = stack.get(depth + 1);
        }

        return entity;
    }
}
