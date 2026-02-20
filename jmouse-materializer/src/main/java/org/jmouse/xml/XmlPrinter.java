package org.jmouse.xml;

import org.w3c.dom.Node;

import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.StringWriter;

public final class XmlPrinter {

    private XmlPrinter() {
    }

    public static String toString(Node node) {
        try {
            TransformerFactory factory     = TransformerFactory.newInstance();
            Transformer        transformer = factory.newTransformer();
            StringWriter       writer      = new StringWriter();

            transformer.transform(new DOMSource(node), new StreamResult(writer));

            return writer.toString();
        } catch (Exception exception) {
            throw new IllegalStateException("Cannot serialize XML", exception);
        }
    }

    public static String toPrettyString(Node node) {
        try {
            TransformerFactory factory = TransformerFactory.newInstance();
            StringWriter       writer  = new StringWriter();

            Transformer transformer = factory.newTransformer();
            transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "no");
            transformer.setOutputProperty(OutputKeys.METHOD, "xml");
            transformer.setOutputProperty(OutputKeys.INDENT, "yes");
            transformer.setOutputProperty(OutputKeys.ENCODING, "UTF-8");

            transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "2");
            transformer.transform(new DOMSource(node), new StreamResult(writer));

            return writer.toString();
        } catch (Exception exception) {
            throw new IllegalStateException("Cannot serialize XML", exception);
        }
    }

}