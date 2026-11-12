package org.jmouse.eagle;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;

/**
 * Reads an Eagle file — a {@code .sch} or a {@code .brd}, the XML Eagle 6 and later write — into its model. Nothing it
 * names is fetched: the {@code eagle.dtd} it declares is not loaded, and no entity outside the file is read.
 */
public final class EagleReader {

    private EagleReader() {
    }

    /**
     * The file's design.
     *
     * @param name what the file is called, as a refusal names it
     * @throws EagleReadException when it is no Eagle XML, or a value in it is not one Eagle writes
     */
    public static EagleDesign read(String name, String text) {
        Element eagle = parse(name, text).getDocumentElement();
        if (!"eagle".equals(eagle.getTagName())) {
            throw new EagleReadException(name + " is no Eagle file: its root is <" + eagle.getTagName() + ">, not <eagle>");
        }
        Element drawing = child(eagle, "drawing")
                .orElseThrow(() -> new EagleReadException(name + " holds no <drawing>"));
        try {
            return new EagleDesign(eagle.getAttribute("version"), child(drawing, "schematic").map(EagleReader::schematic),
                    child(drawing, "board").map(EagleReader::board));
        } catch (NumberFormatException unreadable) {
            throw new EagleReadException(name + ": a number in it is not one: " + unreadable.getMessage(), unreadable);
        }
    }

