package org.jmouse.web.http.request.multipart;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.Part;
import org.jmouse.core.matcher.Matcher;
import org.jmouse.core.matcher.TextMatchers;
import org.jmouse.web.http.HttpHeader;
import org.jmouse.web.http.request.ContentDisposition;

import java.util.*;

/**
 * 📦 Specialized HTTP request wrapper for handling <b>multipart/form-data</b> requests.
 *
 * <p>Parses form fields and uploaded files from {@link HttpServletRequest#getParts()} and
 * provides access to them alongside regular request parameters.</p>
 *
 * @author Ivan
 */
public class MultipartWebHttpRequest extends AbstractMultipartWebHttpRequest {

    /** Set of additional parameter names extracted from multipart parts. */
    private Set<String> parameterNames;

    private static final Matcher<String> EXCEPTION_MATCHER = TextMatchers.containsAny(
            "exceed", "large", "length", "size", "maximum");

    /**
     * 🆕 Create a new multipart request wrapper.
     *
     * @param request the original {@link HttpServletRequest}
     * @throws MultipartRequestException if parsing fails
     */
    public MultipartWebHttpRequest(HttpServletRequest request) {
        super(request);
        initializeMultipartRequest(request);
    }

    /**
     * 📜 Returns the parameter map, including any from multipart form fields.
     */
    @Override
    public Map<String, String[]> getParameterMap() {
        return super.getParameterMap();
    }

    /**
     * 🏷 Returns all parameter names, merging standard request parameters
     * with additional multipart form field names.
     */
    @Override
    public Enumeration<String> getParameterNames() {
        if (parameterNames == null || parameterNames.isEmpty()) {
            return super.getParameterNames();
        }

        Set<String>         names       = new HashSet<>();
        Enumeration<String> enumeration = super.getParameterNames();

        while (enumeration.hasMoreElements()) {
            names.add(enumeration.nextElement());
        }

        names.addAll(parameterNames);

        return Collections.enumeration(names);
    }

    /**
     * ⚙️ Initializes the multipart request by parsing {@link Part} objects.
     *
     * <p>File parts are stored in the multipart file map, while simple form fields
     * are tracked in {@link #parameterNames}.</p>
     *
     * @param request the servlet request to parse
     * @throws MultipartRequestException if an I/O or servlet error occurs
     */
    private void initializeMultipartRequest(HttpServletRequest request) {
        try {
            Map<String, List<MultipartFile>> files = new LinkedHashMap<>();
            Collection<Part>                 parts = request.getParts();

            parameterNames = new LinkedHashSet<>();

            for (Part part : parts) {
                String             header      = part.getHeader(HttpHeader.CONTENT_DISPOSITION.toString());
                ContentDisposition disposition = ContentDisposition.parse(header);

                if (disposition.filename() != null) {
                    files.computeIfAbsent(disposition.name(), k -> new ArrayList<>())
                            .add(new UploadedFile(disposition, part));
                    continue;
                }

                parameterNames.add(disposition.name());
            }

            setMultipartFiles(files);

        } catch (Throwable e) {
            if (EXCEPTION_MATCHER.matches(e.getMessage())) {
                throw new UploadLimitExceededException("MAXIMUM UPLOAD LIMIT EXCEEDED", e.getCause());
            }

            throw new MultipartRequestException("FAILED TO INITIALIZE MULTIPART-REQUEST!", e);
        }
    }

}
