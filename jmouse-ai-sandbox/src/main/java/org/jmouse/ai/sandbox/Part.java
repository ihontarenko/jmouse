package org.jmouse.ai.sandbox;

import java.util.LinkedHashMap;
import java.util.Map;

/** One thing on a shelf. Belongs to a {@link Workshop}, which is what makes parts scope-confined. */
public record Part(String id, String workshopId, String name, String shelf, int quantity) {

    public Part withQuantity(int newQuantity) {
        return new Part(id, workshopId, name, shelf, newQuantity);
    }

    /**
     * What a trail keeps once this part no longer exists.
     *
     * <p>Captured where the rows are already loaded, because the moment after the resolver is the last
     * honest one — see {@code AffectedRecords.Record#previousState}.
     */
    public Map<String, String> state() {
        Map<String, String> state = new LinkedHashMap<>();

        state.put("name",     name);
        state.put("shelf",    shelf);
        state.put("quantity", String.valueOf(quantity));

        return state;
    }
}
