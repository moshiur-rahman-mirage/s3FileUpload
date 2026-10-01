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
     * Read-side validation. Deliberately permissive about characters so keys
     * created by older versions (spaces, unicode) still resolve; it only blocks
     * anything that could escape the storage root or break an object key.
     */
    public static void validate(String key) {
        if (key == null || key.isBlank()
                || key.indexOf('/') >= 0 || key.indexOf('\\') >= 0
                || key.equals(".") || key.equals("..")
                || key.chars().anyMatch(Character::isISOControl)) {
            throw new IllegalArgumentException("Invalid file key");
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
        return UUID.randomUUID() + "_" + safe;
    }
}