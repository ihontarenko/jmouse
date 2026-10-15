package org.jmouse.ai.reranking;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** Checked JSON decoding shared by wire adapters; errors never carry provider content. */
public final class RerankResponseReader {

    private RerankResponseReader() {
    }

    public static Map<?, ?> object(Object value) {
        if (!(value instanceof Map<?, ?> object)) {
            throw invalidResponse();
        }
        return object;
    }

    public static List<?> array(Object value) {
        if (!(value instanceof List<?> array)) {
            throw invalidResponse();
        }
        return array;
    }

    public static long nonnegativeInteger(Object value) {
        if (!(value instanceof Number number)) {
            throw invalidResponse();
        }
        try {
            long integer = new BigDecimal(number.toString()).longValueExact();
            if (integer < 0) {
                throw invalidResponse();
            }
            return integer;
        } catch (ArithmeticException | NumberFormatException malformedNumber) {
            throw invalidResponse();
        }
    }

    public static double finiteNumber(Object value) {
        if (!(value instanceof Number number) || !Double.isFinite(number.doubleValue())) {
            throw invalidResponse();
        }
        return number.doubleValue();
    }

    /** A nested optional integer, such as {@code usage.total_tokens}; any missing step means "not reported". */
    public static Long optionalInteger(Map<?, ?> response, String... path) {
        Object value = response;
        for (String step : path) {
            if (value == null) {
                return null;
            }
            value = object(value).get(step);
        }
        return value == null ? null : nonnegativeInteger(value);
    }

    /** Rows of {index, score} under the protocol's own field names. */
    public static List<RerankModel.Ranked> rows(List<?> rows, String indexField, String scoreField) {
        List<RerankModel.Ranked> ranked = new ArrayList<>(rows.size());
        for (Object item : rows) {
            Map<?, ?> row = object(item);
            long index = nonnegativeInteger(row.get(indexField));
            if (index > Integer.MAX_VALUE) {
                throw invalidResponse();
            }
            ranked.add(new RerankModel.Ranked((int) index, finiteNumber(row.get(scoreField))));
        }
        return ranked;
    }

    /**
     * The contract every protocol's result must meet: exactly the expected number of distinct indices that
     * exist in the request. Returned best first, whatever order the provider sent them in; equal scores
     * keep the provider's order.
     */
    static RerankModel.Response validated(RerankModel.Response response, RerankModel.Request request) {
        if (response == null || response.ranked().size() != request.expectedResults()) {
            throw invalidResponse();
        }
        Set<Integer> seen = new HashSet<>();
        for (RerankModel.Ranked ranked : response.ranked()) {
            if (ranked.index() >= request.documents().size() || !seen.add(ranked.index())) {
                throw invalidResponse();
            }
        }
        List<RerankModel.Ranked> ordered = new ArrayList<>(response.ranked());
        ordered.sort(Comparator.comparingDouble(RerankModel.Ranked::score).reversed());
        return new RerankModel.Response(ordered, response.inputTokens());
    }

    public static RerankException invalidResponse() {
        return new RerankException(RerankException.Reason.INVALID_RESPONSE, null);
    }
}
