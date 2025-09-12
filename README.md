# COMP3000: Programming Languages - River System DSL

This repository is for the COMP3000 unit, "Programming Languages". We are following the book [Crafting Interpreters](https://craftinginterpreters.com/).

This DSL models **river flow distributions** using simple expressions.

## Grammar (Nystrom’s notation)

Expression for river flows

```java
statement → "print" expression ";"

expression → combineFlow ";"

combineFlow → scaledFlow ( "<<" scaledFlow )* ";"

scaledFlow → baseFlow ( "#" NUMBER )? ";"

baseFlow → "(" NUMBER "^" NUMBER ")" ";"
```

### Explanation

* `NUMBER` → a numeric literal (e.g., `2`, `3.5`)
* `^` → computes **base flow distribution**, takes: `NUMBER ^ NUMBER`, returns `double[]`
* `#` → scales a distribution by a **fixed rain unit**, takes: `double[] # NUMBER`, returns `double[]`
* `<<` → sums **daily flows**, takes: `double[] << double[]`, returns `double[]`

**Precedence (highest → lowest):**

1. `^`
2. `#`
3. `<<`

---

## Example Expressions

### Base distribution

```java
print 2 ^ 3;
Output: [0.1, 0.2, 0.3, 0.4, 0.5, ...]
```

### Scaled by rain unit

```java
print (2 ^ 3) # 2.0;
Output: [2.28, 0.0, 0.8, 0.0, ...]
```

### Sum of two scaled distributions

```java
print ((2 ^ 3) # 2.0) << ((1 ^ 2) # 1.0);
Output: [3.28, 0.0, 1.0, 0.0, ...]
```

---

## Parameter Types Reference

| Operator | Left Type  | Right Type | Returns    |
| -------- | ---------- | ---------- | ---------- |
| `^`      | NUMBER     | NUMBER     | `double[]` |
| `#`      | `double[]` | NUMBER     | `double[]` |
| `<<`     | `double[]` | `double[]` | `double[]` |

## Instructions to Compile

1. Compile the source code:
  ```
  javac ./craftinginterpreters/*/*.java -d bin
  ```
2. Run the interpreter:
  ```
  java -cp bin com.craftinginterpreters.lox.Lox
  ```
