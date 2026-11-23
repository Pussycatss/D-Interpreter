package pussycats.interpreter.error;

import pussycats.interpreter.runtime.CallStackFrame;

import java.nio.file.Paths;
import java.util.List;

public record DiagnosticError(
        String message,
        String suggestion,
        String source,
        String filename,
        int line,
        int column,
        int length,
        String errorType,
        List<CallStackFrame> callStack,
        String functionName,
        SourceManager sourceManager
) {
    public DiagnosticError {
        callStack = callStack == null ? List.of() : List.copyOf(callStack);
    }

    public String format() {
        StringBuilder sb = new StringBuilder("Traceback (most recent call last):\n");
        if (callStack.isEmpty()) {
            sb.append("  File \"")
                    .append(toTracebackPath(filename))
                    .append("\", line ")
                    .append(line)
                    .append(", in ").append(functionName)
                    .append('\n');
            appendSourceLine(sb, source, line, column, length);
        } else {
            for (CallStackFrame frame : callStack) {
                var start = frame.span().start();
                int frameLine = start.line();
                sb.append("  File \"")
                        .append(toTracebackPath(frame.sourceName()))
                        .append("\", line ")
                        .append(frameLine)
                        .append(", in ")
                        .append(frame.functionName())
                        .append('\n');
                appendSourceLine(
                        sb,
                        sourceOf(frame.sourceName()),
                        frameLine,
                        start.column(),
                        frame.span().length()
                );
            }
        }
        sb.append(errorType).append(": ").append(message);
        if (suggestion != null && !suggestion.isBlank()) {
            sb.append('\n').append("  hint: ").append(suggestion);
        }
        return sb.toString();
    }

    private static String toTracebackPath(String name) {
        if (name == null) return "<unknown>";
        if (name.isEmpty()) return name;
        if (name.startsWith("<") && name.endsWith(">")) return name;
        if (name.startsWith("file:") || name.startsWith("jrt:")
                || name.startsWith("jar:") || name.startsWith("http:")
                || name.startsWith("https:")) {
            return name;
        }
        try {
            return Paths.get(name).toAbsolutePath().normalize().toString();
        } catch (IllegalArgumentException e) {
            return name;
        }
    }

    private void appendSourceLine(StringBuilder sb, String source, int line, int column, int length) {
        String srcLine = lineOf(source, line);
        sb.append("    ").append(expandTabs(srcLine)).append('\n');
        int displayColumn = displayColumn(srcLine, column);
        sb.append("    ").repeat(" ", displayColumn);
        sb.repeat("^", Math.max(1, length)).append('\n');
    }

    private String sourceOf(String sourceName) {
        if (sourceName == null || sourceName.equals(filename)) {
            return source;
        }
        if (sourceManager == null) {
            return "";
        }

        String registeredSource = sourceManager.get(sourceName);
        return registeredSource != null ? registeredSource : "";
    }

    private String lineOf(String source, int line) {
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
        return end < 0
                ? source.substring(from)
                : source.substring(from, end);
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
