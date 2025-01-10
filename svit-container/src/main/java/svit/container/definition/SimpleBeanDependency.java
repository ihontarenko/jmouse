package svit.container.definition;

public record SimpleBeanDependency(Class<?> type, String name) implements BeanDependency {

}
