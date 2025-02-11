# jMouse Message Context

**jMouse Message Context** is a flexible and efficient message localization system designed for internationalization (i18n) and error message management in Java applications. It supports multiple message sources, caching, and fallback strategies.

## Features
- **Resource Bundle Loading** – Supports `.properties` and custom message sources.
- **Multiple Message Sources** – Register multiple resource bundles (e.g., `"i18n.messages"`, `"errors"`).
- **Locale-Based Message Retrieval** – Fetch localized messages based on `Locale`.
- **Placeholder Formatting** – Supports dynamic message arguments.
- **Customizable Fallbacks** – Configure fallback behavior when a key is missing.

## Quick Start

### 1. Add Message Bundles
```java
StandardResourceBundleLoader messageSource =
    new StandardResourceBundleLoader(App.class.getClassLoader());

messageSource.addNames("i18n.messages", "errors");
```

### 2. Retrieve Messages
```java
String name = messageSource.getMessage("project.name", Locale.of("uk_UA"));

System.out.println(messageSource.getMessage("project.description", Locale.of("uk_UA"), name));
System.out.println(messageSource.getMessage("error.http.500"));
```

### 3. Configure Fallback Strategy
```java
messageSource.setFallbackWithCode(true);
messageSource.setFallbackPattern("[? %s ?]");

System.out.println(messageSource.getMessage("unknown.key")); // Outputs: [? unknown.key ?]
```

## Example Output
```
Project XYZ - Ukrainian Description
[? project.description ?]
Internal Server Error
```

## License
This project is licensed under the MIT License.

