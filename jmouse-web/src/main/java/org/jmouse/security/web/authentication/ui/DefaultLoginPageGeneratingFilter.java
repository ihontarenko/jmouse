package org.jmouse.security.web.authentication.ui;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.jmouse.web.http.HttpMethod;
import org.jmouse.web.http.request.RequestContext;
import org.jmouse.web.mvc.View;
import org.jmouse.web.mvc.view.internal.InternalView;
import org.jmouse.web.servlet.filter.BeanFilter;
import org.jmouse.security.web.RequestMatcher;

import java.io.IOException;

import static java.util.Objects.requireNonNull;

/**
 * 🧩 DefaultLoginPageGeneratingFilter
 *
 * Auto-generates a minimal login page on GET {loginPageUrl}.
 * Posts credentials to {actionUrl}.
 */
public final class DefaultLoginPageGeneratingFilter implements BeanFilter {

    private final String         actionUrl;
    private final String         usernameParameter;
    private final String         passwordParameter;
    private final RequestMatcher requestMatcher;

    public DefaultLoginPageGeneratingFilter(
            View view,
            RequestMatcher requestMatcher,
            String action,
            String usernameParameter,
            String passwordParameter
    ) {
        this.requestMatcher = requestMatcher;
        this.actionUrl = requireNonNull(action);
        this.usernameParameter = (usernameParameter != null ? usernameParameter : "username");
        this.passwordParameter = (passwordParameter != null ? passwordParameter : "password");
    }

    @Override
    public void doFilterInternal(RequestContext requestContext, FilterChain chain) throws IOException, ServletException {
        HttpServletRequest  request = requestContext.request();
        HttpServletResponse response = requestContext.response();

        if (!requestMatcher.matches(request)) {
            chain.doFilter(request, response);
            return;
        }

        boolean error  = request.getParameter("error")  != null;
        boolean logout = request.getParameter("logout") != null;

        response.setStatus(HttpServletResponse.SC_OK);
        response.setContentType("text/html;charset=UTF-8");
        response.getWriter().write(renderHtml(error, logout));
    }

    private String renderHtml(boolean error, boolean logout) {
        // very small, dependency-free markup (intentionally plain)
        return """
        <!doctype html>
        <html lang="en">
        <head>
          <meta charset="utf-8" />
          <meta name="viewport" content="width=device-width,initial-scale=1" />
          <title>Sign in • jMouse</title>
          <style>
            body{margin:0;height:100vh;display:flex;align-items:center;justify-content:center;background:#0b1020;font-family:system-ui,Segoe UI,Roboto,Ubuntu,Cantarell,Arial,sans-serif}
            .card{background:#fff;width:360px;padding:28px;border-radius:16px;box-shadow:0 6px 24px rgba(0,0,0,.15)}
            h1{margin:0 0 16px;font-size:22px}
            .field{margin-bottom:12px}
            label{display:block;font-size:12px;margin-bottom:6px;color:#333}
            input{width:100%;padding:10px;border:1px solid #d0d5dd;border-radius:10px;outline:none}
            button{width:100%;padding:10px;border:0;border-radius:10px;cursor:pointer;background:#0ea5e9;color:#fff;font-weight:600}
            .note{font-size:12px;color:#6b7280;margin-top:12px}
            .alert{padding:10px;border-radius:8px;margin-bottom:12px}
            .error{background:#fee2e2;color:#991b1b;border:1px solid #fecaca}
            .ok{background:#dcfce7;color:#166534;border:1px solid #bbf7d0}
          </style>
        </head>
        <body>
          <form class="card" method="post" action="%s">
            <h1>jMouse — Sign in</h1>
            %s
            %s
            <div class="field">
              <label for="u">Username</label>
              <input id="u" name="%s" type="text" autocomplete="username" required>
            </div>
            <div class="field">
              <label for="p">Password</label>
              <input id="p" name="%s" type="password" autocomplete="current-password" required>
            </div>
            <button type="submit">Sign in</button>
            <div class="note">Default login page (auto-generated). Replace it with your own whenever you want.</div>
          </form>
        </body>
        </html>
        """.formatted(
                actionUrl,
                error ? "<div class='alert error'>Invalid credentials</div>" : "",
                logout ? "<div class='alert ok'>You have been signed out</div>" : "",
                usernameParameter, passwordParameter
        );
    }
}
