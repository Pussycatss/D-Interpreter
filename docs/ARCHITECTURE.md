# Architecture

```
interpreter/
│
├── Main.java
├── SourceLocation.java
├── SourceSpan.java
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
│   └── Parser.java
│
├── ast/
│   ├── AstPrinter.java
│   ├── Expression.java
│   ├── Node.java
│   ├── Statement.java
│   └── TypeIndicator.java
│
├── semantic/
│   ├── Analyzer.java
│   └── Optimizer.java
│
├── error/
│   ├── DiagnosticError.java
│   ├── ErrorReporter.java
│   └── SourceManager.java
│
├── runtime/
│   ├── Interpreter.java
│   ├── StackFrame.java
│   ├── Cell.java
│   ├── ImmutableCell.java
│   ├── MutableCell.java
│   ├── FunctionCell.java
│   ├── ArrayCell.java
│   ├── TupleCell.java
│   ├── Signal.java
│   ├── CallStackFrame.java
│   ├── InterpretationException.java
│   ├── ModuleLoader.java
│   └── ModuleLoadException.java
│
└── cli/
    ├── CommandLineArgs.java
    ├── Evaluable.java
    ├── LexerEval.java
    ├── ParserEval.java
    ├── OptimizerEval.java
    ├── InterpreterEval.java
    ├── CliModuleLoader.java
    └── REPL.java
```

### Detailed component description:
- **lexer** – Lexical analyzer. Includes token types and keywords registry, tokenizer and scanner. Handles lexical errors.
- **parser** – Syntax analyzer. Builds the AST from tokens, recovers from errors via synchronization.
- **semantic** – `Analyzer` performs semantic checks (declarations, scopes, control-flow context); `Optimizer` rewrites the AST before execution.
- **error** – Collects `DiagnosticError`s across the pipeline (up to 200), renders source snippets via `SourceManager`.
- **ast** – Abstract syntax tree nodes (`Expression`, `Statement`, `Node`) plus `AstPrinter` and `TypeIndicator`.
- **runtime** – Execution engine. `Interpreter` evaluates the AST over `StackFrame`s; values are wrapped in `Cell`s (`Immutable`, `Mutable`, `Function`, `Array`, `Tuple`) implementing capture-by-reference closures. `Signal` carries non-error control flow (`return`, `exit`); `ModuleLoader`/`CliModuleLoader` resolve `import ... from` modules through the full lexer→parser→analyzer→optimizer pipeline.
- **cli** – Entry point (`Main` → `CommandLineArgs`, picocli). Runs files/`-c` code through the whole pipeline, or drops into `REPL` with per-stage handlers (`LexerEval`, `ParserEval`, `OptimizerEval`, `InterpreterEval`). The interpreter REPL keeps one `Interpreter` and reuses `interpretTopLevel` so declarations persist across inputs; `Analyzer.declareGlobal` seeds the global scope with previously declared names.
