# Architecture

Proposed architecture:

```
interpreter/
│
├── Main.java
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
│   └── ...
│
└── runtime/
    ├── Interpreter.java
    └── Cell.java
```

### Detailed component description:
- lexer – Lexical analyzer. Includes token types and keywords registry, tokenizer and scanner. Handles lexical errors.
- parser – Syntax analyzer. Includes parser and parser errors.
- ast – Abstract syntax tree. Includes AST. Builds tree from tokens.
- runtime – Runtime environment. Includes interpreter and cell. Cells proposed to resolve closures in capture-by-reference model