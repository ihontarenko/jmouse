package org.jmouse.web.mvc.servlet;

import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.jmouse.beans.annotation.Bean;
import org.jmouse.core.IdGenerator;
import org.jmouse.http.HttpHeader;
import org.jmouse.http.HttpStatus;
import org.jmouse.http.ETag;
import org.jmouse.http.IfNoneMatch;
import org.jmouse.web.mvc.ETagGenerator;
import org.jmouse.web.servlet.BufferingOnlyResponseWrapper;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

/**
 * 🧩 Servlet {@link Filter} that computes and adds a shallow <b>ETag</b> for 2xx responses.
 *
 * <p>Flow:</p>
 * <ol>
 *   <li>📦 Buffer the downstream response.</li>
 *   <li>🧮 Compute an ETag from the response content (shallow = payload-based hash).</li>
 *   <li>🔁 Compare against {@code If-None-Match}; return <b>304 Not Modified</b> on match.</li>
 *   <li>📤 Otherwise, write the buffered body and ETag header.</li>
 * </ol>
 *
 * <p>Notes:</p>
 * <ul>
 *   <li>✔️ Applies only to successful (2xx) responses that are not yet committed.</li>
 *   <li>🧰 Uses {@link BufferingOnlyResponseWrapper} to capture the body.</li>
 *   <li>🔖 Sets {@code ETag} header for client/proxy caching.</li>
 * </ul>
 */
@Bean
public class ShallowETagHeaderFilter implements Filter {

    /**
     * ⚙️ Buffer downstream output, compute shallow ETag, and handle conditional GET logic.
     *
     * <p>If the response is non-2xx or already committed → pass-through (flush buffer).
     * Otherwise → set {@code ETag} and emit <b>304</b> when {@code If-None-Match} matches;
     * else write the buffered body.</p>
     *
     * @throws IOException      on I/O errors
     * @throws ServletException on filter chain errors
     */
    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {

        if (!(request instanceof HttpServletRequest servletRequest)
                || !(response instanceof HttpServletResponse servletResponse)) {
            chain.doFilter(request, response);
            return;
        }

        BufferingOnlyResponseWrapper wrapper = new BufferingOnlyResponseWrapper(servletResponse);

        chain.doFilter(request, wrapper);

        HttpStatus httpStatus = HttpStatus.ofCode(servletResponse.getStatus());

        if (servletResponse.isCommitted() || !httpStatus.is2xx()) {
            wrapper.writeToResponse();
            return;
        }

        byte[]                    byteArray = wrapper.getByteArray();
        IdGenerator<ETag, String> generator = new ETagGenerator(true);
        ETag                      etag      = generator.generate(new String(byteArray, StandardCharsets.UTF_8));

        IfNoneMatch noneMatch = IfNoneMatch.parse(servletRequest.getHeader(
                HttpHeader.IF_NONE_MATCH.toString()
        )).toNoneMatch();

        servletResponse.setHeader(HttpHeader.ETAG.value(), etag.toHeaderValue());

        if (noneMatch.matches(etag, true, true)) {
            servletResponse.setStatus(HttpStatus.NOT_MODIFIED.getCode());
            return;
        }

        wrapper.writeToResponse();
    }
}
