package pussycats.interpreter;

import pussycats.interpreter.cli.CommandLineArgs;
import picocli.CommandLine;

/**
 * Entry point of the D interpreter.
 */
public final class Main {

    public static void main(String[] args) {
        int exitCode = new CommandLine(new CommandLineArgs()).execute(args);
        System.exit(exitCode);
    }

    private Main() {
    }
}