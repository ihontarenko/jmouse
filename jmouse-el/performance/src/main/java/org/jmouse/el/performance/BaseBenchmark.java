package org.jmouse.el.performance;

import org.jmouse.el.performance.model.Stock;
import org.openjdk.jmh.annotations.*;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Fork(5)
@Warmup(iterations = 5, time = 1)
@Measurement(iterations = 10, time = 1)
@BenchmarkMode(Mode.Throughput)
@OutputTimeUnit(TimeUnit.SECONDS)
@State(Scope.Benchmark)
public class BaseBenchmark {

    protected Map<String, Object> getContext() {
        Map<String, Object> context = new HashMap<>();
        context.put("items", Stock.dummyItems());
        return context;
    }

}
