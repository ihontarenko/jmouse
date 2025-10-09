package org.jmouse.web.http.values;

import jakarta.servlet.http.HttpServletRequest;
import org.jmouse.web.http.HttpHeader;

import java.util.*;

public class RequestValues {

    private final Set<RequestValueProvider> providers = new HashSet<>();

    public RequestValues(RequestValueProvider... providers) {
        this.providers.addAll(Set.of(providers));
    }

    public Set<RequestValueProvider> getProviders() {
        return providers;
    }

    public Map<String, Set<String>> getStringMap(HttpServletRequest request) {
        Map<String, Set<String>> map = new HashMap<>();

        for (RequestValueProvider provider : getProviders()) {
            map.put(provider.getTargetName(), provider.getRequestValues(request));
        }

        return Collections.unmodifiableMap(map);
    }

    public static RequestValues defaults() {
        return new RequestValues(
                new HeaderValueProvider(
                        HttpHeader.values()
                ),
                new QueryStringValueProvider(),
                new RequestPathValueProvider()
        );
    }

}
