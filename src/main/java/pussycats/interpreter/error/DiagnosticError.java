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
        SourceManager sourceManager
) {
    public DiagnosticError {
        callStack = callStack == null ? List.of() : List.copyOf(callStack);
    }

    public String format() {
        boolean isRuntime = "RuntimeError".equals(errorType) || !callStack.isEmpty();
        StringBuilder sb = new StringBuilder();
        sb.append("Traceback (most recent call last):\n");
        if (isRuntime) {
            if (callStack.isEmpty()) {
                appendFrame(sb, filename, line, source, column, length, "<module>");
            } else {
                for (CallStackFrame frame : callStack) {
                    var start = frame.span().start();
                    appendFrame(
                            sb,
                            frame.sourceName(),
                            start.line(),
                            sourceOf(frame.sourceName()),
                            start.column(),
                            frame.span().length(),
                            frame.functionName()
                    );
                }
            }
        } else {
            sb.append("  File \"").append(toTracebackPath(filename))
                    .append("\", line ").append(line)
                    .append(", column ").append(column).append('\n');
            appendSourceLine(sb, source, line, column, length);
        }

        sb.append(errorType).append(": ").append(message);
        if (suggestion != null && !suggestion.isBlank()) {
            sb.append('\n').append("  hint: ").append(suggestion);
        }
        return sb.toString();
    }

    private void appendFrame(StringBuilder sb, String file, int lineNo,
                             String src, int col, int len, String funcName) {
        sb.append("  File \"").append(toTracebackPath(file))
                .append("\", line ").append(lineNo)
                .append(", in ").append(funcName).append('\n');
        appendSourceLine(sb, src, lineNo, col, len);
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
