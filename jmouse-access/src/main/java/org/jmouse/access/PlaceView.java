package org.jmouse.access;

import java.lang.reflect.RecordComponent;
import java.util.Arrays;
import java.util.List;

/**
 * What a rule may say about <strong>where</strong> — the declared half of {@link ScopeReference}.
 *
 * <p>Same bargain as {@link CallerView}, for the same reason. A {@code ScopeReference}'s accessors are
 * an implementation detail of the engine rather than a vocabulary anybody agreed to, so binding it
 * directly means every method it happens to have is silently part of the rule language and every one
 * it loses is a rule that breaks.
 *
 * @param kind which floor this is — {@code GLOBAL}, {@code ORGANIZATION}, {@code SPACE}, {@code SELF}
 * @param id   which one, or null at a floor that names no instance
 */
public record PlaceView(String kind, String id) {

    /** Every name a rule may write after {@code place.}, read off the record — see {@link CallerView}. */
    public static final List<String> MEMBERS = Arrays.stream(PlaceView.class.getRecordComponents())
            .map(RecordComponent::getName)
            .toList();

    /** The view of one scope, or a view of nothing where the decision named no place. */
    public static PlaceView of(ScopeReference scope) {
        return scope == null
                ? new PlaceView(null, null)
                : new PlaceView(scope.type().name(), scope.id());
    }
}
