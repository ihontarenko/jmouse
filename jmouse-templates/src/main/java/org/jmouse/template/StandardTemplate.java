package org.jmouse.template;

import org.jmouse.el.lexer.TokenizableSource;
import org.jmouse.el.rendering.EntityStack;
import org.jmouse.el.rendering.Fragment;

import java.util.List;

public class StandardTemplate implements Template {

    private final TokenizableSource source;
    private final EntityStack       stack;

    public StandardTemplate(TokenizableSource source) {
        this.source = source;
        this.stack = EntityStack.empty();
    }

    @Override
    public TokenizableSource getSource() {
        return source;
    }

    @Override
    public String getName() {
        return source.getName();
    }

    @Override
    public void setFragment(Fragment fragment) {

    }

    @Override
    public Fragment getFragment(String name) {
        return null;
    }

    @Override
    public List<Fragment> getFragments() {
        return List.of();
    }

    @Override
    public EntityStack getStack() {
        return stack;
    }

    @Override
    public void setParent(String parent) {

    }

}
