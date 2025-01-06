package svit.provider.data;

import java.util.Map;

public class MapDataProvider extends AbstractDataProvider<String, Object> {

    public MapDataProvider(Map<String, Object> keyValues) {
        super(keyValues);
    }

}
