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
import org.jmouse.core.Sha256HashGenerator;
import org.jmouse.web.http.HttpHeader;
import org.jmouse.web.http.HttpStatus;
import org.jmouse.web.http.request.ETag;
import org.jmouse.web.http.request.IfNoneMatch;
import org.jmouse.web.servlet.BufferingResponseWrapper;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

@Bean
public class ShallowETagHeaderFilter implements Filter {

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {

        if (!(request instanceof HttpServletRequest servletRequest)
                || !(response instanceof HttpServletResponse servletResponse)) {
            chain.doFilter(request, response);
            return;
        }

        BufferingResponseWrapper wrapper = new BufferingResponseWrapper(servletResponse);

        chain.doFilter(request, wrapper);

        HttpStatus httpStatus = HttpStatus.ofCode(servletResponse.getStatus());

        if (servletResponse.isCommitted() || httpStatus.is3xx()) {
            wrapper.flushInternal();
            return;
        }

        byte[]                      byteArray = wrapper.getByteArray();
        IdGenerator<String, String> generator = new Sha256HashGenerator();
        String                      hash      = generator.generate(new String(byteArray, StandardCharsets.UTF_8));
        ETag                        etag      = ETag.strong(hash);

        IfNoneMatch noneMatch = IfNoneMatch.parse(servletRequest.getHeader(
                HttpHeader.IF_NONE_MATCH.toString()
        )).toNoneMatch();

        servletResponse.setHeader(HttpHeader.ETAG.value(), etag.toHeaderValue());

        if (noneMatch.matches(etag, false, false)) {
            servletResponse.setStatus(HttpStatus.NOT_MODIFIED.getCode());
            return;
        }

        wrapper.flushInternal();
    }

}

