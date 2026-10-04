package org.jmouse.script.event;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

/**
 * A moment whose OUTCOME a rule may change — not only watch, not only refuse.
 *
 * <h2>⚠️ A RULE PROPOSES, THE PRODUCT APPLIES</h2>
 *
 * <p>A rule never reaches into the code that raised the event. It records an amendment here — "the
 * title is X, because Y" — and the code that raised it decides how to apply it and writes it into the
 * trace under the rule's name. So every change a rule makes is visible where the outcome is shown,
 * attributed and explained; <b>a script cannot alter a result silently.</b></p>
 *
 * <p>⚠️ Only the fields the event declares may be amended. A rule naming anything else is refused at the
 * call, with the list that would have worked — never accepted and ignored.</p>
 *
 * @author Ivan Hontarenko (Mr. Jerry Mouse)
 * @author ihontarenko@gmail.com
 */
public abstract class AmendableEvent implements DomainEvent {

    /**
     * One change a rule asked for.
     *
     * @param rule   the rule's name, as its author wrote it
     * @param field  what it changes — one of {@link #amendableFields()}
     * @param value  the new value, or the amount for a relative change
     * @param reason why, in the rule author's words — shown to a person
     */
    public record Amendment(String rule, String field, Object value, String reason) {
    }

    /** A line a rule wrote with {@code @log} while this moment ran. */
    public record Note(String rule, String level, String message) {
    }

    private final List<Amendment> amendments = new ArrayList<>();
    private final List<Note>      notes      = new ArrayList<>();

    public void note(String rule, String level, String message) {
        notes.add(new Note(rule, level, message));
    }

    public List<Note> notes() {
        return Collections.unmodifiableList(notes);
    }

    /** What a rule may change at this moment. */
    public abstract Set<String> amendableFields();

    /**
     * The one rule this moment is for, by its document name — or {@code null} for every rule assigned
     * here.
     *
     * <p>⚠️ A step in a declared sequence runs ITS rule and no other: a list of twelve jMS steps is not
     * twelve rules each asked twelve times whether it cares.</p>
     */
    public String onlyRule() {
        return null;
    }

    public void amend(String rule, String field, Object value, String reason) {
        if (!amendableFields().contains(field)) {
            throw new IllegalArgumentException(
                    "'%s' cannot be changed here; this moment accepts %s.".formatted(field, amendableFields()));
        }

        amendments.add(new Amendment(rule, field, value,
                                     reason == null || reason.isBlank() ? "no reason given" : reason));
    }

    /** In the order the rules asked. ⚠️ A later amendment of the same field wins. */
    public List<Amendment> amendments() {
        return Collections.unmodifiableList(amendments);
    }

}
