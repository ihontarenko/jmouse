package org.jmouse.ai.embeddings;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/** Internal checked JSON decoding shared by wire adapters; never includes provider content in errors. */
final class EmbeddingResponseReader {

    private static final String MODEL = "model";
    private static final String DATA = "data";
    private static final String INDEX = "index";
    private static final String EMBEDDING = "embedding";
    private static final String USAGE = "usage";

    private EmbeddingResponseReader() {
    }

    static Map<?, ?> object(Object value) {
        if (!(value instanceof Map<?, ?> object)) {
            throw invalidResponse();
        }
        return object;
    }

    static List<?> array(Object value) {
        if (!(value instanceof List<?> array)) {
            throw invalidResponse();
        }
        return array;
    }

    static long nonnegativeInteger(Object value) {
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

    static void requireModel(Map<?, ?> response, String expectedModel, boolean required) {
        Object actualModel = response.get(MODEL);
        if ((required || actualModel != null) && !expectedModel.equals(actualModel)) {
            throw invalidResponse();
        }
    }

    static Long optionalUsage(Map<?, ?> response, String field) {
        Object usage = response.get(USAGE);
        return usage == null ? null : optionalInteger(object(usage), field);
    }

    static Long optionalInteger(Map<?, ?> response, String field) {
        Object value = response.get(field);
        if (value == null) {
            return null;
        }
        return nonnegativeInteger(value);
    }

    static List<List<Double>> indexedVectors(Map<?, ?> response, EmbeddingModel.Request request) {
        List<?> rows = array(response.get(DATA));
        if (rows.size() != request.inputs().size()) {
            throw invalidResponse();
        }
        var ordered = new ArrayList<Object>(Collections.nCopies(rows.size(), null));
        for (Object item : rows) {
            Map<?, ?> row = object(item);
            long index = nonnegativeInteger(row.get(INDEX));
            if (index >= ordered.size() || ordered.get((int) index) != null) {
                throw invalidResponse();
            }
            ordered.set((int) index, array(row.get(EMBEDDING)));
        }
        return vectors(ordered, request);
    }

    static List<List<Double>> vectors(Object response, EmbeddingModel.Request request) {
        List<?> rows = array(response);
        if (rows.size() != request.inputs().size()) {
            throw invalidResponse();
        }
        var vectors = new ArrayList<List<Double>>(rows.size());
        for (Object row : rows) {
            List<?> coordinates = array(row);
            if (coordinates.size() != request.dimensions()) {
                throw invalidResponse();
            }
            var vector = new ArrayList<Double>(coordinates.size());
            for (Object coordinate : coordinates) {
                if (!(coordinate instanceof Number number) || !Double.isFinite(number.doubleValue())) {
                    throw invalidResponse();
                }
                vector.add(number.doubleValue());
            }
            vectors.add(vector);
        }
        return vectors;
    }

    static void validate(EmbeddingModel.Response response, EmbeddingModel.Request request) {
        if (response == null || response.vectors().size() != request.inputs().size()) {
            throw invalidResponse();
        }
        for (List<Double> vector : response.vectors()) {
            if (vector.size() != request.dimensions() || vector.stream().anyMatch(value -> !Double.isFinite(value))) {
                throw invalidResponse();
            }
        }
    }

    static EmbeddingException invalidResponse() {
        return new EmbeddingException(EmbeddingException.Reason.INVALID_RESPONSE, null);
    }
}