    private static Document parse(String name, String text) {
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
            factory.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false);
            factory.setFeature("http://xml.org/sax/features/external-general-entities", false);
            factory.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
            factory.setExpandEntityReferences(false);
            factory.setValidating(false);
            DocumentBuilder builder = factory.newDocumentBuilder();
            builder.setErrorHandler(null);
            return builder.parse(new InputSource(new StringReader(text)));
        } catch (ParserConfigurationException | SAXException | IOException unreadable) {
            throw new EagleReadException(name + " does not read as XML: " + unreadable.getMessage(), unreadable);
        }
    }

    // ─── a schematic ──────────────────────────────────────────────────────────────────────────────

    private static EagleSchematic schematic(Element schematic) {
        List<EagleSchematic.Part> parts = all(schematic, "parts", "part", part -> new EagleSchematic.Part(
                part.getAttribute("name"), part.getAttribute("library"), part.getAttribute("deviceset"), part.getAttribute("device"),
                part.getAttribute("technology"), part.hasAttribute("value") ? part.getAttribute("value") : null, attributes(part)));
        List<EagleSchematic.Sheet> sheets = all(schematic, "sheets", "sheet", sheet -> new EagleSchematic.Sheet(
                all(sheet, "instances", "instance", instance -> new EagleSchematic.Instance(instance.getAttribute("part"),
                        instance.getAttribute("gate"), number(instance, "x"), number(instance, "y"), rotation(instance))),
                all(sheet, "busses", "bus", bus -> new EagleSchematic.Bus(bus.getAttribute("name"),
                        children(bus, "segment").stream().flatMap(segment -> children(segment, "wire").stream().map(EagleReader::wire)).toList())),
                all(sheet, "nets", "net", net -> new EagleSchematic.Net(net.getAttribute("name"),
                        children(net, "segment").stream().map(EagleReader::segment).toList())),
                child(sheet, "plain").map(EagleReader::graphics).orElse(List.of())));
        return new EagleSchematic(libraries(schematic), parts, sheets);
    }

    private static EagleSchematic.Segment segment(Element segment) {
        return new EagleSchematic.Segment(
                children(segment, "pinref").stream().map(pin -> new EagleSchematic.PinReference(pin.getAttribute("part"),
                        pin.getAttribute("gate"), pin.getAttribute("pin"))).toList(),
                children(segment, "wire").stream().map(EagleReader::wire).toList(),
                children(segment, "junction").stream().map(EagleReader::point).toList(),
                children(segment, "label").stream().map(EagleReader::point).toList());
    }

    // ─── a board ──────────────────────────────────────────────────────────────────────────────────

    private static EagleBoard board(Element board) {
        List<EagleBoard.Element> elements = all(board, "elements", "element", element -> new EagleBoard.Element(
                element.getAttribute("name"), element.getAttribute("library"), element.getAttribute("package"),
                element.getAttribute("value"), number(element, "x"), number(element, "y"), rotation(element)));
        List<EagleBoard.Signal> signals = all(board, "signals", "signal", signal -> new EagleBoard.Signal(
                signal.getAttribute("name"),
                children(signal, "contactref").stream().map(contact -> new EagleBoard.ContactReference(
                        contact.getAttribute("element"), contact.getAttribute("pad"))).toList(),
                children(signal, "wire").stream().map(EagleReader::wire).toList(),
                children(signal, "via").stream().map(via -> new EagleBoard.Via(number(via, "x"), number(via, "y"),
                        number(via, "drill"), numberOr(via, "diameter", 0), via.getAttribute("extent"))).toList(),
                children(signal, "polygon").stream().map(EagleReader::polygon).toList()));
        return new EagleBoard(libraries(board), child(board, "plain").map(EagleReader::graphics).orElse(List.of()), elements,
                signals);
    }

    // ─── libraries ────────────────────────────────────────────────────────────────────────────────

    private static List<EagleLibrary> libraries(Element holder) {
        return all(holder, "libraries", "library", EagleReader::library);
    }

    private static EagleLibrary library(Element library) {
        Map<String, EagleLibrary.Package> packages = named(all(library, "packages", "package", EagleReader::packageOf),
                EagleLibrary.Package::name);
        Map<String, EagleLibrary.Symbol> symbols = named(all(library, "symbols", "symbol", symbol -> new EagleLibrary.Symbol(
                symbol.getAttribute("name"), graphics(symbol), children(symbol, "pin").stream().map(EagleReader::pin).toList())),
                EagleLibrary.Symbol::name);
        Map<String, EagleLibrary.DeviceSet> deviceSets = named(all(library, "devicesets", "deviceset", EagleReader::deviceSet),
                EagleLibrary.DeviceSet::name);
        return new EagleLibrary(library.getAttribute("name"), packages, symbols, deviceSets);
    }

    private static EagleLibrary.Package packageOf(Element found) {
        return new EagleLibrary.Package(found.getAttribute("name"), child(found, "description").map(Node::getTextContent).orElse(""),
                children(found, "pad").stream().map(pad -> new EagleLibrary.Pad(pad.getAttribute("name"), number(pad, "x"),
                        number(pad, "y"), number(pad, "drill"), numberOr(pad, "diameter", 0),
                        word(pad, "shape", EagleLibrary.Pad.Shape.ROUND),
                        rotation(pad))).toList(),
                children(found, "smd").stream().map(smd -> new EagleLibrary.Smd(smd.getAttribute("name"), number(smd, "x"),
                        number(smd, "y"), number(smd, "dx"), number(smd, "dy"), (int) number(smd, "layer"),
                        numberOr(smd, "roundness", 0), rotation(smd))).toList(),
                children(found, "hole").stream().map(hole -> new EagleLibrary.Hole(number(hole, "x"), number(hole, "y"),
                        number(hole, "drill"))).toList(),
                graphics(found));
    }

    private static EagleLibrary.Pin pin(Element pin) {
        return new EagleLibrary.Pin(pin.getAttribute("name"), number(pin, "x"), number(pin, "y"),
                word(pin, "length", EagleLibrary.Pin.Length.LONG), word(pin, "direction", EagleLibrary.Pin.Direction.IO),
                word(pin, "function", EagleLibrary.Pin.Function.NONE), rotation(pin),
                word(pin, "visible", EagleLibrary.Pin.Visibility.BOTH));
    }

    private static EagleLibrary.DeviceSet deviceSet(Element deviceSet) {
        List<EagleLibrary.Gate> gates = all(deviceSet, "gates", "gate", gate -> new EagleLibrary.Gate(gate.getAttribute("name"),
                gate.getAttribute("symbol"), number(gate, "x"), number(gate, "y")));
        List<EagleLibrary.Device> devices = all(deviceSet, "devices", "device", device -> {
            Map<String, Map<String, String>> technologies = new LinkedHashMap<>();
            for (Element technology : all(device, "technologies", "technology", found -> found)) {
                technologies.put(technology.getAttribute("name"), attributes(technology));
            }
            return new EagleLibrary.Device(device.getAttribute("name"),
                    device.hasAttribute("package") ? device.getAttribute("package") : null,
                    all(device, "connects", "connect", connect -> new EagleLibrary.Connect(connect.getAttribute("gate"),
                            connect.getAttribute("pin"), List.of(connect.getAttribute("pad").trim().split("\\s+")))),
                    technologies);
        });
        return new EagleLibrary.DeviceSet(deviceSet.getAttribute("name"), deviceSet.getAttribute("prefix"),
                "yes".equals(deviceSet.getAttribute("uservalue")), child(deviceSet, "description").map(Node::getTextContent).orElse(""),
                gates, devices);
    }

    // ─── what is drawn ────────────────────────────────────────────────────────────────────────────

    /** Every wire, circle, rectangle, polygon and text drawn directly in an element, in the order written. */
    private static List<EagleGraphic> graphics(Element holder) {
        List<EagleGraphic> graphics = new ArrayList<>();
        for (Element found : elements(holder)) {
            switch (found.getTagName()) {
                case "wire" -> graphics.add(wire(found));
                case "circle" -> graphics.add(new EagleGraphic.Circle(number(found, "x"), number(found, "y"), number(found, "radius"),
                        number(found, "width"), (int) number(found, "layer")));
                case "rectangle" -> graphics.add(new EagleGraphic.Rectangle(number(found, "x1"), number(found, "y1"),
                        number(found, "x2"), number(found, "y2"), (int) number(found, "layer"), rotation(found)));
                case "polygon" -> graphics.add(polygon(found));
                case "text" -> graphics.add(new EagleGraphic.Text(number(found, "x"), number(found, "y"), number(found, "size"),
                        (int) number(found, "layer"), rotation(found), found.getTextContent()));
                default -> {
                    // Pads, pins and the like are read by their own holders; a frame or a dimension is not drawn here.
                }
            }
        }
        return graphics;
    }

    private static EagleGraphic.Wire wire(Element wire) {
        return new EagleGraphic.Wire(number(wire, "x1"), number(wire, "y1"), number(wire, "x2"), number(wire, "y2"),
                number(wire, "width"), (int) number(wire, "layer"), numberOr(wire, "curve", 0));
    }

    private static EagleGraphic.Polygon polygon(Element polygon) {
        return new EagleGraphic.Polygon(children(polygon, "vertex").stream().map(vertex -> new EagleGraphic.Vertex(
                number(vertex, "x"), number(vertex, "y"), numberOr(vertex, "curve", 0))).toList(),
                number(polygon, "width"), (int) number(polygon, "layer"));
    }

    private static EagleSchematic.Point point(Element at) {
        return new EagleSchematic.Point(number(at, "x"), number(at, "y"));
    }

    // ─── reading the tree ─────────────────────────────────────────────────────────────────────────

    /** {@code <attribute name value/>} children, by name. */
    private static Map<String, String> attributes(Element holder) {
        Map<String, String> attributes = new LinkedHashMap<>();
        children(holder, "attribute").forEach(attribute -> attributes.put(attribute.getAttribute("name"), attribute.getAttribute("value")));
        return attributes;
    }

    /** Every {@code item} of the {@code group} child — {@code <parts><part/>…</parts>} —, each read. */
    private static <T> List<T> all(Element holder, String group, String item, Function<Element, T> reading) {
        return child(holder, group).map(found -> children(found, item).stream().map(reading).toList()).orElse(List.of());
    }

    private static <T> Map<String, T> named(List<T> items, Function<T, String> name) {
        Map<String, T> named = new LinkedHashMap<>();
        items.forEach(item -> named.put(name.apply(item), item));
        return named;
    }

    private static Optional<Element> child(Element holder, String tag) {
        return elements(holder).stream().filter(element -> element.getTagName().equals(tag)).findFirst();
    }

    private static List<Element> children(Element holder, String tag) {
        return elements(holder).stream().filter(element -> element.getTagName().equals(tag)).toList();
    }

    private static List<Element> elements(Element holder) {
        List<Element> elements = new ArrayList<>();
        for (Node node = holder.getFirstChild(); node != null; node = node.getNextSibling()) {
            if (node instanceof Element element) {
                elements.add(element);
            }
        }
        return elements;
    }

    private static double number(Element element, String attribute) {
        String written = element.getAttribute(attribute);
        if (written.isEmpty()) {
            throw new EagleReadException("<" + element.getTagName() + "> writes no " + attribute);
        }
        return Double.parseDouble(written);
    }

    private static double numberOr(Element element, String attribute, double otherwise) {
        return element.hasAttribute(attribute) ? Double.parseDouble(element.getAttribute(attribute)) : otherwise;
    }

    /** One of Eagle's words for an attribute — {@code dotclk}, {@code sup} —, the constant spelled so; Eagle's default when none. */
    private static <E extends Enum<E>> E word(Element element, String attribute, E otherwise) {
        if (!element.hasAttribute(attribute)) {
            return otherwise;
        }
        String written = element.getAttribute(attribute);
        try {
            return Enum.valueOf(otherwise.getDeclaringClass(), written.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException unknown) {
            throw new EagleReadException("'" + written + "' is no " + attribute + " Eagle writes for a " + element.getTagName());
        }
    }

    private static EagleRotation rotation(Element element) {
        return EagleRotation.of(element.getAttribute("rot"));
    }
}
