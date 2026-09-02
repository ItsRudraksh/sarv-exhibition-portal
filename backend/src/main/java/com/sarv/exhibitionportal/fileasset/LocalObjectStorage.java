package com.sarv.exhibitionportal.fileasset;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HexFormat;
import java.util.UUID;
import org.springframework.stereotype.Component;

@Component
public class LocalObjectStorage {

    public void write(Path root, String storageKey, byte[] bytes) throws IOException {
        Path target = resolve(root, storageKey);
        Files.createDirectories(target.getParent());
        Files.write(target, bytes);
    }

    public byte[] read(Path root, String storageKey) throws IOException {
        return Files.readAllBytes(resolve(root, storageKey));
    }

    public boolean exists(Path root, String storageKey) {
        return Files.isRegularFile(resolve(root, storageKey));
    }

    public Path resolve(Path root, String storageKey) {
        Path base = root.toAbsolutePath().normalize();
        Path target = base.resolve(storageKey).normalize();
        if (!target.startsWith(base)) {
            throw new IllegalArgumentException("Invalid storage key");
        }
        return target;
    }

    public static String keyFor(UUID inquiryId, UUID assetId, String extension) {
        String safeExt = extension == null || extension.isBlank() ? "bin" : extension.replaceAll("[^a-zA-Z0-9]", "");
        if (safeExt.isBlank()) {
            safeExt = "bin";
        }
        return inquiryId + "/" + assetId + "." + safeExt.toLowerCase();
    }

    public static String sha256(byte[] bytes) {
        try {
            var digest = java.security.MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(bytes));
        } catch (java.security.NoSuchAlgorithmException ex) {
            throw new IllegalStateException(ex);
        }
    }

    public static String extensionOf(String filename, String mediaType) {
        if (filename != null) {
            int dot = filename.lastIndexOf('.');
            if (dot >= 0 && dot < filename.length() - 1) {
                return filename.substring(dot + 1);
            }
        }
        return switch (mediaType) {
            case "image/jpeg" -> "jpg";
            case "image/png" -> "png";
            case "image/webp" -> "webp";
            case "application/pdf" -> "pdf";
            default -> "bin";
        };
    }
}
