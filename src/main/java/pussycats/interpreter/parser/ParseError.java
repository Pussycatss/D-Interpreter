package pussycats.interpreter.parser;

public record ParseError(String message, String suggestion, String source, String filename, int line, int column) {
    public String format() {
        StringBuilder sb = new StringBuilder();
        sb.append(" File \"").append(filename).append("\", line ").append(line).append('\n');

        String srcLine = lineOf(line);
        sb.append("    ").append(expandTabs(srcLine)).append('\n');
        sb.append("    ").repeat(" ", displayColumn(srcLine, column)).append("^\n");

        sb.append("SyntaxError: ").append(message);

        if (suggestion != null && !suggestion.isBlank()) {
            sb.append('\n').append("  hint: ").append(suggestion);
        }

        return sb.toString();
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
