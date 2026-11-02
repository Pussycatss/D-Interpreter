package pussycats.interpreter.cli;

import java.util.Scanner;
import sun.misc.Signal;
import sun.misc.SignalHandler;

public class REPL {
    private final Scanner scanner = new Scanner(System.in);
    private static final String PROMPT = "Welcome to the D!" + System.lineSeparator();
    private final Evaluable handler;
    private volatile boolean shouldExit = false;
    private volatile boolean exitRequested = false;
    private SignalHandler oldHandler;

    public REPL(String type) {
        handler = createHandler(type);
        setupSignalHandler();
        readEvalPrintLoop();
    }

    private Evaluable createHandler(String type) {
        return switch (type) {
            case "lexer" -> new LexerEval();
            case "parser" -> new ParserEval();
            case "optimizer" -> new OptimizerEval();
            default -> throw new IllegalArgumentException("Unknown type: " + type);
        };
    }

    private void setupSignalHandler() {
        oldHandler = Signal.handle(new Signal("INT"), _ -> {
            if (exitRequested) {
                shouldExit = true;
            } else {
                exitRequested = true;
            }
        });
    }

    public void readEvalPrintLoop() {
        System.out.print(PROMPT);

        while (!shouldExit) {
            System.out.print("> ");

            StringBuilder input = new StringBuilder();
            boolean incomplete = true;

            while (incomplete) {
                String line;
                try {
                    line = scanner.nextLine();
                } catch (Exception e) {
                    return;
                }

                if (exitRequested) {
                    System.out.println("To exit, type 'exit' or press Ctrl+C again");
                    exitRequested = false;
                    continue;
                }

                if (line.equals("exit")) {
                    shouldExit = true;
                    return;
                }

                input.append(line).append("\n");

                incomplete = isInputIncomplete(input.toString());
                if (incomplete) {
                    System.out.print("... ");
                }
            }

            Object out = handler.eval(input.toString());
            if (out != null) {
                System.out.println(out);
            }
        }

        Signal.handle(new Signal("INT"), oldHandler);
    }

    /**
     * Checks if the input is incomplete (needs more lines).
     * Uses bracket balancing, string literal detection, and keyword matching.
     */
    private boolean isInputIncomplete(String input) {
        int parenCount = 0;
        int bracketCount = 0;
        int braceCount = 0;
        boolean inString = false;
        boolean escapeNext = false;
        int keywordDepth = 0;

        // First, check keyword depth by splitting input into words
        String[] words = input.split("\\s+");
        for (String word : words) {
            if (isBlockKeyword(word)) {
                keywordDepth++;
            } else if ("end".equals(word)) {
                keywordDepth--;
            }
        }

        // Then check bracket balancing (skipping strings)
        for (int i = 0; i < input.length(); i++) {
            char c = input.charAt(i);

            if (escapeNext) {
                escapeNext = false;
                continue;
            }

            if (c == '\\' && inString) {
                escapeNext = true;
                continue;
            }

            if (c == '"') {
                inString = !inString;
                continue;
            }

            if (inString) continue;

            switch (c) {
                case '(' -> parenCount++;
                case ')' -> parenCount--;
                case '[' -> bracketCount++;
                case ']' -> bracketCount--;
                case '{' -> braceCount++;
                case '}' -> braceCount--;
            }
        }

        return parenCount != 0 || bracketCount != 0 || braceCount != 0 || inString || keywordDepth != 0;
    }

    private boolean isBlockKeyword(String word) {
        // Strip trailing punctuation like () := etc.
        String clean = word.replaceAll("[():=,]+$", "");
        return switch (clean) {
            case "if", "while", "loop", "for", "func" -> true;
            default -> false;
        };
    }
}
