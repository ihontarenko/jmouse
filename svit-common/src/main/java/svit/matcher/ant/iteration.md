# AntMatcher Visualization

This document provides a detailed visualization of how the `AntMatcher` works for the given pattern and input string. Each iteration is explained step by step, with the current positions in the pattern and string highlighted.

---

## **Input Data**

### Pattern:
```
/repository/**/app/**/a/*/c/*.java
```

- **Segments:** `["repository", "**", "app", "**", "a", "*", "c", "*.java"]`

### String:
```
/repository/com/app/a/b/c/a/b/c/a/b/c/Main.java
```

- **Segments:** `[
  "repository", "com", "app", "a", "b", "c", "a", "b", "c", "a", "b", "c", "Main.java"
]`

---

## **Matching Process**

### Initial State:
```
Pattern: ["repository", "**", "app", "**", "a", "*", "c", "*.java"]
String:  ["repository", "com", "app", "a", "b", "c", "a", "b", "c", "a", "b", "c", "Main.java"]
Positions: i = 0, j = 0
```

---

### **Iteration 1: "repository"**

**Comparison:**
- Pattern: `"repository"`
- String: `"repository"`

**Check:**
- `"repository".equals("repository")` → `true`

**Result:**
- Move to the next segment.
```
Positions: i = 1, j = 1
```

---

### **Iteration 2: "**"**

**Pattern:** Matches zero or more segments.

**Action:** Skip "com" and keep "**" active.

**Result:**
```
Positions: i = 2, j = 1
```

---

### **Iteration 3: "app"**

**Comparison:**
- Pattern: `"app"`
- String: `"app"`

**Check:**
- `"app".equals("app")` → `true`

**Result:**
- Move to the next segment.
```
Positions: i = 3, j = 2
```

---

### **Iteration 4: "**"**

**Pattern:** Matches zero or more segments.

**Action:** Skip "a" and keep "**" active.

**Result:**
```
Positions: i = 4, j = 3
```

---

### **Iteration 5: "a"**

**Comparison:**
- Pattern: `"a"`
- String: `"a"`

**Check:**
- `"a".equals("a")` → `true`

**Result:**
- Move to the next segment.
```
Positions: i = 5, j = 4
```

---

### **Iteration 6: "*"**

**Pattern:** Matches any single segment.

**Action:** Skip "b".

**Result:**
```
Positions: i = 6, j = 5
```

---

### **Iteration 7: "c"**

**Comparison:**
- Pattern: `"c"`
- String: `"c"`

**Check:**
- `"c".equals("c")` → `true`

**Result:**
- Move to the next segment.
```
Positions: i = 7, j = 6
```

---

### **Iteration 8: "*.java"**

**Pattern:** Matches any segment ending with `.java`.

**Check:**
- `"Main.java".endsWith(".java")` → `true`

**Result:**
```
Positions: i = 13, j = 7
```

---

## **Final Result**

- Both the pattern and the string are fully processed.
- The result is:
```
true
```

---

## **Summary Table**

| Iteration | Pattern       | String         | Check/Action                     | Positions     |
|-----------|---------------|----------------|----------------------------------|---------------|
| 1         | `repository`  | `repository`   | Exact match                      | `i = 1, j = 1`|
| 2         | `**`          | `com`          | Skip                             | `i = 2, j = 1`|
| 3         | `app`         | `app`          | Exact match                      | `i = 3, j = 2`|
| 4         | `**`          | `a`            | Skip                             | `i = 4, j = 3`|
| 5         | `a`           | `a`            | Exact match                      | `i = 5, j = 4`|
| 6         | `*`           | `b`            | Matches single segment           | `i = 6, j = 5`|
| 7         | `c`           | `c`            | Exact match                      | `i = 7, j = 6`|
| 8         | `*.java`      | `Main.java`    | Matches segment ending with `.java`| `i = 13, j = 7`|

---

