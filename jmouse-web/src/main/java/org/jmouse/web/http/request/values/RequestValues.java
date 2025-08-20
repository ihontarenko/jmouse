package org.jmouse.web.http.request.values;

import jakarta.servlet.http.HttpServletRequest;

import java.util.*;

import static org.jmouse.web.http.HttpHeader.*;

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
                        X_FORWARDED_FOR, X_FORWARDED_PROTO
                ),
                new QueryStringValueProvider(),
                new RequestPathValueProvider()
        );
    }

}
