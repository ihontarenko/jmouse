package org.jmouse.core.bind;

@FunctionalInterface
public interface ValueNavigator {

    Object navigate(ObjectAccessor accessor, String path);

    static ValueNavigator defaultNavigator() {
        return defaultNavigator(simpleNavigator());
    }

    static ValueNavigator defaultNavigator(ValueNavigator simple) {
        return (accessor, path) -> {
            PropertyPath propertyPath = PropertyPath.forPath(path);

            if (propertyPath.isSimple()) {
                return simple.navigate(accessor, path);
            }

            if (accessor.navigate(propertyPath) instanceof ObjectAccessor objectAccessor) {
                return objectAccessor.unwrap();
            }

            return null;
        };
    }

    static ValueNavigator simpleNavigator() {
        return (accessor, path) -> {
            if (accessor.get(path) instanceof ObjectAccessor objectAccessor) {
                return objectAccessor.unwrap();
            }
            return null;
        };
    }

}
