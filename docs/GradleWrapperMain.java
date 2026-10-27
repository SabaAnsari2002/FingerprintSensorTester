package org.gradle.wrapper;

import java.io.*;
import java.net.*;
import java.nio.file.*;
import java.security.*;
import java.util.*;
import java.util.zip.*;

/**
 * Self-contained Gradle bootstrap used because the official wrapper binary is not available
 * in the generation environment. Downloads only the fixed HTTPS Gradle distribution,
 * checks SHA-256, safely extracts, and delegates to Gradle's own launcher.
 * Not affiliated with Gradle Inc. Replace with the official wrapper via `gradle wrapper`.
 */
public final class GradleWrapperMain {
    private static final String VERSION = "9.6.1";
    private static final String SHA256 = "9c0f7faeeb306cb14e4279a3e084ca6b596894089a0638e68a07c945a32c9e14";
    private static final String URL = "https://services.gradle.org/distributions/gradle-9.6.1-bin.zip";

    public static void main(String[] args) throws Exception {
        String home = System.getenv("GRADLE_USER_HOME");
        Path dir = Paths.get(home == null || home.isBlank()
                ? System.getProperty("user.home") + "/.gradle" : home)
                .resolve("wrapper/dists/fingerprint-test-gradle-" + VERSION);
        Path install = dir.resolve("gradle-" + VERSION);
        Path ready = install.resolve(".fingerprint-bootstrap-ready");
        if (!Files.exists(ready)) {
            Files.createDirectories(dir);
            Path zip = dir.resolve("gradle-" + VERSION + "-bin.zip");
            if (!Files.exists(zip) || !verify(zip)) {
                System.err.println("Downloading verified Gradle " + VERSION + " distribution (first run only)...");
                Path tmp = Files.createTempFile(dir, "gradle-download", ".zip");
                try {
                    URLConnection connection = URI.create(URL).toURL().openConnection();
                    connection.setConnectTimeout(20_000);
                    connection.setReadTimeout(60_000);
                    try (InputStream in = connection.getInputStream()) {
                        Files.copy(in, tmp, StandardCopyOption.REPLACE_EXISTING);
                    }
                    if (!verify(tmp)) throw new SecurityException("Gradle distribution SHA-256 mismatch");
                    Files.move(tmp, zip, StandardCopyOption.REPLACE_EXISTING);
                } finally {
                    Files.deleteIfExists(tmp);
                }
            }
            System.err.println("Extracting Gradle " + VERSION + "...");
            try (ZipInputStream zis = new ZipInputStream(Files.newInputStream(zip))) {
                for (ZipEntry entry; (entry = zis.getNextEntry()) != null;) {
                    Path target = dir.resolve(entry.getName()).normalize();
                    if (!target.startsWith(dir)) throw new SecurityException("Unsafe ZIP entry: " + entry.getName());
                    if (entry.isDirectory()) Files.createDirectories(target);
                    else {
                        Files.createDirectories(target.getParent());
                        Files.copy(zis, target, StandardCopyOption.REPLACE_EXISTING);
                    }
                    zis.closeEntry();
                }
            }
            Files.createFile(ready);
        }
        boolean windows = System.getProperty("os.name").toLowerCase(Locale.ROOT).contains("win");
        String launcher = install.resolve("bin").resolve(windows ? "gradle.bat" : "gradle").toString();
        if (!windows) install.resolve("bin/gradle").toFile().setExecutable(true);
        List<String> command = new ArrayList<>();
        if (windows) { command.add("cmd"); command.add("/c"); }
        command.add(launcher);
        command.addAll(Arrays.asList(args));
        Process process = new ProcessBuilder(command).inheritIO().start();
        System.exit(process.waitFor());
    }

    private static boolean verify(Path path) throws Exception {
        if (!Files.exists(path)) return false;
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        try (InputStream in = Files.newInputStream(path)) {
            byte[] buffer = new byte[32768];
            for (int n; (n = in.read(buffer)) != -1;) digest.update(buffer, 0, n);
        }
        StringBuilder hex = new StringBuilder();
        for (byte b : digest.digest()) hex.append(String.format("%02x", b & 0xff));
        return SHA256.equals(hex.toString());
    }
}
