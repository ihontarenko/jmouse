package org.jmouse.core.parameters;

/**
 * ✒️ How many ways a name may say where it stands.
 *
 * <ul>
 *   <li>{@link #ANY} — every way the grammar reads ({@code filter[admin]} as well as {@code filter.admin}, {@code groups[]}): what a
 *       request sends.</li>
 *   <li>{@link #ONE} — only as {@link RequestParametersPathWriter} writes it, and only a place ({@code []} names none): what a store
 *       keeps, where two spellings of one place would be two rows of it.</li>
 * </ul>
 */
public enum RequestParametersSpelling {
    ANY,
    ONE
}
