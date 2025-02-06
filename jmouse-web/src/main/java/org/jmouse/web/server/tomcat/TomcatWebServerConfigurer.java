package org.jmouse.web.server.tomcat;

import jakarta.servlet.ServletContainerInitializer;
import org.apache.catalina.Context;
import org.apache.catalina.connector.Connector;
import org.apache.catalina.startup.Tomcat;
import org.jmouse.web.server.WebServer;

import java.io.File;
import java.util.Collections;


/**
 * Configures an Apache Tomcat server instance. Implements the {@link WebServer.Configurer} interface
 * to adjust various properties of the Tomcat server (e.g., port, document base directory, etc.).
 *
 * <p>Example usage:
 * <pre>{@code
 * Tomcat tomcat = new Tomcat();
 *
 * TomcatWebServerConfigurer configurer = new TomcatWebServerConfigurer(9090,
 *      new DefaultServletContainerInitializer(), WebInitializerClass.class);
 *
 * configurer.setDocument("/path/to/docBase");
 * configurer.setContextRoot("/myApp");
 * configurer.configure(tomcat);
 *
 * tomcat.start();
 * }</pre>
 */
public class TomcatWebServerConfigurer implements WebServer.Configurer<Tomcat> {

    /**
     * The default document base directory.
     */
    public static final String DOCUMENT_DEFAULT = ".";

    /**
     * The default context path.
     */
    public static final String CONTEXT_DEFAULT = "";

    /**
     * The default temporary directory name.
     */
    public static final String TEMPORARY_DEFAULT = "ApacheTomcatTmp";

    /**
     * The default port on which Tomcat listens.
     */
    public static final int DEFAULT_PORT = 8080;

    private final ServletContainerInitializer containerInitializer;
    private       int                         port;
    private       String                      document    = DOCUMENT_DEFAULT;
    private       String                      contextRoot = CONTEXT_DEFAULT;
    private       String                      temporary   = TEMPORARY_DEFAULT;

    /**
     * Constructs a {@code TomcatWebServerConfigurer} with a specified port,
     * {@link ServletContainerInitializer}, and optional initializer classes.
     *
     * @param port                the port on which Tomcat listens
     * @param containerInitializer the {@link ServletContainerInitializer} to configure the servlet container
     */
    public TomcatWebServerConfigurer(int port, ServletContainerInitializer containerInitializer) {
        this.containerInitializer = containerInitializer;
        this.port = port;
    }

    /**
     * Configures the Tomcat server by:
     * <ul>
     *     <li>Setting its base directory using {@link #createTemporaryDirectory(int)}.</li>
     *     <li>Creating a new {@link Context} with the given context root and document base.</li>
     *     <li>Setting the port on the {@link Connector}.</li>
     *     <li>Registering the provided {@link ServletContainerInitializer} and initializer classes.</li>
     * </ul>
     *
     * @param server the {@link Tomcat} server instance to configure
     */
    @Override
    public void configure(Tomcat server) {
        server.setBaseDir(createTemporaryDirectory(getPort()));

        Context   context   = server.addContext(getContextRoot(), createDocumentBaseDirectory());
        Connector connector = server.getConnector();

        connector.setPort(port);
        context.addServletContainerInitializer(containerInitializer, Collections.emptySet());
    }

    /**
     * Retrieves the current port.
     *
     * @return the port number
     */
    public int getPort() {
        return port;
    }

    /**
     * Sets the port for the Tomcat server.
     *
     * @param port the port to use
     */
    public void setPort(int port) {
        this.port = port;
    }

    /**
     * Retrieves the document base directory.
     *
     * @return the document base directory as a string
     */
    public String getDocument() {
        return document;
    }

    /**
     * Sets the document base directory for the Tomcat server.
     *
     * @param document the document base directory path
     */
    public void setDocument(String document) {
        this.document = document;
    }

    /**
     * Retrieves the current context root.
     *
     * @return the context root as a string
     */
    public String getContextRoot() {
        return contextRoot;
    }

    /**
     * Sets the context root for the Tomcat server.
     *
     * @param contextRoot the context path (e.g., "/app")
     */
    public void setContextRoot(String contextRoot) {
        this.contextRoot = contextRoot;
    }

    /**
     * Retrieves the temporary directory path used by the Tomcat server.
     *
     * @return the temporary directory path
     */
    public String getTemporary() {
        return temporary;
    }

    /**
     * Sets the temporary directory path for the Tomcat server.
     *
     * @param temporary the path to the temporary directory
     */
    public void setTemporary(String temporary) {
        this.temporary = temporary;
    }

    /**
     * Creates an absolute path from the document base setting.
     *
     * @return the absolute path to the document base directory
     */
    private String createDocumentBaseDirectory() {
        return new File(getDocument()).getAbsolutePath();
    }

    /**
     * Creates an absolute path from the temporary directory setting.
     *
     * @param port the port for creating temporary directory name
     *
     * @return the absolute path to the temporary directory
     */
    private String createTemporaryDirectory(int port) {
        return new File(getTemporary() + "-" + port).getAbsolutePath();
    }
}
