package org.jmouse.ipc2581;

import org.jmouse.settings.Label;
import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.JAXBElement;
import jakarta.xml.bind.JAXBException;
import jakarta.xml.bind.Marshaller;
import org.glassfish.jaxb.runtime.marshaller.NamespacePrefixMapper;
import org.jmouse.ipc2581.schema.*;

import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.datatype.DatatypeFactory;
import javax.xml.datatype.XMLGregorianCalendar;
import java.io.StringWriter;
import java.math.BigInteger;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.GregorianCalendar;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * A printed board and its assembly as one IPC-2581 revision C file, composed part by part in any order and written by
 * JAXB from the classes generated out of the standard's own schema ({@code src/main/xsd/IPC-2581C.xsd}): the order of the
 * sections, which parts are required and what each may hold are the schema's, never this class's. Units are millimetres,
 * Y upwards; the shapes pads and flashes use are written once into the dictionary and named where they are used; every
 * name is written unique where the standard keys it ({@link Ipc2581Names}). What a board is stays the caller's: this
 * knows the format.
 */
public final class Ipc2581Composer {

    private static final String NAMESPACE = "http://webstds.ipc.org/2581";
    private static final String SCHEMA    = NAMESPACE + " http://webstds.ipc.org/2581/IPC-2581C.xsd";
    private static final String ROLE      = "Owner";

    /** How many places a millimetre is written to: a nanometre, what no arithmetic leaves worth saying beyond. */
    private static final double PLACES = 1e6;

    private static final ObjectFactory ELEMENTS = new ObjectFactory();
    private static final JAXBContext   CONTEXT  = context();

    /** A side as the file says it. */
    public enum Side { TOP, BOTTOM, INTERNAL, ALL }

    /** Whether a hole is plated, as the file says it. */
    public enum Plating { PLATED, NONPLATED, VIA }

    /**
     * Who the file is from and what wrote it.
     *
     * @param enterprise the enterprise the file is from
     * @param person     the person it is from
     * @param software   the software that wrote it
     * @param vendor     who makes that software
     * @param revision   the software's revision
     */
    public record Sender(@Label("Enterprise") String enterprise,
                         @Label("Person") String person,
                         @Label("Software") String software,
                         @Label("Vendor") String vendor,
                         @Label("Revision") String revision) {
    }

    /** @param function the layer's function as the standard names it: {@code CONDUCTOR}, {@code SOLDERMASK} … */
    public record Layer(String name, String function, Side side) {
    }

    public record StackupLayer(String layer, double thickness) {
    }

    /** A pin of a package, where it stands in the package's own frame, and the shape of its pad. */
    public record Pin(String number, boolean through, Ipc2581Point location, Ipc2581Shape shape) {
    }

    /** A part's package as the board seats it: its outline in its own frame, and its pins. */
    public record Package(String name, String pinOne, Ipc2581Polygon outline, List<Pin> pins) {
    }

    /**
     * A part where it stands.
     *
     * @param rotation degrees, counter-clockwise seen from the top
     * @param mirrored on the bottom: seen through the board
     */
    public record Component(String designator, String packageName, String part, String layer, boolean through, double rotation,
                            boolean mirrored, Ipc2581Point location) {
    }

    /** One line of the bill of materials: a part number, how many of it, and where each stands. */
    public record BomItem(String partNumber, String description, int pinCount, List<String> designators,
                          Map<String, String> characteristics) {
    }

    /** A pin of a part on a net. */
    public record PinReference(String designator, String pin) {
    }

    /** A pad of a part where it stands on a layer, and the pin it is. */
    public record PlacedPad(Ipc2581Shape shape, Ipc2581Point location, PinReference pin) {
    }

    private final String                    design;
    private final String                    step;
    private final Sender                    sender;
    private final OffsetDateTime            written;
    private final Ipc2581Names              names = new Ipc2581Names();
    private final List<LayerType>           layers = new ArrayList<>();
    private StackupType                     stackup;
    private ContourType                     profile;
    private final Map<String, PackageType>  packages = new LinkedHashMap<>();
    private final List<Component>           components = new ArrayList<>();
    private final List<BomItem>             bom = new ArrayList<>();
    private final Map<String, LogicalNetType> nets = new LinkedHashMap<>();
    private final Map<String, LayerFeatureType> features = new LinkedHashMap<>();
    private final Map<Ipc2581Shape, String> shapes = new LinkedHashMap<>();
    private final DictionaryStandardType    dictionary = new DictionaryStandardType();

