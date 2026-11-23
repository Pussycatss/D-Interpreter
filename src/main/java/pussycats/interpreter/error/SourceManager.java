package pussycats.interpreter.error;

import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

public final class SourceManager {
    private final Map<String, String> sources = new HashMap<>();

    public void register(String filename, String source) {
        sources.put(normalize(filename), source);
    }

    public String get(String filename) {
        return sources.get(normalize(filename));
    }

    private String normalize(String filename) {
        if (filename == null || filename.isEmpty()
                || (filename.startsWith("<") && filename.endsWith(">"))
                || filename.contains(":")) {
            return filename;
        }

        return Path.of(filename).toAbsolutePath().normalize().toString();
    }
}