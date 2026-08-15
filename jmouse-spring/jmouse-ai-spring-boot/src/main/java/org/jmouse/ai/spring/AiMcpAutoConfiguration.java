package org.jmouse.ai.spring;

import io.modelcontextprotocol.server.McpSyncServer;
import io.modelcontextprotocol.server.transport.HttpServletStreamableServerTransportProvider;
import io.modelcontextprotocol.spec.McpStreamableServerTransportProvider;
import jakarta.servlet.http.HttpServlet;
import org.jmouse.ai.ToolDispatcher;
import org.jmouse.ai.mcp.McpEndpointAgreement;
import org.jmouse.ai.mcp.McpToolServer;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.web.servlet.ServletRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Serving the tool catalogue to clients outside this application.
 *
 * <p>⚠️ <strong>Off unless asked for</strong>, and having the module on the classpath is not asking.
 * Switching this on publishes every action in the catalogue to whatever can authenticate against the
 * endpoint, which is not a thing to acquire by adding a dependency — a product may well take
 * {@code jmouse-ai-mcp} only to <em>connect</em> to somebody else's server.
 *
 * <p>Nothing here is a second implementation of anything. The protocol server is a transport over
 * {@link ToolDispatcher}, so a call arriving over the wire passes exactly the identity, permission,
 * scope and guard steps an in-app assistant's call passes, and is recorded by the same trail.
 *
 * <h2>The check that runs before anything is served</h2>
 *
 * <p>{@link McpEndpointAgreement} compares where the protocol is served against where the product's
 * authentication confines a credential to, and refuses to start when they differ. That is worth a
 * startup failure because the alternative is silent: the protocol ends up reachable at a path
 * authentication was never configured to cover, which moves the whole feature outside its security
 * boundary <em>without failing, logging, or looking wrong from either side</em>.
 */
@AutoConfiguration
@ConditionalOnClass(McpToolServer.class)
@ConditionalOnProperty(name = "jmouse.ai.protocol.enabled", havingValue = "true")
@EnableConfigurationProperties(AiProperties.class)
public class AiMcpAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public McpToolServer aiMcpToolServer(ToolDispatcher dispatcher, AiProperties properties) {
        AiProperties.Protocol protocol = properties.getProtocol();

        return new McpToolServer(
                dispatcher,
                protocol.getServerName(),
                protocol.getServerVersion(),
                protocol.getInstructions());
    }

    /**
     * The HTTP half, present only in a servlet application.
     *
     * <p>Nested and conditional rather than guarded method by method, because every bean in it needs the
     * same two classes and a partially wired transport is worse than none: a provider with nothing
     * registering it is a server that answers 404 while the log says it started.
     */
    @Configuration(proxyBeanMethods = false)
    @ConditionalOnClass({HttpServlet.class, ServletRegistrationBean.class,
                         HttpServletStreamableServerTransportProvider.class})
    public static class ServletTransport {

        /**
         * Where the protocol is served.
         *
         * <p>⚠️ The endpoint agreement is checked here rather than anywhere more prominent, because
         * this is the bean that decides the answer — checking it somewhere that merely <em>reads</em>
         * the property would leave the check passing while the servlet was mapped somewhere else.
         */
        @Bean
        @ConditionalOnMissingBean
        public HttpServletStreamableServerTransportProvider aiMcpTransport(AiProperties properties) {
            AiProperties.Protocol protocol = properties.getProtocol();

            McpEndpointAgreement.require(protocol.getEndpoint(), protocol.getCredentialsConfinedTo());

            return HttpServletStreamableServerTransportProvider.builder()
                    .mcpEndpoint(protocol.getEndpoint())
                    .build();
        }

        /**
         * The protocol server, publishing the catalogue over that transport.
         *
         * <p>The tool list is built once, at startup, which is also when a name the protocol will not
         * accept would have been refused by the catalogue — a whole conversation earlier than the
         * client-side rejection it would otherwise surface as.
         */
        @Bean
        @ConditionalOnMissingBean
        public McpSyncServer aiMcpSyncServer(
                McpToolServer tools, McpStreamableServerTransportProvider transport) {

            return tools.serving(transport);
        }

        /**
         * Mapping the transport into the servlet container.
         *
         * <p>A registration bean rather than relying on a {@code Servlet} bean being picked up: Boot
         * maps an unregistered servlet bean to {@code /}, which would put the protocol at the root of
         * the application and past whatever the endpoint agreement just checked.
         */
        @Bean
        @ConditionalOnMissingBean(name = "aiMcpServletRegistration")
        public ServletRegistrationBean<HttpServletStreamableServerTransportProvider>
                aiMcpServletRegistration(
                        HttpServletStreamableServerTransportProvider transport,
                        AiProperties                                 properties) {

            ServletRegistrationBean<HttpServletStreamableServerTransportProvider> registration =
                    new ServletRegistrationBean<>(transport, properties.getProtocol().getEndpoint());

            registration.setName("jmouse-ai-mcp");
            registration.setAsyncSupported(true);

            return registration;
        }
    }
}
