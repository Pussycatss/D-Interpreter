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
            
            String line;
            try {
                line = scanner.nextLine();
            } catch (Exception e) {
                break;
            }

            if (exitRequested) {
                System.out.println("To exit, type 'exit' or press Ctrl+C again");
                exitRequested = false;
                continue;
            }

            if (line.equals("exit")) {
                shouldExit = true;
                break;
            }

            Object out = handler.eval(line);
            if (out != null) {
                System.out.println(out);
            }
        }
        
        Signal.handle(new Signal("INT"), oldHandler);
    }
}
