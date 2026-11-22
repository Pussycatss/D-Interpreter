package pussycats.interpreter.error;

import pussycats.interpreter.runtime.CallStackFrame;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

public record DiagnosticError(String message, String suggestion, String source, String filename,
                              int line, int column, int length, String errorType, List<CallStackFrame> callStack) {
    public DiagnosticError {
        callStack = callStack == null ? List.of() : List.copyOf(callStack);
    }

    public String format() {
        StringBuilder sb = new StringBuilder("Traceback (most recent call last):\n");
        Path path = Paths.get(filename).toAbsolutePath();
        for (CallStackFrame frame : callStack) {
            int frameLine = frame.span().start().line();
            sb.append("  File \"")
                    .append(path)
                    .append("\", line ")
                    .append(frameLine)
                    .append(", in ")
                    .append(frame.functionName())
                    .append('\n');
            appendSourceLine(
                    sb,
                    frameLine,
                    frame.span().start().column(),
                    frame.span().length()
            );
        }
        sb.append("  File \"")
                .append(path)
                .append("\", line ")
                .append(line)
                .append('\n');
        appendSourceLine(sb, line, column, length);
        sb.append(errorType).append(": ").append(message);
        if (suggestion != null && !suggestion.isBlank()) {
            sb.append('\n').append("  hint: ").append(suggestion);
        }
        return sb.toString();
    }

    private void appendSourceLine(StringBuilder sb, int line, int column, int length) {
        String srcLine = lineOf(line);
        sb.append("    ").append(expandTabs(srcLine)).append('\n');
        int displayColumn = displayColumn(srcLine, column);
        sb.append("    ").repeat(" ", displayColumn);
        sb.repeat("^", length).append('\n');
    }

    private String lineOf(int line) {
        if (line < 1) {
            return "";
        }
        int from = 0;
        int currentLine = 1;
        while (currentLine < line) {
            int nl = source.indexOf('\n', from);
            if (nl < 0) {
                return "";
            }
            from = nl + 1;
            currentLine++;
        }
        int end = source.indexOf('\n', from);
        return end < 0 ? source.substring(from) : source.substring(from, end);
    }

    private static String expandTabs(String s) {
        return s.replace("\t", "    ");
    }

    private static int displayColumn(String srcLine, int column) {
        int col = 0;
        int i = 0;
        int target = Math.max(0, column - 1);
        while (i < target && i < srcLine.length()) {
            col += (srcLine.charAt(i) == '\t') ? 4 : 1;
            i++;
        }
        return col;
    }
}
