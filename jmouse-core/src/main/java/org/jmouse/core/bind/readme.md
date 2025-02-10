# **jMouse - Data Binder 🔬**

### **Lightweight and Flexible Data Binding for Java**

jMouse Data Binder is a powerful tool for mapping structured data (`Map`, `Properties`, `Environment`, etc.) to Java objects, including **Beans, Collections, Maps, Arrays, and Records**.

## **Quick Start ⚡**

### **1️⃣ Bind a Simple Object**
```java
Map<String, Object> source = Map.of(
    "server.port", "8090",
    "server.name", "jMouseServer"
);

Binder binder = Binder.withDataSource(source);
ServerConfig config = binder.bind("server", Bindable.of(ServerConfig.class)).getValue();

System.out.println(config.getPort()); // Integer 8090
```

### **2️⃣ Binding to Objects**
#### You can bind configurations directly into Java objects:
```java
public record Package(String name, String version) {}
// Bind a source to record
Package pkg = Bind.with(binder).to("package", Package.class).getValue();
```

### **3️⃣ Binding Maps & Collections**
```java
// Bind a list  
List<String> services = Bind.with(binder).toList("services", String.class).getValue();

// Bind a map of objects  
Map<String, Object> configs = Bind.with(binder).toMap("default.configs", Object.class).getValue();
```

### **4️⃣ Binding an Existing Object**
#### Bind values directly to an existing object:

```java
AppContext context = new AppContext();
// Bind source values to existing object
Bind.with(binder).to("app.context", context);
// or
Bind.with(binder).to("app.context", this);
```

### ⚡ Advanced Usage 
#### Property Placeholders
The binder supports placeholder resolution:

```java
"app.description": "Application: '${app.name}' running on Java ${app.java.version}"
```
