package pussycats.interpreter.runtime;

import pussycats.interpreter.ast.Statement;

import java.nio.file.Path;

@FunctionalInterface
public interface ModuleLoader {
    Statement.Block load(Path path);
}
