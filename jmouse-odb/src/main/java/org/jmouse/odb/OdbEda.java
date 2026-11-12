package org.jmouse.odb;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * A step's {@code eda/data}: which layer features belong to which electrical net, and the packages the components are of.
 * Each net holds subnets — a trace, a via, a plane, a component's toeprint —, and each subnet the features it is drawn with,
 * by their number in their layer's {@code features} file ({@link OdbFeatures#count()}).
 */
public final class OdbEda {

    /** What a job calls a feature with no net: every {@code $NONE$} point may or may not be joined to another. */
    public static final String NO_NET = "$NONE$";

    /** A portion of a net. */
    public sealed interface Subnet {

        /** The record's words after {@code SNT}. */
        String written();
    }

    /** A collection of lines or arcs leading from point to point. */
    public record Trace() implements Subnet {
        @Override
        public String written() {
            return "TRC";
        }
    }

    /** A connectivity padstack between layers. */
    public record Via() implements Subnet {
        @Override
        public String written() {
            return "VIA";
        }
    }

    /** A solid surface used for connectivity. */
    public record Plane() implements Subnet {
        @Override
        public String written() {
            return "PLN S E 0";
        }
    }

    /**
     * A component's pin joined to the board.
     *
     * @param side      where the component stands
     * @param component its number in its side's {@code components} file
     * @param toeprint  the toeprint's number in that component
     */
    public record Toeprint(OdbComponents.Side side, int component, int toeprint) implements Subnet {
        @Override
        public String written() {
            return "TOP " + (side == OdbComponents.Side.TOP ? "T" : "B") + " " + component + " " + toeprint;
        }
    }

    /** What a feature is, for a link to it. */
    public enum FeatureType {
        COPPER("C"),
        HOLE("H");

        private final String code;

        FeatureType(String code) {
            this.code = code;
        }
    }

    /** How a package's pin goes through the board. */
    public enum PinType {
        THROUGH_HOLE("T"),
        SURFACE("S");

        private final String code;

        PinType(String code) {
            this.code = code;
        }
    }

    /** What a package's pin is electrically. */
    public enum PinElectrical {
        ELECTRICAL("E"),
        MECHANICAL("M");

        private final String code;

        PinElectrical(String code) {
            this.code = code;
        }
    }

    /** How a package's pin is mounted. */
    public enum PinMount {
        SURFACE("S"),
        THROUGH_HOLE("T"),
        HOLE("H");

        private final String code;

        PinMount(String code) {
            this.code = code;
        }
    }

    /** A pin's or a package's shape, in the package's frame, millimetres, Y upwards. */
    public sealed interface Outline {

        String written();
    }

    /** A rectangle, from its lower left corner. */
    public record Rectangle(double x, double y, double width, double height) implements Outline {
        @Override
        public String written() {
            return "RC " + OdbNumbers.millimetres(x) + " " + OdbNumbers.millimetres(y) + " " + OdbNumbers.millimetres(width) + " "
                   + OdbNumbers.millimetres(height);
        }
    }

    /** A circle. */
    public record Circle(double x, double y, double radius) implements Outline {
        @Override
        public String written() {
            return "CR " + OdbNumbers.millimetres(x) + " " + OdbNumbers.millimetres(y) + " " + OdbNumbers.millimetres(radius);
        }
    }

    /** A pin of a package, at its centre in the package's frame. */
    public record Pin(String name, PinType type, double x, double y, PinElectrical electrical, PinMount mount, Outline outline) {
    }

    /**
     * The generic shape of a component.
     *
     * @param pitch   the distance between the closest pins' centres; 0 with fewer than two
     * @param outline what it covers, in its own frame
     */
    public record Package(String name, double pitch, Rectangle outline, List<Pin> pins) {

        public Package {
            pins = List.copyOf(pins);
        }
    }

    private record NetRecord(String name, List<SubnetRecord> subnets) {
    }

    private record SubnetRecord(Subnet subnet, List<String> features) {
    }

    private final List<String>          layers;
    private final List<NetRecord>       nets      = new ArrayList<>();
    private final Map<String, Integer>  byNet     = new LinkedHashMap<>();
    private final List<Package>         packages  = new ArrayList<>();
    private final Map<String, Integer>  byPackage = new LinkedHashMap<>();

    /** @param layers the layers features are linked on, by name, as the matrix names them */
    public OdbEda(List<String> layers) {
        this.layers = List.copyOf(layers);
    }

    /** The number of a net by its name, a new one the first time; a name a job cannot hold is made one, kept apart from the others. */
    public int net(String name) {
        return byNet.computeIfAbsent(name == null ? NO_NET : name, ignored -> {
            nets.add(new NetRecord(OdbNames.unique(name == null ? NO_NET : name, nets.stream().map(NetRecord::name).toList()), new ArrayList<>()));
            return nets.size() - 1;
        });
    }

    /** @return the subnet's number in its net */
    public int subnet(int net, Subnet subnet) {
        List<SubnetRecord> subnets = nets.get(net).subnets();
        subnets.add(new SubnetRecord(Objects.requireNonNull(subnet, "subnet"), new ArrayList<>()));
        return subnets.size() - 1;
    }

    /**
     * Links a subnet to a feature.
     *
     * @param number the feature's number in its layer's {@code features} file
     */
    public void feature(int net, int subnet, FeatureType type, String layer, int number) {
        int index = layers.indexOf(layer);
        if (index < 0) {
            throw new IllegalArgumentException("The EDA data names no layer '" + layer + "'.");
        }
        nets.get(net).subnets().get(subnet).features().add("FID " + type.code + " " + index + " " + number);
    }

    /** The number of a package, kept the first time its name is given. */
    public int packageOf(Package written) {
        return byPackage.computeIfAbsent(written.name(), ignored -> {
            packages.add(written);
            return packages.size() - 1;
        });
    }

    /** @param source the system the data comes from */
    String text(String source) {
        StringBuilder text = new StringBuilder("HDR ").append(source).append("\nUNITS=MM\nLYR ").append(String.join(" ", layers)).append("\n#\n");
        for (int index = 0; index < nets.size(); index++) {
            NetRecord net = nets.get(index);
            text.append("# NET ").append(index).append("\nNET ").append(net.name()).append('\n');
            for (SubnetRecord subnet : net.subnets()) {
                text.append("SNT ").append(subnet.subnet().written()).append('\n');
                subnet.features().forEach(feature -> text.append(feature).append('\n'));
            }
        }
        for (int index = 0; index < packages.size(); index++) {
            Package written = packages.get(index);
            Rectangle box = written.outline();
            text.append("# PKG ").append(index).append("\nPKG ").append(OdbNames.word(written.name())).append(' ')
                    .append(OdbNumbers.millimetres(written.pitch())).append(' ')
                    .append(OdbNumbers.millimetres(box.x())).append(' ').append(OdbNumbers.millimetres(box.y())).append(' ')
                    .append(OdbNumbers.millimetres(box.x() + box.width())).append(' ').append(OdbNumbers.millimetres(box.y() + box.height())).append('\n')
                    .append(box.written()).append('\n');
            for (Pin pin : written.pins()) {
                text.append("PIN ").append(OdbNames.word(pin.name())).append(' ').append(pin.type().code).append(' ')
                        .append(OdbNumbers.millimetres(pin.x())).append(' ').append(OdbNumbers.millimetres(pin.y())).append(" 0 ")
                        .append(pin.electrical().code).append(' ').append(pin.mount().code).append('\n')
                        .append(pin.outline().written()).append('\n');
            }
        }
        return text.toString();
    }
}
