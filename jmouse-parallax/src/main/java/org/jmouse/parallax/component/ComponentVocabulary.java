package org.jmouse.parallax.component;

import org.jmouse.parallax.object.ObjectDefinition.BenchConditions;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * The names the engine gives every component document, beside its own pins, states and parameters: what its equations,
 * its heat law and its instruments may read without declaring. A document may not declare any of them itself.
 *
 * <ul>
 *   <li>{@link #TEMPERATURE} — the part's temperature, kelvin: the bench's, raised on a realistic bench by the part's
 *       own heat.</li>
 *   <li>{@link #REALISTIC} — 1 on a realistic bench, 0 on an ideal one.</li>
 *   <li>{@link #TOLERANCE_CORNER} — which corner of its spread a part stands at: −1, 0 or +1.</li>
 *   <li>{@link #DETAIL} — the bench's level of detail: 0 draft, 1 standard, 2 precise; what it changes is the
 *       document's to say.</li>
 *   <li>{@link #TIME} — the circuit's time, seconds.</li>
 *   <li>{@link #POWER} — what a heat law and an instrument read: the power the part takes, watts.</li>
 * </ul>
 */
public final class ComponentVocabulary {

    public static final String TEMPERATURE      = "temperature";
    public static final String REALISTIC        = "realistic";
    public static final String TOLERANCE_CORNER = "tolerance_corner";
    public static final String DETAIL           = "detail";
    public static final String TIME             = "time";
    public static final String POWER            = "power";

    private ComponentVocabulary() {
    }

    /**
     * The bench's figures as a nominal bench sets them — 25 °C in kelvin, ideal, every part at its value —, in the order
     * every part's parameters begin with. A bench that is otherwise overrides them on each part.
     */
    public static Map<String, Double> nominalBench() {
        Map<String, Double> figures = new LinkedHashMap<>();
        figures.put(TEMPERATURE, PhysicalConstants.kelvin(BenchConditions.NOMINAL_TEMPERATURE));
        figures.put(REALISTIC, 0.0);
        figures.put(TOLERANCE_CORNER, 0.0);
        figures.put(DETAIL, (double) BenchConditions.Detail.STANDARD.figure());
        return figures;
    }

    /** Whether a name is one the engine gives, which a document may not declare. */
    public static boolean isReserved(String name) {
        return name.equals(TEMPERATURE) || name.equals(REALISTIC) || name.equals(TOLERANCE_CORNER) || name.equals(DETAIL)
                || name.equals(TIME) || name.equals(POWER);
    }
}
