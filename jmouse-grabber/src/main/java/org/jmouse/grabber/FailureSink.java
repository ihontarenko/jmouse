package org.jmouse.grabber;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 🚑 Where a visit goes when it could not be completed.
 *
 * <p>⚠️ <b>A handler throwing is a failure of that visit, never of the run.</b> A crawl of ten
 * thousand pages that dies on the one page with unexpected markup has wasted everything before it, so
 * the failure is recorded here and the run carries on. What to do about it afterwards — retry the
 * list by hand, look at the markup, ignore it — is the caller's.</p>
 */
public interface FailureSink {

    /**
     * 🚑 The default: a warning per exhausted failure, and nothing kept.
     */
    static FailureSink logging() {
        Logger logger = LoggerFactory.getLogger("org.jmouse.grabber");

        return failure -> {
            if (failure.exhausted()) {
                logger.warn("{}", failure);
            }
        };
    }

    /**
     * 🚑 Hands each failure to this consumer.
     */
    static FailureSink of(Consumer<Failure> consumer) {
        return consumer::accept;
    }

    /**
     * 📚 Keeps every exhausted failure, so a run can be asked afterwards what it could not do.
     */
    static Collecting collecting() {
        return new Collecting();
    }

    void accept(Failure failure);

    final class Collecting implements FailureSink {

        private final List<Failure> failures = new ArrayList<>();

        @Override
        public synchronized void accept(Failure failure) {
            if (failure.exhausted()) {
                failures.add(failure);
            }
        }

        public synchronized List<Failure> failures() {
            return List.copyOf(failures);
        }

        public synchronized int size() {
            return failures.size();
        }
    }

}
