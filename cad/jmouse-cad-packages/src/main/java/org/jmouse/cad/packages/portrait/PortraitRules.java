package org.jmouse.cad.packages.portrait;

import org.jmouse.cad.packages.PackageTable;
import org.jmouse.sheet.Sheet;
import org.jmouse.sheet.SheetEntry;
import org.jmouse.sheet.SheetField;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * How a package is drawn from above, read from {@code package-portrait-rules.sht}: the grid one pitch becomes, the shares
 * of the body its corner, notch, dot and marking take, how each form is mounted, and the properties each element is
 * painted with — so a change of taste is one edit there and a regeneration.
 *
 * @param grid          the sheet's grid step one pitch of the package is drawn as
 * @param shortestLead  how far a lead reaches past the body at the least, so even a short gull wing is seen
 * @param corner        a moulded body's corner, as a share of its narrower half
 * @param notch         a dual in-line body's notch at its first end, as a share of its half width
 * @param dot           the moulded dot beside pin 1, as a share of the body's half width
 * @param dotInset      how many of its own radii the dot stands off the body's edges
 * @param markingWidth  the printed marking's cell across, as a share of the body's length
 * @param markingHeight the printed marking's cell high, as a share of the body's half width
 * @param markingInput  the input the part's component gives the marking's words by
 * @param mountings     how each form drawn is mounted, by the form a package table names: its tag
 * @param styles        the properties each element is painted with, by the element they style
 */
public record PortraitRules(double grid, double shortestLead, double corner, double notch, double dot, double dotInset,
                            double markingWidth, double markingHeight, String markingInput, Map<String, String> mountings,
                            Map<String, List<Property>> styles) {

    /** The field of an entry that makes it a form, rather than a style. */
    private static final String MOUNTING = "mounting";

    public PortraitRules {
        mountings = Map.copyOf(mountings);
        styles = Map.copyOf(styles);
    }

    /** A property an element is painted with — {@code fill silver} —, its value as the rules write it. */
    public record Property(String name, String written) {
    }

    public static PortraitRules of(Sheet sheet) {
        PackageTable head = PackageTable.head(sheet);
        Map<String, String> mountings = new LinkedHashMap<>();
        Map<String, List<Property>> styles = new LinkedHashMap<>();
        for (SheetEntry entry : sheet.entries()) {
            Optional<SheetField> mounting = entry.field(MOUNTING);
            if (mounting.isPresent()) {
                mountings.put(entry.id(), new PackageTable(entry).word(MOUNTING));
            } else {
                styles.put(entry.id(), entry.fields().stream().map(field -> new Property(field.name(), field.value().written())).toList());
            }
        }
        return new PortraitRules(head.number("grid"), head.number("shortest-lead"), head.number("corner"), head.number("notch"),
                head.number("dot"), head.number("dot-inset"), head.number("marking-width"), head.number("marking-height"),
                head.word("marking-input"), mountings, styles);
    }

    /** The properties an element is painted with; refused when the rules style no such element. */
    public List<Property> style(String element) {
        List<Property> style = styles.get(element);
        if (style == null) {
            throw new IllegalArgumentException("The portrait rules style no '" + element + "'; they style " + styles.keySet());
        }
        return style;
    }

    /** How a form is mounted, when the rules draw it. */
    public Optional<String> mounting(String form) {
        return Optional.ofNullable(mountings.get(form));
    }
}
