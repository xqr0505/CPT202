package edu.xjtlu.cpt202.backend.testsupport;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Test-only helper: loads key/value pairs from repo-root .env (../.env when running from backend/).
 * This avoids manually exporting env vars when running integration tests locally.
 */
public final class DotenvTestSupport {

    private DotenvTestSupport() {
    }

    public static Map<String, String> loadRepoRootDotenv() {
        Path dotenvPath = Path.of("..", ".env");
        return load(dotenvPath);
    }

    public static Map<String, String> load(Path path) {
        if (path == null || !Files.exists(path) || !Files.isRegularFile(path)) {
            return Map.of();
        }
        Map<String, String> values = new LinkedHashMap<>();
        try (BufferedReader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
            String line;
            while ((line = reader.readLine()) != null) {
                String trimmed = line.trim();
                if (trimmed.isEmpty() || trimmed.startsWith("#")) {
                    continue;
                }
                int idx = trimmed.indexOf('=');
                if (idx <= 0) {
                    continue;
                }
                String key = trimmed.substring(0, idx).trim();
                String rawValue = trimmed.substring(idx + 1).trim();
                String value = unquote(rawValue);
                if (!key.isEmpty()) {
                    values.putIfAbsent(key, value);
                }
            }
        } catch (IOException ignored) {
            return Map.of();
        }
        return Map.copyOf(values);
    }

    private static String unquote(String value) {
        if (value == null) {
            return "";
        }
        String trimmed = value.trim();
        if (trimmed.length() >= 2) {
            char first = trimmed.charAt(0);
            char last = trimmed.charAt(trimmed.length() - 1);
            if ((first == '"' && last == '"') || (first == '\'' && last == '\'')) {
                return trimmed.substring(1, trimmed.length() - 1);
            }
        }
        return trimmed;
    }
}

