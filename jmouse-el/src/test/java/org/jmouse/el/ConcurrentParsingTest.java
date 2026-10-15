package org.jmouse.el;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * One language instance, shared by many threads parsing at once — which is how every product holds one.
 *
 * <p>Each expression is different, so the language's cache answers none of them and every one goes through
 * the lexer and the parser. Each carries a function call, a parenthesised group and a filter chain: the three
 * places that touch the recognizer list and the parser options the threads share.</p>
 */
class ConcurrentParsingTest {

    private static final int THREADS     = 24;
    private static final int EXPRESSIONS = 400;

    @Test
    @DisplayName("many threads parsing through one language all read what a single thread reads")
    void sharedLanguageParsesConcurrently() throws Exception {
        ExpressionLanguage language = new ExpressionLanguage();
        ExecutorService    executor = Executors.newFixedThreadPool(THREADS);
        CountDownLatch     start    = new CountDownLatch(1);

        try {
            List<Future<Void>> results = new ArrayList<>();

            for (int thread = 0; thread < THREADS; thread++) {
                int offset = thread * EXPRESSIONS;

                Callable<Void> parsing = () -> {
                    start.await();

                    for (int index = offset; index < offset + EXPRESSIONS; index++) {
                        String expression = "max(%d, 2) + (%d * 3) + ('ab' | upper | length)".formatted(index, index);
                        long   expected   = Math.max(index, 2) + index * 3L + 2;

                        assertEquals(expected, language.evaluate(expression, Long.class), expression);
                    }

                    return null;
                };

                results.add(executor.submit(parsing));
            }

            start.countDown();

            for (Future<Void> result : results) {
                result.get(60, TimeUnit.SECONDS);
            }
        } finally {
            executor.shutdownNow();
            assertTrue(executor.awaitTermination(10, TimeUnit.SECONDS));
        }
    }
}
