package org.jmouse.jdbc.bulk;

import org.jmouse.core.Contract;
import org.jmouse.jdbc.SimpleTemplate;
import org.jmouse.jdbc.statement.BinderFactory;
import org.jmouse.jdbc.statement.PreparedStatementBinder;
import org.jmouse.transaction.TransactionCallback;
import org.jmouse.transaction.TransactionDefinition;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;
import java.util.stream.IntStream;

public final class ParallelBatchUpdater<T> {

    private final SimpleTemplate        template;
    private final TransactionCallback   transactionCallback;
    private final ExecutorService       executor;
    private final TransactionDefinition definition;

    public ParallelBatchUpdater(
            SimpleTemplate template,
            ExecutorService executor,
            TransactionCallback transactionCallback,
            TransactionDefinition definition
    ) {
        this.template = Contract.nonNull(template, "template");
        this.executor = Contract.nonNull(executor, "executor");
        this.transactionCallback = Contract.nonNull(transactionCallback, "transactionCallback");
        this.definition = Contract.nonNull(definition, "definition");
    }

    public BulkBatchResult execute(
            String sql, List<T> items, BinderFactory<T> binderFactory, BulkExecutionPolicy policy
    ) throws SQLException {
        Contract.nonNull(sql, "sql");
        Contract.nonNull(items, "items");
        Contract.nonNull(binderFactory, "binderFactory");
        Contract.nonNull(policy, "policy");

        List<List<T>> chunks = Chunker.split(items, policy.chunkSize());

        if (chunks.isEmpty()) {
            return BulkBatchResult.empty();
        }

        CompletionService<ChunkResult> completionService = new ExecutorCompletionService<>(executor);

        int  submitted = 0;
        int  completed = 0;
        long totalRows = 0;

        List<Throwable> errors = new ArrayList<>();

        // submit with backpressure (maxInFlight)
        while (submitted < chunks.size()) {
            while (submitted < chunks.size() && (submitted - completed) < policy.maxInFlight()) {
                List<T> chunk      = chunks.get(submitted);
                int     chunkIndex = submitted;

                completionService.submit(() -> runChunk(sql, chunk, chunkIndex, binderFactory));
                submitted++;
            }

            try {
                Future<ChunkResult> future      = completionService.take();
                ChunkResult         chunkResult = future.get();

                completed++;
                totalRows += chunkResult.affectedRows();

                if (chunkResult.error() != null) {
                    errors.add(chunkResult.error());
                    if (policy.failFast()) {
                        cancelRemaining(completionService, submitted - completed);
                        throw toSqlException("Bulk batch failed (fail-fast).", chunkResult.error());
                    }
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                cancelRemaining(completionService, submitted - completed);
                throw new SQLException("Bulk batch interrupted.", e);
            } catch (ExecutionException e) {
                Throwable cause = (e.getCause() != null) ? e.getCause() : e;
                errors.add(cause);

                if (policy.failFast()) {
                    cancelRemaining(completionService, submitted - completed);
                    throw toSqlException("Bulk batch failed (fail-fast).", cause);
                }
            }
        }

        // drain remaining completions
        while (completed < submitted) {
            try {
                Future<ChunkResult> future      = completionService.take();
                ChunkResult         chunkResult = future.get();

                completed++;
                totalRows += chunkResult.affectedRows();

                if (chunkResult.error() != null) {
                    errors.add(chunkResult.error());
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                cancelRemaining(completionService, submitted - completed);
                throw new SQLException("Bulk batch interrupted.", e);
            } catch (ExecutionException e) {
                Throwable cause = (e.getCause() != null) ? e.getCause() : e;
                errors.add(cause);
            }
        }

        return new BulkBatchResult(chunks.size(), totalRows, List.copyOf(errors));
    }

    private ChunkResult runChunk(
            String sql, List<T> chunk, int chunkIndex, BinderFactory<T> binderFactory
    ) {
        try {
            long affected = transactionCallback.inTransaction(definition, () -> {
                List<PreparedStatementBinder> binders = new ArrayList<>(chunk.size());
                for (T item : chunk) {
                    binders.add(binderFactory.binderFor(item));
                }
                return IntStream.of(template.batchUpdate(sql, binders)).reduce(0, Integer::sum);
            });
            return ChunkResult.ok(chunkIndex, affected);
        } catch (Throwable e) {
            return ChunkResult.fail(chunkIndex, e);
        }
    }

    private void cancelRemaining(CompletionService<ChunkResult> completionService, int inFlight) {
        // We cannot directly cancel tasks via CompletionService API.
        // In practice you’d keep Future references. For Stage 7, we keep it simple:
        // rely on fail-fast throwing; tasks still finish but results are ignored.
        // If you want hard cancellation, we can evolve this with stored Futures.
    }

    private SQLException toSqlException(String message, Throwable cause) {
        if (cause instanceof SQLException sql) {
            return sql;
        }
        return new SQLException(message, cause);
    }

    private record ChunkResult(int chunkIndex, long affectedRows, Throwable error) {
        static ChunkResult ok(int chunkIndex, long affected) {
            return new ChunkResult(chunkIndex, affected, null);
        }
        static ChunkResult fail(int chunkIndex, Throwable error) {
            return new ChunkResult(chunkIndex, 0, error);
        }
    }
}
