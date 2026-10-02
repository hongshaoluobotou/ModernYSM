package com.elfmcys.yesstevemodel.config;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class ConfigSpecTest {
    @TempDir
    Path directory;

    @Test
    void malformedConfigIsPreservedAcrossInitializationAndLaterChanges() throws Exception {
        Path file = directory.resolve("client.toml");
        String original = "UserSetting = \"keep me\"\n[unfinished\n";
        Files.writeString(file, original);
        ConfigSpec.Builder builder = ConfigSpec.builder(file);
        ConfigSpec.BooleanValue option = builder.define("Enabled", true);
        ConfigSpec spec = builder.build();
        assertTrue(option.get());
        assertEquals(original, Files.readString(file));
        option.set(false);
        spec.save();
        assertFalse(option.get(), "in-memory settings remain usable");
        assertEquals(original, Files.readString(file), "subsequent saves must preserve the damaged file too");
    }

    @Test
    void validConfigKeepsUnknownValuesAndPersistsUpdates() throws Exception {
        Path file = directory.resolve("server.toml");
        Files.writeString(file, "Custom = \"untouched\"\nEnabled = false\n");
        ConfigSpec.Builder builder = ConfigSpec.builder(file);
        ConfigSpec.BooleanValue option = builder.define("Enabled", true);
        builder.build();
        assertFalse(option.get());
        option.set(true);
        ConfigSpec.Builder reopened = ConfigSpec.builder(file);
        assertTrue(reopened.define("Enabled", false).get());
        assertEquals("untouched", reopened.define("Custom", "missing").get());
        try (var files = Files.list(directory)) {
            assertEquals(1, files.count(), "successful saves leave no temporary files");
        }
    }

    @Test
    void firstRunCreatesParentDirectoriesAndDefaults() throws Exception {
        Path file = directory.resolve("nested/client.toml");
        ConfigSpec.Builder builder = ConfigSpec.builder(file);
        builder.define("Enabled", true);
        builder.build();
        assertTrue(Files.isRegularFile(file));
        assertTrue(ConfigSpec.builder(file).define("Enabled", false).get());
    }
}