    /**
     * @param design  what the design is called
     * @param step    what the board's step is called
     * @param written when the file is written, as its history says
     */
    public Ipc2581Composer(String design, String step, Sender sender, OffsetDateTime written) {
        this.design = design;
        this.step = step;
        this.sender = sender;
        this.written = written;
        dictionary.setUnits(UnitsType.MILLIMETER);
    }

    /** A layer of the board, in the order of the stack from the top. */
    public Ipc2581Composer layer(Layer layer) {
        layers.add(layerOf(layer.name(), layer.function(), layer.side()));
        return this;
    }

    /** A layer of holes drilled from one layer to another, both included. */
    public Ipc2581Composer drillLayer(String name, String from, String to) {
        LayerType drill = layerOf(name, LayerFunctionType.DRILL.value(), Side.ALL);
        SpanType span = new SpanType();
        span.setFromLayer(names.name(from));
        span.setToLayer(names.name(to));
        drill.setSpan(span);
        layers.add(drill);
        return this;
    }

    /** The stack's layers from the top, each as thick as it is, and the whole as thick as the board — proposed, not approved. */
    public Ipc2581Composer stackup(double thickness, List<StackupLayer> stacked) {
        stackup = new StackupType();
        stackup.setName(names.name("stackup"));
        stackup.setOverallThickness(rounded(thickness));
        stackup.setWhereMeasured(WhereMeasuredType.METAL);
        stackup.setStackupStatus(StackupStatusType.PROPOSED);
        StackupGroupType group = new StackupGroupType();
        group.setName(names.name("stackup-group"));
        group.setThickness(rounded(thickness));
        for (int index = 0; index < stacked.size(); index++) {
            StackupLayerType layer = new StackupLayerType();
            layer.setLayerOrGroupRef(names.name(stacked.get(index).layer()));
            layer.setThickness(rounded(stacked.get(index).thickness()));
            layer.setSequence((double) (index + 1));
            group.getStackupLayer().add(layer);
        }
        stackup.getStackupGroup().add(group);
        return this;
    }

    /** The board's edge: its outline, its cut-outs. */
    public Ipc2581Composer profile(Ipc2581Polygon edge) {
        profile = contourOf(edge);
        return this;
    }

    /** A package, once for every part that is seated on it; where its pin one stands is worked out from its pins. */
    public Ipc2581Composer packageOf(Package seated) {
        if (packages.containsKey(seated.name())) {
            return this;
        }
        PackageType element = new PackageType();
        element.setName(names.name(seated.name()));
        element.setType(PackageTypeType.OTHER);
        element.setPinOne(seated.pinOne());
        element.setPinOneOrientation(pinOneOrientation(seated));
        OutlineType outline = new OutlineType();
        outline.setPolygon(polygonOf(seated.outline().outline()));
        outline.setLineDescGroup(ELEMENTS.createLineDesc(pen(0)));
        element.setOutline(outline);
        for (Pin pin : seated.pins()) {
            PinType written = new PinType();
            written.setNumber(pin.number());
            written.setType(pin.through() ? CadPinType.THRU : CadPinType.SURFACE);
            written.setElectricalType(PinElectricalType.ELECTRICAL);
            written.setLocation(locationOf(pin.location()));
            written.setStandardShape(ELEMENTS.createStandardPrimitiveRef(shapeReference(pin.shape())));
            element.getPin().add(written);
        }
        packages.put(seated.name(), element);
        return this;
    }

    public Ipc2581Composer component(Component component) {
        components.add(component);
        return this;
    }

    public Ipc2581Composer bomItem(BomItem item) {
        bom.add(item);
        return this;
    }

    /** A pin on a net; a net's pins are written in the order they are named. */
    public Ipc2581Composer netPin(String net, PinReference pin) {
        nets.computeIfAbsent(net, name -> {
            LogicalNetType logical = new LogicalNetType();
            logical.setName(names.name(name));
            return logical;
        }).getPinRef().add(pinReferenceOf(pin));
        return this;
    }

