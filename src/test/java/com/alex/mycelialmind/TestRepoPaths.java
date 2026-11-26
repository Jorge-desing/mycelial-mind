package com.alex.mycelialmind;

import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Shared repository path locator for pure-Java JUnit suites.
 */
final class TestRepoPaths {
    private TestRepoPaths() {
    }

    static Path resolveRepoRoot() {
        Path cursor = Path.of("").toAbsolutePath().normalize();
        while (cursor != null) {
            if (Files.isRegularFile(cursor.resolve("build.gradle"))) {
                return cursor;
            }
            cursor = cursor.getParent();
        }
        throw new IllegalStateException("Unable to locate repository root containing build.gradle");
    }
}
