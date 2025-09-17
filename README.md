# COMP3000: Programming Languages

This repository is for the COMP3000 unit, "Programming Languages". We are following the book [Crafting Interpreters](https://craftinginterpreters.com/).

## Instructions to Compile

1. Compile the source code:
  ```bash
  javac ./craftinginterpreters/*/*.java -d bin
  ```
2. Run the interpreter:
  ```bash
  java -cp bin com.craftinginterpreters.lox.Lox
  ```

### Using `Makefile` (recommended)

1. **Compile all source files**:

```bash
make
```

2. **Run the interpreter** (without a file):

```bash
make runLox
```

3. **Run the interpreter with a `.lox` source file**:

```bash
make runLox FILE=./test.lox
```

3. **Clean compiled files**:

```bash
make clean
```

For more details, refer to the [official Crafting Interpreters GitHub repository](https://github.com/munificent/craftinginterpreters).
