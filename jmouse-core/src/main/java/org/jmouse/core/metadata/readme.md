# jMouse - MetaDescriptor

**MetaDescriptor** is a Java reflection-based metadata API for analyzing and describing Java classes, methods, constructors, fields, and annotations. It provides structured descriptors and utility methods for working with JavaBeans, records, and executable elements.

## Features
- Class, method, field, and constructor metadata extraction
- JavaBean property detection (getter/setter analysis)
- Record component analysis
- Annotation introspection
- Caching for efficient metadata retrieval

## Usage

### Generating Class Descriptors
```java
ClassDescriptor descriptor = MetaDescriptor.forClass(UserService.class);
System.out.println("Class: " + descriptor.getName());
```

### Analyzing Methods
```java
MethodDescriptor method = MetaDescriptor.forMethod(UserService.class.getDeclaredMethod("getValue"));
System.out.println("Method Return Type: " + method.getReturnType().getName());
```

### Working with JavaBeans
```java
BeanDescriptor<UserService> beanDescriptor = MetaDescriptor.forBean(UserService.class);
for (PropertyDescriptor<UserService> property : beanDescriptor.getProperties()) {
    System.out.println("Property: " + property.getName() + " Type: " + property.getType().getName());
}
```

### Handling Records
```java
public record UserHolder(@Qualifier("client") User user) { }

BeanDescriptor<MyRecord> descriptor = MetaDescriptor.forValueObject(UserHolder.class);
descriptor.getBeanClass().getField("user").getAnnotation(Qualifier.class); // AnnotationDescriptor
```

## License
This project is licensed under the MIT License.

