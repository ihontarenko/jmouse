package org.jmouse.crawler.html;

import com.jayway.jsonpath.JsonPath;
import org.jmouse.core.Verify;

import java.nio.charset.StandardCharsets;

public final class JaywayJsonPathSelector implements JsonPathSelector {

    @Override
    public <T> T read(byte[] body, String path) {
        Verify.nonNull(body, "body");
        Verify.nonNull(path, "path");
        String json = new String(body, StandardCharsets.UTF_8);
        return JsonPath.read(json, path);
    }

}