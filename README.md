# D Interpreter

Interpreter on Java with handwritten parser.

## Overview

D is a dynamic language with interpretation-based execution. It features dynamic typing where object types are not specified and can change during program execution.

## Key Features

- **Dynamic typing**: Variables can hold values of any type and change types at runtime
- **Built-in types**: integer, real, boolean, string
- **User-defined types**: array, tuple, function
- **Implicit type conversions**: Automatic conversion between compatible types
- **First-class functions**: Functions are treated as literals that can be assigned to variables and passed as arguments
- **Control structures**: if/while/loop statements with exit and return

## Language Basics

### Variables

Variables are declared with `var` and can optionally be initialized:

```d
var x := 42
var name := "hello"
var y // initialized to none
```

### Types

- **Integer**: `42`, `-10`
- **Real**: `3.14`, `-0.5`
- **String**: `"hello"` or `'world'`
- **Boolean**: `true`, `false`
- **Array**: Dynamic-sized array, e.g., `[1, 2, 3]`
- **Tuple**: Fixed-size collection, e.g., `{a := 1, b := "text"}`
- **Function**: Function literal, e.g., `func(x) => x + 1`

### Control Flow

```d
// If statement
if x > 0 then
    print "positive"
else
    print "non-positive"
end

// Short if
if x > 0 => print "positive"

// While loop
while x < 10 loop
    x := x + 1
end

// For loop with range
for 1..10 loop
    print i
end

// For loop over array
var arr := [1, 2, 3]
for i in arr loop
    print i
end

// Exit loop
loop
    print "hello"
    if condition => exit
end
```

### Functions

```d
// Function literal
var add := func(a, b) => a + b

// Function with body
var factorial := func(n) is
    if n <= 1 => 1
    else => n * factorial(n - 1)
end

// Calling functions
print add(5, 3)
print factorial(5)
```

### Arrays and Tuples

```d
// Arrays
var arr := [1, 2, 3]
arr[4] := 4  // dynamic sizing
print arr[2]

// Tuples
var t := {a := 1, b := "text", 3.14}
print t.a
print t.2  // access by index (1-based)
```

## Type Checking

Use the `is` operator to check types at runtime:

```d
if x is int => print "integer"
if x is string => print "string"
```

## Operators

- **Arithmetic**: `+`, `-`, `*`, `/`
- **Comparison**: `<`, `>`, `<=`, `>=`, `=`, `/=`
- **Logical**: `and`, `or`, `xor`, `not`
- **String concatenation**: `+`
- **Array/tuple concatenation**: `+`

## Output

Use `print` to output values:

```d
print "Hello, World!"
print x, y, z  // multiple values
```

## Project Structure

- `docs/`: Project documentation and specification
- `src/main/java/`: Main source code
- `src/test/java/`: JUnit test sources
- `pom.xml`: Maven build file

## Testing

The project uses **JUnit 5** for unit and integration tests. Tests live under
`src/test/java/` and mirror the package structure of the main sources.

### Test layout

- `pussycats.interpreter.lexer.*` — tests for the lexer (token kinds, literals,
  line/column tracking, error cases such as unterminated strings).
- `pussycats.interpreter.parser.*` — tests for the parser (AST shape, precedence,
  associativity, syntax errors).
- `pussycats.interpreter.interpreter.*` — tests for evaluation semantics
  (arithmetic, control flow, functions, arrays/tuples, dynamic typing).
- `pussycats.interpreter.sources.*` — examples of code in D language.
- `pussycats.interpreter.*` — end-to-end tests that tokenize, parse, and
  evaluate a source snippet and compare the printed output.

### Running the tests

**All tests**

With Maven:

```bash
mvn test
```

**A single test class**

```bash
mvn -Dtest=LexerTest test
```

**A single test method**

```bash
mvn -Dtest=LexerTest#twoCharacterOperators test
```

**From an IDE**

In IntelliJ IDEA or Eclipse, right-click the `src/test/java` folder and choose
*Run 'All Tests'* (or use the green gutter icon next to a test class or method).

## Documentation

See [docs/Project_D_Description.md](docs/Project_D_Description.md) for the complete language specification.
