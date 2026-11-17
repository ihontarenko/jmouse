package org.jmouse.settings;

import java.util.Map;
import java.util.Set;

/**
 * 🗄️ Where an application keeps its settings' values: one row a parameter — its holder, its section's owner and path, its key, its
 * value —, never a section's declaration. A value may be null (a structured section's null); a row that is gone is a parameter the
 * holder keeps none of.
 */
public interface ParameterStore {

    /** The values a holder keeps of a section, by key. */
    Map<String, String> kept(SettingHolder holder, String owner, String section);

    /** Keeps values of a holder's section, each under its key — all of them or none. */
    void keep(SettingHolder holder, String owner, String section, Map<String, String> values);

    /** Forgets values of a holder's section: the rows of those keys are gone. */
    void forget(SettingHolder holder, String owner, String section, Set<String> keys);

    /** A holder's section as these values alone, every other row of it gone — all of it or none. */
    void replace(SettingHolder holder, String owner, String section, Map<String, String> values);

    /** The values a holder keeps of a section and of every section below it — `interface` and `interface/editor/grid` —, by section, then key. */
    Map<String, Map<String, String>> keptUnder(SettingHolder holder, String owner, String section);
}
