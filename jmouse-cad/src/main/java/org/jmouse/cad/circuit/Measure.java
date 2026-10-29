package org.jmouse.cad.circuit;

import org.jmouse.cad.Formula;
import org.jmouse.cad.Setting;

import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * What an expectation measures: a statistic — {@code final}, {@code maximum}, {@code average}, {@code frequency}… — of a
 * subject — a net, {@code current R1}, {@code out - in} —, and when: {@code after 60 ms}, {@code above 2.5 V},
 * {@code at 1 kHz}, with settings changed for it.
 *
 * @param subject    the net or expression it reads, or null where it reads something else
 * @param reading    what else it reads — {@code current}, {@code level}, {@code levels}, {@code figure}, {@code figures} —,
 *                   or null for a subject
 * @param targets    what that reading names: a part, a net, a part's figures
 * @param conditions each condition's word and its value in SI: {@code after}, {@code above}, {@code below}, {@code at}
 */
public record Measure(String statistic, Formula subject, String reading, List<String> targets, Map<String, Double> conditions,
                      List<Setting> settings) {

    public Measure {
        targets = List.copyOf(targets);
        conditions = Map.copyOf(conditions);
        settings = List.copyOf(settings);
    }

    public Optional<Double> condition(String word) {
        return Optional.ofNullable(conditions.get(word));
    }
}
