package org.jmouse.security.web.configuration;

import org.jmouse.beans.BeanScope;
import org.jmouse.beans.InitializingBeanSupport;
import org.jmouse.beans.annotation.Bean;
import org.jmouse.beans.annotation.BeanFactories;
import org.jmouse.security.authentication.AuthenticationManager;
import org.jmouse.security.authentication.ResolversAuthenticationManager;
import org.jmouse.security.authentication.dao.DaoAuthenticationResolver;
import org.jmouse.security.web.SecurityFilterChainDelegatorRegistration;
import org.jmouse.security.web.configuration.builder.HttpSecurity;
import org.jmouse.web.context.WebBeanContext;

import java.util.List;

/**
 * 🧱 Core web-security configuration for jMouse Security.
 *
 * <p>Defines essential beans that integrate the {@link HttpSecurity}
 * builder into the {@link WebBeanContext} lifecycle.
 * Configures a default {@link AuthenticationManager} based on a
 * {@link ResolversAuthenticationManager} delegating to
 * {@link DaoAuthenticationResolver}.</p>
 *
 * <p>✨ <b>Highlights:</b></p>
 * <ul>
 *   <li>Exposes {@code httpSecurity} as a <b>prototype-scoped</b> bean.</li>
 *   <li>Registers {@link SecurityFilterChainDelegatorRegistration} to activate
 *       dynamic filter-chain assembly.</li>
 *   <li>Provides default security building blocks:
 *       <code>securityContext</code>, <code>exceptionHandling</code>, and
 *       <code>anonymous</code> sections initialized as no-ops.</li>
 * </ul>
 *
 * @see HttpSecurity
 * @see AuthenticationManager
 * @see WebBeanContext
 */
@BeanFactories
public class HttpSecurityConfiguration implements InitializingBeanSupport<WebBeanContext> {

    /**
     * ⚙️ Creates and configures a prototype-scoped {@link HttpSecurity} builder.
     *
     * <p>Injects shared objects such as:</p>
     * <ul>
     *   <li>{@link AuthenticationManager} — built via {@link ResolversAuthenticationManager}</li>
     *   <li>{@link WebBeanContext} — current web application context</li>
     * </ul>
     *
     * <p>Initializes default configurers as no-op placeholders
     * to ensure consistent builder state.</p>
     *
     * @param webBeanContext active web bean context
     * @return a fully initialized {@link HttpSecurity} builder instance
     */
    @Bean(scope = BeanScope.PROTOTYPE, value = "httpSecurity")
    public HttpSecurity httpSecurity(WebBeanContext webBeanContext) {
        HttpSecurity httpSecurity = new HttpSecurity();

        httpSecurity.setSharedObject(AuthenticationManager.class, new ResolversAuthenticationManager(
                List.of(new DaoAuthenticationResolver())
        ));
        httpSecurity.setSharedObject(WebBeanContext.class, webBeanContext);

        httpSecurity
                .securityContext(Customizer.noop())
                .exceptionHandling(Customizer.noop())
                .anonymous(Customizer.noop());

        return httpSecurity;
    }

    /**
     * 🔗 Registers the {@link SecurityFilterChainDelegatorRegistration} bean.
     *
     * <p>Responsible for dynamically delegating HTTP security chains during
     * application startup, allowing multiple independent filter chains
     * to coexist and apply based on request matching rules.</p>
     *
     * @return a singleton {@link SecurityFilterChainDelegatorRegistration}
     */
    @Bean
    public SecurityFilterChainDelegatorRegistration securityFilterChainDelegatorRegistration() {
        return new SecurityFilterChainDelegatorRegistration();
    }
}
