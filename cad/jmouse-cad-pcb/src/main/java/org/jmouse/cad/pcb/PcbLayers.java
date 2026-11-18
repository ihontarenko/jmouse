package org.jmouse.cad.pcb;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * A pcb's layers by their ids, as its language names them: a side's layer of a purpose ({@code top-silkscreen}), the copper stack
 * from the top down ({@code top}, {@code inner-1} …, {@code bottom}), and the side opposite another.
 */
public final class PcbLayers {

    /**
     * Each count's copper layers, listed once: a pcb's stack is asked for every pad and via on every layer as its nets and nodes are
     * found, and listing it afresh each time — a list, a name made for each inner layer — was most of what finding them cost.
     */
    private static final Map<Integer, List<String>> COPPER_STACKS = new ConcurrentHashMap<>();

    private PcbLayers() {
    }

    /** The side opposite a side. */
    public static String otherSide(String side) {
        return PcbVocabulary.TOP.equals(side) ? PcbVocabulary.BOTTOM : PcbVocabulary.TOP;
    }

    /** A side's layer of a purpose: {@code top-silkscreen}, {@code bottom-mask}. */
    public static String layerOf(String side, String purpose) {
        return side + PcbVocabulary.LAYER_JOINER + purpose;
    }

    /** A side's silkscreen layer: {@code top-silkscreen}. */
    public static String silkscreen(String side) {
        return layerOf(side, PcbVocabulary.SILKSCREEN);
    }

    /** An inner copper layer's id, counted from the top: {@code inner-1}. */
    public static String innerCopper(int number) {
        return PcbVocabulary.INNER_COPPER + PcbVocabulary.LAYER_JOINER + number;
    }

    /** Whether a copper layer is one between the outer two: {@code inner-1}. */
    public static boolean isInnerCopper(String layer) {
        return layer.startsWith(PcbVocabulary.INNER_COPPER + PcbVocabulary.LAYER_JOINER);
    }

    /** The copper layers of a stack of this many, from the top down; a stack of one is copper on the bottom alone. */
    public static List<String> copperStackOf(int count) {
        return COPPER_STACKS.computeIfAbsent(count, PcbLayers::listedStackOf);
    }

    private static List<String> listedStackOf(int count) {
        if (count == 1) {
            return List.of(PcbVocabulary.BOTTOM);
        }
        List<String> layers = new ArrayList<>();
        layers.add(PcbVocabulary.TOP);
        for (int inner = 1; inner <= count - 2; inner++) {
            layers.add(innerCopper(inner));
        }
        layers.add(PcbVocabulary.BOTTOM);
        return List.copyOf(layers);
    }
}
