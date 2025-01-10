package svit.invocable;

public interface ClassTypeDescriptor extends TypeDescriptor {

    MethodDescriptor getMethod(String name, Class<?>... parameterTypes);

    FieldDescriptor getField(String name);

    Class<?> getNativeClass();

}
