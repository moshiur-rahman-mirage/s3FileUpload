package com.ec2iams3.learning.storage;

import java.util.UUID;
import java.util.regex.Pattern;

/**
 * Single source of truth for storage-key rules (previously duplicated in
 * LocalFileStorage and S3FileStorage).
 */
public final class StorageKeys {

    private static final int MAX_NAME_LENGTH = 100;
    private static final int MAX_EXTENSION_LENGTH = 10;
    private static final Pattern UNSAFE_CHARS = Pattern.compile("[^A-Za-z0-9._-]");
    private static final Pattern LEADING_DOTS = Pattern.compile("^\\.+");

    private StorageKeys() {
    }

    /**
    /** Read-side validation. Accepts nested paths, but blocks traversal and malformed segments. */
    public static void validate(String key) {
        if (key == null || key.isBlank() || key.indexOf('\\') >= 0 || key.chars().anyMatch(Character::isISOControl)) {
            throw new IllegalArgumentException("Invalid file key");
        }

        String[] segments = key.split("/", -1);
        for (String segment : segments) {
            if (segment.isEmpty() || segment.equals(".") || segment.equals("..")) {
                throw new IllegalArgumentException("Invalid file key");
            }
        }
    }

    /** Strips any client-supplied directory part (old browsers send full paths). */
    public static String baseName(String rawFilename) {
        String name = rawFilename == null ? "" : rawFilename;
        int separator = Math.max(name.lastIndexOf('/'), name.lastIndexOf('\\'));
        name = name.substring(separator + 1).strip();
        return name.isEmpty() ? "upload" : name;
    }

    /** Write-side: new keys are ASCII-safe, length-bounded and never start with a dot. */
    public static String newKey(String baseName) {
        return newKey(baseName, null);
    }

    public static String newKey(String baseName, String folder) {
        String safe = UNSAFE_CHARS.matcher(baseName).replaceAll("_");
        safe = LEADING_DOTS.matcher(safe).replaceFirst("");
        if (safe.isEmpty()) {
            safe = "upload";
        }
        if (safe.length() > MAX_NAME_LENGTH) {
            int dot = safe.lastIndexOf('.');
            String extension = dot > 0 && safe.length() - dot <= MAX_EXTENSION_LENGTH
                    ? safe.substring(dot) : "";
            safe = safe.substring(0, MAX_NAME_LENGTH - extension.length()) + extension;
        }

        String key = UUID.randomUUID() + "_" + safe;
        if (folder == null || folder.isBlank()) {
            return key;
        }

        String normalizedFolder = folder.replace('\\', '/').replaceAll("^/+|/+$", "");
        return normalizedFolder + "/" + key;
    }
}