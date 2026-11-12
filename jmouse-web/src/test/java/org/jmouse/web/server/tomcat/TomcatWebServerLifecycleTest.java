package org.jmouse.web.server.tomcat;

import static org.junit.jupiter.api.Assertions.*;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Path;
import java.time.Duration;
import org.apache.catalina.LifecycleState;
import org.apache.catalina.startup.Tomcat;
import org.jmouse.web.server.WebServerException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class TomcatWebServerLifecycleTest {

    @TempDir
    Path directory;

    @Test
    void realHttpSupportsIdempotentStartAndTerminalStopReleasesPort() throws Exception {
        TomcatWebServer server = configured(
            new HttpServlet() {
                protected void doGet(HttpServletRequest request, HttpServletResponse response)
                    throws IOException {
                    response.getWriter().write("ready");
                }
            }
        );
        int port;
        try (server; HttpClient client = HttpClient.newHttpClient()) {
            server.start();
            port = server.port();
            assertEquals("ready", get(client, port).body());
            server.start();
            assertEquals(port, server.port());
            server.stop();
            assertThrows(WebServerException.class, server::start);
        }
        try (ServerSocket socket = new ServerSocket(port)) {
            assertEquals(port, socket.getLocalPort());
        }
        server.close();
        assertThrows(WebServerException.class, server::start);
        assertEquals(LifecycleState.DESTROYED, server.server().getServer().getState());
    }

    @Test
    void failedContextStartupDestroysTheServer() {
        TomcatWebServer server = configured(
            new HttpServlet() {
                public void init() throws ServletException {
                    throw new ServletException("intentional startup failure");
                }
            }
        );
        assertThrows(WebServerException.class, server::start);
        assertEquals(LifecycleState.DESTROYED, server.server().getServer().getState());
        server.close();
    }

    private TomcatWebServer configured(HttpServlet servlet) {
        TomcatWebServer server = new TomcatWebServer();
        server.server().setBaseDir(directory.toString());
        server.server().setPort(0);
        var context = server.server().addContext("", directory.toString());
        ((org.apache.catalina.core.StandardContext) context).setFailCtxIfServletStartFails(true);
        var wrapper = Tomcat.addServlet(context, "test", servlet);
        wrapper.setLoadOnStartup(1);
        context.addServletMappingDecoded("/", "test");
        return server;
    }

    private HttpResponse<String> get(HttpClient client, int port) throws Exception {
        return client.send(
            HttpRequest.newBuilder(URI.create("http://localhost:" + port + "/"))
                .timeout(Duration.ofSeconds(5))
                .GET()
                .build(),
            HttpResponse.BodyHandlers.ofString()
        );
    }
}