    /** A part's pad on a copper layer, on its net, as the pin it is. */
    public Ipc2581Composer pad(String layer, String net, PlacedPad pad) {
        PadType element = new PadType();
        element.setLocation(locationOf(pad.location()));
        element.setFeature(ELEMENTS.createStandardPrimitiveRef(shapeReference(pad.shape())));
        if (pad.pin() != null) {
            element.setPinRef(pinReferenceOf(pad.pin()));
        }
        set(layer, net).getNonstandardAttributeOrPadOrFiducial().add(ELEMENTS.createPad(element));
        return this;
    }

    /** A shape flashed where it stands on a layer — a mask's opening, paste, a via's land —, on a net or none. */
    public Ipc2581Composer flash(String layer, String net, Ipc2581Shape shape, Ipc2581Point location) {
        FeaturesType flashed = new FeaturesType();
        flashed.getLocation().add(locationOf(location));
        flashed.setFeature(ELEMENTS.createStandardPrimitiveRef(shapeReference(shape)));
        set(layer, net).getNonstandardAttributeOrPadOrFiducial().add(ELEMENTS.createFeatures(flashed));
        return this;
    }

    /** What is drawn where it is on a layer — tracks, strokes, areas —, on a net or none. */
    public Ipc2581Composer graphics(String layer, String net, List<Ipc2581Graphic> graphics) {
        if (graphics.isEmpty()) {
            return this;
        }
        UserSpecialType special = new UserSpecialType();
        graphics.forEach(graphic -> special.getFeature().add(featureOf(graphic)));
        FeaturesType drawn = new FeaturesType();
        drawn.getLocation().add(locationOf(new Ipc2581Point(0, 0)));
        drawn.setFeature(ELEMENTS.createUserSpecial(special));
        set(layer, net).getNonstandardAttributeOrPadOrFiducial().add(ELEMENTS.createFeatures(drawn));
        return this;
    }

    /** A round hole of a drill layer. */
    public Ipc2581Composer hole(String layer, String net, String name, double diameter, Plating plating, Ipc2581Point at) {
        HoleType hole = new HoleType();
        hole.setName(names.name(name));
        hole.setDiameter(rounded(diameter));
        hole.setPlatingStatus(PlatingStatusType.valueOf(plating.name()));
        hole.setX(rounded(at.x()));
        hole.setY(rounded(at.y()));
        set(layer, net).getNonstandardAttributeOrPadOrFiducial().add(ELEMENTS.createHole(hole));
        return this;
    }

    /**
     * A slot of a drill layer: an oval {@code length} long and {@code width} wide, its middle at {@code at}, turned
     * {@code rotation} degrees counter-clockwise from lying along X.
     */
    public Ipc2581Composer slot(String layer, String net, String name, Plating plating, Ipc2581Point at, double length, double width,
                                double rotation) {
        SlotCavityType slot = new SlotCavityType();
        slot.setName(names.name(name));
        slot.setPlatingStatus(PlatingStatusType.valueOf(plating.name()));
        slot.setLocation(locationOf(at));
        if (rotation != 0) {
            XformType turned = new XformType();
            turned.setRotation(rounded(rotation));
            slot.setXform(turned);
        }
        slot.setFeature(ELEMENTS.createOval(ovalOf(length, width)));
        set(layer, net).getNonstandardAttributeOrPadOrFiducial().add(ELEMENTS.createSlotCavity(slot));
        return this;
    }

