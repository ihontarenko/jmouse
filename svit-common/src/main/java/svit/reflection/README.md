
# Finder Utility Library

This library provides utilities to find various members (constructors, methods, fields) of Java classes, with powerful abstractions to easily work with reflection.

## Overview

The library contains the following key components:

- **AbstractFinder**: An abstract class to implement finders for different kinds of class members (fields, methods, constructors).
- **ConstructorFinder**: A concrete implementation of `AbstractFinder` that finds constructors.
- **FieldFinder**: A concrete implementation of `AbstractFinder` that finds fields in a class.
- **Finder**: An interface for finding class members (methods, fields, constructors).
- **JavaTypes**: A utility class for mapping primitive types and their wrappers.
- **MethodFinder**: A concrete implementation of `AbstractFinder` that finds methods in a class.

## Usage Examples

### 1. Using `ConstructorFinder`

`ConstructorFinder` helps in finding constructors of a class based on certain criteria.

```java


Class<?> clazz=MyClass.class;

// Find the first constructor with a specific parameter type
Constructor<?> constructor=ConstructorFinder.getAllConstructors(clazz,true)
        .stream()
        .filter(MethodMatchers.hasParameterTypes(String.class)::matches)
        .findFirst()
        .orElseThrow(()->new RuntimeException("Constructor not found!"));
```

### 2. Using `FieldFinder`

`FieldFinder` allows finding fields in a class, including inherited fields.

```java


Class<?> clazz=MyClass.class;

// Find all fields annotated with @Deprecated
Field[]deprecatedFields=FieldFinder.getAnnotatedWith(clazz,Deprecated.class);

// Find all fields, including inherited fields
        Set<Field> allFields=FieldFinder.getAllFields(clazz,true);
```

### 3. Using `MethodFinder`

`MethodFinder` helps in finding methods of a class, including inherited methods.

```java


Class<?> clazz=MyClass.class;

// Find all methods, including those from superclasses
Set<Method> methods=MethodFinder.getAllMethods(clazz,true);

// Find a method by name and parameters
        Optional<Method> method=MethodFinder.getAllMethods(clazz).stream()
        .filter(MethodMatchers.nameEquals("myMethod")
        .and(MethodMatchers.hasParameterTypes(String.class))::matches)
        .findFirst();
```

### 4. Using `AbstractFinder`

`AbstractFinder` is the base class for creating custom finders for any kind of `Member` (fields, methods, constructors).

```java
import df.common.reflection.AbstractFinder;

import java.lang.reflect.Member;
import java.util.List;

public class MyMemberFinder extends AbstractFinder<Member> {
    @Override
    protected Collection<Member> getMembers(Class<?> clazz) {
        // Custom logic to retrieve class members
        return List.of();
    }
}
```

### 5. Using `Finder`

The `Finder<T extends Member>` interface allows for searching different types of class members.

```java


Finder<Method> methodFinder=new MethodFinder();
        Optional<Method> method=methodFinder.findFirst(MyClass.class,MethodMatchers.isPublic(),MatchContext.createDefault());
```

### 6. Working with `JavaTypes`

The `JavaTypes` class provides mappings between primitive types and their wrapper types.

```java


// Get the wrapper type for a primitive type
Class<?> wrapper=JavaTypes.WRAPPERS.get(int.class); // Integer.class

// Get the primitive type for a wrapper type
        Class<?> primitive=JavaTypes.PRIMITIVES.get(Integer.class); // int.class
```

## Core Classes

### `AbstractFinder`

The `AbstractFinder<T extends Member>` class provides a base implementation for creating custom finders.

### `ConstructorFinder`

`ConstructorFinder` extends `AbstractFinder<Constructor<?>>` and provides utility methods to find constructors.

### `FieldFinder`

`FieldFinder` extends `AbstractFinder<Field>` and provides utility methods to find fields, including those annotated with specific annotations.

### `MethodFinder`

`MethodFinder` extends `AbstractFinder<Method>` and provides utility methods to find methods, including inherited methods.

### `Finder`

The `Finder<T extends Member>` interface defines a contract for finding class members, with methods for finding all matching members or just the first match.

---

## Example Usage

Below is an example of using `FieldFinder`, `MethodFinder`, and `ConstructorFinder` to find class members.

```java

import df.base.common.matcher.MatchContext;

Class<?> clazz=MyClass.class;

// Find the first constructor with a specific parameter type
Constructor<?> constructor=ConstructorFinder.getAllConstructors(clazz,true)
        .stream()
        .filter(MethodMatchers.hasParameterTypes(String.class)::matches)
        .findFirst()
        .orElseThrow(()->new RuntimeException("Constructor not found!"));

// Find all fields annotated with @Deprecated
        Field[]deprecatedFields=FieldFinder.getAnnotatedWith(clazz,Deprecated.class);

// Find a method by name and parameters
        Optional<Method> method=MethodFinder.getAllMethods(clazz).stream()
        .filter(MethodMatchers.nameEquals("myMethod")
        .and(MethodMatchers.hasParameterTypes(String.class))::matches)
        .findFirst();
```

## Contributing

If you'd like to contribute to this library, feel free to submit pull requests or open issues on the repository. We welcome any contributions or improvements.

## License

This project is licensed under the MIT License.
