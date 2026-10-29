# Architecture

Proposed architecture:

```
interpreter/
│
├── Main.java
├── StackFrame.java
├── InterpreterException.java
│
├── lexer/
│   ├── Lexer.java
│   ├── Token.java
│   ├── TokenType.java
│   ├── Keywords.java
│   └── LexerException.java
│
├── parser/
│   ├── Parser.java
│   └── ParserException.java
│
├── ast/
│   ├── Node.java
│   ├── Program.java
│   ├── Statement.java
│   ├── Expression.java
│   └── Program.java
│
├── semantic/
│   ├── Optimizer.java
│   └── Analyzer.java
│
├── error/
│   ├── DiagnosticError.java
│   └── ErrorReporter.java
│
└── runtime/
    ├── Interpreter.java
    └── Cell.java
```

### Detailed component description:
- lexer – Lexical analyzer. Includes token types and keywords registry, tokenizer and scanner. Handles lexical errors.
- parser – Syntax analyzer. Includes parser and parser errors.
- semantic – Semantic analyzer. Performs analysis and optimization after parsing and before execution.
- error – Handler of errors from analyzers. Stores up to 200 errors.
- ast – Abstract syntax tree. Includes AST. Builds tree from tokens.
- runtime – Runtime environment. Includes interpreter and cell. Cells proposed to resolve closures in capture-by-reference model.
