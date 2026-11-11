package com.praxis.mdp;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;

final class TestDirs {

    private TestDirs() {
    }

    static Path newTempDir(String prefix) {
        try {
            Path dir = Files.createTempDirectory(prefix);
            dir.toFile().deleteOnExit();
            return dir;
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
