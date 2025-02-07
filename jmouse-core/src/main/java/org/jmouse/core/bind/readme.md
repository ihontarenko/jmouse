# **jMouse - Data Binder 🔬**

### **Lightweight and Flexible Data Binding for Java**

jMouse Data Binder is a powerful tool for mapping structured data (`Map`, `Properties`, `Environment`, `JSON`, `YAML`) to Java objects, including **Beans, Collections, Maps, Arrays, and Records**.

## **Quick Start ⚡**

### **🔹 Bind a Simple Object**
```java
Map<String, Object> source = Map.of(
    "server.port", 8080,
    "server.name", "jMouseServer"
);

Binder binder = Binder.of(source);
ServerConfig config = binder.bind("server", Bindable.of(ServerConfig.class)).getValue();

System.out.println(config.getPort()); // 8080
```

### **🔹 Bind a List of Objects**
```java
List<User> users = Binder.of(source)
    .bind("users", Bindable.ofList(User.class))
    .getValue();

System.out.println(users.get(0).getName()); // John
```

### **🔹 Bind a Record**
```java
record AppConfig(String name, int port) {}

AppConfig config = Binder.of(source)
    .bind("app", Bindable.of(AppConfig.class))
    .getValue();

System.out.println(config.name()); // jMouse
```

### **🔹 Use Callbacks for Custom Logic 🎯**
```java
Binder binder = Binder.of(source)
    .withCallback((name, value) -> System.out.println("Bound: " + name + " -> " + value));
```

## **Features ✅**
✔ **Simple & Intuitive API**  
✔ **Supports Complex Structures**  
✔ **Deep Binding & Type Conversion**  
✔ **Callback System for Custom Logic**

jMouse Data Binder is designed for **jMouse Framework**, but it works **standalone** as well.

🔗 **License:** Apache 2.0

