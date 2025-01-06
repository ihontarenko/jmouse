package svit.scanner;

import svit.matcher.Matcher;

import java.util.ArrayList;
import java.util.List;

public abstract class AbstractScanner<T> implements Scanner<T>, CompositeScanner<T> {

    protected final List<Scanner<T>> scanners = new ArrayList<>();
    private         Matcher<T>       matcher  = Matcher.constant(true);

    @Override
    public void addScanner(Scanner<T> scanner) {
        scanners.add(scanner);
    }

    public Matcher<T> getMatcher() {
        return matcher;
    }

    public void setMatcher(Matcher<T> matcher) {
        this.matcher = matcher;
    }

    protected boolean filter(T target) {
        return matcher.matches(target);
    }

}
