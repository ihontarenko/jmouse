package svit.observer;

import svit.util.Strings;
import svit.matcher.Matcher;
import svit.reflection.TypeMatchers;

abstract public class AbstractEventListener<T> implements EventListener<T> {

    protected final Matcher<Class<?>> matcher = TypeMatchers.isSimilar(applicableType());

    @Override
    public String name() {
        return Strings.underscored(getClass().getSimpleName(), true);
    }

    @Override
    public boolean supports(Class<?> actualType) {
        return matcher.matches(actualType);
    }

}
