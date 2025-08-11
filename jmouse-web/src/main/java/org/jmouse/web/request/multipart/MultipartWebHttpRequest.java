package org.jmouse.web.request.multipart;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.Part;
import org.jmouse.web.request.WebHttpRequest;
import org.jmouse.web.request.http.HttpHeader;

import java.io.IOException;
import java.util.*;

public class MultipartWebHttpRequest extends AbstractMultipartWebHttpRequest {

    private Set<String>                      parameterNames;

    public MultipartWebHttpRequest(HttpServletRequest request) {
        super(request);
        initializeMultipartRequest(request);
    }

    @Override
    public Map<String, String[]> getParameterMap() {
        return super.getParameterMap();
    }

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

    private void initializeMultipartRequest(HttpServletRequest request) {
        try {
            Map<String, List<MultipartFile>> files = new LinkedHashMap<>();
            Collection<Part>                 parts = request.getParts();

            parameterNames = new LinkedHashSet<>();

            for (Part part : parts) {
                String      header      = part.getHeader(HttpHeader.CONTENT_DISPOSITION.toString());
                Disposition disposition = Disposition.parse(header);

                if (disposition.filename() != null) {
                    files.computeIfAbsent(disposition.name(), k -> new ArrayList<>())
                            .add(new SimpleMultipartFile(disposition, part));
                    continue;
                }

                parameterNames.add(disposition.name());
            }

            setMultipartFiles(files);

        } catch (IOException | ServletException e) {
            throw new MultipartRequestException("FAILED TO INITIALIZE MULTIPART-REQUEST!", e);
        }
    }

}