    /** The file. */
    public String write() {
        IPC2581Type root = new IPC2581Type();
        root.setRevision("C");
        root.setContent(content());
        root.setLogisticHeader(logistics());
        root.setHistoryRecord(history());
        if (!bom.isEmpty()) {
            root.getBom().add(billOfMaterials());
        }
        root.setEcad(ecad());
        StringWriter text = new StringWriter();
        try {
            Marshaller marshaller = CONTEXT.createMarshaller();
            marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true);
            marshaller.setProperty(Marshaller.JAXB_ENCODING, "UTF-8");
            marshaller.setProperty(Marshaller.JAXB_SCHEMA_LOCATION, SCHEMA);
            marshaller.setProperty("org.glassfish.jaxb.namespacePrefixMapper", new DefaultNamespace());
            marshaller.marshal(ELEMENTS.createIPC2581(root), text);
        } catch (JAXBException failure) {
            throw new IllegalStateException("The IPC-2581 file could not be written", failure);
        }
        return text.toString();
    }

    private ContentType content() {
        ContentType content = new ContentType();
        content.setRoleRef(ROLE);
        FunctionModeType mode = new FunctionModeType();
        mode.setMode(ModeType.FABRICATION);
        content.setFunctionMode(mode);
        StepRefType stepReference = new StepRefType();
        stepReference.setName(names.name(step));
        content.getStepRef().add(stepReference);
        for (LayerType layer : layers) {
            LayerRefType reference = new LayerRefType();
            reference.setName(layer.getName());
            content.getLayerRef().add(reference);
        }
        if (!bom.isEmpty()) {
            BomRefType reference = new BomRefType();
            reference.setName(names.name(bomName()));
            content.getBomRef().add(reference);
        }
        content.setDictionaryColor(new DictionaryColorType());
        DictionaryLineDescType lines = new DictionaryLineDescType();
        lines.setUnits(UnitsType.MILLIMETER);
        content.setDictionaryLineDesc(lines);
        DictionaryFillDescType fills = new DictionaryFillDescType();
        fills.setUnits(UnitsType.MILLIMETER);
        content.setDictionaryFillDesc(fills);
        content.setDictionaryStandard(dictionary);
        DictionaryUserType user = new DictionaryUserType();
        user.setUnits(UnitsType.MILLIMETER);
        content.setDictionaryUser(user);
        return content;
    }

    private LogisticHeaderType logistics() {
        LogisticHeaderType header = new LogisticHeaderType();
        RoleType role = new RoleType();
        role.setId(ROLE);
        role.setRoleFunction(RoleFunctionType.SENDER);
        header.getRole().add(role);
        EnterpriseType enterprise = new EnterpriseType();
        enterprise.setId(sender.enterprise());
        enterprise.setCode("NONE");
        header.getEnterprise().add(enterprise);
        PersonType person = new PersonType();
        person.setName(sender.person());
        person.setEnterpriseRef(sender.enterprise());
        person.setRoleRef(ROLE);
        header.getPerson().add(person);
        return header;
    }

    private HistoryRecordType history() {
        XMLGregorianCalendar moment = calendarOf(written);
        HistoryRecordType record = new HistoryRecordType();
        record.setNumber("1");
        record.setOrigination(moment);
        record.setSoftware(sender.software());
        record.setLastChange(moment);
        FileRevisionType revision = new FileRevisionType();
        revision.setFileRevisionId("1");
        revision.setComment("");
        revision.setLabel("");
        SoftwarePackageType software = new SoftwarePackageType();
        software.setName(sender.software());
        software.setRevision(sender.revision());
        software.setVendor(sender.vendor());
        CertificationType certification = new CertificationType();
        certification.setCertificationStatus(CertificationStatusType.SELFTEST);
        software.getCertification().add(certification);
        revision.setSoftwarePackage(software);
        record.setFileRevision(revision);
        return record;
    }

    private String bomName() {
        return design + "-bom";
    }

    private BomType billOfMaterials() {
        BomType element = new BomType();
        element.setName(names.name(bomName()));
        BomHeaderType header = new BomHeaderType();
        header.setAssembly(design);
        header.setRevision("1");
        StepRefType stepReference = new StepRefType();
        stepReference.setName(names.name(step));
        header.getStepRef().add(stepReference);
        element.setBomHeader(header);
        for (BomItem item : bom) {
            BomItemType line = new BomItemType();
            line.setOEMDesignNumberRef(names.of(Ipc2581Names.Kind.PART_NUMBER, item.partNumber()));
            line.setQuantity(String.valueOf(item.designators().size()));
            line.setPinCount(BigInteger.valueOf(item.pinCount()));
            line.setCategory(BomCategoryType.ELECTRICAL);
            line.setDescription(item.description());
            for (String designator : item.designators()) {
                Component placed = components.stream().filter(component -> component.designator().equals(designator)).findFirst()
                        .orElseThrow(() -> new IllegalStateException("The bill of materials names " + designator + ", no component"));
                RefDesType reference = new RefDesType();
                reference.setName(names.of(Ipc2581Names.Kind.DESIGNATOR, designator));
                reference.setPackageRef(names.name(placed.packageName()));
                reference.setPopulate(true);
                reference.setLayerRef(names.name(placed.layer()));
                line.getBomDes().add(ELEMENTS.createRefDes(reference));
            }
            CharacteristicsType characteristics = new CharacteristicsType();
            characteristics.setCategory(BomCategoryType.ELECTRICAL);
            item.characteristics().forEach((name, value) -> {
                TextualType textual = new TextualType();
                textual.setDefinitionSource(sender.software());
                textual.setTextualCharacteristicName(name);
                textual.setTextualCharacteristicValue(value);
                characteristics.getTextual().add(textual);
            });
            line.setCharacteristics(characteristics);
            element.getBomItem().add(line);
        }
        return element;
    }

    private EcadType ecad() {
        EcadType ecad = new EcadType();
        ecad.setName(design);
        CadHeaderType header = new CadHeaderType();
        header.setUnits(UnitsType.MILLIMETER);
        ecad.setCadHeader(header);
        CadDataType data = new CadDataType();
        data.getLayer().addAll(layers);
        if (stackup != null) {
            data.getStackup().add(stackup);
        }
        StepType board = new StepType();
        board.setName(names.name(step));
        board.setType(StepKind.BOARD);
        board.setDatum(locationOf(new Ipc2581Point(0, 0)));
        board.setProfile(profile);
        board.getPackage().addAll(packages.values());
        for (Component component : components) {
            ComponentType element = new ComponentType();
            element.setRefDes(names.of(Ipc2581Names.Kind.DESIGNATOR, component.designator()));
            element.setPackageRef(names.name(component.packageName()));
            element.setPart(names.of(Ipc2581Names.Kind.PART_NUMBER, component.part()));
            element.setLayerRef(names.name(component.layer()));
            element.setMountType(component.through() ? MountType.THMT : MountType.SMT);
            if (component.rotation() != 0 || component.mirrored()) {
                XformType turned = new XformType();
                turned.setRotation(rounded(component.rotation()));
                if (component.mirrored()) {
                    turned.setMirror(true);
                }
                element.setXform(turned);
            }
            element.setLocation(locationOf(component.location()));
            board.getComponent().add(element);
        }
        board.getLogicalNet().addAll(nets.values());
        board.getLayerFeature().addAll(features.values());
        data.getStep().add(board);
        ecad.setCadData(data);
        return ecad;
    }

    private LayerType layerOf(String name, String function, Side side) {
        LayerType layer = new LayerType();
        layer.setName(names.name(name));
        layer.setLayerFunction(LayerFunctionType.fromValue(function));
        layer.setSide(SideType.valueOf(side.name()));
        layer.setPolarity(PolarityType.POSITIVE);
        return layer;
    }

    /** A set of one net's features on a layer: the layer's features gathered under it in its order. */
    private SetType set(String layer, String net) {
        if (layers.stream().noneMatch(declared -> declared.getName().equals(names.name(layer)))) {
            throw new IllegalArgumentException("No layer " + layer + " was declared");
        }
        SetType set = new SetType();
        if (net != null) {
            set.setNet(names.name(net));
        }
        features.computeIfAbsent(layer, name -> {
            LayerFeatureType feature = new LayerFeatureType();
            feature.setLayerRef(names.name(name));
            return feature;
        }).getSet().add(set);
        return set;
    }

    /** A reference to the shape in the dictionary, written into it the first time it is used. */
    private StandardPrimitiveRefType shapeReference(Ipc2581Shape shape) {
        String id = shapes.computeIfAbsent(shape, unseen -> {
            String named = names.name("SHAPE_" + (shapes.size() + 1));
            EntryStandardType entry = new EntryStandardType();
            entry.setId(named);
            entry.setStandardPrimitive(primitiveOf(unseen));
            dictionary.getEntryStandard().add(entry);
            return named;
        });
        StandardPrimitiveRefType reference = new StandardPrimitiveRefType();
        reference.setId(id);
        return reference;
    }

    private static JAXBElement<?> primitiveOf(Ipc2581Shape shape) {
        return switch (shape) {
            case Ipc2581Shape.Circle circle -> {
                CircleType element = new CircleType();
                element.setDiameter(rounded(circle.diameter()));
                yield ELEMENTS.createCircle(element);
            }
            case Ipc2581Shape.RectCenter rectangle -> {
                RectCenterType element = new RectCenterType();
                element.setWidth(rounded(rectangle.width()));
                element.setHeight(rounded(rectangle.height()));
                yield ELEMENTS.createRectCenter(element);
            }
            case Ipc2581Shape.RectRound rounded -> {
                RectRoundType element = new RectRoundType();
                element.setWidth(rounded(rounded.width()));
                element.setHeight(rounded(rounded.height()));
                element.setRadius(rounded(rounded.radius()));
                element.setUpperRight(true);
                element.setUpperLeft(true);
                element.setLowerRight(true);
                element.setLowerLeft(true);
                yield ELEMENTS.createRectRound(element);
            }
            case Ipc2581Shape.Oval oval -> ELEMENTS.createOval(ovalOf(oval.width(), oval.height()));
            case Ipc2581Shape.Contour contour -> ELEMENTS.createContour(contourOf(contour.polygon()));
        };
    }

    private static JAXBElement<?> featureOf(Ipc2581Graphic graphic) {
        return switch (graphic) {
            case Ipc2581Graphic.Line line -> {
                LineType element = new LineType();
                element.setStartX(rounded(line.from().x()));
                element.setStartY(rounded(line.from().y()));
                element.setEndX(rounded(line.to().x()));
                element.setEndY(rounded(line.to().y()));
                element.setLineDescGroup(ELEMENTS.createLineDesc(pen(line.width())));
                yield ELEMENTS.createLine(element);
            }
            case Ipc2581Graphic.Arc arc -> {
                ArcType element = new ArcType();
                element.setStartX(rounded(arc.from().x()));
                element.setStartY(rounded(arc.from().y()));
                element.setEndX(rounded(arc.to().x()));
                element.setEndY(rounded(arc.to().y()));
                element.setCenterX(rounded(arc.centre().x()));
                element.setCenterY(rounded(arc.centre().y()));
                element.setClockwise(arc.clockwise());
                element.setLineDescGroup(ELEMENTS.createLineDesc(pen(arc.width())));
                yield ELEMENTS.createArc(element);
            }
            case Ipc2581Graphic.Polyline polyline -> {
                PolylineType element = new PolylineType();
                element.setPolyBegin(beginOf(polyline.points().getFirst()));
                polyline.points().stream().skip(1).forEach(point -> element.getPolyStep().add(stepOf(point)));
                element.setLineDescGroup(ELEMENTS.createLineDesc(pen(polyline.width())));
                yield ELEMENTS.createPolyline(element);
            }
            case Ipc2581Graphic.Area area -> ELEMENTS.createContour(contourOf(area.polygon()));
        };
    }

    /** Where pin one stands round the middle of the package's pins, as the standard says it; the middle when it is alone. */
    private static PinOneOrientationType pinOneOrientation(Package seated) {
        Pin first = seated.pins().stream().filter(pin -> pin.number().equals(seated.pinOne())).findFirst().orElse(null);
        if (first == null || seated.pins().size() < 2) {
            return PinOneOrientationType.CENTER;
        }
        double left = seated.pins().stream().mapToDouble(pin -> pin.location().x()).min().orElse(0);
        double right = seated.pins().stream().mapToDouble(pin -> pin.location().x()).max().orElse(0);
        double bottom = seated.pins().stream().mapToDouble(pin -> pin.location().y()).min().orElse(0);
        double top = seated.pins().stream().mapToDouble(pin -> pin.location().y()).max().orElse(0);
        int across = Double.compare(Math.round((first.location().x() - (left + right) / 2) * PLACES), 0);
        int up = Double.compare(Math.round((first.location().y() - (bottom + top) / 2) * PLACES), 0);
        return switch (up) {
            case 1 -> across < 0 ? PinOneOrientationType.UPPER_LEFT : across > 0 ? PinOneOrientationType.UPPER_RIGHT
                    : PinOneOrientationType.UPPER_CENTER;
            case -1 -> across < 0 ? PinOneOrientationType.LOWER_LEFT : across > 0 ? PinOneOrientationType.LOWER_RIGHT
                    : PinOneOrientationType.LOWER_CENTER;
            default -> across < 0 ? PinOneOrientationType.LEFT_CENTER : across > 0 ? PinOneOrientationType.RIGHT_CENTER
                    : PinOneOrientationType.CENTER;
        };
    }

    private static ContourType contourOf(Ipc2581Polygon polygon) {
        ContourType contour = new ContourType();
        contour.setPolygon(polygonOf(polygon.outline()));
        polygon.cutouts().forEach(cutout -> contour.getCutout().add(polygonOf(cutout)));
        return contour;
    }

    /** A ring begun, stepped along, and closed on its first point. */
    private static PolygonType polygonOf(List<Ipc2581Point> ring) {
        PolygonType polygon = new PolygonType();
        polygon.setPolyBegin(beginOf(ring.getFirst()));
        ring.stream().skip(1).forEach(point -> polygon.getPolyStep().add(stepOf(point)));
        polygon.getPolyStep().add(stepOf(ring.getFirst()));
        return polygon;
    }

    private static PolyBeginType beginOf(Ipc2581Point point) {
        PolyBeginType begin = new PolyBeginType();
        begin.setX(rounded(point.x()));
        begin.setY(rounded(point.y()));
        return begin;
    }

    private static JAXBElement<PolyStepSegmentType> stepOf(Ipc2581Point point) {
        PolyStepSegmentType step = new PolyStepSegmentType();
        step.setX(rounded(point.x()));
        step.setY(rounded(point.y()));
        return ELEMENTS.createPolyStepSegment(step);
    }

    private static OvalType ovalOf(double width, double height) {
        OvalType oval = new OvalType();
        oval.setWidth(rounded(width));
        oval.setHeight(rounded(height));
        return oval;
    }

    private static LineDescType pen(double width) {
        LineDescType pen = new LineDescType();
        pen.setLineWidth(rounded(width));
        pen.setLineEnd(LineEndType.ROUND);
        return pen;
    }

    private static LocationType locationOf(Ipc2581Point point) {
        LocationType location = new LocationType();
        location.setX(rounded(point.x()));
        location.setY(rounded(point.y()));
        return location;
    }

    private PinRefType pinReferenceOf(PinReference pin) {
        PinRefType reference = new PinRefType();
        reference.setComponentRef(names.of(Ipc2581Names.Kind.DESIGNATOR, pin.designator()));
        reference.setPin(pin.pin());
        return reference;
    }

    /** A figure — a length, an angle — rounded to the file's places, and never a negative zero. */
    private static double rounded(double value) {
        double rounded = Math.round(value * PLACES) / PLACES;
        return rounded == 0 ? 0 : rounded;
    }

    private static XMLGregorianCalendar calendarOf(OffsetDateTime moment) {
        try {
            return DatatypeFactory.newInstance().newXMLGregorianCalendar(GregorianCalendar.from(moment.toZonedDateTime()));
        } catch (DatatypeConfigurationException failure) {
            throw new IllegalStateException("The JDK has no XML date factory", failure);
        }
    }

    private static JAXBContext context() {
        try {
            return JAXBContext.newInstance(ObjectFactory.class);
        } catch (JAXBException failure) {
            throw new IllegalStateException("The IPC-2581 classes cannot be bound", failure);
        }
    }

    /** The standard's namespace as the file's default: {@code <IPC-2581 xmlns="…">}, not a prefix on every element. */
    private static final class DefaultNamespace extends NamespacePrefixMapper {

        @Override
        public String getPreferredPrefix(String namespace, String suggestion, boolean required) {
            return NAMESPACE.equals(namespace) ? "" : suggestion;
        }
    }
}
