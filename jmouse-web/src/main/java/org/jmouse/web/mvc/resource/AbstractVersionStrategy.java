package org.jmouse.web.mvc.resource;

import org.jmouse.core.matcher.ant.AntMatcher;

/**
 * 🧩 Base class for {@link VersionStrategy} implementations
 * that rely on {@link AntMatcher} to filter supported paths.
 */
public abstract class AbstractVersionStrategy implements VersionStrategy {

    /**
     * 🔎 Ant matcher for supported resource paths.
     */
    private final AntMatcher matcher;

    /**
     * 🏗️ Create strategy with a precompiled matcher.
     *
     * @param matcher ant matcher instance
     */
    protected AbstractVersionStrategy(AntMatcher matcher) {
        this.matcher = matcher;
    }

    /**
     * 🏗️ Create strategy with an ant-style pattern string.
     *
     * @param pattern ant-style path pattern (e.g. {@code /static/**})
     */
    protected AbstractVersionStrategy(String pattern) {
        this(new AntMatcher(pattern));
    }

    /**
     * @return underlying {@link AntMatcher} used for path checks
     */
    public AntMatcher getMatcher() {
        return matcher;
    }

    /**
     * ✅ Check if this strategy applies to a request path.
     *
     * @param requestPath candidate path
     * @return {@code true} if matches pattern
     */
    @Override
    public boolean isSupports(String requestPath) {
        return getMatcher().matches(requestPath);
    }
}
