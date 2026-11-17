package org.jmouse.core.parameters;

import java.util.List;

/**
 * ⚙️ How {@link RequestParametersTreeParser} builds a tree out of names: how they may be spelled, what happens where two of them
 * disagree on a place, what the tree's root may be, and which values stand for an empty record or list.
 *
 * @param spelling  how many ways a name may say where it stands
 * @param conflict  what a place two names disagree on becomes
 * @param root      what the tree's root may be
 * @param emptiness the values that stand for an empty record and an empty list, if any do
 */
public record RequestParametersTreeOptions(RequestParametersSpelling spelling, Conflict conflict, Root root, Emptiness emptiness) {

    /** What a request's parameters are read with: any spelling, the last name standing, a record at the root, no emptiness. */
    public static RequestParametersTreeOptions defaults() {
        return new RequestParametersTreeOptions(RequestParametersSpelling.ANY, Conflict.LAST_WINS, Root.RECORD, Emptiness.NONE);
    }

    /**
     * What a place two names disagree on becomes — one says a value, another more below it; one a record, another a list.
     *
     * <ul>
     *   <li>{@link #LAST_WINS} — the later name's shape replaces the earlier's, and what stood there is gone.</li>
     *   <li>{@link #REFUSE} — the names are refused, naming the one that disagrees.</li>
     * </ul>
     */
    public enum Conflict {
        LAST_WINS,
        REFUSE
    }

    /**
     * What the tree's root may be.
     *
     * <ul>
     *   <li>{@link #RECORD} — always a record: a name that begins with an index is not a parameter of it, and is passed over.</li>
     *   <li>{@link #RECORD_OR_LIST} — a list where the names begin with an index ({@code [0].ending}).</li>
     * </ul>
     */
    public enum Root {
        RECORD,
        RECORD_OR_LIST
    }

    /**
     * The values that stand for an empty record and an empty list — a name can say nothing else of a place with nothing in it. An
     * empty place gives way to what another name puts there.
     *
     * @param record the value of an empty record, or null where none stands for one
     * @param list   the value of an empty list, or null where none stands for one
     */
    public record Emptiness(String record, String list) {

        /** No value stands for emptiness: an empty record or list has no name, and so is not in a tree read from names. */
        public static final Emptiness NONE = new Emptiness(null, null);

        /** Whether the values a name holds say an empty record. */
        boolean saysRecord(List<String> values) {
            return record != null && values.size() == 1 && record.equals(values.getFirst());
        }

        /** Whether the values a name holds say an empty list. */
        boolean saysList(List<String> values) {
            return list != null && values.size() == 1 && list.equals(values.getFirst());
        }
    }
}
